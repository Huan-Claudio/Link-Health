# Link Health API

Backend REST do Link Health, separado do aplicativo Android em `App/`.

## Recursos implementados

- API HTTP com Spring Boot;
- validação de requisições e respostas de erro JSON consistentes;
- documentação OpenAPI em `/swagger-ui.html` e contrato JSON em `/api-docs`;
- perfil `postgres` preparado por variáveis de ambiente, sem conexão ativada por padrão;
- lista de compras e produtos recomendados por paciente.
- registros de água, refeições realizadas e peso por paciente.

No perfil `local` (padrão), os dados ficam somente em memória para permitir a integração inicial sem banco. Ao ativar o perfil `postgres`, a API usa PostgreSQL e executa as migrações do Flyway.

## Rotas da etapa 2

As rotas usam o identificador do paciente até que a autenticação e o vínculo nutricionista-paciente sejam entregues pela parte responsável.

| Método | Rota | Função |
| --- | --- | --- |
| `GET` | `/api/v1/patients/{patientId}/shopping-items` | Lista itens de compra |
| `POST` | `/api/v1/patients/{patientId}/shopping-items` | Adiciona item |
| `PATCH` | `/api/v1/patients/{patientId}/shopping-items/{itemId}` | Edita item |
| `PATCH` | `/api/v1/patients/{patientId}/shopping-items/{itemId}/purchase-status` | Marca/desmarca como comprado |
| `DELETE` | `/api/v1/patients/{patientId}/shopping-items/{itemId}` | Remove item |
| `GET` | `/api/v1/patients/{patientId}/recommended-products` | Lista produtos recomendados |
| `POST` | `/api/v1/patients/{patientId}/recommended-products` | Adiciona produto |
| `PATCH` | `/api/v1/patients/{patientId}/recommended-products/{productId}` | Edita produto |
| `DELETE` | `/api/v1/patients/{patientId}/recommended-products/{productId}` | Remove produto |
| `GET/POST` | `/api/v1/patients/{patientId}/water-intakes` | Consulta/registra consumo de água |
| `PATCH/DELETE` | `/api/v1/patients/{patientId}/water-intakes/{intakeId}` | Edita/remove registro de água |
| `GET/POST` | `/api/v1/patients/{patientId}/meal-records` | Consulta/registra refeições realizadas |
| `PATCH/DELETE` | `/api/v1/patients/{patientId}/meal-records/{recordId}` | Edita/remove registro de refeição |
| `GET/POST` | `/api/v1/patients/{patientId}/weight-records` | Consulta/registra peso |
| `PATCH/DELETE` | `/api/v1/patients/{patientId}/weight-records/{recordId}` | Edita/remove registro de peso |

O endpoint de água aceita opcionalmente `?date=yyyy-MM-dd`, com a data interpretada em UTC.

## Executar localmente

Requer Java 17 ou superior. A partir desta pasta, use o Gradle Wrapper já versionado no projeto Android:

```powershell
..\App\gradlew.bat test
..\App\gradlew.bat bootRun
```

Com a aplicação ativa, consulte `http://localhost:8080/swagger-ui.html`.

## PostgreSQL futuro

O perfil padrão não configura nem abre conexão com banco. Quando o banco for disponibilizado, copie `.env.example`, defina as variáveis sem incluí-las no Git e ative o perfil `postgres`. A inclusão de JPA, Flyway e das migrações ocorrerá junto aos primeiros recursos persistidos.
