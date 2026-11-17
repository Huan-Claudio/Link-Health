# Link-Health

## Como rodar os testes no seu computador

No GitHub Desktop, receba as alterações pela branch combinada com a equipe: **Fetch origin** e depois **Pull origin**, quando aparecer.

### 1. Abra o PowerShell na raiz do projeto

É a pasta **Link-Health**, onde estão `testar.cmd`, `App` e `backend`:

```text
Link-Health/
├── testar.cmd
├── scripts/
├── App/
├── backend/
├── README.md
└── TESTAR_EM_2_COMANDOS.md
```

Os comandos abaixo são executados nessa pasta, acima de `App` e `backend`.

**No computador do Felipe**, o `cd` é:

```powershell
cd "C:\Users\felip\OneDrive\Documents\api\Link-Health"
```

**Nos outros computadores**, use o caminho do próprio clone. Por exemplo, se clonou em `C:\Projetos\Link-Health`:

```powershell
cd "C:\Projetos\Link-Health"
```

Para confirmar a pasta atual, execute `dir testar.cmd`. O arquivo deve aparecer na listagem. Se aparecer “arquivo não encontrado”, confira o caminho usado no `cd`.

### 2. Inicie o ambiente e depois o aplicativo

No mesmo PowerShell, execute:

```powershell
.\testar.cmd api
```

Aguarde a mensagem **API de testes pronta** e o comando terminar. Ele prepara o PostgreSQL separado, as tabelas, a API e as contas de demonstração. Depois execute, na mesma pasta:

```powershell
.\testar.cmd app
```

O segundo comando abre o emulador, compila, instala e inicia o app. Se já houver um dispositivo conectado e autorizado, ele é utilizado. A primeira execução pode levar alguns minutos para baixar dependências e compilar.

### 3. Entre com uma conta de teste

| Perfil no login | E-mail | Senha de demonstração |
| --- | --- | --- |
| Paciente | `paciente@linkhealth.test` | `TesteApp123!` |
| Nutricionista | `nutricionista@linkhealth.test` | `TesteApp123!` |

As contas já têm vínculo ativo, meta de água de 3 litros e exemplos para testar compras, produtos e refeições. Fotos e peso podem ser registrados pela interface. Os dados ficam no computador de quem está testando.

## Pré-requisitos da primeira instalação

- Windows com Android Studio e Java/JBR.
- SDK 37, Platform-Tools e licenças do SDK aceitas; emulador configurado no Device Manager ou celular por USB com Depuração USB autorizada.
- PostgreSQL instalado. O script usa os executáveis para criar uma instância própria, sem pedir a senha do banco normal.
- Internet para baixar dependências na primeira compilação.

Com essas ferramentas preparadas, os scripts fazem a configuração local: não é necessário criar banco ou contas nem editar `.env` manualmente.

## Conferir ou encerrar

Execute também na raiz do projeto:

```powershell
.\testar.cmd status
.\testar.cmd parar
```

`status` mostra se a API está pronta. `parar` encerra a API e o PostgreSQL de testes, preservando os dados. Para voltar a testar, repita `api` e `app`.

O roteiro completo, incluindo testes dentro do app e solução de erros, está em [TESTAR_EM_2_COMANDOS.md](TESTAR_EM_2_COMANDOS.md), nesta mesma pasta.
