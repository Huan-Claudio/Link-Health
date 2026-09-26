# Link Health API

Backend REST do Link Health, separado do aplicativo Android em `App/`.

## Etapa 1 — fundação

- API HTTP com Spring Boot;
- validação de requisições e respostas de erro JSON consistentes;
- documentação OpenAPI em `/swagger-ui.html` e contrato JSON em `/api-docs`;
- perfil `postgres` preparado por variáveis de ambiente, sem conexão ativada por padrão.

Nesta etapa ainda não há entidades, repositórios, migrações nem endpoints de domínio. Eles serão acrescentados nos próximos passos, somente para a parte de Felipe Araújo Lemos.

## Executar localmente

Requer Java 17 ou superior. A partir desta pasta, use o Gradle Wrapper já versionado no projeto Android:

```powershell
..\App\gradlew.bat test
..\App\gradlew.bat bootRun
```

Com a aplicação ativa, consulte `http://localhost:8080/swagger-ui.html`.

## PostgreSQL futuro

O perfil padrão não configura nem abre conexão com banco. Quando o banco for disponibilizado, copie `.env.example`, defina as variáveis sem incluí-las no Git e ative o perfil `postgres`. A inclusão de JPA, Flyway e das migrações ocorrerá junto aos primeiros recursos persistidos.
