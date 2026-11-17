# Testar o Link Health em dois comandos

Depois de receber as alteracoes pelo **Fetch origin / Pull origin** no GitHub Desktop, abra o PowerShell **na raiz do clone**: a pasta `Link-Health` onde ficam `App`, `backend`, `scripts` e `testar.cmd`.

## Qual pasta usar no PowerShell

No computador do Felipe:

```powershell
cd "C:\Users\felip\OneDrive\Documents\api\Link-Health"
```

Cada colega usa o caminho do proprio clone. Por exemplo, se o clone esta em `C:\Projetos\Link-Health`:

```powershell
cd "C:\Projetos\Link-Health"
```

O trecho `api` do caminho do Felipe e apenas uma pasta do computador dele; os colegas nao precisam ter o mesmo caminho. Os comandos usam o arquivo `testar.cmd` que fica na raiz, acima de `backend` e `App`.

Confira com:

```powershell
dir testar.cmd
```

Se o arquivo aparecer, voce esta na pasta certa. Se nao aparecer, corrija o `cd` para a pasta do clone que contem esse arquivo.

## Executar os dois comandos

Execute primeiro:

```powershell
.\testar.cmd api
```

Aguarde **API de testes pronta** e o retorno ao prompt do PowerShell. Depois, no mesmo terminal e na mesma pasta, execute:

```powershell
.\testar.cmd app
```

Espere o primeiro terminar antes de executar o segundo. Nao precisa editar codigo ou `.env`, criar banco, informar a senha do PostgreSQL, cadastrar contas nem preparar vinculos manualmente.

## O que precisa existir no computador uma vez

- **Windows e Android Studio**, com Java/JBR, SDK 37, Platform-Tools, licencas do SDK aceitas e os componentes exigidos pelo Gradle do projeto.
- **Um emulador configurado no Device Manager**, ou um celular conectado por USB com Depuracao USB autorizada. Se nenhum dispositivo estiver aberto, o script inicia o primeiro emulador configurado.
- **PostgreSQL instalado**. O script procura sua pasta `bin`, normalmente `C:\Program Files\PostgreSQL\18\bin`. Ele usa os executaveis para iniciar uma instancia propria, sem usar a senha ou o servico do banco normal.
- Internet no primeiro build para baixar dependencias. Virtualizacao precisa estar funcionando para o emulador.

As instalacoes/licencas iniciais das ferramentas continuam necessarias. Os dois comandos automatizam a preparacao do projeto depois desses pre-requisitos. Nao exigem Docker.

## O que cada comando faz

**`api`** gera chaves aleatorias locais, inicia PostgreSQL apenas em `127.0.0.1`, compila e inicia a API com o perfil `postgres`. As tabelas sao criadas pelo Flyway. Prepara duas contas sinteticas, acompanhamento ativo, objetivo, meta de agua de 3 litros, tres refeicoes de plano publicado, um item de compras e um produto de demonstracao.

Esses dados sao preparacao do ambiente de testes: o script usa as rotas administrativas existentes para criar o vinculo e o plano. Isso **nao implementa as telas de convite ou de autoria de planos** de outros integrantes. As operacoes do paciente e do nutricionista sao testadas no aplicativo contra PostgreSQL real.

**`app`** localiza Java/SDK, seleciona o dispositivo, aguarda o Android iniciar, configura `adb reverse`, compila o APK debug, instala e abre o aplicativo. A conexao usa `127.0.0.1:8080` dentro do Android, encaminhado para a API de testes. Assim nao depende do IP do Wi-Fi e nao precisa liberar porta de entrada no firewall.

A primeira compilacao e a primeira abertura do emulador podem levar minutos. Os proximos builds reutilizam o cache. O comando `api` reutiliza a API se o backend nao mudou; depois de atualizar o codigo, execute os dois comandos novamente.

## Contas prontas para entrar no app

| Perfil selecionado no login | E-mail | Senha de demonstracao |
| --- | --- | --- |
| Paciente | `paciente@linkhealth.test` | `TesteApp123!` |
| Nutricionista | `nutricionista@linkhealth.test` | `TesteApp123!` |

Estas sao contas publicas de demonstracao de um banco local isolado. Nao sao credenciais do banco, chaves de API ou contas reais. Nao publique esse ambiente na internet.

Como paciente, confira:

1. **Agua:** +500 ml deve preencher aproximadamente 17% da meta de 3 L. Personalizado permite somar outra quantidade ou corrigir o total de hoje.
2. **Compras:** marque/desmarque o item, edite, adicione e remova. Reabra para conferir persistencia.
3. **Refeicoes:** marque/desmarque as refeicoes planejadas e registre/edite/remova uma refeicao avulsa.
4. **Evolucao:** adicione Frente, Lado e Costas, informe o peso e salve. Confira um unico card, o album de tres fotos e a data/hora; remova a avaliacao inteira.
5. **Historico:** confira os registros e os totais. Feche/reabra o app e entre novamente.

Como nutricionista, abra a ficha de **Paciente de Teste**, altere objetivo/meta e crie/edite/remova produtos recomendados. Volte ao paciente e confira os mesmos dados. O produto inicial abre `example.com`, um endereco demonstrativo, sem compra real.

Se apagar os exemplos durante os testes, eles nao reaparecem automaticamente a cada inicio: seus testes e alteracoes ficam preservados. A preparacao ocorre uma vez. Voce tambem pode criar outras contas pela interface.

## Parar, conferir ou escolher dispositivo

```powershell
.\testar.cmd status
.\testar.cmd parar
```

`parar` encerra somente a API e a instancia PostgreSQL deste ambiente; mantem os dados, o emulador e o banco normal. Para voltar, execute `api` e `app`. Reiniciar a API invalida sessoes antigas: entre novamente.

Se houver mais de um dispositivo conectado, o script pede para especificar qual usar, por exemplo:

```powershell
.\testar.cmd app -Serial emulator-5554
```

Um celular precisa permanecer conectado por USB. Depois de reiniciar/reconectar o Android, execute `app` novamente para restaurar o encaminhamento. Aparelho fisico depende de autorizacao e drivers locais.

## Dados locais e seguranca

- Banco, chaves, logs e compilacoes ficam em `%LOCALAPPDATA%\LinkHealth\Testes\<identificador-do-clone>`, fora do repositorio e do OneDrive.
- Nao le nem modifica `backend/.env`, nao acessa o banco de desenvolvimento e nao altera seu servico PostgreSQL.
- Gera uma senha aleatoria para a instancia de testes e preserva a chave de criptografia das fotos entre reinicios.
- API e PostgreSQL escutam apenas no computador local. O app recebe token de login; a chave administrativa fica fora do APK.
- O script completa `App/local.properties` com o SDK desta maquina, preservando as outras propriedades. Esse arquivo ja e ignorado pelo Git.
- Os dois arquivos Gradle aceitam a propriedade opcional `linkHealthBuildDir` para isolar os builds. Sem essa propriedade, a execucao normal continua usando os caminhos existentes.
- Nao apaga banco nem dados automaticamente. Nao encerra processos por nome: confere a identidade do processo desta instancia antes de parar.
- Cada colega tem dados independentes. Receber codigo pelo Git nao sincroniza bancos.

## Validacao desta automacao em 16/11/2026

- Banco novo inicializado sem senha informada e migracoes aplicadas pela API.
- Clone limpo, sem `.env` e com espacos no caminho: API preparada, emulador iniciado automaticamente e APK compilado, instalado e aberto.
- Login real com a conta pronta; 500 ml registrados no app e barra confirmada em 17% de 3 litros.
- Repeticao dos comandos e reinicio preservando contas, vinculo e consumo, sem recriar os exemplos.
- Encerramento somente da instancia isolada; servico PostgreSQL normal mantido em execucao.
- Backend: 62 testes aprovados, incluindo 24 PostgreSQL, sem falhas ou casos ignorados.
- Android: compilacao e 3 testes JVM aprovados; Lint com 0 erros e 123 avisos existentes.
- Nenhuma senha/chave local incluida nos arquivos que serao enviados ao Git.

A validacao foi neste Windows/emulador. Outros computadores dependem dos pre-requisitos acima; aparelho fisico nao foi validado nesta revisao. A automacao prepara o ambiente e abre o app, mas nao substitui os testes funcionais do grupo.

## Se falhar

O comando informa o erro e a pasta dos logs. `backend-build.log` e `android-build.log` mostram problemas de compilacao; `api.log`, `api-error.log` e `postgres.log` mostram inicializacao/conexao. Nao compartilhe `ambiente.json`, pois contem segredos locais.

- **PostgreSQL nao localizado:** instale PostgreSQL uma vez. Para instalacao personalizada, defina `LINK_HEALTH_PG_BIN` com a pasta `bin` antes do comando.
- **SDK/emulador ausente:** conclua a configuracao no SDK Manager/Device Manager uma vez; o script nao baixa imagens Android gigantes nem aceita licencas por voce.
- **Dispositivo unauthorized/offline:** aceite a autorizacao USB ou reinicie o emulador.
- **Mais de um dispositivo:** use `-Serial` conforme acima.
- **Porta de testes ocupada depois da primeira execucao:** encerre o processo que usa a porta indicada no log; o script nao derruba outros programas.
- **Tela antiga:** execute `app` depois de atualizar a branch. O APK de testes usa ambiente proprio; um Run comum do Android Studio usa a configuracao normal de desenvolvimento.

Referencia das ferramentas utilizadas: [PostgreSQL initdb](https://www.postgresql.org/docs/18/app-initdb.html), [pg_ctl](https://www.postgresql.org/docs/18/app-pg-ctl.html), [Android ADB](https://developer.android.com/tools/adb) e [emulador por linha de comando](https://developer.android.com/studio/run/emulator-commandline).

O roteiro detalhado de verificacao manual continua em [GUIA_TESTES_DO_GRUPO.txt](GUIA_TESTES_DO_GRUPO.txt). Ele descreve tambem o caminho tradicional, com configuracao manual, para quem precisar investigar outras partes do sistema.
