# Backend Link Health - Etapa 4 (parte 1)

**Responsável:** Felipe Araújo Lemos

## Entrega realizada

Foi criada a base para fotos de evolução corporal. Nesta primeira parte, a API
registra e consulta os metadados da foto, sem receber ou guardar os bytes da
imagem. Isso permite integrar a tela sem fingir que uma foto sensível já foi
armazenada.

### O que está disponível

- criação de um registro pendente de foto por paciente;
- listagem dos registros em ordem da foto mais recente para a mais antiga;
- exclusão de um registro pendente;
- validação de nome do arquivo, tipo de imagem (JPEG ou PNG), data e observação;
- status explícito PENDING_UPLOAD;
- repositório local para testes sem banco;
- entidade, adaptador PostgreSQL e migração Flyway V3 preparados;
- teste automatizado do fluxo de criação, consulta e exclusão.

## Rotas

| Método | Rota | Função |
| --- | --- | --- |
| GET | /api/v1/patients/{patientId}/evolution-photos | Lista os metadados das fotos de evolução |
| POST | /api/v1/patients/{patientId}/evolution-photos | Cria um registro pendente de foto |
| DELETE | /api/v1/patients/{patientId}/evolution-photos/{photoId} | Remove um registro pendente |

Exemplo para criar o registro:

{
  "originalFileName": "evolucao-frontal.jpg",
  "contentType": "image/jpeg",
  "notes": "Foto frontal para acompanhamento"
}

## Continuação

A segunda metade foi entregue no documento ETAPA_4_FOTOS_EVOLUCAO_PARTE_2.md.
Ela acrescenta o envio real do arquivo, consulta e exclusão coordenada.
