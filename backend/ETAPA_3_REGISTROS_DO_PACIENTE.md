# Backend Link Health — Etapa 3 concluída

**Responsável:** Felipe Araújo Lemos

## Entrega realizada

Foram implementados os registros diários e de evolução do paciente: água, refeições realizadas e peso.

### Água

- inclusão de consumo em mililitros;
- consulta de todos os consumos ou filtragem por data (`?date=yyyy-MM-dd`);
- edição e remoção de registro;
- validação de quantidade entre 1 e 10.000 ml.

### Refeições realizadas

- inclusão, consulta, edição e remoção;
- nome da refeição, horário de consumo e observação opcional;
- campo opcional `mealPlanItemId`, pronto para vincular à parte de plano alimentar quando ela existir.

### Peso

- inclusão, consulta, edição e remoção;
- peso em quilogramas, data/hora da medição e observação opcional;
- validação de 1 a 500 kg.

## Persistência

- repositórios locais em memória para uso sem banco no perfil padrão;
- entidades e repositórios PostgreSQL preparados;
- migração `V2__create_patient_records.sql` criada para água, refeições e peso;
- nenhum dado de saúde é colocado em logs pela API.

## Dependências que continuam fora deste escopo

- autenticação, autorização e vínculo nutricionista-paciente;
- criação do plano alimentar; o campo de vínculo da refeição está preparado, mas não cria planos;
- fotos de evolução, que serão a etapa 4 após a definição do armazenamento.
