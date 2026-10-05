# Segurança do backend Link Health

## O que foi implementado

- autenticação obrigatória por chave de API no cabeçalho X-API-Key;
- chaves mantidas fora do código, no arquivo .env ignorado pelo Git ou em
  variáveis de ambiente;
- sessões desativadas: cada requisição precisa se autenticar;
- CORS fechado por padrão; apenas origens explicitamente configuradas podem
  chamar a API a partir de um navegador;
- proteção contra muitas requisições: máximo padrão de 120 chamadas por IP a
  cada minuto;
- cabeçalhos HTTP de proteção, incluindo bloqueio de iframes, proteção contra
  content sniffing, política de conteúdo e política de permissões;
- respostas de erro sem stack trace;
- tamanho máximo de upload de 5 MB;
- validação real de JPEG e PNG, não apenas da extensão do nome;
- criptografia AES-256-GCM das fotos antes da gravação;
- verificação de integridade da foto ao descriptografar;
- arquivos locais fora do projeto e sem nomes controlados pelo usuário;
- migração V5 para guardar a foto criptografada no PostgreSQL.

## Configuração inicial

1. Copie .env.example para .env dentro da pasta backend.
2. Gere duas chaves diferentes: uma para LINK_HEALTH_API_KEY e outra para
   LINK_HEALTH_PHOTOS_ENCRYPTION_KEY.
3. Cole as chaves no .env.
4. Inicie a API.

No PowerShell, cada execução abaixo gera uma chave Base64 aleatória válida:

    $bytes = New-Object byte[] 32
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    [Convert]::ToBase64String($bytes)

Execute o comando duas vezes. A primeira saída pode ser usada como chave da API
e a segunda como chave de criptografia. Elas não devem ser mandadas no grupo,
coladas em prints ou enviadas ao GitHub.

## Como testar no Swagger

1. Inicie o backend.
2. Acesse http://localhost:8080/swagger-ui.html.
3. Clique em Authorize.
4. Informe somente o valor configurado em LINK_HEALTH_API_KEY.
5. Execute as rotas normalmente.

Sem uma chave válida, qualquer rota em /api receberá HTTP 401. A interface do
Swagger e o arquivo OpenAPI continuam visíveis para facilitar o desenvolvimento,
mas não permitem executar operações protegidas sem a chave.

## Fotos e banco de dados

No perfil local, o arquivo criptografado é guardado em
AppData/Local/LinkHealth/evolution-photos. Ele não tem extensão de imagem e não
pode ser aberto diretamente como JPEG ou PNG.

No perfil postgres, a migração V5 cria a tabela evolution_photo_files. A coluna
encrypted_content guarda somente bytes criptografados. A tabela se relaciona com
evolution_photo_records pelo identificador da foto e é apagada automaticamente
quando o registro da foto é apagado.

A chave de criptografia nunca é salva no banco. Se ela for perdida, as fotos
não poderão ser recuperadas. Se houver suspeita de vazamento, a troca segura da
chave exige um processo de recriptografia das fotos existentes.

## Configuração para produção

Antes de publicar, configure:

- SPRING_PROFILES_ACTIVE=postgres;
- LINK_HEALTH_REQUIRE_HTTPS=true;
- LINK_HEALTH_ALLOWED_ORIGINS com os domínios exatos do site, separados por
  vírgula;
- credenciais de banco com um usuário exclusivo da aplicação e permissões
  mínimas;
- backups criptografados e controle de acesso ao ambiente onde ficam as chaves.

Aplicativos Android nativos não precisam de CORS. A variável de origens é
necessária apenas para um navegador ou site que chame a API.

## Limite importante

A chave de API protege o servidor contra acesso anônimo, mas ainda não existe
neste backend uma base de usuários, login ou vínculo entre paciente e
nutricionista. Por isso, a autorização individual por paciente será a próxima
camada obrigatória: o token do usuário precisará ser associado ao paciente e às
permissões dele. Também é necessário manter HTTPS, banco protegido e segredos
fora do Git para considerar uma publicação real.
