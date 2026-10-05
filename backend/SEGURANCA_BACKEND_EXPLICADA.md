# Segurança do backend explicada

## 1. O que muda para quem usa o aplicativo

Para o paciente ou nutricionista, a resposta é: nada na tela e nada na rotina.
Ele continua registrando água, refeições, peso, compras e fotos normalmente.
Não deve ser pedido que ele digite uma chave técnica, configure segurança ou
faça uma etapa extra por causa deste backend.

A proteção acontece por trás do aplicativo:

1. O aplicativo faz a requisição como sempre.
2. A camada técnica acrescenta o cabeçalho X-API-Key sem mostrar isso na tela.
3. O backend valida a requisição antes de entregar ou alterar os dados.
4. Se a requisição for aceita, a tela recebe a resposta normal.

Portanto, a chave de API é responsabilidade da integração técnica do Android,
não do paciente.

## 2. Uma pequena integração técnica ainda necessária no Android

Quando a tela Android começar a chamar este backend, o código de rede precisa
incluir automaticamente X-API-Key em todas as chamadas. Isso é feito uma única
vez no cliente de rede, por exemplo em um interceptor do Retrofit ou OkHttp, e
não em cada tela. Depois disso, as telas continuam iguais.

Importante: uma chave fixa dentro de um aplicativo Android pode ser extraída por
alguém que analise o APK. Ela ajuda a bloquear chamadas aleatórias e é adequada
para a fase atual de integração, mas não substitui login individual. Quando a
equipe entregar cadastro e login, a próxima etapa deve usar token de usuário
com permissões de paciente e nutricionista. Esse token substituirá a chave fixa
na autorização das rotas.

## 3. Camadas de segurança já implementadas

| Camada | Tecnologia | O que faz | Efeito para o usuário |
| --- | --- | --- | --- |
| Entrada na API | Spring Security e X-API-Key | Exige uma chave antes de aceitar rotas da API | Invisível |
| Comparação da chave | MessageDigest.isEqual | Compara a chave de modo resistente a comparação por tempo | Invisível |
| Estado do servidor | Sessão stateless | Não cria sessão web nem cookie de login no servidor | Invisível |
| Navegador externo | CORS restrito | Bloqueia sites não autorizados de chamar a API pelo navegador | Invisível no Android |
| Ataques automatizados | Limite por IP | Freia excesso de chamadas em um minuto | Só afeta abuso |
| Respostas HTTP | Spring Security Headers | Adiciona proteção contra iframe, content sniffing e referências indevidas | Invisível |
| Upload | Spring Multipart e ImageIO | Aceita somente JPEG ou PNG reais, até 5 MB | Mensagem clara se o arquivo for inválido |
| Fotos no armazenamento | AES-256-GCM | Criptografa a foto e verifica se ela foi alterada | Invisível |
| Fotos no PostgreSQL | BYTEA e Flyway | Guarda bytes criptografados e cria a estrutura do banco por migração | Invisível |
| Erros | ApiExceptionHandler | Não devolve rastros internos do servidor para quem chamou a API | Invisível |
| Transporte em produção | HTTPS configurável | Obriga conexão segura quando LINK_HEALTH_REQUIRE_HTTPS for verdadeiro | Invisível |

## 4. Explicação simples de cada tecnologia

### Spring Security

É a biblioteca que fica na porta de entrada da API. Ela analisa a chamada antes
dos controllers de água, refeições, produtos ou fotos. Assim, um pedido sem
credencial válida não chega à regra de negócio.

### X-API-Key

É um valor secreto enviado no cabeçalho técnico da requisição. Ele não aparece
na tela. No Swagger, somente quem está testando precisa informar a chave pelo
botão Authorize.

### AES-256-GCM

É a criptografia usada nas fotos. AES-256 protege o conteúdo; GCM também
detecta alteração ou corrupção dos dados. Uma foto copiada diretamente da
pasta local ou da tabela do banco não abre como imagem sem a chave correta.

### PostgreSQL BYTEA

BYTEA é o tipo de coluna do PostgreSQL usado para guardar bytes. A migração V5
cria a tabela evolution_photo_files. Ela recebe bytes já criptografados, e não
o JPEG ou PNG original em texto aberto.

### Flyway

É a ferramenta que organiza alterações no banco. Quando o PostgreSQL for
configurado, as migrações V1 até V5 criam as tabelas na ordem correta. Isso
evita criar banco manualmente e esquecer alguma estrutura.

### CORS

É uma regra para navegadores. Ela impede que um site desconhecido use o
navegador da pessoa para fazer chamadas à API. O Android nativo não depende de
CORS. Se houver um site no futuro, seus domínios precisam ser incluídos em
LINK_HEALTH_ALLOWED_ORIGINS.

### Cabeçalhos de segurança

O backend adiciona instruções ao navegador para impedir que a API seja
incorporada em iframes, reduzir interpretações erradas de conteúdo e limitar
recursos que uma página pode usar. São proteções de navegador e não mudam a
interface do aplicativo.

### Limite de requisições

Cada endereço IP pode fazer até 120 chamadas à API por minuto na configuração
atual. Isso reduz tentativas repetidas de descobrir chaves ou sobrecarregar o
servidor. Em uma publicação com vários servidores, esse limite deve ficar em
um gateway ou serviço compartilhado.

## 5. Fotos: onde ficam agora e onde ficarão com banco

No perfil local de desenvolvimento, a foto é criptografada e salva fora do
projeto, em AppData/Local/LinkHealth/evolution-photos. O arquivo local termina
em .enc e não deve ser aberto nem enviado ao Git.

No perfil postgres, o conteúdo criptografado vai para a tabela
evolution_photo_files. A tabela se liga ao registro da foto e é apagada junto
quando o registro é removido. A chave de criptografia fica fora do banco, na
variável LINK_HEALTH_PHOTOS_ENCRYPTION_KEY.

Perder essa chave impede recuperar as fotos. Por isso, em produção ela precisa
ficar em um cofre de segredos ou variável protegida do ambiente, nunca no
GitHub.

## 6. Como os testes mudam

O comando de testes automatizados não muda:

    cd "C:\Users\felip\OneDrive\Documents\api\Link-Health\backend"
    ..\App\gradlew.bat test

Os testes antigos continuam verificando as regras de água, refeições, peso,
compras e fotos. Eles desligam a autenticação somente dentro do ambiente de
teste para testar a regra de negócio isoladamente.

O novo arquivo BackendSecurityTest testa a segurança ligada: requisição sem
chave recebe HTTP 401, chave válida é aceita, cabeçalhos de proteção existem e
origem desconhecida é bloqueada. PhotoEncryptionServiceTest testa que uma foto
criptografada pode ser recuperada apenas com a chave correta e acusa alteração
nos bytes.

Para testar manualmente no Swagger, existe uma mudança: depois de iniciar a
API, clique em Authorize e informe LINK_HEALTH_API_KEY. Isso é somente para
quem desenvolve ou apresenta o backend; não é uma etapa para o usuário do
aplicativo.

## 7. O que falta para uma segurança forte em produção

Esta etapa torna invasões e acessos aleatórios mais difíceis e protege fotos no
armazenamento. Para chegar a uma proteção adequada para usuários reais, ainda
precisamos de:

- cadastro, login e recuperação de senha;
- token individual por usuário, de curta duração;
- vínculo entre usuário, paciente e nutricionista;
- regra que confira se o usuário pode acessar aquele patientId;
- HTTPS ativo com certificado válido;
- banco com usuário exclusivo, permissões mínimas e backups criptografados;
- armazenamento protegido das chaves de produção;
- auditoria de acessos e monitoramento de falhas.

Sem login e vínculo de permissões, não é honesto afirmar que a API já tem
segurança máxima por paciente. Essa será a próxima evolução quando existir a
parte de identidade no projeto.
