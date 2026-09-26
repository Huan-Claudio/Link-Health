# Backend Link Health — Tecnologias e Etapa 1

**Responsável:** Felipe Araújo Lemos  
**Escopo:** backend dos recursos de lista de compras, produtos recomendados e registros do paciente.

## Tecnologias utilizadas

- **Java 17:** linguagem do backend.
- **Spring Boot:** estrutura da API REST que o aplicativo Android consumirá.
- **Gradle:** ferramenta de compilação e gerenciamento de dependências, seguindo o padrão já existente no projeto Android.
- **Spring Web:** criação dos endpoints HTTP que recebem e devolvem JSON.
- **Spring Validation:** validação dos dados enviados pelo aplicativo.
- **OpenAPI / Swagger:** documentação interativa dos endpoints da API.
- **PostgreSQL:** banco de dados previsto para persistir os dados do sistema. A configuração foi apenas preparada; não há conexão ativa, credenciais ou banco configurado nesta etapa.
- **Git e GitHub:** controle de versão e integração com o repositório compartilhado do grupo.

## O que foi feito na etapa 1

1. Criação da pasta `backend/`, isolada do módulo Android `App/`.
2. Criação da base da API REST em Spring Boot.
3. Criação do endpoint de disponibilidade:

   ```http
   GET /api/v1/health
   ```

   Ele retorna o estado da API em JSON e serve para confirmar que o servidor está acessível.

4. Configuração da documentação da API:

   - Swagger UI: `/swagger-ui.html`
   - Contrato OpenAPI JSON: `/api-docs`

5. Criação de um padrão de respostas de erro JSON para dados inválidos ou requisições malformadas.
6. Preparação do perfil `postgres`, com variáveis de ambiente documentadas em `.env.example` e sem expor senhas.
7. Criação de um teste automatizado para o endpoint de disponibilidade.

## Próximas etapas

1. Lista de compras e produtos recomendados.
2. Registros de água, refeições e peso.
3. Fotos de evolução, após definição do serviço de armazenamento.
4. Migrações e persistência no PostgreSQL quando o banco estiver disponível.

Nenhuma funcionalidade de outros integrantes — como login, gestão de pacientes, convites ou plano alimentar completo — foi implementada nesta etapa.
