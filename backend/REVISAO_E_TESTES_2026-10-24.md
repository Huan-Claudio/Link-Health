# Revisão e testes — 24/10/2026

> Diagnóstico inicial, antes das correções autorizadas. Os resultados abaixo são históricos.
> Para o estado posterior, consulte [Correções e validação](CORRECOES_E_VALIDACAO_2026-10-24.md).

Projeto: Link Health. Commit revisado: 62ef137, na branch main.

## Resultado

Os fluxos normais já integrados de lista de compras e produtos recomendados funcionam. A revisão encontrou problemas que impedem considerar todo o projeto aprovado, especialmente no envio de fotos ao PostgreSQL, na validação de entradas e nos testes de tela.

Esta revisão não altera funcionalidades. O único arquivo novo no repositório é este relatório. Não houve commit ou push.

## Ambiente e preservação dos dados

- Windows, Java 25.0.1, Gradle 9.5.0 e PostgreSQL 18.6.
- Testes de banco e testes pela API executados em um banco temporário exclusivo desta revisão. Os cadastros e acompanhamentos do banco de desenvolvimento não foram usados como dados de teste.
- O banco de desenvolvimento foi consultado apenas para verificar as migrações e as chaves estrangeiras já aplicadas.
- Ao terminar, os servidores de teste foram encerrados, as configurações temporárias do emulador foram restauradas e o banco temporário foi removido. Os dados de desenvolvimento foram preservados.
- Aplicativo debug executado no emulador Pixel 5 com Android API 37.
- Credenciais lidas do arquivo local ignorado pelo Git. A busca nos arquivos de texto versionados não encontrou os valores locais da senha do banco, da chave administrativa ou da chave de criptografia.
- Para a bateria extensa de requisições, o servidor temporário usou limite de 10.000 requisições/minuto. O limitador foi verificado separadamente em outra instância com limite de 3, sem alterar a configuração do projeto.

## Testes existentes

| Verificação | Resultado |
| --- | --- |
| Backend: suíte completa | 31 testes, 31 aprovados, nenhum ignorado |
| Testes específicos de PostgreSQL incluídos na suíte | 8 aprovados |
| Empacotamento executável do backend | bootJar gerado com sucesso |
| Android: teste unitário local | 1 aprovado; é o teste de exemplo do projeto |
| Compilação Android debug e APK de testes | Sucesso |
| Android Lint | 0 erros, 72 avisos |
| Android: suíte instrumentada existente | 9 executados, 2 aprovados e 7 falharam |
| Android: verificações diretas da integração e de casos especiais | 10 verificações, 8 aprovadas; falhas de moeda e de duplicação durante rotação |
| Migrações Flyway | V1 a V10 reconhecidas; criação de base nova e validação concluídas |

## Fluxos integrados conferidos diretamente no emulador

Com contas temporárias criadas para a revisão, também foram conferidos os fluxos pela interface, consultando a API/banco para verificar a persistência:

- Paciente entra com autenticação real e vê os produtos da própria conta.
- Marcar e desmarcar compra pelo aplicativo altera os dados persistidos; reabrir a tela mantém a marcação.
- Nutricionista escolhe seu paciente vinculado, cria, edita e remove um produto pelo aplicativo.
- Rascunho sem salvamento em andamento é preservado após girar a tela.
- O teste de moeda estrangeira revelou exibição incorreta de R$ para USD.
- A rotação durante um salvamento pendente revelou a possibilidade de criação duplicada, detalhada abaixo.

Essas verificações da conexão real são separadas da suíte Espresso antiga, que ainda contém expectativas de MockData.

A execução instrumentada inicial foi prejudicada por uma janela “System UI isn't responding” do próprio emulador. Ela foi fechada, as animações foram desativadas temporariamente e os nove testes foram repetidos. O resultado continuou em 2 aprovados e 7 falhas, agora com causas identificáveis nos testes e no layout.

As sete falhas da segunda execução:

1. **loginVazioMostraErros:** espera “Campo obrigatório” no e-mail, enquanto a implementação atual valida o formato de e-mail.
2. **cadastroAbreHome:** verifica a home imediatamente após uma operação HTTP assíncrona e usa dados fixos, sem preparar um estado de banco independente.
3. **loginAbreHomeEAguaNaoFicaNegativa:** usa uma conta fixa que não é criada pelo teste e também não espera a autenticação HTTP terminar.
4. **evolucaoAbreEVoltaParaHome:** o Espresso recusa clicar na seta de voltar porque menos de 90% da área dela está visível.
5. **aceitarUltimoConviteMostraEstadoVazio:** esbarra no mesmo problema de visibilidade da seta de voltar.
6. **planoAtualizaResumoAoVoltar:** esbarra no mesmo problema de visibilidade da seta de voltar.
7. **compraContinuaMarcadaAoReabrir:** entra com uma sessão de exemplo sem token e procura uma compra do MockData em uma tela que agora exige dados reais do backend.

Arquivos: App/app/src/androidTest/java/br/edu/pucgoias/app/FluxoPacienteTest.java, PerfilPacienteTest.java e TelasPacienteTest.java. O recorte da seta vem de android:translationX="-8dp" em App/app/src/main/res/layout/view_header.xml:18. Não é correto tratar essas sete falhas como sete quebras diferentes na conexão com o banco.

## Verificações adicionais pela API real

Foram executados 71 cenários HTTP com o perfil postgres, usando a segurança habilitada. Após considerar 401/403 como respostas válidas de bloqueio nos endpoints ainda exclusivos da chave administrativa, 62 cenários atenderam à expectativa e 9 expuseram os problemas detalhados abaixo.

Entre os cenários aprovados estão:

- Registro de pacientes e nutricionistas, emissão de tokens, rejeição de cadastro duplicado e senha incorreta.
- Rejeição de chave administrativa incorreta, token forjado e requisições anônimas.
- Compras: criar, listar, editar, marcar, consultar a persistência, desmarcar e excluir.
- Recomendações: criar, listar, editar e excluir; paciente pode ler seus produtos, mas não pode modificá-los.
- Bloqueio de outro paciente e de nutricionista sem acompanhamento ativo.
- Convite pendente não concede acesso; vínculo ativo concede; vínculo desativado revoga.
- Gravação, edição e exclusão de água, refeições e peso via chave administrativa no PostgreSQL.
- Swagger e documento OpenAPI disponíveis.

Verificações separadas: origem CORS não autorizada recebeu 403; com limite de três requisições, as respostas foram 200, 200, 200 e 429, com Retry-After: 60. Um teste adicional de senha com caracteres multibyte encontrou mais um erro de validação, descrito abaixo.

## Problemas confirmados

### P1 — Envio de foto falha no PostgreSQL

Criar o registro de uma foto PNG funciona, mas enviar seu conteúdo retorna HTTP 500. O teste existente de foto usa o armazenamento local; ele não verifica essa implementação PostgreSQL.

Em backend/src/main/java/br/edu/pucgoias/linkhealth/infrastructure/postgres/PostgresEvolutionPhotoStorage.java:48, o JdbcTemplate recebe Instant.now() como argumento SQL sem conversão. Uma verificação direta com o driver PostgreSQL confirmou SQLState 07006: ele não infere um tipo SQL para java.time.Instant.

Ajuste indicado: converter a data para um tipo aceito pelo JDBC, por exemplo Timestamp.from(...), e acrescentar um teste PostgreSQL do ciclo de envio, consulta e remoção da imagem. O teste unitário AES-GCM passou, inclusive a rejeição de conteúdo adulterado; isso não substitui o teste do armazenamento no banco.

### P2 — Dados inválidos viram erro interno

Receberam HTTP 500:

- UUID inválido na URL de compras.
- Quantidade maior que a capacidade NUMERIC(12,3) da tabela.
- Preço maior que a capacidade NUMERIC(12,2) de produtos.
- Cadastro de produto apontando para um paciente inexistente: o banco protege a relação, mas a API não traduz a violação em erro de entrada.

Arquivos: backend/src/main/java/br/edu/pucgoias/linkhealth/api/error/ApiExceptionHandler.java:76, DTOs de compras e produtos, e os respectivos serviços/repositórios.

Ajustes indicados: validar precisão/escala numérica, tratar erros de conversão dos parâmetros e responder 400/404 quando o paciente não existe. A resposta atual não revela a mensagem técnica do banco, mas dificulta identificar a causa.

### P2 — Corpo vazio pode alterar o estado de uma compra

PATCH .../shopping-items/{id}/purchase-status aceita {} e responde 200. O boolean primitivo vira false quando purchased não é enviado.

Arquivo: backend/src/main/java/br/edu/pucgoias/linkhealth/api/shopping/PurchaseStatusRequest.java:3.

Ajuste indicado: Boolean obrigatório com validação e @Valid no controller. O teste atual confirmou a aceitação do corpo incompleto; uma requisição assim pode desmarcar um item já comprado.

### P2 — Produto pode ficar sem nome após edição

PATCH de produto com {"name":"   "} responde 200; o serviço remove os espaços e persiste um nome vazio. @Size verifica o comprimento antes dessa normalização.

Arquivos: backend/src/main/java/br/edu/pucgoias/linkhealth/api/product/UpdateRecommendedProductRequest.java:10 e service/RecommendedProductService.java.

Ajuste indicado: manter os campos parciais opcionais, mas exigir pelo menos um caractere não branco quando name for informado.

### P2 — Link aceito pelo backend não é aceito pelo aplicativo

O backend aceita ftp://example.org/item e cria o produto com 201. O aplicativo só abre HTTP/HTTPS e recusa esse link.

Arquivos: DTOs CreateRecommendedProductRequest e UpdateRecommendedProductRequest, e App/app/src/main/java/br/edu/pucgoias/app/ui/paciente/ListaComprasActivity.java.

Ajuste indicado: padronizar a validação dos esquemas HTTP/HTTPS no servidor e no aplicativo. O teste não demonstrou execução de um esquema perigoso; demonstrou uma incompatibilidade entre os contratos.

### P2 — Registros de água podem apontar para paciente inexistente

POST de água com um UUID de paciente inexistente respondeu 201. A migração V2 não cria chaves estrangeiras de paciente para água, refeições e peso; a V3 também não cria essa relação para registros de fotos.

As tabelas de compras e produtos já possuem essa proteção, adicionada pelas migrações V9 e V10.

Ajuste indicado antes da integração desses registros: verificar dados órfãos, validar a conta e acrescentar as relações necessárias sem apagar dados existentes.

### P2 — Senha multibyte pode causar erro 500

Uma senha com 40 letras acentuadas, correspondendo a 80 bytes UTF-8, passou pela validação de tamanho em caracteres e recebeu HTTP 500 ao chegar ao BCrypt.

Arquivos: backend/src/main/java/br/edu/pucgoias/linkhealth/api/account/RegisterAccountRequest.java:16 e service/UserAccountService.java:50.

Ajuste indicado: validar o limite de bytes aceito pelo BCrypt, com mensagem clara, no cadastro e no login. Não truncar a senha silenciosamente.

### P2 — Moeda da recomendação não é respeitada no Android

A API permite moedas de três letras, mas o cliente descarta currency e o adapter fixa R$. Uma recomendação com currency USD foi exibida no emulador como R$ 19,5.

Arquivos: App/app/src/main/java/br/edu/pucgoias/app/data/remote/RecommendedProductsApi.java e ui/adapter/ProdutoRecomendadoAdapter.java:46.

Ajuste indicado: ou restringir o contrato a BRL, se essa for a regra do projeto, ou preservar a moeda no modelo e formatar o valor corretamente.

### P2 — Girar a tela durante o salvamento pode duplicar produto

Foi reproduzido no emulador com um atraso controlado no banco temporário: salvar um produto novo, girar a tela antes da resposta e salvar novamente depois. O PostgreSQL ficou com dois registros de mesmo nome e preço.

O callback da Activity antiga retorna ao perceber isDestroyed antes de associar o id confirmado pelo servidor ao produto. O estado salvo ainda tem id nulo e o conjunto pendentes também não sobrevive à recriação. A nova tentativa usa POST em vez de atualizar a criação já confirmada.

Arquivo: App/app/src/main/java/br/edu/pucgoias/app/ui/nutricionista/EditarProdutosActivity.java:118, em conjunto com onSaveInstanceState e RecommendedProductsApi.save.

Ajuste indicado: manter a operação e seu resultado em um ViewModel/repositório que sobreviva à recriação da tela, reconciliar o registro confirmado e considerar uma chave de idempotência para criação. O teste de rascunho sem requisição em andamento passou; a falha ocorre na operação pendente.

## Integrações ainda pendentes

A leitura do código confirmou dados de exemplo nas telas de água/resumo, plano alimentar, convites, ficha geral de pacientes, registro diário, perfil e fotos de evolução. As fotos tiradas pela tela atual são guardadas na estrutura MockData; o Android ainda não faz upload delas para a API.

Isso é trabalho pendente das etapas de integração, não uma confirmação de que essas telas já salvam no PostgreSQL. O acesso real aos produtos recomendados está no botão específico de pacientes vinculados da home do nutricionista.

A variante release está com BACKEND_BASE_URL vazio. A variante debug incorpora um endereço HTTP calculado ao compilar. Assim, a versão atual foi avaliada como desenvolvimento local, não como aplicativo já publicado com servidor HTTPS disponível pela internet.

## Próximos ajustes sugeridos

1. Corrigir o armazenamento de fotos no PostgreSQL e cobrir esse ciclo com teste de persistência.
2. Alinhar validações e tratamento de erros, incluindo pacientes inexistentes, purchased obrigatório, nome e senha.
3. Corrigir os testes de tela: preparar contas reais, esperar HTTP, retirar expectativas de MockData da tela de compras e ajustar a área da seta de voltar.
4. Padronizar moeda e link dos produtos.
5. Preservar e reconciliar operações de salvamento durante a recriação da tela, evitando duplicações.
6. Continuar as integrações previstas, com relações de banco e autorização próprias para água, refeições, peso e fotos.

Os testes não cobrem aparelho físico, outros fabricantes, uma implantação pública HTTPS, expiração real de 12 horas, grande volume de usuários ou um teste de invasão externo. A revisão verifica o código e os cenários executados neste ambiente; não certifica todos os estados possíveis do aplicativo.

