# Ajustes de uso — água, evolução corporal e refeições

Esta revisão atende ao fluxo esclarecido pelo Felipe depois dos testes no aplicativo. Substitui a apresentação anterior de fotos e peso separados para os novos registros de evolução.

## Água e acompanhamento

A barra já utilizava verde, mas permanecia vazia quando o acompanhamento não tinha meta. Agora o nutricionista pode abrir a ficha real do paciente e tocar em **Editar meta**, ou no card do objetivo, para definir **objetivo** e **meta diária em litros**. O backend confere se o nutricionista autenticado possui acompanhamento ativo com aquele paciente; o aplicativo não envia uma chave administrativa ou um ID de nutricionista escolhido pelo cliente.

Na home do paciente ficam **500 ml**, **1 L** e **Personalizado**. Dentro de Personalizado há duas ações distintas: adicionar uma quantidade e corrigir o total consumido no dia. A correção é explícita e substitui o total; não significa retirar água do corpo. Não há mais botões de diminuir na home.

Com meta de 3 L, 500 ml preenchem 17% da barra; ao atingir ou superar a meta, ela fica completa. Sem meta, o consumo continua sendo salvo e a tela informa que a meta não foi definida, sem inventar um valor de referência.

`POST /api/v1/patients/{id}/water-intakes/daily-total` faz a correção de forma transacional, preservando os registros compatíveis e ajustando a diferença. `GET/PATCH .../care-settings` consulta/atualiza objetivo e meta do acompanhamento ativo do nutricionista autenticado.

## Avaliação corporal: três fotos e peso juntos

1. O paciente abre **Evolução Corporal → Nova avaliação · fotos e peso**.
2. Adiciona **Frente**, **Lado** e **Costas**, pela câmera ou galeria. Pode trocar cada foto antes de salvar.
3. Depois das três fotos aparece o campo de peso.
4. **Salvar evolução** envia as três imagens e o peso na mesma operação. O servidor registra uma única data/hora para os quatro dados.
5. A avaliação aparece em um card com as três miniaturas, peso e data. **Ver 3 fotos** abre o álbum; é possível deslizar horizontalmente ou usar as setas para mudar de página. A posição, peso e data identificam o registro.
6. **Remover avaliação** remove o conjunto completo. Não é permitido apagar uma foto isolada ou editar a pesagem pelo histórico deixando a avaliação inconsistente.

A migração **V12** cria `evolution_assessments`, que associa as imagens e pesagens às tabelas existentes. O conteúdo das fotos continua criptografado no PostgreSQL pela infraestrutura anterior. As validações de imagem, limites e permissões continuam em vigor. São exigidas três partes de imagem válidas, com até 5 MB cada, e peso válido; o limite total multipart foi ajustado para 16 MB.

O salvamento usa uma transação PostgreSQL. Uma avaliação incompleta ou com imagem inválida não deixa uma pesagem ou fotos parciais. Cada envio tem UUID e assinatura do conteúdo: repetir o mesmo envio após uma falha de comunicação retorna o mesmo registro, sem duplicar a avaliação. A assinatura também impede reutilizar o identificador com outro conteúdo.

O rascunho e a operação em andamento sobrevivem à recriação da Activity. Os rascunhos não constituem uma fila offline persistente após encerramento do processo; nesse caso consulte a lista antes de repetir um registro. A câmera utiliza cache privado e o temporário é removido após preparar a imagem.

Os registros antigos não são apagados nem associados por suposição. **Ver registros anteriores** permite consultar fotos e pesos salvos antes do formato agrupado. Não é possível deduzir corretamente quais três fotos antigas pertenciam a cada peso.

## Refeições e recomendações

A tela de refeições mantém o cadastro avulso, as marcações do plano publicado e os botões de editar/remover. O diário passou a mostrar cards com nome, data e observação somente quando preenchida, com ações lado a lado. O botão Atualizar e o aviso permanente Dados atualizados foram retirados; as consultas acontecem ao abrir/retornar e após salvar. Tentar novamente aparece somente se uma consulta falhar.

Os produtos continuam sendo cadastrados em **Ficha do paciente → Editar Produtos Recomendados**, com nome, preço e link. O paciente os consulta na Lista de Compras e abre o link de compra. Quando a consulta confirma uma lista vazia, a interface explica que ainda não existem recomendações. Falha de conexão não é tratada como confirmação de ausência de produtos.

## Classes novas

- `EvolutionAssessment`, repositórios local/PostgreSQL, `EvolutionAssessmentService` e `EvolutionAssessmentController`: associação, validação, gravação conjunta, repetição segura e remoção da avaliação.
- `AssessmentRequest`: validação dos dados recebidos junto às três imagens.
- `AssessmentDraftViewModel`, `NovaEvolucaoActivity`: seleção das três posições, rascunho, peso e envio único no Android.
- `EvolutionBook`: miniaturas e álbum autenticado com três páginas.
- `CareSettingsController` e `CareSettingsDialog`: objetivo e meta editáveis pelo nutricionista vinculado.
- `DailyWaterTotalRequest`, `WaterDialog`: correção explícita ou adição personalizada de água.

Resultados e roteiro atualizados: `backend/TESTES_ESCOPO_FELIPE.txt`. A revisão não implementa envio/aceitação de convites nem autoria de planos alimentares.

## Resultados desta revisão

- Backend completo: 62 aprovados, incluindo 24 testes PostgreSQL reais; nenhuma falha ou caso ignorado nessa execução.
- Comando comum sem banco de testes configurado: 38 aprovados e 24 PostgreSQL ignorados por condição; build concluído.
- Regressão HTTP real: 147 verificações aprovadas.
- Android, suíte dedicada: 15 aprovados, sem falhas ou casos ignorados. Inclui configuração da meta de 3 L na ficha, 500 ml preenchendo 17% da barra, produto visível para o paciente, envio conjunto, álbum de três páginas, cancelamento sem dados parciais e repetição sem duplicar.
- Android, suíte geral: 20 de 21 aprovados. A única falha continua sendo a seta de voltar na tela de convites (`TelasPacienteTest.aceitarUltimoConviteMostraEstadoVazio`), fora deste escopo. O teste não foi removido nem apresentado como aprovado.
- Compilação debug, testes JVM e Lint concluídos sem erros; permanecem avisos de análise estática.
- Conferência manual no emulador: as três posições foram escolhidas pela galeria, a frente foi substituída por captura da câmera, e a avaliação foi salva com peso. A navegação por gesto passou por Frente, Lado e Costas. A barra verde foi conferida visualmente com 500/3000 ml.
- PostgreSQL de desenvolvimento: inicialização da versão nova e migração V12 confirmadas, sem inserir contas ou avaliações sintéticas nesse banco. Os dados de teste foram criados em banco separado.
