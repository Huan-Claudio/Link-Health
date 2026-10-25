# Correções e validação — 24/10/2026

## Limite da autorização

Este trabalho corrige defeitos das integrações já existentes de lista de compras (Etapa 1)
e produtos recomendados (Etapa 2), além de infraestrutura já implementada que dá suporte
à parte do Felipe. **A Etapa 3 de integração não foi iniciada.**

Não foram conectadas novas telas de água, refeições, peso ou fotos. Não foram criadas
migrações para essas integrações, novas funcionalidades de convites/plano alimentar,
nem funcionalidades de outras partes da equipe.

O envio de fotos já existia no backend antes desta revisão. Corrigir sua persistência
não significa que a tela Android de fotos já esteja conectada.

## O que foi corrigido

### Compras e produtos: validação e respostas da API

- Quantidade respeita a capacidade da coluna: até 9 dígitos inteiros e 3 casas decimais.
- Preço respeita até 10 dígitos inteiros e 2 casas decimais.
- Editar um nome com somente espaços é recusado; atualizações parciais continuam permitidas.
- Marcar/desmarcar uma compra exige `purchased: true` ou `purchased: false`.
  Um corpo vazio não desmarca mais uma compra silenciosamente.
- UUID malformado retorna 400, em vez de erro interno 500.
- Compras e produtos verificam se a conta de destino existe e é de paciente.
- Violações de integridade recebem mensagem genérica, sem expor detalhes SQL.
- Links de produtos aceitam apenas HTTP/HTTPS válidos, sem credenciais embutidas.
- Códigos de moeda são validados e normalizados.

Arquivos principais: DTOs em `src/main/java/br/edu/pucgoias/linkhealth/api/shopping/`
e `api/product/`; `ApiExceptionHandler`, `ShoppingItemService`,
`RecommendedProductService` e `UserAccountService`.

### Produtos no Android

- O modelo e o cliente HTTP preservam a moeda devolvida pela API.
- A lista mostra o preço formatado na moeda correta; USD não é exibido como real.
- A edição mantém a moeda original ao salvar e informa qual moeda está sendo editada.
- Rascunhos, operações pendentes e o ID confirmado pelo servidor ficam em um `ViewModel`.
  Girar a tela durante um salvamento não perde o resultado da requisição.
- Campos e botões do produto ficam desabilitados enquanto sua operação está pendente,
  evitando envios repetidos por cliques durante a mesma operação.
- A seta de voltar do cabeçalho compartilhado deixou de ficar parcialmente recortada.
  Foi uma alteração mínima necessária à navegação e aos testes dessas telas.

Arquivos principais no Android: `ProdutosViewModel`, `EditarProdutosActivity`,
`RascunhoProduto`, `ProdutoEditavelAdapter`, `ProdutoRecomendadoAdapter`,
`RecommendedProductsApi`, `Produto`, `view_header.xml` e `strings.xml`.

A proteção de rotação cobre a recriação da Activity com a operação em andamento.
Não é uma garantia geral de idempotência após perda da resposta HTTP, encerramento do
processo ou reinstalação do aplicativo. Esses casos precisam de desenho próprio se exigidos.

### Infraestrutura e segurança já existentes

- A data do conteúdo da foto é convertida para `java.sql.Timestamp` antes do envio JDBC.
  Isso corrige o erro do driver PostgreSQL que impedia armazenar a imagem.
- As dimensões da imagem são verificadas antes de decodificar o bitmap completo,
  mantendo o limite existente de 36 milhões de pixels e o limite de tamanho do arquivo.
- Cadastro e login validam o limite de 72 bytes UTF-8 aceito pelo BCrypt. Senhas com
  muitos acentos/emojis não provocam mais a exceção de tamanho do BCrypt.
- A configuração dos testes foi transferida de `src/test/resources/application.properties`
  para a tarefa `Test` do Gradle. O arquivo antigo foi removido para evitar a falha
  de snapshot do OneDrive. Sem perfil explícito, os testes usam o perfil local;
  as classes PostgreSQL continuam com `@ActiveProfiles("postgres")`.

Não houve desativação da autenticação do aplicativo nem inclusão de chave administrativa
no APK. As senhas e as chaves locais continuam fora dos arquivos enviados ao Git.

## Testes e resultados

| Verificação | Resultado |
| --- | --- |
| Backend, suíte completa | 42 testes aprovados; 0 falhas e 0 ignorados |
| Incluídos na suíte acima, com PostgreSQL real | 14 aprovados |
| Repetição sem variáveis de banco de testes | 28 aprovados; 14 PostgreSQL ignorados por condição; build concluído |
| API real com autenticação habilitada | 71 verificações aprovadas |
| Android, testes unitários locais | 3 aprovados, incluindo 2 novos de preço/moeda |
| Android, build debug e APK de testes | Compilação concluída |
| Android Lint | 0 erros e 50 avisos |
| Android, suíte instrumentada completa no Pixel 5/API 37 | 9 testes: 8 aprovados e 1 falha fora do escopo |
| Conferência direta de compras, produtos e rotação no emulador | 10 cenários aprovados |

Os testes da conexão foram executados em banco PostgreSQL temporário, separado dos dados
de desenvolvimento. O Flyway validou as dez migrações existentes. Há um aviso de versão:
este Flyway informa suporte testado até PostgreSQL 17, enquanto a máquina usa 18.6.
Conexão, migrações e os cenários descritos passaram; isso não equivale a certificar toda
a compatibilidade dessas versões. Não foram atualizadas dependências nessa correção.

O novo teste PostgreSQL de fotos confirma o ciclo completo de envio, conteúdo criptografado
na tabela, download idêntico ao original e remoção. Há também teste de cabeçalho PNG com
dimensões enormes, que deve ser recusado antes de alocar o bitmap. Os testes de validação
de compras/produtos são repetidos tanto no perfil local quanto no PostgreSQL.
Depois da bateria completa, a execução comum foi forçada novamente com `test --rerun-tasks
--no-daemon`, sem variáveis de banco de testes. Passou sem a falha de snapshot do arquivo
de recursos do OneDrive; os 14 casos PostgreSQL foram ignorados conforme previsto.

A busca nos arquivos alterados não encontrou os valores das credenciais locais. O `.env`
continua ignorado pelo Git. O servidor temporário usou um limite de requisições elevado
somente durante a bateria extensa, sem alterar o limite configurado no projeto.

A primeira tentativa instrumentada foi interrompida porque uma janela de ANR de
`com.android.systemui` tomou o foco do emulador. Após fechá-la, a suíte foi repetida;
o resultado válido acima corresponde à repetição completa. Não era uma exceção do app.

Na conferência direta, paciente consultou recomendações e marcou/desmarcou compras com
persistência confirmada pela API. Nutricionista criou, editou e excluiu produtos pela
interface. A moeda USD foi exibida como `US$ 19,50`. O rascunho sobreviveu à rotação normal.
Com o banco temporariamente bloqueado para atrasar a resposta, salvar, girar a tela e
salvar novamente deixou **um único produto**, corrigindo a duplicação reproduzida antes.

### Única falha instrumentada restante: convites

`TelasPacienteTest.aceitarUltimoConviteMostraEstadoVazio` chega ao botão de voltar,
mas o Espresso não o clica porque só 80% de sua área está visível. O motivo confirmado
é `android:translationX="-8dp"` em `App/app/src/main/res/layout/view_header_saudacao.xml`.
Esse cabeçalho é usado por `activity_convites.xml` e `activity_buscar_paciente.xml`.
Ele é diferente de `view_header.xml`, corrigido para as telas desta integração.

Não alterei esse segundo cabeçalho nem o teste de convites: essa alteração não é
necessária aos fluxos de compras/produtos. É uma pendência para a equipe responsável;
não foi verificada a autoria individual. A falha não demonstra perda de compras ou
produtos no banco. Os testes de login, cadastro, compras, plano e perfil passaram,
mas os de plano/perfil ainda exercitam dados de exemplo, não uma integração PostgreSQL.

## Como repetir os testes

O servidor e o emulador usados nesta conferência foram encerrados. As animações e a
orientação temporárias do emulador foram restauradas. O banco temporário foi removido
com seus cadastros descartáveis de teste; ele não foi mantido como banco de apresentação.
O banco de desenvolvimento e o arquivo `.env` não foram modificados.
Para testar novamente, inicie sua API normalmente conforme os passos abaixo.

### Backend: suíte comum, sem depender do banco

No PowerShell:

```powershell
cd "C:\Users\felip\OneDrive\Documents\api\Link-Health\backend"
..\App\gradlew.bat test --no-daemon
```

Confirme `BUILD SUCCESSFUL`. As classes específicas de PostgreSQL são condicionais:
para executá-las, é necessário informar `LINK_HEALTH_TEST_DB_URL`,
`LINK_HEALTH_TEST_DB_USERNAME` e `LINK_HEALTH_TEST_DB_PASSWORD` no ambiente do terminal.
Use um banco exclusivo de testes, não o banco com os dados da apresentação.
Essas variáveis não devem ser gravadas em arquivos versionados.

### Android: testes locais e compilação

```powershell
cd "C:\Users\felip\OneDrive\Documents\api\Link-Health\App"
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

Os testes instrumentados com contas reais precisam da API em execução e de um emulador
com acesso ao endereço compilado no build debug:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Eles criam contas com dados únicos. Por isso, execute-os com a API apontando para um banco
de testes. Não colocam a chave administrativa no aplicativo.

### Conferência manual das Etapas 1 e 2

1. Inicie o PostgreSQL e a API pela pasta `backend`, com `..\App\gradlew.bat bootRun`.
   A configuração local é lida de `.env`. Confirme a mensagem de inicialização e o perfil `postgres`.
2. No Android Studio, abra a pasta `App` deste clone, compile novamente se a rede mudou
   e execute o app no emulador. O build debug incorpora o endereço detectado na compilação;
   ele não é uma implantação pública independente da rede.
3. Entre como paciente com uma conta cadastrada nesse banco. Abra a lista, marque uma
   compra, volte e reabra: a marcação deve continuar. Desmarque e confira novamente.
4. Confirme que os produtos recomendados pertencem à conta correta e que o preço/moeda
   correspondem ao que foi cadastrado.
5. Entre como nutricionista com acompanhamento ativo, abra os produtos do paciente
   pelo acesso específico a produtos recomendados e crie, edite e exclua um produto.
   Confira a mudança ao consultar a lista do paciente.
6. Digite um produto novo e gire o emulador. O rascunho deve continuar. Ao salvar,
   os controles ficam temporariamente desabilitados; depois, o mesmo registro é atualizado,
   em vez de uma nova criação por perda de ID durante a rotação.

## O que continua pendente, sem antecipar a Etapa 3

- Água, refeições, peso e fotos ainda precisam de integração Android própria e das
  verificações de vínculo/relacionamentos de paciente antes dessa integração.
  A revisão inicial confirmou que o backend de água ainda aceita paciente inexistente;
  esse defeito permanece registrado para a etapa correspondente, não foi ocultado nem corrigido aqui.
- Plano alimentar, convites, ficha geral e outras telas ainda usam dados de exemplo
  em trechos do Android. A divisão de responsáveis deve ser confirmada com a equipe;
  não é possível atribuir cada pendência a um colega só pelo nome da tela.
- A variante release ainda não tem URL de servidor HTTPS configurada. O funcionamento
  em desenvolvimento local não prova acesso pela internet de qualquer rede.
- Avisos do Lint não equivalem todos a falhas de conexão e não foram corrigidos em massa
  em arquivos de outras funcionalidades.
- Não foram verificados aparelho físico, múltiplos fabricantes, carga de produção,
  expiração real de sessão de 12 horas ou um teste de invasão externo.

As alterações ficam locais para revisão no GitHub Desktop. Não foi feito commit ou push automático.
