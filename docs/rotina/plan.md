# Plano de implementação da Rotina

## Estado e objetivo — 29/10/2026

Plano documental concluído para orientar a evolução da Rotina existente até o calendário de hábitos com criar, editar, excluir e consumo estimado. Implementação não iniciada nesta etapa. A pessoa usuária pediu para adiar as definições restantes, marcá-las como **INCERTO** e fechar o plano, conforme [D13](decisions.md#d13-adiar-decisões-restantes-e-fechar-o-plano).

Fontes: [AGENTS.md](../../AGENTS.md), [README](../../README.md), [briefing](briefing.md), [design/](design/), [OpenAPI local](openapi.json), [contrato analisado](api-contract.md) e [decisões](decisions.md). As escolhas técnicas e os nomes de arquivos novos abaixo são **INCERTO — proposta de implementação**; os requisitos aprovados são identificados separadamente. Nada neste plano declara que o contrato ampliado já está implementado ou publicado.

## Requisitos já definidos

| Tema | Definição recebida | Fonte |
| --- | --- | --- |
| Calendário | Hoje selecionado na entrada, seleção de dia, navegação entre meses, lista do dia, litros com unidade, total e estado vazio | [briefing](briefing.md) |
| Gestão | Criar, editar e excluir com confirmação; atualizar a agenda sem reabrir a tela; erro com nova tentativa sem perder o digitado | [briefing](briefing.md) |
| Recorrência | Frequência em vezes por semana e seleção dos dias; formulário para distribuir a quantidade entre os dias | D03/D04 em [decisions.md](decisions.md) |
| API | Considerar ampliação para receber, persistir e devolver a distribuição diária | D06 em [decisions.md](decisions.md) |
| Horários | Cada execução pode ter horário individual e esse preenchimento é obrigatório | D11/D12 em [decisions.md](decisions.md) |
| Consumo | Total diário por hábito, calculado com média fixa por execução e tabela inicial aprovada | D07–D10 em [decisions.md](decisions.md) |
| Qualidade | Rotação preserva dia/formulário; textos em recursos; teclado sem cobrir campos; celular/tablet, retrato/paisagem; acessibilidade e toque mínimo de 48dp | [briefing](briefing.md) |
| Fora do escopo | Marcar hábito como concluído | [briefing](briefing.md) |

Tabela aprovada de estimativa por execução: banho longo 80 L; lavar quintal 150 L; lavar roupa 100 L; regar plantas 15 L; lavar carro 200 L; lavar louça 20 L. Fonte de produto: D10 em [decisions.md](decisions.md). Fonte dos valores: tabela da IA identificada no registro de decisões. São estimativas, sem usar os valores das imagens como dados da conta.

## Ponto de partida e reaproveitamento

O CRUD já existe em [ApiService][api] e é chamado por [SectionsViewModel][sections]. A Rotina atual é renderizada por [MainSectionFragment][generic-fragment], no destino `goalsFragment`, com `screen="routine"`. Seus DTOs ainda não representam categoria, horários e litros do OpenAPI analisado. Fontes: [ApiModels][api-models], [main_navigation.xml][navigation], [contrato](api-contract.md).

Reutilizar o cliente Delta de [AuthRepository][auth]/[RetrofitClient][retrofit], [SessionInterceptor][interceptor], [NetworkConfig][network] e [ApiErrors][errors]. O [README](../../README.md) orienta usar o mesmo service no repositório responsável. Não há necessidade de outro cliente HTTP para o CRUD de Rotina.

Reaproveitar [ScreenState][screen-state], [StatusNoticeView][notice], carregamento de [AuthFlowFragment][auth-flow], componentes Material dos [formulários de hábitos][signup-habits] e coordenação de menu/IME do [MainActivity][main]. A referência para separação repositório/estado é [Consumo anormal][abnormal-repository]; para lista reciclável, há [ChatSessionAdapter][session-adapter]. **INCERTO — proposta:** criar componentes próprios da Rotina usando essas bases, sem copiar limitações do fluxo antigo.

Atenção ao contrato existente: `ScreenState.displayMessage()` esconde texto em ERROR, preservando-o em `message`. O editor atual dispara a mutação e fecha o diálogo; não fornece a preservação de formulário exigida pelo novo briefing. Essas limitações precisam ser tratadas no fluxo da feature. Fontes: [ScreenState][screen-state], [MainSectionFragment][generic-fragment], [briefing](briefing.md).

## Sequência de execução

Cada etapa deve ser apresentada e verificada separadamente, conforme [AGENTS.md](../../AGENTS.md). A sequência abaixo é **INCERTO — proposta técnica de execução**; não reabre as decisões de produto já aprovadas. Definições adiadas são retomadas apenas quando necessárias à etapa dependente.

| Etapa | Entrega | Dependência principal |
| --- | --- | --- |
| 0 | Especificação do contrato ampliado e regras que afetam os dados | P01–P06/P11, conforme o recorte abaixo |
| 1 | DTOs e modelo de recorrência/agenda/estimativa | Representação e regras definidas na etapa 0 |
| 2 | Repositório usando sessão, service e erros existentes | Contrato e DTOs; serviço substituído para testes |
| 3 | Estado da tela e rascunho de formulário no ViewModel | Modelo e repositório |
| 4 | Calendário, agenda do dia, totais e estados responsivos | Estado de leitura; P07/P09/P10 |
| 5 | Detalhes e formulários com criar/editar/excluir | Estado de mutação; P03–P06/P08/P11 |
| 6 | Integração real e verificação do briefing | API ampliada disponível e etapas anteriores |

### Etapa 0 — Especificar a ampliação sem inventar o contrato

**Entrega:** atualizar [api-contract.md](api-contract.md) e, quando houver especificação oficial disponível, conferir a nova versão de [openapi.json](openapi.json). O contrato precisa representar distribuição por dia e horários de cada execução, mantendo distinção entre conta, tipo de hábito e registro/ocorrência. A ampliação foi aprovada; o formato dos dados continua **INCERTO**. Fontes: D04/D06/D11/D12 e P01 em [decisions.md](decisions.md).

Definições a resolver no momento desta etapa, sem exigir respostas agora:

- **INCERTO — P01/P03:** estrutura, formato e precisão dos horários; somente início ou início/fim; associação entre dia, quantidade e execuções; compatibilidade dos registros antigos sem horário.
- **INCERTO — P02:** responsabilidade pelo cálculo (mobile ou backend), regra diária e representação dos totais. A tabela por execução está aprovada; a proposta de multiplicá-la pela quantidade diária está registrada como dedução em D09.
- **INCERTO — P04:** vigência, exceções e alcance de edição/exclusão sobre série ou ocorrência; projeção de meses passados/futuros.
- **INCERTO — P05/P06:** catálogo válido, limites, duplicidade, omissão no PUT e erros por campo.
- **INCERTO — P11:** confirmação de mutações, timeout e compatibilidade com atualizações externas.

Fonte de todos os itens: [pendências P01–P11](decisions.md) e [lacunas do contrato](api-contract.md). Essas definições podem ser fechadas por recorte antes de cada etapa dependente; não é necessário resolver aparência de tablet para especificar o payload.

**Critério de saída:** exemplos revisáveis de criar/listar/detalhar/editar/excluir, com correspondência dos horários e da distribuição ao retornar o registro; regras e compatibilidade descritas. Distinguir contrato especificado de serviço publicado. A implementação do backend é dependência externa a este plano Android; não estão disponíveis aqui fontes Java do serviço Delta para indicar arquivos ou migrações. Fonte do serviço analisado: [OpenAPI](openapi.json).

### Etapa 1 — Modelos e regras da agenda

**INCERTO — arquivos propostos:** adaptar `model/ApiModels.java` e criar um modelo próprio, como `model/RoutineData.java`. Os caminhos Java são relativos a `app/src/main/java/com/aula/delta_mobile_app/`. São nomes de planejamento, não arquivos já criados. Precedentes: [ApiModels][api-models], [AbnormalConsumptionData][abnormal-data].

Separar o DTO recebido da API da projeção da agenda selecionada. Preservar IDs reais e dados da conta; representar quantidades por dia e horários individuais segundo a etapa 0. Não usar repetição de IDs em `daysOfWeek` ou registros duplicados como codificação presumida. Fontes: [contrato](api-contract.md), D04/D06/D11/D12 em [decisions.md](decisions.md).

**INCERTO — proposta de cálculo:** `total diário do hábito = média por execução × quantidade naquele dia`; total da agenda como soma dos totais diários dos hábitos. Se a agenda mostrar uma linha por execução, diferenciar estimativa da execução e resumo diário para não somar o total diário várias vezes. A regra, precisão e valores ausentes dependem de P02; o local do cálculo permanece em aberto. Fontes: D07–D10/P02 em [decisions.md](decisions.md).

**Verificação proposta:** testes JVM de seleção em fronteiras de mês/ano, mais de uma execução no mesmo dia, distribuição semanal preservada, agrupamento diário e não duplicação de totais; incluir ausência de referência e registros incompatíveis conforme regras fechadas. Testar a regra aprovada, não a reprodução dos números das imagens. Fontes das necessidades: [briefing](briefing.md), [contrato](api-contract.md).

### Etapa 2 — Repositório autenticado

**INCERTO — arquivo proposto:** `repository/RoutineRepository.java`, recebendo `AuthRepository` por construtor. Manter os métodos existentes de [ApiService][api]; mudar assinaturas ou acrescentar consultas somente quando o contrato ampliado exigir. Molde: [AbnormalConsumptionRepository][abnormal-repository]; origem da composição: [README](../../README.md).

Executar HTTP por `auth.resource()`, obter a identidade real, conferir vínculo com a conta e mapear falhas com [ApiErrors][errors]. Uma resposta `[]` confirmada deve ser distinta de corpo ausente ou falha. Não alterar Bearer/refresh, timeouts ou armazenamento de tokens para implementar a Rotina. Fontes: [AuthRepository][auth], [SectionsViewModel][sections], [SessionInterceptor][interceptor], [NetworkConfig][network].

**Verificação proposta:** serviço substituído para listar, detalhe, criação, edição e exclusão; falha de rede, códigos de negócio confirmados, body inesperado e dados de outra conta. Verificar o comportamento de DELETE sem body. Reusar a cobertura de sessão existente; só ampliar os testes de sessão se alguma mudança atingir esse contrato. Fontes: [ApiService][api], [SessionInterceptorTest][session-tests], [contrato](api-contract.md).

### Etapa 3 — Estado de tela e formulário

**INCERTO — arquivo proposto:** `viewmodel/RoutineViewModel.java`. Manter dia e mês selecionados, dados carregados e rascunho do editor fora das Views; separar estado de leitura e de mutação para preservar a agenda durante um salvamento. Referências: [AbnormalConsumptionViewModel][abnormal-vm], [SectionsViewModel][sections], [SignupViewModel][signup-vm].

Planejar seleção sem reaproveitar resposta antiga de outro dia/conta, evitar chamadas equivalentes em andamento e observar com o ciclo de vida da View. Após sucesso de mutação, atualizar a projeção/lista e o total sem exigir reentrada na tela. Após falha, preservar o rascunho e permitir nova tentativa conforme política de P11. Fontes: [briefing](briefing.md), [AGENTS.md](../../AGENTS.md), [AbnormalConsumptionFragment][abnormal-fragment].

**INCERTO — P07/P08/P11:** comportamento de reentrada na aba versus rotação, descarte do rascunho, saída durante mutação e resultado sem confirmação. A exigência aprovada é preservar dia/formulário na rotação; persistência após morte do processo não foi definida. Fontes: [briefing](briefing.md), [decisions.md](decisions.md).

**Verificação proposta:** troca rápida de seleção com respostas fora de ordem, mutações concorrentes, resposta após encerramento do ViewModel, alteração de sessão e preservação de rascunho após erro. Rotação real será exercitada também na instrumentação. Fontes: modelo de geração em [AbnormalConsumptionViewModel][abnormal-vm] e requisitos do [briefing](briefing.md).

### Etapa 4 — Calendário, agenda e estados

**INCERTO — arquivos propostos:** `view/fragments/RoutineFragment.java`, layout `fragment_routine.xml`, layout de item de execução, adapter da agenda e recursos `routine_strings.xml`, `routine_styles.xml`, `routine_dimens.xml`. Reutilizar a entrada `goalsFragment`; uma troca da classe desse destino deve preservar o acesso pelo menu existente. Fontes: [main_navigation.xml][navigation], [MainActivity][main], [MainSectionFragment][generic-fragment].

Construir a partir da faixa semanal e agenda das [imagens](design/), sem tratar pixels como dp. Implementar hoje selecionado na entrada, troca de dia, navegação entre meses, lista e total do dia, vazio, carregando, erro e tentar novamente. Textos fixos em recursos. Fontes: [briefing](briefing.md), [agenda](design/habitos.png), [vazio](design/habitosvazio.png).

**INCERTO — P07/P10:** faixa própria com Views/RecyclerView e seletor Material, calendário Android ou biblioteca externa; interação do mês/semanas e composição de tablet/paisagem. Há [MaterialDatePicker no app][profile] e [RecyclerView declarado][build], mas nenhuma dependência nova foi escolhida. Proposta de coluna limitada ou duas áreas nas telas largas continua INCERTO. Fonte: seção de opções em [decisions.md](decisions.md).

Prever rolagem, texto ampliado, alvos de 48dp e fim da agenda acima do menu desde esta etapa. As sete células não podem ser comprimidas indiscriminadamente: sete alvos de 48dp precisam de 336dp úteis. Respeitar as áreas seguras e o teclado coordenados pelo shell. Fontes: [briefing](briefing.md), [MainActivity][main], opções e cálculo geométrico em [decisions.md](decisions.md).

**INCERTO — P09:** alertas de pico, ajuste automático, ignorar, metas e percentual não têm contrato funcional fechado. Não simular esses comportamentos nem assumir sua remoção definitiva. O recorte de implementação/aceite visual correspondente será decidido na etapa que os envolver. Fontes: [alerta](design/habitosAlerta.png), [detalhes](design/detalhesHabito.png), [OpenAPI](openapi.json).

**Verificação proposta:** renderização com agenda vazia, vários horários do mesmo hábito, nomes longos e falha de leitura; conferir seleção, total diário e acesso às ações em telas pequenas/largas. Comparação visual deve registrar diferenças e escolhas ainda INCERTO, sem declarar equivalência apenas por compilar. Fontes: [briefing](briefing.md), [AGENTS.md](../../AGENTS.md).

### Etapa 5 — Detalhes, formulário e mutações

**INCERTO — arquivos propostos:** Fragment/layout de detalhes, Fragment/layout do editor ou diálogo com estado restaurável; nomes e container dependem de P08. O molde de componentes é [SignupHabitsFragment][signup-habits]/[SelectionHelper][selection-helper], e o detalhe visual é [detalhesHabito.png](design/detalhesHabito.png). A implementação antiga do editor em [MainSectionFragment][generic-fragment] serve como referência de operações, não como garantia de preservação do rascunho.

O formulário deve selecionar o tipo, frequência semanal, dias, divisão das ocorrências e horário obrigatório de cada execução, além de mostrar a estimativa calculada conforme regras fechadas. **INCERTO — validações derivadas a definir:** soma das quantidades igual à frequência semanal e quantidade de horários coerente com as execuções; limites, início/fim, sobreposição e duplicidade dependem de P01/P03/P05. Fontes: D03/D04/D09–D12 e [briefing](briefing.md).

Criar/editar só devem apresentar confirmação de sucesso quando houver confirmação do serviço; o formulário precisa continuar recuperável após falha. Excluir exige confirmação e atualização da lista/total. **INCERTO — P04/P11:** escopo da exclusão, idempotência/reconciliação e nova tentativa após timeout. Não repetir automaticamente uma gravação ambígua como regra presumida. Fontes: [briefing](briefing.md), [contrato](api-contract.md), [SectionsViewModel][sections].

**Verificação proposta:** preenchimento/distribuição, erro no campo obrigatório, salvar duas vezes, falha com rascunho preservado, editar dados existentes, cancelar/confirmar exclusão e atualização da agenda. Rotacionar com editor aberto e com teclado, verificando dados e acessibilidade. Fontes: [briefing](briefing.md), [decisions.md](decisions.md).

**INCERTO — migração da UI:** ao conectar o novo fluxo, retirar somente o ramo antigo de Rotina em `MainSectionFragment`/`SectionsViewModel` que deixar de ser utilizado. Não refatorar os fluxos de contas, ranking, consumo ou cadastro nesta tarefa. A mudança deve preservar o CRUD do cadastro. Fontes da responsabilidade compartilhada: [MainSectionFragment][generic-fragment], [SectionsViewModel][sections], [SignupHabitsFragment][signup-habits].

### Etapa 6 — Integração e aceite

Executar build, testes JVM e lint pelo Wrapper, conforme [AGENTS.md](../../AGENTS.md):

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Executar instrumentação com aparelho/emulador de teste:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Instrumentação exige ambiente de teste apropriado. Consultar os testes envolvidos antes de executar: testes de autenticação existentes manipulam a sessão do app de teste. Fontes: [AGENTS.md](../../AGENTS.md), [AUTH_MOBILE.md][auth-doc]. Os comandos são planejamento; não foram executados nesta etapa documental.

| Verificação | Evidência esperada |
| --- | --- |
| CRUD real com API ampliada | Criar, reler, editar e excluir hábito em conta/ambiente de teste acordados; confirmar distribuição e horários após recarga, sem dados de demonstração na produção |
| Isolamento | Identidade real, conta correta e tratamento de sessão alterada; não apresentar dados de outra conta |
| Agenda e cálculo | Dia/mês correto, mais de uma execução, estimativas identificadas e totais sem duplicação |
| Resiliência | Falhas de leitura/salvamento/exclusão, tentativas conforme política definida, rascunho preservado e nenhum loading infinito |
| Rotação/IME | Dia e formulário preservados, campos/ações alcançáveis com teclado aberto, menu conforme shell |
| Layout | ~360dp, ~412dp e ~800dp, retrato/paisagem e fonte normal/ampliada; capturas e limites reais registrados |
| Acessibilidade | Descrições, ordem de leitura, seleção perceptível sem depender só da cor, alvos mínimos e revisão com TalkBack |

Fonte dos requisitos: [briefing](briefing.md), [AGENTS.md](../../AGENTS.md), [contrato](api-contract.md). **INCERTO — especificação de execução:** Androids do aparelho/emulador, escala ampliada exata e confirmação de perfil em dp versus diagonal física do tablet serão registrados no aceite, sem inventar matriz oficial além do briefing.

**Entrega proposta:** relatório de aceite documental com resultados, capturas e limites; assinalar os critérios do briefing somente quando houver evidência. **INCERTO — nome proposto:** `docs/rotina/acceptance.md`, a criar na etapa de validação, não nesta documentação. Testes com serviço substituído não comprovam integração; build não comprova aparência ou instalação. Fontes: [AGENTS.md](../../AGENTS.md), precedente de evidências em [aceite do chatbot](../chatbot/acceptance.md).

## Rastreabilidade e definições adiadas

| Requisito ou decisão | Etapas | Definições INCERTO que afetam a entrega |
| --- | --- | --- |
| Calendário, dia atual e troca de mês | 1, 3, 4, 6 | P04/P07/P10 |
| Distribuição semanal por dia | 0, 1, 3, 5, 6 | P01/P05/P06 |
| Horário obrigatório por execução | 0, 1, 3, 5, 6 | P01/P03/P08 |
| Média fixa e total diário | 0, 1, 4, 5, 6 | P02; tabela já aprovada |
| Detalhar/criar/editar/excluir | 0, 2, 3, 5, 6 | P04/P05/P06/P08/P11 |
| Erros, nova tentativa e preservar rascunho | 2, 3, 4, 5, 6 | P05/P08/P11 |
| Rotação, teclado, responsividade e acessibilidade | 3, 4, 5, 6 | P07/P08/P10 |
| Metas e alertas das imagens | 0 ou etapa visual correspondente, 6 | P09; inclusão/exclusão funcional ainda não decidida |

Fontes: [briefing](briefing.md), decisões D03–D12 e pendências P01–P11 em [decisions.md](decisions.md). A definição entre apenas início e intervalo início/fim fica explicitamente **INCERTO**, conforme D13; não impede encerrar este plano e deve ser retomada antes de fixar contrato, validação e representação visual dependentes.

## Resultado desta etapa documental

Criado este plano e atualizados [decisions.md](decisions.md) e [api-contract.md](api-contract.md) para registrar D13 e o adiamento das definições restantes como **INCERTO**. Sem mudanças no código, dependências, backend ou OpenAPI; sem build, testes ou chamadas à API nesta etapa. Origem do escopo: pedido da pessoa usuária registrado em D13.

[api]: ../../app/src/main/java/com/aula/delta_mobile_app/services/api/ApiService.java
[api-models]: ../../app/src/main/java/com/aula/delta_mobile_app/model/ApiModels.java
[sections]: ../../app/src/main/java/com/aula/delta_mobile_app/viewmodel/SectionsViewModel.java
[generic-fragment]: ../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/MainSectionFragment.java
[navigation]: ../../app/src/main/res/navigation/main_navigation.xml
[auth]: ../../app/src/main/java/com/aula/delta_mobile_app/repository/AuthRepository.java
[retrofit]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/RetrofitClient.java
[interceptor]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/SessionInterceptor.java
[network]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/NetworkConfig.java
[errors]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/ApiErrors.java
[screen-state]: ../../app/src/main/java/com/aula/delta_mobile_app/model/ScreenState.java
[notice]: ../../app/src/main/java/com/aula/delta_mobile_app/view/StatusNoticeView.java
[auth-flow]: ../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/AuthFlowFragment.java
[signup-habits]: ../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/SignupHabitsFragment.java
[selection-helper]: ../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/SelectionHelper.java
[main]: ../../app/src/main/java/com/aula/delta_mobile_app/view/activities/main/MainActivity.java
[abnormal-repository]: ../../app/src/main/java/com/aula/delta_mobile_app/repository/AbnormalConsumptionRepository.java
[abnormal-data]: ../../app/src/main/java/com/aula/delta_mobile_app/model/AbnormalConsumptionData.java
[abnormal-vm]: ../../app/src/main/java/com/aula/delta_mobile_app/viewmodel/AbnormalConsumptionViewModel.java
[abnormal-fragment]: ../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/AbnormalConsumptionFragment.java
[session-adapter]: ../../app/src/main/java/com/aula/delta_mobile_app/view/ChatSessionAdapter.java
[signup-vm]: ../../app/src/main/java/com/aula/delta_mobile_app/viewmodel/SignupViewModel.java
[session-tests]: ../../app/src/test/java/com/aula/delta_mobile_app/services/network/SessionInterceptorTest.java
[profile]: ../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/SignupProfileFragment.java
[build]: ../../app/build.gradle.kts
[auth-doc]: ../../devdocs/fluxodavi/AUTH_MOBILE.md
