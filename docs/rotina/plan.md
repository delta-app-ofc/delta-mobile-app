# Plano de implementação da Rotina

## Escopo e fontes — 29/10/2026

Plano documental para entregas pequenas e verificáveis. Nesta rodada, somente este arquivo será revisado: sem implementação, chamadas à API, dependências, build ou testes Android. As verificações abaixo são futuras, não evidência de execução.

Fontes: [AGENTS.md](../../AGENTS.md), [briefing](briefing.md), [api-contract.md](api-contract.md), [decisions.md](decisions.md), as quatro exportações em [design/](design/) e os fontes citados neste documento. As decisões aprovadas D03–D12 permanecem válidas. Definições adiadas em D13/P01–P11 continuam **INCERTO**; não precisam ser respondidas para concluir o plano, mas antes da implementação que depender delas.

**INCERTO — proposta técnica:** todos os nomes de arquivos novos, escolhas de componentes e comportamentos sugeridos neste plano são propostas, não políticas já consolidadas. Cada etapa pode ser revisada e validada isoladamente depois dos seus pré-requisitos. Independência não significa ausência de dependências entre modelo, repositório e tela.

O aceite é o do [briefing](briefing.md): calendário, hoje selecionado, troca de mês/dia, lista/estimativas/total diário, vazio, adicionar/editar/excluir, confirmação, validação por campo, erros/retry, atualização sem reabrir, estado na rotação, textos em recursos, teclado, responsividade e acessibilidade. **Fora do escopo: marcar hábito como concluído.** Não criar ação, checkbox, endpoint ou teste de conclusão.

## Ponto de partida e limites

- O destino `goalsFragment`, com `screen="routine"`, usa [MainSectionFragment][fragment]. O CRUD está em [SectionsViewModel][sections], e os cinco métodos já existem em [ApiService][api]. O GET de coleção não recebe data/mês. Não inventar consultas ou parâmetros. Fontes: [navegação][navigation], [contrato](api-contract.md).
- [ApiModels][models] ainda não representa categoria, horários e litros do OpenAPI analisado. Distribuição por dia e horários obrigatórios por execução exigem a ampliação aprovada, ainda não especificada/publicada nesta evidência. Não representar quantidades repetindo IDs de dias nem duplicando hábitos. Fontes: D04/D06/D11/D12 em [decisões](decisions.md), [contrato](api-contract.md).
- Reutilizar `AuthRepository.getInstance(context).api()`, `auth.resource()`, identidade de `currentProfile()` e [ApiErrors][errors]. O callback de `resource()` ocorre no executor de trabalho; publicar estado na main thread. Conferir conta/dono dos registros. Esses endpoints são por usuário: não inventar exigência de imóvel. Fontes: [AuthRepository][auth], [SectionsViewModel][sections], [AGENTS.md](../../AGENTS.md).
- Preservar cliente, Bearer, refresh, Keystore e timeouts. Não é necessário alterar [RetrofitClient][retrofit], [SessionInterceptor][interceptor] ou [NetworkConfig][network] para esta feature. Fonte: [AGENTS.md](../../AGENTS.md).
- Reutilizar [ScreenState][state] e [StatusNoticeView][notice]. `displayMessage()` retorna vazio em ERROR; usar o detalhe de erro de maneira explícita, sem alterar o contrato global. `ApiErrors` não fornece mapa de erros por campo. Fontes: [ScreenStateTest][state-test], [contrato](api-contract.md).
- As imagens mostram faixa semanal/agenda em [habitos.png](design/habitos.png), vazio em [habitosvazio.png](design/habitosvazio.png), alerta em [habitosAlerta.png](design/habitosAlerta.png) e detalhe em [detalhesHabito.png](design/detalhesHabito.png). Não há referência de tablet nem formulário completo. D01 registra ausência de Figma navegável. Não usar datas, litros, nomes ou percentuais das imagens como dados reais. Fontes: [design/](design/), [decisões](decisions.md).

## Ordem das entregas

| Etapa | Entrega | Pré-requisito |
| --- | --- | --- |
| 0 | Contrato confirmado do próximo recorte | Usuário/backend |
| 1 | Modelos e projeção da leitura diária | Contrato de leitura/regras diárias |
| 2 | Repository de leitura e testes | 1 |
| 3 | ViewModel de leitura e testes | 2 |
| 4 | Calendário + lista + total + estados responsivos | 3; proposta visual avaliada |
| 5 | Detalhes, somente leitura | 4; semântica do detalhe |
| 6 | Formulário/rascunho/validação, sem gravar | 4; regras do formulário |
| 7 | Adicionar e atualizar a agenda | 6; contrato de POST |
| 8 | Editar e atualizar a agenda | 5 e 7; contrato de PUT/escopo |
| 9 | Excluir com confirmação e atualizar a agenda | 8; alcance do DELETE |
| 10 | Regressão integrada e evidências de aceite | 4–9; ambiente de teste |

A etapa 4 conclui o primeiro marco funcional: leitura completa antes do CRUD. Adicionar, editar e excluir têm commits próprios. Os testes acompanham cada entrega. A substituição da tela na etapa 4 é um marco intermediário de leitura, não uma versão final liberada; não remover o código antigo de mutações antes de concluir suas substituições.

Nas tabelas, caminhos relativos à raiz usam: `J = app/src/main/java/com/aula/delta_mobile_app`, `T = app/src/test/java/com/aula/delta_mobile_app`, `I = app/src/androidTest/java/com/aula/delta_mobile_app` e `R = app/src/main/res`. São abreviações, não novos diretórios. Os comandos VJ/VB/VT/VI são definidos ao final.

## Commits por etapa

**Definido pela pessoa usuária em 29/10/2026:** prever pelo menos dois commits por etapa, incluindo as documentais 0 e 10. Fonte direta nesta conversa: “deixe pelo menos dois commits por etapa do plano também”. São **22 commits planejados no mínimo**, nas etapas 0–10. A divisão abaixo é proposta técnica; mensagens e fronteiras podem ser ajustadas na revisão, preservando entregas úteis e o mínimo solicitado.

Cada commit deve ser coeso, compilável e verificável após seus pré-requisitos, incluindo os testes necessários ao comportamento que introduz. Não deixar referências quebradas, testes essenciais para um commit posterior, commits vazios ou alterações cosméticas apenas para atingir a contagem. Recursos ainda sem fluxo público não devem expor botões sem ação. Se o segundo recorte depender de contrato, decisão ou evidência indisponível, a etapa permanece pendente; não inventar execução para completá-la.

Os dois commits de cada etapa são sequenciais, não etapas independentes. Podem existir commits adicionais; não contar merges, commits de outras etapas ou o mesmo commit duas vezes. A previsão não autoriza implementação de etapas posteriores nem execução de commits. O pedido vigente de revisão do diff antes de qualquer commit permanece válido. Nesta atualização, somente o plano é alterado; as mudanças documentais da etapa 0 continuam preparadas para revisão.

## Etapa 0 — Confirmar o contrato por recorte

| Arquivos futuros | Responsabilidade |
| --- | --- |
| Alterar `docs/rotina/api-contract.md` | Registrar payloads e regras efetivamente confirmados |
| Alterar `docs/rotina/decisions.md` | Registrar decisões com origem, substituindo apenas as pendências resolvidas |
| Atualizar `docs/rotina/openapi.json`, somente com fonte oficial | Snapshot entregue pelo backend, sem schema inventado |

**Por quê e trade-offs.** Fechar primeiro a parte de P01–P04/P06 necessária à leitura: distribuição recuperável, horários, legado, vigência, cálculo e catálogo. Antes da escrita, fechar validações, PUT, exclusão e P11 conforme operação. Isso permite avançar por recorte sem esperar decisões exclusivas de excluir. Usar apenas os três campos atuais perderia requisitos aprovados; inventar um contrato anteciparia retrabalho. Testes substituídos podem avançar após especificação, mas não provam deploy. Fontes: [contrato](api-contract.md), P01–P11 em [decisões](decisions.md).

**Como verificar.** Revisar exemplos de listagem/detalhe com várias execuções no mesmo dia e horários distintos; depois, exemplos de POST/PUT/DELETE e releitura sem perda da distribuição. Distinguir contrato especificado de serviço publicado. Sem build para esse commit documental. Não é necessário resolver tablet para fechar payload.

**O que aprender.** Converter requisito em contrato e separar decisão de produto, schema e capacidade publicada.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo |
| --- | --- | --- |
| 0.1 | `docs: definir contrato confirmado para o recorte inicial` | Consolidar em `api-contract.md` os payloads, regras, exemplos e limites do recorte confirmado; atualizar `openapi.json` somente se houver fonte oficial. Identificar a origem das confirmações no próprio contrato. |
| 0.2 | `docs: registrar decisoes e pendencias do recorte inicial` | Consolidar em `decisions.md` as respostas com origem e o estado das pendências, distinguindo especificação de publicação; incluir a previsão de commits em `plan.md` nesta rodada documental. |

## Etapa 1 — Modelos de leitura, agenda e total

| Arquivos | Responsabilidade |
| --- | --- |
| Alterar `J/model/ApiModels.java` | Completar respostas de hábitos conforme contrato, sem antecipar requests de escrita |
| Criar `J/model/RoutineData.java` | Projeção do dia, execuções, agrupamento, estimativas e total, separados do DTO |
| Criar `T/model/RoutineDataTest.java` | Regras de seleção, recorrência, unidade e somatório |

**Por quê e trade-offs.** Usar `java.time` e relógio injetável; formatos no transporte seguem o contrato confirmado. Propor projeção local da coleção completa, pois o GET atual não recebe data. Se o contrato futuro devolver ocorrências expandidas, consumir essa representação sem duplicar o algoritmo no app. Separar DTO/projeção custa mapeamento, mas evita cálculo no Fragment e permite testes JVM. Precedente: [AbnormalConsumptionData][abnormal-data]; lacuna: [contrato](api-contract.md).

**INCERTO — P02:** fórmula proposta: total diário do hábito = média por execução × quantidade naquele dia; total do dia = soma dos totais diários dos hábitos. Se houver linha por execução, mostrar a estimativa da execução nessa linha e não somar várias vezes o total diário do hábito. Se o backend calcular, consumir sua unidade confirmada sem recalcular com outra regra. D07–D10 aprovam total diário, média fixa e tabela, mas não o local/fórmula final.

Tabela inicial aprovada em D10: banho longo 80 L; lavar quintal 150 L; lavar roupa 100 L; regar plantas 15 L; lavar carro 200 L; lavar louça 20 L por execução. São estimativas, não medições. Ausência de distribuição/referência/estimativa não vira zero: **INCERTO — proposta** indicar valor indisponível e total incompleto conforme P02/legado. Coleção válida sem hábitos do dia permite vazio e zero confirmado. Fontes: [decisões](decisions.md), [contrato](api-contract.md), [AGENTS.md](../../AGENTS.md).

**Como verificar.** VJ: fronteiras de mês/ano/semana, fevereiro bissexto, várias execuções por dia, vazio, legado e estimativa ausente. Após aprovação da fórmula, testar duas execuções de banho longo como 80 L cada e 160 L no resumo, sem virar 320 L por duplicação. Fixtures ficam nos testes. Nenhuma tela muda.

**O que aprender.** Separar transporte de domínio, projetar recorrência e preservar unidades no cálculo.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo |
| --- | --- | --- |
| 1.1 | `feat: completar modelos de leitura dos habitos` | Ampliar somente os DTOs de resposta em `ApiModels.java` conforme contrato; iniciar `RoutineDataTest.java` com casos de desserialização que preservem distribuição/horários e representem legado/ausências. |
| 1.2 | `feat: modelar agenda e estimativas do dia` | Criar `RoutineData.java` com projeção, agrupamento e totais; ampliar o mesmo teste para calendário, unidades, ausência e prevenção de soma duplicada. |

## Etapa 2 — Repository de leitura autenticada

| Arquivos | Responsabilidade |
| --- | --- |
| Criar `J/repository/RoutineActions.java` | Contrato de leitura para ViewModel/substitutos; crescer por operação depois |
| Criar `J/repository/RoutineRepository.java` | Listar e consultar detalhe, validar conta/dono/corpo e traduzir falhas |
| Criar `T/repository/RoutineRepositoryTest.java` | Testar a lógica real do Repository com acesso/serviço substituídos |

**Por quê e trade-offs.** Usar métodos existentes de [ApiService][api] por `auth.resource()`, seguindo o [README](../../README.md). Prever pequeno contrato interno injetável de acesso a identidade, serviço e execução autenticada; produção delega a [AuthRepository][auth]. Ele é `final`, com construtor privado: não planejar herança ou mock do singleton. Essa adaptação local custa algum código, mas permite testes sem Context/tokens e evita refatorar autenticação. Contratos de ações têm precedente em [ChatActions][chat-actions].

`[]` é sucesso vazio; body nulo, identidade necessária ausente ou dado de outra conta não significa agenda livre. Consultar detalhe pelo `id` do registro, distinguindo-o do `habitId` do catálogo. Campos opcionais e legado seguem a etapa 0. Fontes: [contrato](api-contract.md), [SectionsViewModel][sections].

**Como verificar.** VJ: lista válida/vazia/nula, dados incompatíveis/de outra conta, detalhe 200/404, rede, 401/403/429/5xx e `code/retryAt`. Verificar identidade e caminho corretos. Usar respostas Retrofit/interceptor OkHttp em memória; [ChatbotNetworkTest][network-test] já demonstra interceptação sem servidor externo. Esses testes não substituem os de Bearer/refresh existentes em [SessionInterceptorTest][session-test]. Nenhuma tela muda.

**O que aprender.** Isolar rede/identidade por composição e distinguir vazio de erro sem chamar produção.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo |
| --- | --- | --- |
| 2.1 | `feat: consultar colecao autenticada de habitos` | Criar `RoutineActions`, `RoutineRepository` e a composição injetável para listagem; incluir em `RoutineRepositoryTest` identidade, dono, vazio/nulo e erros da coleção. |
| 2.2 | `feat: consultar detalhe autenticado do habito` | Acrescentar o detalhe por ID ao contrato/repositório e ampliar os testes para alvo, dono, corpo incompatível, 404 e demais falhas relevantes. |

## Etapa 3 — Estado de leitura no ViewModel

| Arquivos | Responsabilidade |
| --- | --- |
| Criar `J/model/RoutineUiState.java` | Snapshot de dia, mês exibido, coleção/projeção e estado de leitura |
| Criar `J/viewmodel/RoutineViewModel.java` | Seleção, mês, carga, retry, invalidação e publicação |
| Criar `T/viewmodel/RoutineViewModelTest.java` | Testes diretos do ViewModel com relógio/callbacks/executores controlados |

**Por quê e trade-offs.** Propor `ViewModel` com LiveData e construtor recebendo repositório, relógio e despachante da main thread. Uma Factory interna faz a composição manual via ViewModelProvider. O construtor de teste não cria Handler/Looper incondicionalmente. Mais parâmetros substituem dependência nova de DI e tornam concorrência testável. Precedentes: [AbnormalConsumptionViewModel][abnormal-vm], [ChatViewModel][chat-vm], [AGENTS.md](../../AGENTS.md).

Reutilizar `ScreenState<RoutineData>`: vazio é SUCCESS sem itens, não novo status global. Como `loading()` não contém dados, guardar separadamente a última projeção validada durante refresh. Dia/mês ficam no ViewModel. Inicializar hoje uma vez; rotação reaproveita a instância, sem voltar para hoje. Persistência após morte do processo não está prometida. Fuso/reentrada continuam P07/P08.

**INCERTO — P07:** propor que mudar mês selecione o mesmo número de dia limitado ao último dia daquele mês; selecionar data sincroniza o mês exibido. Com coleção completa carregada, trocar dia/mês apenas reprojeta, sem novo GET. Identificar cargas por conta/geração; detalhe também por alvo. Descartar respostas antigas após refresh, mutação, conta alterada ou `onCleared()`. Não invalidar a coleção inteira apenas porque o dia mudou durante a consulta.

**Como verificar.** VJ: hoje com relógio fixo, troca de mês, vazio, erro→retry→sucesso, deduplicação de carga, callbacks fora de ordem, conta alterada e resposta após limpeza. Testar ações e LiveData do ViewModel real, não apenas helper. Proposta sem biblioteca nova: delegado síncrono de `ArchTaskExecutor` transitivo de Lifecycle, restaurado em `@After`; confirmar sua presença no classpath antes de usar. Rotação real será verificada na etapa 4. Se precisar de biblioteca adicional, listar para aprovação.

**O que aprender.** Fonte única de estado, concorrência determinística e diferença entre rotação e morte do processo.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo |
| --- | --- | --- |
| 3.1 | `feat: definir estado de leitura e selecao` | Criar o snapshot `RoutineUiState.java`, com dia/mês, coleção/projeção e estado de leitura separados, incluindo representação dos dados anteriores durante refresh. Não introduzir dependência de um ViewModel ainda inexistente. |
| 3.2 | `feat: manter selecao e carregamento no viewmodel` | Criar `RoutineViewModel`, Factory e testes reais de seleção/carga/retry, concorrência, troca de conta e limpeza; manter os checks de lifecycle previstos para a etapa visual. |

## Etapa 4 — Calendário, lista e total responsivos

| Arquivos | Responsabilidade |
| --- | --- |
| Criar `J/view/fragments/RoutineFragment.java` | Observar com `getViewLifecycleOwner()`, renderizar e encaminhar ações |
| Criar `J/view/RoutineDayAdapter.java`, `J/view/RoutineAgendaAdapter.java` | Faixa de dias e lista, sem consultas/cálculos de domínio |
| Criar `R/layout/fragment_routine.xml`, `R/layout/item_routine_day.xml`, `R/layout/item_routine_habit.xml` | Composição compacta e itens de altura adaptável |
| Criar `R/layout-w800dp-h480dp/fragment_routine.xml` | Composição larga proposta, com mesmos IDs |
| Criar `R/values/routine_strings.xml`, `R/values/routine_styles.xml`, `R/values/routine_dimens.xml`, `R/values-w800dp-h480dp/routine_dimens.xml` | Textos/estilos/medidas locais da feature |
| Alterar `R/navigation/main_navigation.xml` | Trocar só a classe de `goalsFragment`, preservando ID/menu |
| Alterar `T/viewmodel/RoutineViewModelTest.java` | Cobrir ações do calendário conectadas à UI |

**Por quê e trade-offs.** Propor faixa semanal própria com RecyclerView, MaterialDatePicker para escolher data/mês e ações explícitas de mês anterior/próximo. Aproxima a exportação, mas exige testes de acessibilidade. CalendarView reduz código e diverge mais visualmente; biblioteca externa exigiria aprovação/manutenção. Material/RecyclerView já estão no [build][build]; [SignupProfileFragment][profile] já usa MaterialDatePicker. Não há nova dependência escolhida.

Exibir seleção sem depender só de cor, lista cronológica, unidade, total, vazio, loading, erro e retry. Usar [LoadingView][loading-view] coordenado pela Rotina, sem depender da prontidão da Home; refresh com dados anteriores deve indicar atualização. Não colocar botão sem ação: Adicionar será ativado na etapa 7. Fontes: [briefing](briefing.md), [design/](design/), [AGENTS.md](../../AGENTS.md).

**INCERTO — proposta responsiva P07/P10, incluída desde esta etapa:**

- Celular/janela estreita: coluna vertical, cards em altura natural e conteúdo rolável, com fim da lista/total acima do menu. Agrupar cronologicamente por horário permite crescer o texto; grade com posições absolutas aproxima pixels, mas pode sobrepor campos e depende de P03.
- Tablet/janela com pelo menos 800dp de largura e 480dp de altura: calendário/seleção/total à esquerda, aproximadamente 352dp; agenda à direita, largura restante; margens de 24dp e vão de 24dp. Em 800dp sobram aproximadamente 376dp para agenda. Celular em paisagem com pouca altura permanece compacto. Esses valores são ponto inicial, não padrão oficial.
- Sete alvos de 48dp exigem 336dp. Em 360dp, propor margens de 12dp na faixa e 16dp no resto. Se fonte/conteúdo exigir mais, permitir rolagem horizontal e manter o dia selecionado visível. Não encolher alvos.
- Usar `sp`, `Start/End`, `wrap_content`, descrições localizadas e estado selecionado acessível. Excluir decoração da leitura. Medidas locais evitam aplicar indiscriminadamente margens globais de tablet. Fontes: [briefing](briefing.md), [AGENTS.md](../../AGENTS.md), [dimens.xml][dimens].

**Como verificar.** VB/VT: hoje, seleção, fronteiras de mês/ano, lista/total e vazio confirmado, falha/retry e rotação sem perder dia/mês ou repetir GET. Conferir ~360dp, ~412dp e tablet ~800dp, nas duas orientações, nomes longos, muitas execuções, fonte ampliada e TalkBack. Comparar capturas às imagens e registrar desvios. Fixtures só por substituição em testes; sem dados fictícios como fallback de produção. Integração requer API compatível.

**O que aprender.** Renderizar estado, adaptar à janela e validar acessibilidade de calendário próprio.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo |
| --- | --- | --- |
| 4.1 | `feat: exibir calendario agenda e estados de leitura` | Entregar Fragment, adapters, layout compacto, recursos necessários, navegação e testes das ações conectadas; incluir lista/total, loading, vazio, erro/retry, rolagem e acessibilidade básica sem botões de mutação inativos. |
| 4.2 | `feat: adaptar agenda para janelas largas` | Acrescentar layout/dimensões da composição larga e ajustar comportamento ao espaço/fonte; executar a matriz visual da etapa, verificando as duas orientações, teclado, rotação e acessibilidade, com correções locais necessárias. |

## Etapa 5 — Detalhe somente para leitura

| Arquivos | Responsabilidade |
| --- | --- |
| Criar `J/view/fragments/RoutineDetailsDialogFragment.java`, `R/layout/dialog_routine_details.xml` | Detalhe restaurável com nome/categoria/dia/execuções/estimativa |
| Alterar `J/model/RoutineUiState.java`, `J/viewmodel/RoutineViewModel.java`, `J/view/fragments/RoutineFragment.java` | Alvo selecionado, estado do detalhe e abertura por item |
| Alterar recursos `routine_strings.xml`, `routine_styles.xml`, `routine_dimens.xml` e variante larga | Textos e composição do detalhe |
| Alterar `T/viewmodel/RoutineViewModelTest.java`, `T/repository/RoutineRepositoryTest.java` | Dono, falhas e detalhes concorrentes |

**Por quê e trade-offs.** Propor DialogFragment de tela inteira no celular e largura limitada no tablet, como filho de RoutineFragment. Compartilhar seu ViewModel via `ViewModelProvider(requireParentFragment())`: mantém estado sem torná-lo global na Activity. Destinos separados dariam back stack mais explícito, com escopo compartilhado adicional; diálogo simples não gerenciado dificulta restauração. Composição continua P08.

Usar ID real e contexto do dia/execução clicados, GET e retry. Duração só se P03 definir intervalo/regra. Meta ideal, percentual/meta máxima, pico, Ajustar automático e Ignorar dependem de P09: não simular, nem declarar exclusão definitiva do aceite sem decisão. Editar/Excluir só serão ativados em suas etapas. Fontes: [detalhes](design/detalhesHabito.png), [contrato](api-contract.md), [decisões](decisions.md).

**Como verificar.** VB/VT: abrir itens rapidamente sem aceitar detalhe antigo, 404/rede/retry, rotação com detalhe aberto e retorno à seleção preservada. Diferenciar estimativa por execução de resumo diário. Sem escrita.

**O que aprender.** Compartilhar ViewModel no escopo adequado e distinguir registro recorrente de execução.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo |
| --- | --- | --- |
| 5.1 | `feat: coordenar leitura do detalhe selecionado` | Acrescentar alvo/estado/comandos do detalhe ao estado e ViewModel; ampliar testes do ViewModel/repositório para dono, erro/retry, seleção concorrente e retorno tardio. |
| 5.2 | `feat: apresentar detalhes do habito selecionado` | Criar diálogo/layout/recursos e conectar a agenda ao detalhe; verificar restauração, rotação, unidade e responsividade sem ativar Editar/Excluir antes das etapas correspondentes. |

## Etapa 6 — Formulário, rascunho e validação sem gravar

| Arquivos | Responsabilidade |
| --- | --- |
| Criar `J/model/RoutineFormState.java` | Modo criar/editar, ID alvo, campos brutos, dias/quantidades/horários, erros e alterações |
| Criar `J/model/RoutineFormValidator.java` | Validar regras confirmadas e devolver erros identificados por campo/execução |
| Criar `J/view/fragments/RoutineEditorDialogFragment.java`, `R/layout/dialog_routine_editor.xml`, `R/layout/item_routine_execution_field.xml` | Editor rolável e campos repetidos por execução |
| Alterar `J/model/RoutineUiState.java`, `J/viewmodel/RoutineViewModel.java` | Rascunho e ações em memória, ainda sem request de escrita |
| Alterar recursos `routine_strings.xml`, `routine_styles.xml`, `routine_dimens.xml` e variante larga | Rótulos, erros e dimensões do editor |
| Criar `T/model/RoutineFormValidatorTest.java`; alterar `T/viewmodel/RoutineViewModelTest.java` | Validação e preservação do formulário |
| Criar `I/RoutineFormInstrumentedTest.java` | Abrir o editor de forma controlada pelo teste, sem CTA de produção, e validar rotação/IME |

**Por quê e trade-offs.** Reutilizar catálogo/componentes Material confirmados, mantendo texto bruto no ViewModel, inclusive vazio/inválido; parsear só para validar/montar payload. Guardar apenas números parseados apagaria a entrada inválida. Listeners atualizam rascunho; renderização não deve resetar entrada/cursor. Erros usam identificadores traduzidos em recursos. Precedentes: [SignupHabitsFragment][signup-habits], [SelectionHelper][selection-helper].

Editor filho compartilha ViewModel como o detalhe. Propor tela inteira compacta e painel central com largura máxima inicial de 600dp no tablet, conteúdo rolável e Salvar alcançável. `adjustResize` da Activity não garante IME da janela de diálogo: configurar/verificar também o DialogFragment, preservando menu/insets de [MainActivity][main]. Fonte: [manifest][manifest], [AGENTS.md](../../AGENTS.md).

**Validações:** tipo obrigatório do catálogo, frequência informada/numérica, seleção de dias e horário obrigatório em cada execução. Frequência positiva já é regra local do app, não limite publicado. **INCERTO — P01/P03/P05:** limites, quantidade por dia, soma igual à frequência semanal, positividade em cada dia selecionado, intervalo/fuso e horários iguais/sobrepostos precisam ser formalizados antes de codificar. Não limitar frequência a sete nem igualá-la ao número de dias. Definir descarte de horários ao reduzir execuções em P08. Fontes: D03/D04/D11/D12, [contrato](api-contract.md).

Erro local fica em TextInputLayout ou aviso associado ao grupo/dia/execução, com foco/rolagem ao primeiro erro e revalidação ao corrigir. Erro remoto só será associado a campo com mapeamento confirmado em P05; 409 sem schema não autoriza adivinhar campo. Erro geral usa StatusNoticeView com ação.

**Como verificar.** VB: obrigatórios/número inválido/distribuição/horários conforme regras aprovadas; rascunho e erros preservados no ViewModel. VT: abertura controlada em teste de UI, vários horários e rotação com IME; conferir último campo e ação acessíveis. Esta etapa não ativa CTA público sem persistência e não instala fluxo de demonstração em produção.

**O que aprender.** Separar texto em edição de valor validado e validar coleções de campos sem perder rascunho.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo |
| --- | --- | --- |
| 6.1 | `feat: modelar rascunho e validacao do formulario` | Criar `RoutineFormState`/`RoutineFormValidator`, ações em memória e testes de validação/preservação no ViewModel, conforme regras confirmadas; sem gravação. |
| 6.2 | `feat: preparar editor restauravel de habitos` | Criar diálogo, campos dinâmicos e recursos; conectar rascunho/erros e incluir `RoutineFormInstrumentedTest` para abertura controlada, rotação, IME e ações alcançáveis, sem CTA público de demonstração. |

## Etapa 7 — Adicionar

| Arquivos | Responsabilidade |
| --- | --- |
| Alterar `J/model/ApiModels.java` | Completar CreateUserHabitRequest conforme contrato oficial |
| Alterar `J/repository/RoutineActions.java`, `J/repository/RoutineRepository.java` | POST autenticado e resposta 201, com rota existente |
| Alterar `J/model/RoutineUiState.java`, `J/viewmodel/RoutineViewModel.java` | Estado de criação, bloqueio de envio duplicado e recarga |
| Alterar `J/view/fragments/RoutineFragment.java`, `J/view/fragments/RoutineEditorDialogFragment.java` | Ativar Adicionar no vazio/agenda preenchida e salvar/cancelar/retry |
| Alterar layouts agenda/editor e `R/values/routine_strings.xml` | Ações e mensagens de criação/atualização |
| Alterar `T/repository/RoutineRepositoryTest.java`, `T/viewmodel/RoutineViewModelTest.java` | POST, falhas e gravação→recarga |

**Por quê e trade-offs.** Validar antes da rede, enviar uma vez por ação, bloquear duplo toque e fechar editor só em sucesso confirmado. Recarregar coleção após sucesso conforme protocolo abaixo. Atualização otimista seria mais rápida, mas precisaria de rollback e poderia divergir dos IDs/horários/cálculo do serviço. O GET extra custa latência e confirma a leitura no servidor.

**Como verificar.** VB: inválido sem POST; duplo toque gera um POST; 201→GET; 400/409/rede mantêm rascunho; falha de recarga não repete POST. VT: adicionar no vazio e com agenda preenchida, distribuir ocorrências/horários, girar durante envio e ver lista/total sem reabrir. Validar em conta/API de teste; sem contrato publicado, registrar como teste substituído. Fontes: [briefing](briefing.md), D04/D06/D12.

**O que aprender.** Coordenar validação, gravação e leitura sem duplicação nem perda de entrada.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo |
| --- | --- | --- |
| 7.1 | `feat: persistir criacao autenticada de habito` | Completar request de criação e operação no contrato/repositório com testes de payload, identidade, resposta 201 e falhas; não expor ainda ação pública de criação. |
| 7.2 | `feat: adicionar habito e atualizar agenda` | Coordenar envio/recarga no ViewModel e ativar Adicionar/Salvar com recursos e testes de duplo toque, rascunho, falha de recarga e respostas tardias; executar cenários de tela da etapa. |

## Etapa 8 — Editar

| Arquivos | Responsabilidade |
| --- | --- |
| Alterar `J/model/ApiModels.java` | Completar UpdateUserHabitRequest e opcionais conforme semântica do PUT |
| Alterar `J/repository/RoutineActions.java`, `J/repository/RoutineRepository.java` | Conferir dono via detalhe e executar PUT pelo ID do registro |
| Alterar `J/model/RoutineFormState.java`, `J/viewmodel/RoutineViewModel.java` | Preencher uma vez, coordenar edição e recarga |
| Alterar `J/view/fragments/RoutineDetailsDialogFragment.java`, `J/view/fragments/RoutineEditorDialogFragment.java` | Ativar Editar e apresentar resultado/erro |
| Alterar layouts detalhe/editor e `R/values/routine_strings.xml` | Ações e textos de edição |
| Alterar `T/repository/RoutineRepositoryTest.java`, `T/viewmodel/RoutineViewModelTest.java` | PUT, preservação do rascunho e mudanças de dia/horário |

**Por quê e trade-offs.** Reutilizar formulário com modo/ID explícitos, sem excluir+criar: essa alternativa muda identidade e adiciona ponto de falha. Conferir dono antes do PUT. Preencher ao iniciar edição; GET tardio/refresh não sobrescreve rascunho já modificado. Não assumir que omitir opcional no PUT o preserva. Série/ocorrência e efeito no passado/futuro dependem de P04; não oferecer seletor de escopo sem suporte. Fontes: [contrato](api-contract.md), [SectionsViewModel][sections].

**Como verificar.** VB: ID correto, dono incompatível/falha no detalhe sem PUT, opcionais conforme regra, inválido sem rede, erro preservando rascunho, 200→GET e falha de GET sem repetir PUT. VT: editar horário/distribuição; mover hábito a outro dia e conferir saída/entrada e totais a partir da coleção recarregada; girar/cancelar sem request. Fontes: [briefing](briefing.md), P04/P05/P08.

**O que aprender.** Reusar formulário com identidade estável e separar detalhe recebido de alterações locais.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo |
| --- | --- | --- |
| 8.1 | `feat: persistir edicao autenticada do habito` | Completar request de atualização e PUT no contrato/repositório; testar ID, conferência de dono, opcionais conforme contrato, resposta 200 e falhas, sem expor Editar antes da coordenação completa. |
| 8.2 | `feat: editar habito preservando rascunho e identidade` | Conectar detalhe/editor e estado de edição/recarga, incluindo recursos e testes de preservação, callbacks tardios e mudanças de dia/horário; executar os cenários visuais previstos. |

## Etapa 9 — Excluir com confirmação

| Arquivos | Responsabilidade |
| --- | --- |
| Criar `J/view/fragments/RoutineDeleteDialogFragment.java` | Confirmação restaurável com alvo e alcance definidos |
| Alterar `J/repository/RoutineActions.java`, `J/repository/RoutineRepository.java` | Conferir dono, DELETE e sucesso 204 sem body |
| Alterar `J/model/RoutineUiState.java`, `J/viewmodel/RoutineViewModel.java` | Alvo pendente, cancelamento, exclusão e recarga |
| Alterar `J/view/fragments/RoutineDetailsDialogFragment.java`, layout detalhe e `R/values/routine_strings.xml` | Ativar Excluir, confirmar, mostrar erro/retry |
| Alterar `T/repository/RoutineRepositoryTest.java`, `T/viewmodel/RoutineViewModelTest.java` | Cancelamento/confirmar, falhas e atualização |

**Por quê e trade-offs.** Separar pedir exclusão de confirmar; só confirmar chama Repository. Alvo no ViewModel e DialogFragment impedem perder/trocar alvo na rotação. Não excluir por índice da lista. Exclusão imediata com desfazer exigiria restauração não publicada. DELETE atual remove registro, não ocorrência; texto/alcance finais dependem de P04. Fonte: [contrato](api-contract.md).

**Como verificar.** VB: abrir/cancelar sem DELETE; confirmar uma vez; dono errado sem escrita; 204 sem JSON; 404/rede conforme política, sem sucesso presumido; falha de recarga não repete DELETE. VT: cancelar/confirmar, girar com confirmação aberta e durante envio, falhar/retry e excluir último hábito do dia. Vazio/zero só após leitura confirmada; falha mantém alvo/contexto recuperáveis. Fonte: [briefing](briefing.md).

**O que aprender.** Modelar ação destrutiva em duas fases e tratar sucesso sem conteúdo.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo |
| --- | --- | --- |
| 9.1 | `feat: coordenar exclusao autenticada por alvo` | Acrescentar DELETE ao contrato/repositório e estado/comandos de pedir/cancelar/confirmar no ViewModel; testar dono, alvo, ausência de request ao cancelar, 204, falhas e recarga. |
| 9.2 | `feat: excluir habito com confirmacao restauravel` | Criar diálogo/layout/recursos e conectar a ação pública somente com confirmação; verificar rotação, cliques repetidos, erro/retry e exclusão do último hábito, ampliando testes se a ligação revelar novos casos. |

## Protocolo de atualização após as três mutações

**INCERTO — proposta para P11, a definir antes da etapa 7.** Implementar e testar incrementalmente em cada operação, sem refatoração global antecipada:

1. Validar/capturar conta, alvo e geração da operação. Manter estado de mutação separado da leitura, usando ScreenState e identificador da operação; rascunho permanece no RoutineFormState. Bloquear envio concorrente. Não guardar View/Activity no ViewModel.
2. Após confirmação do servidor, registrar gravação concluída e encerrar editor/confirmador como consequência do estado. Invalidar GET anterior à mutação e detalhe afetado. Replay de LiveData/recriação de View nunca reenvia mutação.
3. Fazer um único GET da coleção autenticada. Preservar dia/mês atuais e projetar lista/total do mesmo snapshot. Mudanças em outro dia atualizam a coleção inteira; não forçar hoje nem primeiro item.
4. Enquanto atualiza, manter projeção anterior explicitamente desatualizada, sem tratá-la como total confirmado; bloquear ações sobre dados antigos. Sucesso substitui coleção/projeção, incluindo vazio confirmado. Detalhe editado recarrega; detalhe excluído fecha.
5. Gravação confirmada com recarga falha mostra aviso equivalente a “Alteração salva; não foi possível atualizar a agenda”, com retry somente do GET. Não reapresentar a gravação como falha nem habilitar reenvio do mesmo payload. Textos fixos em recursos.
6. Falha conclusiva da mutação mantém rascunho/alvo e retry explícito, respeitando retryAt. Timeout/resposta perdida pode significar resultado desconhecido: preservar entrada e oferecer reconciliação por leitura. Resposta 2xx com corpo incompatível também exige reconciliação, sem concluir que nada foi salvo. Uma leitura isolada não prova ausência de gravação sem garantia de consistência do backend. Reenvio seguro depende de P11; não inventar idempotency key nem repetir POST automaticamente.
7. Logout/troca de conta limpa coleção, detalhe, rascunho e alvo de exclusão e invalida callbacks. Remover observers de sessão/callbacks em onCleared. Rotação mantém instância e operação. Reentrada/alterações pelo cadastro ou outro dispositivo dependem de P11; não prometer sincronização em tempo real.

Testar os passos no ViewModel real e códigos/respostas no Repository. Fontes: [briefing](briefing.md), [AuthRepository][auth], geração em [AbnormalConsumptionViewModel][abnormal-vm], P11 em [decisões](decisions.md).

## Etapa 10 — Regressão integrada e aceite

| Arquivos | Responsabilidade |
| --- | --- |
| Criar `I/RoutineStateInstrumentedTest.java` | Rotação real de agenda/editor/confirmador com operações controladas |
| Criar `docs/rotina/acceptance.md` | Ambiente, resultados, capturas, desvios aprovados e limites |
| Alterar `J/view/fragments/MainSectionFragment.java`, `J/viewmodel/SectionsViewModel.java`, após busca de referências | Retirar exclusivamente ramo/métodos antigos de Rotina sem chamadores |
| Alterar arquivos/testes da Rotina somente se a regressão mostrar falhas | Correções localizadas com causa e nova evidência |

**Por quê e trade-offs.** Serviço substituído dá estados determinísticos; passagem separada com backend compatível confirma integração. Uma cobertura não substitui a outra. Remover ramo antigo no final mantém migração isolada, ao custo de código sem uso temporário. Buscar chamadores e preservar cadastro/demais seções; sem refatorar contas, ranking, metas ou consumo. Fontes: [SectionsViewModel][sections], [MainSectionFragment][fragment], [SignupHabitsFragment][signup-habits].

**Como verificar.** VB/VI e matriz abaixo. Instrumentação usa ActivityScenario se disponível no classpath existente, com respostas controladas e montagem restrita à Rotina. Testes de autenticação existentes podem limpar sessão do app de teste: consultar [AUTH_MOBILE.md][auth-doc]. Não usar produção como fixture.

| Critério do briefing | Entregue em | Evidência esperada |
| --- | --- | --- |
| Calendário, hoje, seleção de dia/mês | 1, 3, 4 | Capturas/interação em fronteiras de mês/ano/bissexto |
| Lista, litros com unidade, total e vazio | 1–4 | Várias execuções, lacunas e vazio confirmado; total sem duplicação |
| Adicionar | 6, 7 | POST e releitura com dados preservados |
| Editar | 5, 6, 8 | PUT e releitura de distribuição/horário |
| Excluir com confirmação | 9 | Cancelar sem escrita; confirmar e reler |
| Validação por campo | 6–8 | Obrigatório/inválido e foco no erro; remoto só com contrato |
| Atualização sem reabrir | 7–9 | Lista/total do mesmo snapshot; falha de recarga repete só GET |
| Loading/erro/retry sem perder entrada | 2–9 | Falhas de leitura/detalhe e de cada mutação, retryAt e timeout conforme P11 |
| Dia/formulário preservados na rotação | 3–10 | Rotação real em repouso, com IME e durante requests; mês/rascunho/erros preservados |
| Textos em recursos | 4–9 | Revisão Java/XML: texto fixo, erro, rótulo e descrição em routine_strings.xml |
| Celular/tablet e orientações | 4–10 | ~360dp/~412dp/tablet 10"/~800dp, retrato/paisagem, fonte ampliada e capturas |
| Teclado não cobre formulário | 6–10 | Último campo/campos dinâmicos/Salvar alcançáveis; menu conforme shell |
| Acessibilidade/toque 48dp | 4–10 | TalkBack, foco, seleção sem depender só de cor e medição de alvos |
| Testes unitários Repository/ViewModel | 2, 3, 5–9 | Relatórios JVM com falhas, callbacks fora de ordem e conta alterada |

Todo texto fixo novo fica em recursos; mensagens dinâmicas de negócio exigem contrato/filtragem. [ApiErrors][errors] compartilhado já contém textos literais: não declarar a base toda sem hardcode nem alterar globalmente seu parser. Mapear mensagens fixas apresentadas pela Rotina para recursos, preservando code/retryAt e filtragem; evitar displayMessage() em ERROR. Se P05 confirmar schema específico de erros por campo, prever condicionalmente `J/services/network/RoutineErrors.java` e `T/services/network/RoutineErrorsTest.java` no commit dependente, sem presumir o formato agora.

Registrar Android, densidade/largura real em dp, escala de fonte, versão do backend e resultados. **INCERTO — escala proposta:** começar com 1,3×/2,0×, sem inventar matriz oficial. Build não prova aparência/instalação; teste substituído não prova integração. Não marcar critérios aceitos sem evidência. P09 precisa ser resolvido antes do aceite visual dos elementos sem contrato.

**O que aprender.** Separar evidência de lógica, integração e UI e concluir migração sem ampliar escopo.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo |
| --- | --- | --- |
| 10.1 | `test: completar regressao integrada dos fluxos` | Completar instrumentação relevante e retirar exclusivamente código antigo de Rotina comprovadamente sem chamadores; executar checks e incluir correções/testes locais necessários, sem criar testes redundantes para cumprir contagem. |
| 10.2 | `docs: registrar evidencias de responsividade e aceite` | Criar `acceptance.md` com comandos, ambiente, capturas, matriz visual, resultados reais e pendências; distinguir testes substituídos de integração e não declarar aceite sem evidência. |

Se a instrumentação e a retirada do código antigo já estiverem concluídas antes da etapa 10, o primeiro commit poderá registrar a parte automatizada das evidências em `acceptance.md`, com mensagem `docs: registrar verificacoes automatizadas`; o segundo acrescentará evidências visuais e de integração realmente obtidas. Se faltarem dispositivos/backend, registrar a limitação e manter o aceite correspondente pendente, sem fabricar uma segunda entrega.

## Verificações previstas

Executar pelo Wrapper na raiz, conforme [AGENTS.md](../../AGENTS.md). São comandos futuros; não foram executados nesta revisão documental. Cada etapa roda a verificação indicada e repete o afetado após correção.

**VJ — modelos/Repository/ViewModel:**

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

**VB — entregas funcionais/visuais e regressão:**

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

**VT — tela:** instalar/abrir debug em aparelho/emulador de teste e executar cenários da etapa, com capturas/limites registrados. Preview auxilia, mas não substitui rotação/IME/interação/backend.

**VI — instrumentação com dispositivo disponível:**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Proposta de testes sem novas bibliotecas: JUnit existente, doubles manuais de RoutineActions/acesso autenticado, respostas Retrofit/OkHttp em memória, relógio fixo, executores/callbacks controlados e delegado de LiveData restaurado após teste. Testar as classes reais do Repository e ViewModel. Rotação real pertence à instrumentação/manual. Confirmar ArchTaskExecutor/ActivityScenario no classpath resolvido antes de usar; dependência adicional exige aprovação. Fontes: [build][build], [network-test][network-test], [AGENTS.md](../../AGENTS.md).

## Pendências do usuário e do backend

Todas continuam **INCERTO — definição adiada**, conforme D13. A coluna final identifica o trecho bloqueado; não exige resolver tudo de uma vez. Origem: P01–P11 em [decisions.md](decisions.md), [api-contract.md](api-contract.md).

| ID | Responsável | Decisão/entrega necessária | Antes de |
| --- | --- | --- | --- |
| P01 | Backend + usuário | Distribuição/execuções: estrutura, validações, persistência/retorno, compatibilidade/legado e OpenAPI atualizado | 1–2 leitura; 6–8 escrita |
| P02 | Usuário + backend | Fórmula/local do cálculo, unidade execução/resumo, total diário, precisão/ausência e representação; tabela D10 já aprovada | 1 e total na 4 |
| P03 | Usuário + backend | Início ou intervalo, formato/fuso/precisão, meia-noite/sobreposição e legado sem horário; obrigatoriedade já aprovada | 1/4/5 representação; 6–8 validação |
| P04 | Usuário + backend | Vigência/histórico/passado/futuro e série versus ocorrência na edição/exclusão | 1 projeção; 8–9 mutações |
| P05 | Backend + usuário para UX | Schema/exemplos reais de erros por campo, limites, duplicidade, nulos/omissão PUT e comportamento DELETE 404 | 6–9 conforme operação |
| P06 | Backend + usuário | Catálogo/IDs válidos; só tipos existentes ou tipo livre, que não tem criação publicada na API atual | 1–2 mapeamento; 6–8 seleção |
| P07 | Usuário | Faixa/semanas, limites/mês, marcações, fuso de hoje e acesso a Adicionar na agenda preenchida; avaliar propostas | 3–4; CTA na 7 |
| P08 | Usuário | Avaliar editor/detalhe, descarte/retorno/reentrada, redução de execuções e saída durante envio | 5–8 |
| P09 | Usuário + backend | Fonte/escopo de Meta ideal, percentual/meta máxima, pico, Ajustar automático/Ignorar: inclusão com contrato ou exclusão explícita do aceite correspondente | Trecho visual na 4/5 e aceite |
| P10 | Usuário | Avaliar duas áreas tablet, ponto de troca/medidas e calendário sem nova biblioteca | 4 |
| P11 | Backend + usuário | Resultado desconhecido, idempotência/consistência/retry, refresh na reentrada e após alterações externas; avaliar protocolo | 7–9 e entrega final |
| Ambiente | Backend + usuário | API ampliada disponível/versionada, conta/catálogo de teste e autorização de CRUD nesses dados | Integração de cada operação/10 |
| Dispositivos | Usuário/ambiente | Aparelho/emulador para celular/tablet, orientações e instrumentação; registrar indisponibilidades reais | VT/VI/aceite |

Não foram disponibilizados fontes internos/migrações do backend Delta para nomeá-los neste plano. Implementar o serviço é dependência externa, não trabalho Android implicitamente autorizado. Aprovar a ampliação não comprova deploy.

## Dependências e aprovação

**Proposta principal: nenhuma dependência nova.** Usar Java/XML, Lifecycle, Navigation, Material, RecyclerView, Retrofit/Gson, OkHttp e JUnit declarados no [build][build]. Propor findViewById como escolha local próxima aos exemplos; ViewBinding habilitado não exige migração. Não acrescentar Kotlin/Compose, framework de DI ou calendário externo.

**Pendência condicional para aprovação:** se surgir necessidade de biblioteca de calendário, `androidx.arch.core:core-testing`, AndroidX Test adicional, Mockito, MockWebServer, Robolectric ou outra biblioteca, listar primeiro pacote/versão, finalidade, alternativa sem acréscimo, compatibilidade/licença e impacto. Nenhuma está escolhida/autorizada aqui. Confirmar o classpath existente; não alterar `app/build.gradle.kts`/`gradle/libs.versions.toml` para acrescentar dependência sem aprovação. Origem: instrução explícita do usuário nesta solicitação.

## Resultado desta revisão documental

Somente `docs/rotina/plan.md` foi revisado. Leitura vem antes do CRUD; cada operação tem etapa própria, com arquivos/responsabilidades, trade-offs, verificação, aprendizado e commit previsto. Briefing, decisões, contrato, OpenAPI, dependências e código permanecem sem alterações nesta rodada. Nenhum build/teste/chamada à API foi executado e nenhum critério é declarado aceito.

[fragment]: ../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/MainSectionFragment.java
[sections]: ../../app/src/main/java/com/aula/delta_mobile_app/viewmodel/SectionsViewModel.java
[api]: ../../app/src/main/java/com/aula/delta_mobile_app/services/api/ApiService.java
[models]: ../../app/src/main/java/com/aula/delta_mobile_app/model/ApiModels.java
[navigation]: ../../app/src/main/res/navigation/main_navigation.xml
[auth]: ../../app/src/main/java/com/aula/delta_mobile_app/repository/AuthRepository.java
[errors]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/ApiErrors.java
[retrofit]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/RetrofitClient.java
[interceptor]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/SessionInterceptor.java
[network]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/NetworkConfig.java
[state]: ../../app/src/main/java/com/aula/delta_mobile_app/model/ScreenState.java
[state-test]: ../../app/src/test/java/com/aula/delta_mobile_app/model/ScreenStateTest.java
[notice]: ../../app/src/main/java/com/aula/delta_mobile_app/view/StatusNoticeView.java
[abnormal-data]: ../../app/src/main/java/com/aula/delta_mobile_app/model/AbnormalConsumptionData.java
[abnormal-vm]: ../../app/src/main/java/com/aula/delta_mobile_app/viewmodel/AbnormalConsumptionViewModel.java
[chat-actions]: ../../app/src/main/java/com/aula/delta_mobile_app/repository/ChatActions.java
[chat-vm]: ../../app/src/main/java/com/aula/delta_mobile_app/viewmodel/ChatViewModel.java
[profile]: ../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/SignupProfileFragment.java
[signup-habits]: ../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/SignupHabitsFragment.java
[selection-helper]: ../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/SelectionHelper.java
[main]: ../../app/src/main/java/com/aula/delta_mobile_app/view/activities/main/MainActivity.java
[loading-view]: ../../app/src/main/java/com/aula/delta_mobile_app/view/LoadingView.java
[manifest]: ../../app/src/main/AndroidManifest.xml
[dimens]: ../../app/src/main/res/values/dimens.xml
[build]: ../../app/build.gradle.kts
[network-test]: ../../app/src/test/java/com/aula/delta_mobile_app/services/network/ChatbotNetworkTest.java
[session-test]: ../../app/src/test/java/com/aula/delta_mobile_app/services/network/SessionInterceptorTest.java
[auth-doc]: ../../devdocs/fluxodavi/AUTH_MOBILE.md
