[CmdletBinding()]
param(
    [ValidateSet('api', 'app', 'status', 'parar', 'ajuda')]
    [string]$Acao = 'ajuda',
    [string]$Serial
)

# Windows PowerShell 5.1. Nenhuma senha do ambiente normal e lida ou alterada.
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$script:Repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$sha = [Security.Cryptography.SHA256]::Create()
try {
    $idBytes = $sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($script:Repo.ToLowerInvariant()))
    $script:Instance = ([BitConverter]::ToString($idBytes).Replace('-', '').ToLowerInvariant()).Substring(0, 12)
} finally { $sha.Dispose() }
$script:Work = Join-Path $env:LOCALAPPDATA "LinkHealth\Testes\$script:Instance"
$script:StatePath = Join-Path $script:Work 'ambiente.json'
$script:AppDir = Join-Path $script:Repo 'App'
$script:BackendDir = Join-Path $script:Repo 'backend'
$script:Wrapper = Join-Path $script:AppDir 'gradlew.bat'
$script:DemoPassword = 'TesteApp123!'

function Write-Utf8([string]$Path, [string]$Content) {
    [IO.File]::WriteAllText($Path, $Content, (New-Object Text.UTF8Encoding($false)))
}

function New-Secret([switch]$Base64) {
    $bytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    if ($Base64) { return [Convert]::ToBase64String($bytes) }
    return [BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
}

function Use-Environment([hashtable]$Values, [scriptblock]$Body) {
    $saved = @{}
    foreach ($name in $Values.Keys) {
        $saved[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
        [Environment]::SetEnvironmentVariable($name, $Values[$name], 'Process')
    }
    try { & $Body } finally {
        foreach ($name in $saved.Keys) {
            [Environment]::SetEnvironmentVariable($name, $saved[$name], 'Process')
        }
    }
}

function Invoke-Tool([string]$Exe, [string[]]$Arguments, [string]$Log) {
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $lines = @(& $Exe @Arguments 2>&1 | ForEach-Object {
            $line = $_.ToString()
            if ($Log) { [IO.File]::AppendAllText($Log, $line + [Environment]::NewLine) }
            if ($line -match '^(> Task |BUILD |Downloading )') { Write-Host $line }
            $line
        })
        $code = $LASTEXITCODE
    } finally { $ErrorActionPreference = $previousPreference }
    $result = ($lines | ForEach-Object { $_.ToString() }) -join [Environment]::NewLine
    if ($code -ne 0) {
        $tail = ($lines | Select-Object -Last 15 | ForEach-Object { $_.ToString() }) -join [Environment]::NewLine
        throw "Falhou: $([IO.Path]::GetFileName($Exe)) (codigo $code).`n$tail"
    }
    return $result
}

function Quote-NativeArgument([string]$Value) {
    if ($Value -and $Value -notmatch '[\s"]') { return $Value }
    $escaped = [regex]::Replace($Value, '(\\*)"', '$1$1\"')
    $escaped = [regex]::Replace($escaped, '(\\+)$', '$1$1')
    return '"' + $escaped + '"'
}

function Invoke-PgControl($State, [string[]]$Arguments) {
    # O servidor herda handles do pg_ctl no Windows. Nao capture sua saida por
    # um pipe PowerShell: o pipe so fecharia quando o proprio banco encerrasse.
    $stdout = Join-Path $script:Work 'pg-control.log'
    $stderr = Join-Path $script:Work 'pg-control-error.log'
    $commandArguments = ($Arguments | ForEach-Object { Quote-NativeArgument $_ }) -join ' '
    $info = New-Object Diagnostics.ProcessStartInfo
    $info.FileName = Join-Path $State.pgBin 'pg_ctl.exe'
    $info.Arguments = $commandArguments
    $info.WorkingDirectory = $script:Work
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $process = New-Object Diagnostics.Process
    $process.StartInfo = $info
    try {
        $null = $process.Start()
        $outputTask = $process.StandardOutput.ReadToEndAsync()
        $errorTask = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit(60000)) { throw "pg_ctl nao respondeu. Confira postgres.log em $script:Work." }
        # Algumas versoes conservam o pipe no processo servidor. Nao aguarde
        # EOF indefinidamente; o ExitCode aqui vem do handle real do pg_ctl.
        $output = "pg_ctl exit=$($process.ExitCode)"
        if ($outputTask.Wait(200)) { $output += "`n" + $outputTask.Result }
        Write-Utf8 $stdout $output
        if ($errorTask.Wait(200)) { Write-Utf8 $stderr $errorTask.Result }
        if ($process.ExitCode -ne 0) { throw "pg_ctl falhou. Confira $stdout, $stderr e postgres.log." }
    } finally {
        $process.StandardOutput.Dispose()
        $process.StandardError.Dispose()
        $process.Dispose()
    }
}

function Find-Java {
    $candidates = @(
        (Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'),
        $env:JAVA_HOME
    )
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($javaCommand) { $candidates += Split-Path (Split-Path $javaCommand.Source -Parent) -Parent }
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath (Join-Path $candidate 'bin\java.exe'))) {
            $version = Invoke-Tool (Join-Path $candidate 'bin\java.exe') @('-version')
            if ($version -match 'version "(\d+)' -and [int]$Matches[1] -ge 17) { return $candidate }
        }
    }
    throw 'Java 17+ nao localizado. Instale o Android Studio, que inclui o JBR, ou defina JAVA_HOME.'
}

function Find-Postgres {
    $candidates = @($env:LINK_HEALTH_PG_BIN)
    $pgCommand = Get-Command pg_ctl.exe -ErrorAction SilentlyContinue
    if ($pgCommand) { $candidates += Split-Path $pgCommand.Source -Parent }
    $pgRoot = Join-Path $env:ProgramFiles 'PostgreSQL'
    if (Test-Path -LiteralPath $pgRoot) {
        $candidates += @(Get-ChildItem -LiteralPath $pgRoot -Directory |
            Where-Object { $_.Name -match '^\d+$' -and [int]$_.Name -ge 14 } |
            Sort-Object { [int]$_.Name } -Descending |
            ForEach-Object { Join-Path $_.FullName 'bin' })
    }
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath (Join-Path $candidate 'initdb.exe')) -and
            (Test-Path -LiteralPath (Join-Path $candidate 'pg_ctl.exe'))) { return $candidate }
    }
    throw 'PostgreSQL nao localizado. Instale o PostgreSQL uma vez (sem configurar banco). Para instalacao personalizada, defina LINK_HEALTH_PG_BIN com a pasta bin.'
}

function Get-FreePort([int]$Start) {
    foreach ($port in $Start..($Start + 49)) {
        $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, $port)
        try { $listener.Start(); return $port } catch [Net.Sockets.SocketException] {
            continue
        } finally { $listener.Stop() }
    }
    throw "Nao existe porta livre proxima de $Start."
}

function Save-State($State) {
    $temporary = "$script:StatePath.tmp"
    Write-Utf8 $temporary ($State | ConvertTo-Json -Depth 8)
    Move-Item -LiteralPath $temporary -Destination $script:StatePath -Force
}

function Read-State {
    if (-not (Test-Path -LiteralPath $script:StatePath)) {
        throw 'Ambiente ainda nao criado. Execute .\testar.cmd api primeiro.'
    }
    $state = Get-Content -LiteralPath $script:StatePath -Raw | ConvertFrom-Json
    if ($state.format -ne 1 -or $state.repository -ne $script:Repo) {
        throw 'O estado local nao pertence a este clone. Nenhum processo foi alterado.'
    }
    return $state
}

function New-State {
    $state = [PSCustomObject]@{
        format = 1; repository = $script:Repo; pgBin = (Find-Postgres)
        pgPort = (Get-FreePort 55432); apiPort = (Get-FreePort 18080)
        dbPassword = (New-Secret); apiKey = (New-Secret); photoKey = (New-Secret -Base64)
        apiPid = 0; backendFingerprint = ''; seedComplete = $false
        patientId = ''; nutritionistId = ''; followUpId = ''; planId = ''
        lastSerial = ''
    }
    Save-State $state
    return $state
}

function Get-ApiJar { return Join-Path $script:Work 'backend-build\libs\link-health-api-0.0.1-SNAPSHOT.jar' }
function Get-DatabaseDir { return Join-Path $script:Work 'postgres-data' }
function Get-BaseUrl($State) { return "http://127.0.0.1:$($State.apiPort)" }

function Invoke-Api($State, [string]$Method, [string]$Path, $Body = $null) {
    $parameters = @{
        Uri = (Get-BaseUrl $State) + $Path; Method = $Method
        Headers = @{ 'X-API-Key' = $State.apiKey }; TimeoutSec = 15; UseBasicParsing = $true
    }
    if ($null -ne $Body) {
        $parameters.ContentType = 'application/json; charset=utf-8'
        $parameters.Body = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 8 -Compress))
    }
    # Windows PowerShell preserva arrays JSON como um unico objeto na saida do
    # cmdlet. Enumere aqui, inclusive o array vazio, antes dos filtros do seed.
    $response = Invoke-RestMethod @parameters
    if ($response -is [Array]) { foreach ($item in $response) { $item } }
    elseif ($null -ne $response) { $response }
}

function Test-ApiReady($State) {
    try { return (Invoke-Api $State GET '/api/v1/health').status -eq 'UP' } catch { return $false }
}

function Get-OwnedApi($State) {
    if (-not $State.apiPid) { return $null }
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$([int]$State.apiPid)" -ErrorAction SilentlyContinue
    if (-not $process) { return $null }
    if ($process.Name -ne 'java.exe' -or -not $process.CommandLine -or
        $process.CommandLine.IndexOf((Get-ApiJar), [StringComparison]::OrdinalIgnoreCase) -lt 0) {
        # Depois de reiniciar o computador, um PID antigo pode ser reutilizado.
        # A API anterior nao esta mais ali; preserve o processo atual e inicie outra.
        return $null
    }
    return $process
}

function Stop-OwnedApi($State) {
    if (Get-OwnedApi $State) {
        Stop-Process -Id ([int]$State.apiPid)
        Wait-Process -Id ([int]$State.apiPid) -Timeout 20 -ErrorAction SilentlyContinue
    }
    $State.apiPid = 0
    Save-State $State
}

function Get-OwnedPostgres {
    $dataDir = Get-DatabaseDir
    $pidFile = Join-Path $dataDir 'postmaster.pid'
    if (-not (Test-Path -LiteralPath $pidFile)) { return $null }
    $serverId = [int](Get-Content -LiteralPath $pidFile -TotalCount 1)
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$serverId" -ErrorAction SilentlyContinue
    if ($process -and ($process.Name -ne 'postgres.exe' -or -not $process.CommandLine -or
        $process.CommandLine.Replace('/', '\').IndexOf($dataDir, [StringComparison]::OrdinalIgnoreCase) -lt 0)) {
        throw 'O PID do banco nao corresponde a esta instancia. Nenhum outro banco foi alterado.'
    }
    return $process
}

function Start-Database($State) {
    $dataDir = Get-DatabaseDir
    $log = Join-Path $script:Work 'postgres.log'
    if (-not (Test-Path -LiteralPath (Join-Path $dataDir 'PG_VERSION'))) {
        if ((Test-Path -LiteralPath $dataDir) -and @(Get-ChildItem -LiteralPath $dataDir -Force).Count -gt 0) {
            throw "Inicializacao anterior incompleta em $dataDir. Nenhum arquivo foi apagado. Confira postgres-init.log."
        }
        Write-Host '[1/4] Preparando PostgreSQL isolado, sem usar a senha do banco normal...'
        $passwordFile = Join-Path $script:Work 'initdb-password.tmp'
        Write-Utf8 $passwordFile ($State.dbPassword + "`n")
        try {
            $null = Invoke-Tool (Join-Path $State.pgBin 'initdb.exe') @(
                '-D', $dataDir, '-U', 'linkhealth_test', '--auth=scram-sha-256',
                "--pwfile=$passwordFile", '--encoding=UTF8', '--locale=C'
            ) (Join-Path $script:Work 'postgres-init.log')
        } finally { Remove-Item -LiteralPath $passwordFile -ErrorAction SilentlyContinue }
    }
    $installedVersion = (Invoke-Tool (Join-Path $State.pgBin 'pg_ctl.exe') @('--version'))
    $dataVersion = (Get-Content -LiteralPath (Join-Path $dataDir 'PG_VERSION') -Raw).Trim()
    if ($installedVersion -notmatch "PostgreSQL\) $([regex]::Escape($dataVersion))\.") {
        throw "Os dados deste teste exigem PostgreSQL $dataVersion. Instale a mesma versao principal ou use outro clone."
    }
    if (-not (Get-OwnedPostgres)) {
        Write-Host '[1/4] Iniciando PostgreSQL somente em 127.0.0.1...'
        Invoke-PgControl $State @(
            '-D', $dataDir, '-l', $log, '-o', "-p $($State.pgPort) -h 127.0.0.1", '-w', '-t', '45', 'start'
        )
    }
    Use-Environment @{ PGPASSWORD = $State.dbPassword; PGSERVICE = $null; PGOPTIONS = $null } {
        $argsPg = @('-h', '127.0.0.1', '-p', [string]$State.pgPort, '-U', 'linkhealth_test', '-w')
        $exists = Invoke-Tool (Join-Path $State.pgBin 'psql.exe') ($argsPg + @(
            '-d', 'postgres', '-tAc', "SELECT 1 FROM pg_database WHERE datname='link_health_testes'"
        ))
        if ($exists.Trim() -ne '1') {
            $null = Invoke-Tool (Join-Path $State.pgBin 'createdb.exe') ($argsPg + @('link_health_testes'))
        }
    }
}

function Get-BackendFingerprint {
    $files = @(Get-ChildItem -LiteralPath (Join-Path $script:BackendDir 'src\main') -Recurse -File)
    $files += Get-Item -LiteralPath (Join-Path $script:BackendDir 'build.gradle.kts'),
        (Join-Path $script:BackendDir 'settings.gradle.kts'),
        (Join-Path $script:AppDir 'gradle\wrapper\gradle-wrapper.properties')
    $entries = @($files | Sort-Object FullName | ForEach-Object {
        $digest = [Security.Cryptography.SHA256]::Create()
        $stream = [IO.File]::OpenRead($_.FullName)
        try { $_.FullName + ':' + [BitConverter]::ToString($digest.ComputeHash($stream)) }
        finally { $stream.Dispose(); $digest.Dispose() }
    }) -join "`n"
    $hash = [Security.Cryptography.SHA256]::Create()
    try { return [BitConverter]::ToString($hash.ComputeHash([Text.Encoding]::UTF8.GetBytes($entries))) }
    finally { $hash.Dispose() }
}

function Start-Api($State) {
    $javaHome = Find-Java
    Start-Database $State
    $fingerprint = Get-BackendFingerprint
    $jar = Get-ApiJar
    $owned = Get-OwnedApi $State
    if ($owned -and (Test-ApiReady $State) -and $State.backendFingerprint -eq $fingerprint) {
        Write-Host '[2/4] API atualizada ja esta em execucao.'
        return
    }
    Stop-OwnedApi $State
    Write-Host '[2/4] Compilando backend. Na primeira vez, o Gradle baixa as dependencias...'
    Use-Environment @{ JAVA_HOME = $javaHome } {
        $null = Invoke-Tool $script:Wrapper @(
            '-p', $script:BackendDir, '--project-cache-dir', (Join-Path $script:Work 'gradle-backend'),
            "-PlinkHealthBuildDir=$(Join-Path $script:Work 'backend-build')", 'bootJar', '--console=plain'
        ) (Join-Path $script:Work 'backend-build.log')
    }
    if (-not (Test-Path -LiteralPath $jar)) { throw "O Gradle nao produziu $jar." }
    $environment = @{
        SPRING_PROFILES_ACTIVE = 'postgres'
        SPRING_CONFIG_IMPORT = 'optional:classpath:/link-health-quick-test-no-env.properties'
        LINK_HEALTH_DB_URL = "jdbc:postgresql://127.0.0.1:$($State.pgPort)/link_health_testes"
        LINK_HEALTH_DB_USERNAME = 'linkhealth_test'; LINK_HEALTH_DB_PASSWORD = $State.dbPassword
        LINK_HEALTH_API_KEY = $State.apiKey; LINK_HEALTH_PHOTOS_ENCRYPTION_KEY = $State.photoKey
        LINK_HEALTH_SECURITY_ENABLED = 'true'; LINK_HEALTH_REQUIRE_HTTPS = 'false'
        LINK_HEALTH_ALLOWED_ORIGINS = ''; LINK_HEALTH_RATE_LIMIT_PER_MINUTE = '120'
        LINK_HEALTH_PHOTOS_DIRECTORY = (Join-Path $script:Work 'photos')
        SERVER_ADDRESS = '127.0.0.1'; SERVER_PORT = [string]$State.apiPort; PORT = [string]$State.apiPort
    }
    Write-Host '[3/4] Iniciando API e aplicando as migracoes...'
    Use-Environment $environment {
        $process = Start-Process -FilePath (Join-Path $javaHome 'bin\java.exe') -ArgumentList @('-Xmx512m', '-jar', "`"$jar`"") `
            -WorkingDirectory $script:Work -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $script:Work 'api.log') `
            -RedirectStandardError (Join-Path $script:Work 'api-error.log')
        $State.apiPid = $process.Id
        Save-State $State
    }
    $limit = [DateTime]::UtcNow.AddSeconds(90)
    do {
        if (-not (Get-OwnedApi $State)) {
            throw "A API encerrou antes de iniciar. Veja api.log e api-error.log em $script:Work."
        }
        if (Test-ApiReady $State) {
            $State.backendFingerprint = $fingerprint
            Save-State $State
            return
        }
        Start-Sleep -Milliseconds 700
    } while ([DateTime]::UtcNow -lt $limit)
    throw "A API nao ficou pronta em 90 segundos. Veja os logs em $script:Work."
}

function Ensure-DemoAccount($State, [string]$Email, [string]$Role, [string]$Document, [string]$Name) {
    $login = @{ email = $Email; password = $script:DemoPassword }
    try { return Invoke-Api $State POST '/api/v1/auth/login' $login } catch {
        if (-not $_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne 400) { throw }
    }
    return Invoke-Api $State POST '/api/v1/auth/register' @{
        fullName = $Name; email = $Email; document = $Document; password = $script:DemoPassword; role = $Role
    }
}

function Initialize-Demo($State) {
    if ($State.seedComplete) { Write-Host '[4/4] Dados de teste preservados.'; return }
    Write-Host '[4/4] Preparando contas de teste, vinculo ativo, meta e exemplos...'
    $patient = Ensure-DemoAccount $State 'paciente@linkhealth.test' 'PACIENTE' '12345678901' 'Paciente de Teste'
    $nutritionist = Ensure-DemoAccount $State 'nutricionista@linkhealth.test' 'NUTRICIONISTA' 'CRN-TESTE-001' 'Nutricionista de Teste'
    $State.patientId = $patient.id; $State.nutritionistId = $nutritionist.id
    $links = @(Invoke-Api $State GET "/api/v1/patients/$($patient.id)/follow-ups")
    $link = $links | Where-Object { $_.nutritionistId -eq $nutritionist.id -and $_.status -in @('PENDENTE', 'ATIVO') } | Select-Object -First 1
    if (-not $link) {
        $link = Invoke-Api $State POST '/api/v1/follow-ups' @{
            nutritionistId = $nutritionist.id; patientId = $patient.id
            objective = 'Acompanhamento de teste'; waterGoalMl = 3000
        }
    }
    if ($link.status -eq 'PENDENTE') {
        $link = Invoke-Api $State POST "/api/v1/follow-ups/$($link.id)/response" @{ patientId = $patient.id; accept = $true }
    }
    $State.followUpId = $link.id
    $plans = @(Invoke-Api $State GET "/api/v1/follow-ups/$($link.id)/meal-plans")
    $plan = $plans | Select-Object -First 1
    if (-not $plan) { $plan = Invoke-Api $State POST "/api/v1/follow-ups/$($link.id)/meal-plans" }
    if (-not $plan.active) {
        foreach ($meal in @(@{ name = 'Cafe da manha'; time = '08:00:00' }, @{ name = 'Almoco'; time = '12:00:00' }, @{ name = 'Jantar'; time = '19:00:00' })) {
            if (-not @($plan.meals | Where-Object { $_.name -eq $meal.name }).Count) {
                $plan = Invoke-Api $State POST "/api/v1/meal-plans/$($plan.id)/meals" $meal
            }
        }
        foreach ($meal in $plan.meals) {
            if (@($meal.items).Count -eq 0) {
                $null = Invoke-Api $State POST "/api/v1/meal-plans/$($plan.id)/meals/$($meal.id)/items" @{ description = 'Refeicao de exemplo para testar a marcacao' }
            }
        }
        $plan = Invoke-Api $State POST "/api/v1/meal-plans/$($plan.id)/activate"
    }
    $State.planId = $plan.id
    $shoppingPath = "/api/v1/patients/$($patient.id)/shopping-items"
    $items = @(Invoke-Api $State GET $shoppingPath)
    if (-not @($items | Where-Object { $_.name -eq 'Aveia de teste' }).Count) {
        $null = Invoke-Api $State POST $shoppingPath @{ name = 'Aveia de teste'; quantity = 1; unit = 'UNIT' }
    }
    $productsPath = "/api/v1/patients/$($patient.id)/recommended-products"
    $products = @(Invoke-Api $State GET $productsPath)
    if (-not @($products | Where-Object { $_.name -eq 'Produto demonstrativo' }).Count) {
        $null = Invoke-Api $State POST $productsPath @{
            name = 'Produto demonstrativo'; description = 'Exemplo de teste; o link nao e uma loja.'
            price = 10.90; currency = 'BRL'; purchaseUrl = 'https://example.com/'
        }
    }
    $State.seedComplete = $true
    Save-State $State
}

function Show-Accounts($State, [switch]$AppOpened) {
    Write-Host ''
    Write-Host "API de testes pronta: $(Get-BaseUrl $State)"
    Write-Host 'Paciente:       paciente@linkhealth.test'
    Write-Host 'Nutricionista:  nutricionista@linkhealth.test'
    Write-Host "Senha das duas contas de TESTE: $script:DemoPassword"
    Write-Host 'Banco separado, vinculo ativo, meta de 3 L e plano de exemplo.'
    if ($AppOpened) { Write-Host 'Entre no app com uma das contas acima e teste as funcionalidades.' }
    else { Write-Host 'Proximo comando: .\testar.cmd app' }
}

function Find-Sdk {
    $candidates = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, (Join-Path $env:LOCALAPPDATA 'Android\Sdk'))
    $properties = Join-Path $script:AppDir 'local.properties'
    if (Test-Path -LiteralPath $properties) {
        $sdkLine = Get-Content -LiteralPath $properties | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($sdkLine) {
            $decoded = $sdkLine.Substring(8).Replace('\:', ':').Replace('\\', '\')
            $candidates += [regex]::Replace($decoded, '\\u([0-9a-fA-F]{4})', [Text.RegularExpressions.MatchEvaluator]{
                param($match)
                [string][char][Convert]::ToInt32($match.Groups[1].Value, 16)
            })
        }
    }
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath (Join-Path $candidate 'platform-tools\adb.exe'))) { return $candidate }
    }
    throw 'SDK Android nao localizado. Instale o SDK e Platform-Tools pelo Android Studio uma vez.'
}

function ConvertTo-SdkProperty([string]$Path) {
    $value = $Path.Replace('\', '/').Replace(':', '\:')
    # local.properties usa escapes Java, inclusive para nomes de usuario com acento.
    return [regex]::Replace($value, '[^\x20-\x7e]', [Text.RegularExpressions.MatchEvaluator]{
        param($match)
        '\u{0:x4}' -f [int][char]$match.Value[0]
    })
}

function Select-Device([string]$Adb, [string]$Sdk) {
    $devicesText = Invoke-Tool $Adb @('devices')
    $devices = @($devicesText -split '\r?\n' | Where-Object { $_ -match '^\S+\s+device$' } | ForEach-Object { ($_ -split '\s+')[0] })
    if ($Serial) {
        if ($Serial -notin $devices) { throw "O dispositivo $Serial nao esta conectado/autorizado. Confira adb devices." }
        return $Serial
    }
    if ($devices.Count -eq 1) { return $devices[0] }
    if ($devices.Count -gt 1) {
        throw "Ha mais de um dispositivo. Execute .\testar.cmd app -Serial <serial>. Disponiveis: $($devices -join ', ')"
    }
    if ($devicesText -match '\s+(unauthorized|offline)(\s|$)') {
        throw 'Dispositivo sem autorizacao/offline. Aceite Depuracao USB no aparelho ou reinicie o emulador.'
    }
    $emulator = Join-Path $Sdk 'emulator\emulator.exe'
    if (-not (Test-Path -LiteralPath $emulator)) { throw 'Instale Android Emulator pelo SDK Manager.' }
    $avds = @((Invoke-Tool $emulator @('-list-avds')) -split '\r?\n' | Where-Object { $_.Trim() })
    if (-not $avds.Count) { throw 'Configure um emulador no Device Manager uma vez, ou conecte um celular com Depuracao USB.' }
    $avd = $avds[0].Trim()
    Write-Host "Abrindo emulador $avd..."
    $null = Start-Process -FilePath $emulator -ArgumentList @('-avd', "`"$avd`"") -WindowStyle Normal `
        -RedirectStandardOutput (Join-Path $script:Work 'emulator.log') `
        -RedirectStandardError (Join-Path $script:Work 'emulator-error.log')
    $limit = [DateTime]::UtcNow.AddSeconds(180)
    do {
        Start-Sleep -Seconds 2
        $text = Invoke-Tool $Adb @('devices')
        $ready = @($text -split '\r?\n' | Where-Object { $_ -match '^emulator-\d+\s+device$' } | ForEach-Object { ($_ -split '\s+')[0] })
        if ($ready.Count -eq 1) { return $ready[0] }
    } while ([DateTime]::UtcNow -lt $limit)
    throw "Emulador nao conectou em 3 minutos. Confira emulator-error.log em $script:Work."
}

function Start-App($State) {
    if (-not (Get-OwnedApi $State) -or -not (Test-ApiReady $State) -or -not $State.seedComplete) {
        throw 'A API de testes ainda nao esta pronta. Execute .\testar.cmd api primeiro.'
    }
    $javaHome = Find-Java
    $sdk = Find-Sdk
    $adb = Join-Path $sdk 'platform-tools\adb.exe'
    $device = Select-Device $adb $sdk
    Write-Host "Aguardando Android iniciar em $device..."
    $limit = [DateTime]::UtcNow.AddSeconds(180)
    do {
        $booted = (Invoke-Tool $adb @('-s', $device, 'shell', 'getprop', 'sys.boot_completed')).Trim()
        if ($booted -eq '1') { break }
        Start-Sleep -Seconds 2
    } while ([DateTime]::UtcNow -lt $limit)
    if ($booted -ne '1') { throw 'O Android nao terminou de iniciar em 3 minutos.' }
    $null = Invoke-Tool $adb @('-s', $device, 'reverse', 'tcp:8080', "tcp:$($State.apiPort)")
    # Somente a configuracao SDK local e preenchida; nunca uma URL/IP no codigo.
    $properties = Join-Path $script:AppDir 'local.properties'
    $old = if (Test-Path -LiteralPath $properties) { Get-Content -LiteralPath $properties -Raw } else { '' }
    $sdkProperty = 'sdk.dir=' + (ConvertTo-SdkProperty $sdk)
    if ($old -match '(?m)^sdk\.dir=.*$') { $new = [regex]::Replace($old, '(?m)^sdk\.dir=.*$', [Text.RegularExpressions.MatchEvaluator]{ param($m) $sdkProperty }) }
    else { $new = $old.TrimEnd() + "`n" + $sdkProperty + "`n" }
    if ($new -ne $old) { Write-Utf8 $properties $new }
    Write-Host 'Compilando e instalando app debug. Na primeira vez pode demorar...'
    $buildDir = Join-Path $script:Work 'android-build'
    Use-Environment @{ JAVA_HOME = $javaHome; ANDROID_HOME = $sdk; ANDROID_SDK_ROOT = $sdk; LINK_HEALTH_BACKEND_HOST = '127.0.0.1' } {
        $null = Invoke-Tool $script:Wrapper @(
            '-p', $script:AppDir, '--project-cache-dir', (Join-Path $script:Work 'gradle-android'),
            "-PlinkHealthBuildDir=$buildDir", 'assembleDebug', '--console=plain'
        ) (Join-Path $script:Work 'android-build.log')
    }
    $apk = Join-Path $buildDir 'outputs\apk\debug\app-debug.apk'
    if (-not (Test-Path -LiteralPath $apk)) { throw "APK nao encontrado em $apk." }
    $null = Invoke-Tool $adb @('-s', $device, 'install', '-r', $apk)
    $null = Invoke-Tool $adb @('-s', $device, 'shell', 'am', 'force-stop', 'br.edu.pucgoias.app')
    $null = Invoke-Tool $adb @('-s', $device, 'shell', 'am', 'start', '-W', '-n', 'br.edu.pucgoias.app/.MainActivity')
    $State.lastSerial = $device
    Save-State $State
    Write-Host 'Aplicativo aberto. Conexao via ADB, sem depender do IP da rede.'
    Show-Accounts $State -AppOpened
}

function Stop-Environment($State) {
    Stop-OwnedApi $State
    $dataDir = Get-DatabaseDir
    if (Get-OwnedPostgres) {
        Invoke-PgControl $State @('-D', $dataDir, '-m', 'fast', '-w', '-t', '30', 'stop')
    }
    Write-Host 'API e PostgreSQL de testes encerrados. Dados preservados; o banco normal nao foi alterado.'
    Write-Host 'O emulador/celular permanece aberto. Para voltar: .\testar.cmd api e .\testar.cmd app'
}

try {
    if ($Acao -eq 'ajuda') {
        Write-Host 'Na raiz do clone, execute estes dois comandos:'
        Write-Host '  .\testar.cmd api'
        Write-Host '  .\testar.cmd app'
        Write-Host 'Extras: .\testar.cmd status | .\testar.cmd parar'
        Write-Host 'Requer Android Studio/SDK, um emulador configurado e PostgreSQL instalado.'
        exit 0
    }
    $null = New-Item -ItemType Directory -Path $script:Work -Force
    try { $lock = [IO.File]::Open((Join-Path $script:Work 'comando.lock'), 'OpenOrCreate', 'ReadWrite', 'None') }
    catch [IO.IOException] { throw 'Outro comando de testes esta em execucao neste clone. Aguarde terminar e repita.' }
    try {
        if ($Acao -eq 'api') {
            $state = if (Test-Path -LiteralPath $script:StatePath) { Read-State } else { New-State }
            Start-Api $state
            Initialize-Demo $state
            Show-Accounts $state
        } elseif ($Acao -eq 'app') { Start-App (Read-State) }
        elseif ($Acao -eq 'parar') { Stop-Environment (Read-State) }
        elseif ($Acao -eq 'status') {
            $state = Read-State
            $ready = (Get-OwnedApi $state) -and (Test-ApiReady $state)
            Write-Host "API pronta: $ready | Exemplos preparados: $($state.seedComplete)"
            Write-Host "Arquivos locais e logs: $script:Work"
        }
    } finally { $lock.Dispose() }
} catch {
    Write-Host "ERRO: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Logs locais: $script:Work"
    exit 1
}
