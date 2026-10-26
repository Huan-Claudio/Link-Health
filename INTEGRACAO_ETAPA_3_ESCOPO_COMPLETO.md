# Integração do escopo do Felipe — Etapa 3

Esta entrega completa as conexões de compras, produtos e registros do paciente. Complementa as Etapas 1 e 2; os documentos antigos descrevem o estado de cada entrega anterior.

## Funções pelo aplicativo

| Função | Paciente | Nutricionista com vínculo ativo |
| --- | --- | --- |
| Compras | Consultar, criar, editar, remover, marcar/desmarcar | Criar, editar e remover pela ficha real |
| Produtos | Consultar preço/moeda e abrir link de compra | Criar, editar e remover pela ficha ou seletor |
| Água | Adicionar/remover na home; consultar, corrigir e remover no histórico | Consultar consumo e resumo |
| Refeições realizadas | Marcar/desmarcar plano publicado; registrar refeições avulsas; editar/remover registros | Consultar refeições e registros |
| Peso | Registrar, consultar, editar e remover independentemente de fotos | Consultar histórico |
| Fotos | Enviar da câmera/galeria, visualizar, remover e retomar envio pendente | Visualizar fotos |
| Resumo e histórico | Resumo diário na home e histórico por dia | Resumo na ficha e histórico do paciente |

Os salvamentos aguardam confirmação do servidor. Erros não são apresentados como sucesso. ViewModels preservam operações durante a rotação e não repetem POSTs automaticamente. Se o processo for encerrado, entre novamente e consulte os dados antes de repetir um envio cuja resposta tenha sido perdida.

As funções acima utilizam registros reais e estados vazios. Sem plano publicado, é possível registrar uma refeição avulsa. O Android utiliza o horário de Brasília (`America/Sao_Paulo`) para exibir datas e definir o dia, consistente com a unicidade das refeições planejadas.

## Banco de dados

São reutilizadas as tabelas de pacientes, nutricionistas, acompanhamentos, compras, produtos, água, refeições, peso, fotos e planos existentes. Não foi criada outra tabela de pacientes.

A migração `backend/src/main/resources/db/migration/V11__link_patient_records_and_planned_meals.sql` acrescenta as referências dos registros a `patients(user_id)`, a referência de realização a `planned_meals(id)` e a proteção contra realização duplicada da mesma refeição no mesmo dia. O nome legado `meal_plan_item_id` identifica uma refeição planejada, não um alimento individual. O serviço confere se ela pertence a um plano ativo do paciente.

A migração não apaga nem reassocia dados antigos. Referências inválidas e duplicatas precisam ser corrigidas explicitamente antes de migrar um banco que as contenha.

## Segurança e tecnologias

- **Java e Spring Boot:** regras, validação e endpoints dos registros; **Spring Security:** autenticação por token e autorização por paciente/perfil/vínculo. A chave administrativa permanece fora do APK.
- **PostgreSQL, JPA/JDBC e Flyway:** persistência, transações e evolução das tabelas. Ajustes de água usam bloqueio por paciente para impedir perda de atualizações simultâneas e consumo negativo.
- **Android Java, View Binding, RecyclerView e ViewModel:** interface, edição das listas, estados de carregamento/erro e preservação de operações durante a recriação da Activity.
- **FileProvider e contratos de resultado de Activity:** câmera com acesso temporário somente ao arquivo necessário, em cache privado. O arquivo temporário é removido após a preparação.
- **Preparação de imagem:** limita tamanho/dimensões, reduz resolução e recodifica em JPEG, retirando os metadados originais. O backend valida novamente conteúdo JPEG/PNG, dimensões e limite de 5 MB.
- **AES-GCM existente:** as fotos continuam criptografadas no PostgreSQL. A chave continua no `.env` ignorado pelo Git. Downloads exigem autorização, não usam cache HTTP e o cliente não encaminha tokens por redirecionamentos.

O paciente altera apenas seus próprios registros. O nutricionista com acompanhamento `ATIVO` pode consultá-los; não pode registrar consumo, peso ou fotos no lugar do paciente. Compras e produtos seguem as permissões específicas da tabela acima. Conhecer um UUID não concede acesso.

Fotos usam registro pendente seguido de upload. Se o envio falhar depois da criação, a tela permite enviar novamente para o mesmo registro ou removê-lo. Nenhum desses controles representa certificação de segurança ou uma nota numérica.

## Classes principais

Os arquivos Android ficam em `App/app/src/main/java/br/edu/pucgoias/app/` e os do servidor em `backend/src/main/java/br/edu/pucgoias/linkhealth/`.

| Classe | Responsabilidade |
| --- | --- |
| `RecordsApi`, `BackendApi` | Transportar registros e fotos com o token da conta |
| `PatientNavigation` | Identificar o paciente real por UUID entre telas |
| `RecordsViewModel` | Guardar dados, operações pendentes, erros e resultados na rotação |
| `RecordDialogs`, `RecordFormatting`, `RecordsScreen` | Formulários validados, datas e estados visuais |
| `PhotoPreparation` | Preparar imagens sem metadados originais e com limites de memória |
| `HomePacienteActivity` | Resumo persistido e consumo de água |
| `PlanoAlimentarActivity` | Consulta de plano publicado e registro das refeições realizadas |
| `RegistroDiarioActivity` | Totais diários e correção dos registros próprios |
| `EvolucaoCorporalActivity` | Fotos e pesagens reais |
| `ShoppingEditorViewModel`, `ShoppingEditorAdapter`, `ShoppingDraft` | Edição persistida de nome, quantidade e unidade |
| `ShoppingListViewModel` | Consulta de compras/produtos e confirmação de marcação |
| `HomeNutricionistaActivity`, `LinkedPatientAdapter`, `FichaPacienteActivity` | Acesso a estas funções usando pacientes realmente vinculados |
| `PatientAccessService` | Verificação de titularidade, perfil e acompanhamento |
| `RecordsOverviewService`, `RecordsOverviewController` | Resumo e histórico dos registros |
| `PatientPlannedMealsService` | Consulta de planos publicados, sem autoria de prescrições |
| Serviços de água/refeições/peso/fotos | Validação, autorização e persistência dos registros |
| `PatientRecordLock` e implementações | Consistência das alterações por paciente |

## Contratos para a equipe

- `GET /api/v1/patients/{id}/records-summary?date=AAAA-MM-DD&zoneId=America/Sao_Paulo`: consumo de água, meta/objetivo do vínculo ativo mais recente, nutricionista, refeições realizadas/planejadas, último peso até o dia e fotos armazenadas nesse dia.
- `GET .../records-history?zoneId=America/Sao_Paulo`: registros agrupados por dia.
- `GET .../planned-meals`: refeições dos planos publicados em acompanhamentos ativos.
- CRUDs existentes de `water-intakes`, `meal-records`, `weight-records`, `evolution-photos`: preservados e autorizados para a sessão móvel conforme o perfil.
- `POST .../water-intakes/adjustment`: ajuste positivo/negativo de consumo em um dia, de forma transacional.

## Dependências das outras partes

O cadastro e o login existentes identificam as contas. O envio/aceitação de convites e a autoria clínica de planos continuam com os responsáveis por essas partes. Esta entrega consome vínculos e planos reais; não cria vínculos falsos para liberar acesso. O nutricionista precisa ter acompanhamento ativo para acessar o paciente.

Perfil, endereço, avatar e notificações também não estão incluídos. A ficha foi ajustada para acessar compras, produtos e registros com UUID real. A equipe deverá usar esse mesmo identificador ao conectar a edição clínica de planos e demais funções.

O ambiente debug usa conexão local. A variante release ainda exige URL de servidor HTTPS; esta integração não publica a API na internet nem garante acesso a um computador local a partir de qualquer rede.

## Validação

| Verificação | Resultado |
| --- | --- |
| Backend completo | 54 aprovados; 0 falhas/ignorados, incluindo 20 PostgreSQL reais |
| Backend sem variáveis de banco de testes | 34 aprovados; 20 PostgreSQL ignorados por condição; build concluído |
| HTTP real autenticado | 147 verificações aprovadas |
| Concorrência de água | 60 solicitações com totais/rejeições corretos |
| Android: suíte dedicada ao escopo e autenticação de apoio | 13 aprovados; 0 falhas/ignorados |
| Android: suíte geral antes do último caso adicional de retomada PNG | 17 de 18 aprovados; única falha em convites, fora do escopo |
| Android: testes JVM e compilação | Aprovados; Lint sem erros, com avisos |
| Câmera e galeria no emulador | Envio, persistência, visualização e limpeza do cache confirmados |
| Nutricionista vinculado | Consulta da imagem confirmada, sem controles de alteração dos registros |
| PostgreSQL de desenvolvimento | Inicialização e migração V11 confirmadas; sem inserir cadastros sintéticos |

A falha geral é `TelasPacienteTest.aceitarUltimoConviteMostraEstadoVazio`: o cabeçalho `view_header_saudacao.xml` desloca a seta de voltar e o Espresso a considera parcialmente fora da área clicável. Esse fluxo continua com os responsáveis por convites. O teste não foi removido nem tratado como aprovado. O primeiro ensaio também foi interrompido por um diálogo de ANR do Android System UI; os resultados válidos acima foram obtidos após liberar o emulador.

O teste adicional de retomada PNG faz parte dos 13 aprovados: reenvia o conteúdo para o mesmo registro pendente sem criar outra foto. Novas imagens são preparadas em JPEG; um registro PNG pendente mantém seu tipo original.

Não foram testados aparelho físico, todos os fabricantes, implantação pública HTTPS, carga de produção ou um teste externo de invasão. Esses limites não impedem os fluxos locais validados, mas não devem ser apresentados como validações já realizadas.

Resultados finais da interface e roteiro para apresentação: `backend/TESTES_ESCOPO_FELIPE.txt`. Os testes utilizam banco separado dos cadastros de desenvolvimento. Não houve commit/push automático.
