# Backend Link Health — Etapa 2 concluída

**Responsável:** Felipe Araújo Lemos

## Entrega realizada

Foram implementados os recursos de **lista de compras** e **produtos recomendados**, ambos vinculados a um paciente pelo `patientId` presente na rota.

### Lista de compras

- consulta dos itens de um paciente;
- inclusão de item com nome, quantidade e unidade;
- edição de nome, quantidade ou unidade;
- marcação e desmarcação de item como comprado;
- remoção de item;
- validação dos campos obrigatórios e da quantidade maior que zero.

Unidades aceitas: `UNIT`, `GRAM`, `KILOGRAM`, `MILLILITER`, `LITER`, `PACKAGE` e `BOX`.

### Produtos recomendados

- consulta dos produtos recomendados para um paciente;
- inclusão, edição e remoção;
- nome, descrição opcional, preço opcional, moeda e URL externa de compra;
- validação do preço, da moeda e do link de compra.

### Persistência e integração futura

- modelos de domínio e repositórios criados;
- repositórios locais em memória no perfil padrão, para a API funcionar sem banco;
- entidades e adaptadores JPA preparados para PostgreSQL;
- migração Flyway `V1__create_shopping_items_and_recommended_products.sql` criada;
- nenhum dado de saúde ou credencial é registrado em logs;
- autenticação e permissão por usuário ainda dependem da parte compartilhada do projeto. Por enquanto, o `patientId` é recebido na rota.

## Como testar no Swagger

1. Execute `..\App\gradlew.bat bootRun` na pasta `backend`.
2. Abra `http://localhost:8080/swagger-ui.html`.
3. Escolha uma rota e use um UUID de teste como `patientId`.

Exemplo de item:

```json
{
  "name": "Aveia em flocos",
  "quantity": 2,
  "unit": "PACKAGE"
}
```

Exemplo de produto recomendado:

```json
{
  "name": "Iogurte natural",
  "description": "Sem açúcar adicionado",
  "price": 8.99,
  "currency": "BRL",
  "purchaseUrl": "https://exemplo.com/produto"
}
```
