# Backend de contas, acompanhamentos e planos

Esta etapa prepara a parte de Victor no mesmo projeto Spring Boot criado por Felipe. Contas, login e busca estão disponíveis nos perfis `local` e `postgres`. Acompanhamentos e planos ainda ficam somente no perfil `local`, com dados em memória. A conexão com o Android será feita posteriormente.

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

## Planos alimentares

- `POST` e `GET /api/v1/follow-ups/{followUpId}/meal-plans`: cria um rascunho e lista os planos do acompanhamento.
- `GET` e `DELETE /api/v1/meal-plans/{planId}`: consulta um plano ou exclui um rascunho.
- `POST /api/v1/meal-plans/{planId}/activate`: ativa o plano.
- `POST /api/v1/meal-plans/{planId}/meals`, `PUT` e `DELETE /api/v1/meal-plans/{planId}/meals/{mealId}`: adiciona, altera e remove refeições do rascunho.
- `POST /api/v1/meal-plans/{planId}/meals/{mealId}/items`, `PUT` e `DELETE /api/v1/meal-plans/{planId}/meals/{mealId}/items/{itemId}`: adiciona, altera e remove itens de texto livre.

O acompanhamento precisa estar ativo para criar ou editar planos. A ativação exige pelo menos três refeições e ao menos um item por refeição. Ao ativar outro plano do mesmo acompanhamento, o anterior deixa de ser ativo. Planos ativos não são editados nesta estrutura; para ajustar, cria-se um novo rascunho.

Os identificadores novos são UUID para seguir as rotas e tabelas que já existem no backend. Os perfis mantêm a relação 1:1 com a conta prevista no DER; o diagrama deve ser atualizado para representar UUID e os nomes usados nas migrações.

## Verificação da persistência de contas

`PostgresAccountPersistenceTest` só executa quando `LINK_HEALTH_TEST_DB_URL` está definida. Também utiliza `LINK_HEALTH_TEST_DB_USERNAME` e, se necessária, `LINK_HEALTH_TEST_DB_PASSWORD`. Use um banco dedicado a testes: o Spring aplica as migrações Flyway e o teste grava contas fictícias. A verificação cobre os dois perfis, login, busca e rollback quando a gravação do perfil falha. Sem essas variáveis, o teste PostgreSQL é ignorado e os testes locais continuam disponíveis.
