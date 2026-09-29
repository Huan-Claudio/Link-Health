# Backend de contas, acompanhamentos e planos

Esta etapa prepara a parte de Victor no mesmo projeto Spring Boot criado por Felipe. As rotas desta entrega ficam disponíveis somente no perfil `local`, com dados em memória. Elas ainda não se conectam ao Android nem ao PostgreSQL.

## Contas e perfis

- `POST /api/v1/auth/register`: cria uma conta de `PACIENTE` ou `NUTRICIONISTA` com nome, e-mail, documento e senha. Data de nascimento e telefone são opcionais nesta estrutura.
- `POST /api/v1/auth/login`: confere e-mail e senha e devolve os dados básicos da conta.
- `GET /api/v1/patients/search?query=...`: procura pacientes por início de e-mail ou CPF.

O cadastro impede e-mail ou documento duplicado e guarda a senha com bcrypt. A resposta não inclui o hash. O login ainda não gera token ou sessão, e as rotas não têm autorização por usuário. Por isso, esta versão local serve apenas para desenvolvimento com dados de teste.

## Acompanhamentos

- `POST /api/v1/follow-ups`: envia convite do nutricionista para o paciente.
- `POST /api/v1/follow-ups/{id}/response`: paciente aceita ou recusa com `patientId` e `accept`.
- `GET /api/v1/nutritionists/{id}/follow-ups`: lista convites e acompanhamentos do nutricionista.
- `GET /api/v1/patients/{id}/follow-ups`: lista convites e acompanhamentos do paciente.

Um convite começa como `PENDENTE` e muda para `ATIVO` ou `RECUSADO`. Só é possível responder uma vez. Após a recusa, o nutricionista pode enviar um novo convite. A checagem do `patientId` na resposta ainda não substitui autenticação.

Os identificadores novos são UUID para seguir as rotas que já existem no backend. O DER enviado pela equipe usa IDs numéricos; a decisão final sobre IDs e banco deve ser fechada antes das migrações da sua parte.
