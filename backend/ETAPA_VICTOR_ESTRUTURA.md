# Backend de contas, acompanhamentos e planos

Esta etapa prepara a parte de Victor no mesmo projeto Spring Boot criado por Felipe. Contas, login, busca, acompanhamentos e planos estão disponíveis nos perfis `local` e `postgres`. No perfil `local`, os dados ficam em memória; no `postgres`, ficam no banco. A conexão com o Android será feita posteriormente.

## Contas e perfis

- `POST /api/v1/auth/register`: cria uma conta de `PACIENTE` ou `NUTRICIONISTA` com nome, e-mail, documento e senha. Data de nascimento e telefone são opcionais nesta estrutura.
- `POST /api/v1/auth/login`: confere e-mail e senha e devolve os dados básicos da conta.
- `GET /api/v1/patients/search?query=...`: procura pacientes por início de nome, e-mail ou CPF.

O cadastro impede e-mail ou documento duplicado e guarda a senha com bcrypt. A resposta não inclui o hash. No PostgreSQL, a migração V6 cria `user_accounts` para os dados comuns, `patients` para o CPF e `nutritionists` para o registro profissional. Cada perfil usa o UUID da conta como chave e só pode ser ligado a uma conta do tipo correspondente. Conta e perfil são gravados na mesma transação.

O perfil `postgres` usa as variáveis `LINK_HEALTH_DB_URL`, `LINK_HEALTH_DB_USERNAME` e `LINK_HEALTH_DB_PASSWORD` previstas no `.env.example`, além das chaves de segurança existentes. As rotas exigem `X-API-Key`. O login ainda não gera token ou sessão, e a API key não substitui autorização por usuário.

## Acompanhamentos

- `POST /api/v1/follow-ups`: envia convite do nutricionista para o paciente.
- `POST /api/v1/follow-ups/{id}/response`: paciente aceita ou recusa com `patientId` e `accept`.
- `PATCH /api/v1/follow-ups/{id}`: nutricionista altera objetivo e meta de água de um acompanhamento ativo.
- `POST /api/v1/follow-ups/{id}/deactivate`: nutricionista encerra um acompanhamento ativo.
- `GET /api/v1/nutritionists/{id}/follow-ups`: lista convites e acompanhamentos do nutricionista.
- `GET /api/v1/patients/{id}/follow-ups`: lista convites e acompanhamentos do paciente.

Um convite começa como `PENDENTE` e muda para `ATIVO` ou `RECUSADO`. Só é possível responder uma vez. Um vínculo ativo pode mudar para `INATIVO`. Após recusa ou inativação, o nutricionista pode enviar um novo convite. A checagem dos IDs enviados nas requisições ainda não substitui autenticação individual.

A migração V7 cria `follow_ups` com referências aos perfis de paciente e nutricionista da V6. O banco permite somente um convite pendente ou vínculo ativo para o mesmo par, preservando os anteriores recusados ou inativos. A meta de água deve ser positiva quando informada, e o objetivo aceita até 100 caracteres, conforme o DER. A listagem PostgreSQL segue a ordem de envio dos convites.

## Planos alimentares

- `POST` e `GET /api/v1/follow-ups/{followUpId}/meal-plans`: cria um rascunho e lista os planos do acompanhamento.
- `GET` e `DELETE /api/v1/meal-plans/{planId}`: consulta um plano ou exclui um rascunho.
- `POST /api/v1/meal-plans/{planId}/activate`: ativa o plano.
- `POST /api/v1/meal-plans/{planId}/meals`, `PUT` e `DELETE /api/v1/meal-plans/{planId}/meals/{mealId}`: adiciona, altera e remove refeições do rascunho.
- `POST /api/v1/meal-plans/{planId}/meals/{mealId}/items`, `PUT` e `DELETE /api/v1/meal-plans/{planId}/meals/{mealId}/items/{itemId}`: adiciona, altera e remove itens de texto livre.

O acompanhamento precisa estar ativo para criar ou editar planos. A ativação exige pelo menos três refeições e ao menos um item por refeição. Ao ativar outro plano do mesmo acompanhamento, o anterior deixa de ser ativo. Planos ativos não são editados nesta estrutura; para ajustar, cria-se um novo rascunho.

A migração V8 cria `meal_plans`, `planned_meals` e `meal_plan_items`. As refeições e os itens preservam sua ordem e são gravados junto com o plano. A exclusão de um rascunho também remove suas refeições e itens. O banco permite somente um plano ativo por acompanhamento, e a troca de plano ativo ocorre na mesma transação: se a gravação falhar, o plano anterior permanece ativo.

A configuração de CORS permite `PUT`, usado nas rotas de edição de refeições e itens, além dos métodos que já estavam disponíveis.

Os identificadores novos são UUID para seguir as rotas e tabelas que já existem no backend. Os perfis mantêm a relação 1:1 com a conta prevista no DER; o diagrama deve ser atualizado para representar UUID e os nomes usados nas migrações.

## Verificação da persistência de contas

`PostgresAccountPersistenceTest` só executa quando `LINK_HEALTH_TEST_DB_URL` está definida. Também utiliza `LINK_HEALTH_TEST_DB_USERNAME` e, se necessária, `LINK_HEALTH_TEST_DB_PASSWORD`. Use um banco dedicado a testes: o Spring aplica as migrações Flyway e o teste grava contas fictícias. A verificação cobre os dois perfis, login, busca e rollback quando a gravação do perfil falha. Sem essas variáveis, o teste PostgreSQL é ignorado e os testes locais continuam disponíveis.

`PostgresFollowUpPersistenceTest` usa a mesma configuração de teste e verifica o fluxo de convite, aceite, edição, inativação, recusa e novo convite pelas rotas. Também confere as restrições do banco para perfis incorretos, paciente inexistente, meta inválida e duplicidade de vínculo aberto.

`PostgresMealPlanPersistenceTest` verifica criação, consulta, edição, ordem das refeições e itens, exclusão em cascata, ativação e rollback da troca de plano quando a gravação falha. Também verifica a resposta de CORS para `PUT`.

## PostgreSQL local

Cada integrante pode usar seu próprio banco local. Copie `.env.example` para `.env` e configure `SPRING_PROFILES_ACTIVE=postgres`, `LINK_HEALTH_DB_URL`, `LINK_HEALTH_DB_USERNAME` e `LINK_HEALTH_DB_PASSWORD`, além das chaves de segurança já descritas no README do backend. Uma URL local típica é `jdbc:postgresql://localhost:5432/link_health`; ajuste o nome para o banco criado na sua máquina. O banco deve existir antes de iniciar o backend, e o Flyway aplica V1 a V8 automaticamente. O arquivo `.env` contém configurações locais e não deve ser commitado.

Os testes PostgreSQL devem apontar para outro banco, dedicado à verificação, por meio das variáveis `LINK_HEALTH_TEST_DB_*`. O teste pode inserir dados fictícios; não use o banco que contém os dados da apresentação.
