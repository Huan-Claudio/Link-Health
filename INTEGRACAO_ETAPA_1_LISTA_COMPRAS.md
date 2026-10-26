# Integração — Etapa 1: lista de compras

> Registro da entrega inicial. A conexão ampliada, incluindo edição da lista e registros do paciente, está em [Integração do escopo completo](INTEGRACAO_ETAPA_3_ESCOPO_COMPLETO.md).

Esta é a primeira etapa de conexão da parte de lista de compras. Ela foi concluída de ponta a ponta: banco PostgreSQL, API Java e tela Android do paciente.

## Entrega desta etapa

- O cadastro e o login Android usam a API para criar e validar uma conta real.
- Após o login, a sessão guarda o identificador do usuário e um token temporário. O token não é uma chave de administrador e não é salvo no repositório.
- A tela **Lista de compras** busca os itens do paciente autenticado pela API, em vez de usar a lista simulada.
- Ao marcar ou desmarcar um item, a tela atualiza o banco pela API. A interface só confirma a alteração depois da resposta bem-sucedida do servidor.
- A migration `V9__link_shopping_items_to_patients.sql` cria a chave estrangeira entre `shopping_items.patient_id` e `patients.user_id`. Assim, cada item pertence a um paciente existente; ao remover o paciente, seus itens também são removidos.

## Proteções aplicadas

- A senha do PostgreSQL e a chave interna da API ficam somente em `backend/.env`, arquivo ignorado pelo Git.
- O APK não recebe a chave interna da API. O login retorna um token aleatório de curta duração (12 horas), guardado no servidor somente como hash.
- Um paciente com token só pode consultar e alterar a própria lista. Uma tentativa de acessar a lista de outro paciente recebe `403 Forbidden`.
- As rotas de lista sem credencial recebem `401 Unauthorized`.
- O endpoint de criação de item continua protegido pela chave interna, pois a tela de criação/edição do nutricionista não faz parte desta etapa.

## O que não pertence à Etapa 1

- Produtos recomendados exibidos abaixo da lista permanecem dados visuais da tela; a conexão deles é da Etapa 2.
- Criação e edição da lista pelo nutricionista, vínculos clínicos, registros alimentares, água e fotos serão conectados nas próximas etapas.
- O token atual é mantido em memória: ao encerrar o aplicativo ou reiniciar a API, é necessário entrar novamente. Persistência de sessão/renovação de token é uma melhoria futura.

## Como iniciar a API

1. Verifique que o PostgreSQL está iniciado e que o arquivo `backend/.env` existe no seu computador. Não envie esse arquivo ao GitHub.
2. Abra o PowerShell e execute:

```powershell
cd "C:\Users\felip\OneDrive\Documents\api\Link-Health\backend"
..\App\gradlew.bat bootRun --no-daemon
```

3. Se o PowerShell não localizar o Java, execute antes do último comando:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.11"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

4. Espere aparecer no terminal que a aplicação iniciou na porta `8080`. As migrations do Flyway criarão ou atualizarão as tabelas automaticamente.

## Teste manual completo

1. Com a API aberta, acesse `http://localhost:8080/swagger-ui.html`.
2. Em `POST /api/v1/auth/register`, crie uma conta com `role` igual a `PACIENTE`. Copie o `id` retornado pela API.
3. Clique em **Authorize** no Swagger e informe a chave configurada em `LINK_HEALTH_API_KEY` dentro do seu arquivo local `backend/.env`. Essa chave é apenas para desenvolvimento e Swagger; não coloque a chave no Android.
4. Em `POST /api/v1/patients/{patientId}/shopping-items`, cole o `id` da conta na URL e envie, por exemplo:

```json
{
  "name": "Aveia",
  "quantity": 1,
  "unit": "UNIT"
}
```

5. Abra `App` no Android Studio, escolha um emulador e rode a variante `debug`.
6. Entre usando o mesmo e-mail e senha do passo 2. A tela de lista deve mostrar **Aveia**.
7. Toque no item para marcar como comprado. Feche e abra a tela novamente: o estado precisa continuar marcado. No Swagger, o `GET` da mesma rota também deve mostrar `"purchased": true`.

No emulador, `10.0.2.2:8080` aponta para a API no seu computador. Em aparelho físico essa URL não funciona; será necessária uma URL HTTPS ou o IP local da máquina com uma configuração própria.

## Teste automatizado do backend

O teste `ShoppingMobileSessionControllerTest` cobre criação da sessão móvel, inclusão/listagem de item, atualização de compra e bloqueio de acesso entre pacientes. Para executá-lo:

```powershell
cd "C:\Users\felip\OneDrive\Documents\api\Link-Health\backend"
..\App\gradlew.bat test --tests "br.edu.pucgoias.linkhealth.ShoppingMobileSessionControllerTest" --no-daemon
```
