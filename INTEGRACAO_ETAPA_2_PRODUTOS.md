# Etapa 2 — produtos recomendados

## O que foi conectado

- A tabela `recommended_products` existente continua armazenando os produtos. A migração V10 liga cada produto a um paciente em `patients`; não cria outra tabela de pacientes.
- O nutricionista entra no app, escolhe **Produtos recomendados para pacientes vinculados** e seleciona um paciente com acompanhamento ativo. Pode adicionar, alterar e remover recomendações; o aplicativo aguarda a resposta da API antes de confirmar.
- O paciente entra no app e abre a lista de compras para visualizar as recomendações associadas à própria conta e abrir o link de compra no navegador.
- O app precisa de nome, preço e link HTTP/HTTPS para salvar pela tela. O preço é em reais, com até duas casas decimais.
- O backend permite ao paciente consultar suas recomendações, mas não alterá-las. O nutricionista só acessa e altera os dados de paciente com vínculo ativo. A chave administrativa continua fora do aplicativo.

## Atenção: pacientes reais e de exemplo

A lista geral de pacientes da home do nutricionista ainda usa exemplos e não é uma lista de pacientes reais. A ficha desses exemplos também não identifica uma conta real; por isso, não deve ser usada para cadastrar as recomendações. Use o novo botão específico na home.

O convite e a aceitação de acompanhamento ainda não estão conectados às telas Android. Para o teste, use o Swagger e contas de desenvolvimento. Não associe contas sem autorização dos seus titulares.

## Preparar duas contas e o vínculo de teste

1. Inicie o PostgreSQL. No PowerShell, execute:

```powershell
cd "C:\Users\felip\OneDrive\Documents\api\Link-Health\backend"
..\App\gradlew.bat bootRun
```

2. Aguarde a mensagem `Started LinkHealthApiApplication` e mantenha o terminal aberto. A migração V10 será aplicada ao iniciar. Se houver produtos antigos apontando para pacientes inexistentes, a migração falhará: não apague dados; investigue e corrija os vínculos antes de tentar novamente.
3. Abra `http://localhost:8080/swagger-ui.html`. Em **Authorize**, informe o valor de `LINK_HEALTH_API_KEY` do seu `backend/.env`. Essa chave é privada; não compartilhe e não a coloque no GitHub.
4. Use uma conta PACIENTE existente ou registre uma em `POST /api/v1/auth/register`. Registre também uma conta `NUTRICIONISTA`, com e-mail e documento diferentes dos já cadastrados. Exemplo, trocando os valores por dados de teste inéditos:

```json
{
  "fullName": "Nutricionista Teste",
  "email": "nutricionista.teste@example.com",
  "document": "CRN-TESTE-001",
  "password": "SenhaTeste123",
  "role": "NUTRICIONISTA"
}
```

5. Anote o `id` retornado para cada conta. Se já existe, use `POST /api/v1/auth/login` com o e-mail e a senha para consultar o id. Não publique senhas ou IDs reais.
6. No Swagger, em `POST /api/v1/follow-ups`, clique **Try it out** e use os IDs obtidos:

```json
{
  "nutritionistId": "ID-DO-NUTRICIONISTA",
  "patientId": "ID-DO-PACIENTE"
}
```

7. Clique em **Execute**. Na resposta, anote o `id` do acompanhamento e confira o status `PENDENTE`. Se já existir um vínculo aberto para as mesmas contas, não crie outro; consulte os acompanhamentos existentes.
8. Em `POST /api/v1/follow-ups/{id}/response`, coloque o id do acompanhamento no campo `id` e informe no corpo:

```json
{
  "patientId": "ID-DO-PACIENTE",
  "accept": true
}
```

9. Execute e confirme `status: ATIVO`. Os textos `ID-DO-...` devem ser substituídos pelos UUIDs completos, sem as palavras do exemplo.

## Testar no Android Studio

1. Abra a pasta `C:\Users\felip\OneDrive\Documents\api\Link-Health\App` no Android Studio e execute a configuração `app` no emulador.
2. Entre como nutricionista com a conta criada. Na tela inicial, toque em **Produtos recomendados para pacientes vinculados** e escolha o paciente real da lista.
3. Toque em **Adicionar produto**, informe nome, preço e um link válido, por exemplo `https://example.com`, e toque em **Salvar alterações**. Aguarde a mensagem de confirmação.
4. Saia da conta em configurações e entre como o paciente vinculado. Abra **Lista de compras**. O produto deve aparecer na seção **Produtos recomendados**; toque em comprar para abrir o link.
5. Entre novamente como nutricionista, altere o produto e confirme que a mudança aparece para o paciente. Exclua o produto e confira que ele desaparece após reabrir a lista.
6. Para testar isolamento, outra conta de paciente não deve visualizar a recomendação, e um nutricionista sem acompanhamento ativo não deve ver aquele paciente no seletor.

Se não aparecer paciente no seletor, confirme se o acompanhamento está `ATIVO` e se você entrou com o nutricionista correto. Se aparecer erro de conexão, confira se a API continua aberta e se a configuração de rede do aplicativo alcança esse servidor. A configuração atual é de desenvolvimento: por si só não publica o servidor na internet nem garante acesso de qualquer lugar.

## Testes automáticos

```powershell
cd "C:\Users\felip\OneDrive\Documents\api\Link-Health\backend"
..\App\gradlew.bat test
```

Esses testes usam repositórios em memória por padrão. O teste adicional de PostgreSQL só roda quando as variáveis `LINK_HEALTH_TEST_DB_URL`, `LINK_HEALTH_TEST_DB_USERNAME` e `LINK_HEALTH_TEST_DB_PASSWORD` forem configuradas; ele reverte os dados de teste ao terminar. Nunca envie credenciais ao repositório.
