# Backend Link Health - Etapa 4 (parte 2)

**Responsável:** Felipe Araújo Lemos

## Entrega realizada

A segunda parte conclui o fluxo de fotos de evolução no ambiente de
desenvolvimento. O aplicativo primeiro cria os metadados da foto e depois envia
o arquivo para o registro criado. Quando o envio é concluído, o status passa de
PENDING_UPLOAD para STORED.

## O que foi acrescentado

- recebimento de arquivo no formato multipart/form-data, no campo file;
- aceitação somente de imagens JPEG e PNG;
- limite de 5 MB por imagem;
- verificação da assinatura do arquivo e da possibilidade de abrir a imagem;
- bloqueio de imagens com mais de 36 milhões de pixels;
- armazenamento local criptografado, fora do repositório e fora do OneDrive;
- nome físico gerado pela API com os identificadores do paciente e da foto;
- endpoint para visualizar a imagem pelo backend;
- remoção do arquivo ao apagar seu registro;
- coluna storage_key e status STORED na migração V4 do PostgreSQL;
- tabela evolution_photo_files com bytes criptografados na migração V5;
- testes automatizados para envio válido, leitura, exclusão e rejeição de
  arquivo falso.

## Rotas da segunda parte

| Método | Rota | Função |
| --- | --- | --- |
| POST | /api/v1/patients/{patientId}/evolution-photos/{photoId}/content | Envia a imagem no campo file |
| GET | /api/v1/patients/{patientId}/evolution-photos/{photoId}/content | Consulta a imagem armazenada |

O endpoint DELETE já existente agora também remove o arquivo associado, quando
a foto já estiver armazenada.

## Ordem de uso pela tela

1. A tela envia os dados da foto para POST /evolution-photos.
2. A API devolve o identificador da foto e o status PENDING_UPLOAD.
3. A tela envia o arquivo JPEG ou PNG para POST /{photoId}/content usando
   multipart/form-data e o campo file.
4. A API devolve o registro com status STORED.
5. A tela pode usar GET /{photoId}/content para mostrar a imagem.

## Onde os arquivos ficam

No perfil local, o padrão é a pasta AppData/Local/LinkHealth/evolution-photos
do usuário que iniciou a API. Os arquivos nessa pasta são criptografados e
podem ser alterados pela variável LINK_HEALTH_PHOTOS_DIRECTORY. No perfil
postgres, o arquivo criptografado é salvo na tabela evolution_photo_files.
Nenhuma imagem é salva dentro do projeto nem deve entrar no Git.

## Limite atual

O backend agora exige uma chave de API e criptografa as fotos. Antes de publicar
o sistema, ainda é necessário implementar login e autorização por
paciente/nutricionista, além de configurar HTTPS, permissões mínimas no banco
e o ambiente de produção. A documentação SEGURANCA_BACKEND.md detalha essa
configuração.
