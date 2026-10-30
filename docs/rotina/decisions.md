# Decisões da feature Rotina

## Fontes e estado — atualizado em 29/10/2026

Este registro distingue requisito aprovado, capacidade observada e proposta. Uma decisão de produto não significa que a API já a suporte. Fontes: pedido e respostas da pessoa usuária nesta conversa, [briefing](briefing.md), [OpenAPI](openapi.json), [contrato analisado](api-contract.md) e [AGENTS.md](../../AGENTS.md). **INCERTO** identifica dedução ou proposta ainda não aprovada.

**Etapa 0, recorte de leitura especificado em 29/10/2026:** D14–D23 registram a autorização e as confirmações posteriores ao planejamento. Elas substituem apenas as pendências explicitamente resolvidas nos registros históricos D01–D13. A tabela P01–P11 abaixo reflete o estado atual. Não houve implementação Android/backend, chamada ao serviço ou atualização do OpenAPI oficial.

## D01 — Etapa documental

**Decidido pela pessoa usuária:** analisar as fontes na ordem solicitada e escrever somente `docs/rotina/api-contract.md` e `docs/rotina/decisions.md`. Não escrever código, alterar dependências, implementar telas ou antecipar outras etapas. Fonte: pedido inicial desta conversa em 28/10/2026.

**Ampliação documental em 29/10/2026:** a pessoa usuária pediu para adiar as decisões restantes, marcá-las como INCERTO e fechar o plano. O escopo passa a incluir [plan.md](plan.md), mantendo a restrição de não implementar nesta etapa. Fonte: pedido transcrito em D13.

**Executado nesta etapa:** leitura estática do checkout, inspeção das quatro imagens locais e consulta às referências oficiais Android de [MaterialDatePicker](https://developer.android.com/reference/com/google/android/material/datepicker/MaterialDatePicker) e [CalendarView](https://developer.android.com/reference/android/widget/CalendarView) para comparar opções. Sem chamadas à API Delta, build, testes ou instalação. As imagens são exportações locais; não foi fornecido link/arquivo Figma navegável nesta solicitação. Fontes visuais: [design/](design/).

## D02 — Requisitos de calendário, gestão e qualidade

**Requisitos recebidos:** mostrar hábitos e água por hábito com unidade; selecionar hoje ao abrir; navegar entre meses; selecionar dia; listar hábitos daquele dia e total; mostrar vazio; criar/editar/excluir, com confirmação de exclusão; atualizar lista/total após mutações; validar campos; erro com nova tentativa e sem perder o digitado; preservar dia/formulário na rotação; textos em recursos; suporte a celular/tablet em retrato/paisagem, ~360dp/~412dp/~800dp; teclado sem cobrir campos; acessibilidade e toque mínimo de 48dp. **Fora do escopo explícito:** marcar hábito como concluído. Fonte: [briefing](briefing.md).

Esses são requisitos, sem declaração de aceite ou funcionamento atual. O briefing não define o layout exato do formulário, a distribuição por dia ou as regras de dados ausentes. Fontes: [briefing](briefing.md), [design/](design/), [contrato](api-contract.md).

## D03 — Significado da recorrência

**Decidido pela pessoa usuária:** `frequency` significa a quantidade de vezes em que o hábito acontece na semana; `daysOfWeek` indica os dias em que ele acontece.

Fonte direta, resposta à pergunta sobre os campos nesta conversa: “frequency é quantas vezes ele acontece na semana, e daysOfWeek é o dia em que ele acontece”.

**Limite da confirmação:** isso esclarece o significado de produto, mas não acrescenta constraints ou algoritmo ao OpenAPI. Não se presume uma ocorrência por dia selecionado, frequência igual à quantidade de dias, nem frequência máxima de sete. Fonte técnica: [OpenAPI](openapi.json), `CreateUserHabitRequest`, `UpdateUserHabitRequest`, `UserHabitIO`; complemento aprovado em D04.

## D04 — Formulário para distribuir a frequência entre os dias

**Decidido pela pessoa usuária:** no exemplo `frequency=5` com segunda e quarta selecionadas, deve aparecer um formulário para a pessoa distribuir as cinco ocorrências entre esses dois dias. Não impedir essa combinação apenas porque há dois dias selecionados.

Fonte direta, resposta nesta conversa: “deve aparecer um formulario para ele dividir essas 5 entre os dois dias”.

**Capacidade atual:** o OpenAPI só possui a frequência global e o array de IDs dos dias. Não define quantidade por dia nem representação da distribuição. Não se decidiu repetir IDs em `daysOfWeek`, criar registros duplicados ou persistir essa informação apenas no dispositivo. Fonte: [contrato](api-contract.md#recorrência-requisito-confirmado-e-capacidade-publicada), baseado no [OpenAPI](openapi.json).

**Atualização em 29/10/2026:** a ampliação da API para persistir/recuperar a distribuição foi aprovada em D06. **PENDENTE:** representação exata no contrato; se cada dia selecionado exige quantidade positiva; limites e distribuição inicial; como associar horários e litros a várias ocorrências no mesmo dia. **INCERTO — dedução:** a soma das quantidades deverá corresponder à frequência semanal para representar a divisão solicitada, mas a regra formal de validação ainda precisa ser confirmada. Fonte da dedução: resposta acima; a regra não está no [OpenAPI](openapi.json).

## D05 — Reaproveitamento da integração existente

**Diretriz recebida:** partir da API já consumida pelo app e reaproveitar cliente, autenticação e tratamento de erros. Fonte: pedido inicial e [AGENTS.md](../../AGENTS.md), seções Rede e Arquitetura.

**Verificado no checkout:** os cinco endpoints já estão em [ApiService][api]; o cliente vem de [AuthRepository][auth] por [RetrofitClient][retrofit], com [SessionInterceptor][interceptor], [NetworkConfig][network] e [ApiErrors][errors]. A Rotina existente em [MainSectionFragment][fragment]/[SectionsViewModel][sections] já usa esse serviço e não tem calendário. Os DTOs em [ApiModels][models] ainda não incluem os horários, litros e categoria do OpenAPI atualizado. Detalhes em [api-contract.md](api-contract.md).

**INCERTO — proposta técnica, sem aprovação de implementação:** manter `ApiService` e extrair o acesso de Rotina para um novo `RoutineRepository`, seguindo [README](../../README.md) e [AbnormalConsumptionRepository][reference-repository]. Hoje não há repositório dedicado de hábitos; o HTTP está no ViewModel genérico. Nenhum novo endpoint, classe, migração ou refatoração foi aprovado nesta etapa.

## D06 — Ampliação da API para a distribuição por dia

**Decidido pela pessoa usuária em 29/10/2026:** considerar a ampliação da API para receber, persistir e devolver a quantidade de ocorrências de cada dia, permitindo recuperar a divisão feita no formulário de D04.

Fonte direta: após a explicação da ampliação necessária para representar, por exemplo, duas ocorrências na segunda e três na quarta, a pessoa usuária respondeu “pode contar com a ampliação então”.

**Estado:** direção aprovada para o contrato futuro. O [OpenAPI local](openapi.json) continua descrevendo somente frequência semanal global e IDs de dias; a aprovação não comprova suporte já publicado. O exemplo dois/três não fixa a distribuição: ela será escolhida pela pessoa no formulário, conforme D04.

**PENDENTE:** nomes e estrutura dos campos, validações, compatibilidade com hábitos existentes e definição de quem expande a recorrência em ocorrências de calendário. D06 não definiu endpoints de consulta por dia/mês. O significado diário dos litros foi confirmado posteriormente em D07, e a possibilidade de horário por execução, em D11. Esta atualização permanece documental, conforme D01.

## D07 — Litros estimados como total diário do hábito

**Decidido pela pessoa usuária em 29/10/2026:** os litros estimados de um hábito representam seu total no dia.

Fonte direta: à pergunta “os litros estimados de um hábito representam uma execução, o total do dia ou o total da semana?”, a pessoa usuária respondeu “total do dia”.

**INCERTO — consequência para o cálculo, deduzida dessa definição:** multiplicar esse total diário pela quantidade de execuções do mesmo hábito no dia contaria o consumo novamente. O cálculo da agenda deve respeitar a unidade diária; faltam definir tratamento de estimativas ausentes, precisão e representação dos valores na API ampliada.

**Atualização em D08–D10:** a estimativa será calculada com média fixa por execução de cada hábito, usando a tabela da IA aprovada em D10; o local do cálculo ainda não foi decidido. **PENDENTE:** regra de cálculo diário e representação na API ampliada. O [OpenAPI atual](openapi.json) tem somente um `estimatedLiters` no registro e não documenta sua unidade temporal. D07 define o requisito de produto, sem comprovar a semântica já implementada no backend.

## D08 — Estimativa calculada; local e fórmula pendentes

**Decidido pela pessoa usuária em 29/10/2026:** o consumo estimado será calculado. **INCERTO — hipótese expressa pela pessoa usuária:** o cálculo provavelmente ficará na lógica mobile, mas ainda não há confirmação sobre cálculo na API.

Fonte direta: “vai ser calculado, mas nao tenho certeza se vai ser na lógica no mobile ou se tem na api, provavelmente na lógica do mobile”. Isso confirma cálculo automático, sem escolher o local, a fórmula ou seus parâmetros.

**Verificação estática complementar:** o [OpenAPI da Rotina](openapi.json) recebe e devolve `estimatedLiters`, mas não documenta fórmula, parâmetros de estimativa ou endpoint específico de cálculo. O fluxo mobile existente envia apenas hábito, frequência e dias em [SectionsViewModel][sections]; os DTOs de hábitos em [ApiModels][models] não representam `estimatedLiters`. Essas fontes não comprovam cálculo automático no backend Delta.

No outro repositório local, a ferramenta `estimate_habit_water_impact` usa `_HABIT_LITERS_PER_SESSION`, uma tabela de valores por execução, e calcula totais semanal e mensal a partir da frequência. Não recebe duração, vazão ou distribuição de quantidades por dia. O arquivo descreve os valores como médias de referência, sem citar nele uma fonte para a tabela. Fonte: [ferramentas de hábitos da IA][ai-habit-tools]. Essa ferramenta interna não é um endpoint de cálculo da Rotina declarado no [OpenAPI](openapi.json); o serviço de chat tem outro contrato, em [schemas da IA][ai-schemas]. O achado é leitura local, sem execução nem verificação do deploy.

**Atualização em D09/D10:** média fixa por execução escolhida e tabela da IA aprovada como estimativa inicial. **PENDENTE:** regra de cálculo diário, precisão/ausência e decisão mobile ou backend. A adoção dos valores não exige chamar o chatbot; a forma de implementação continua pendente e esta etapa permanece documental.

## D09 — Média fixa por execução de cada hábito

**Decidido pela pessoa usuária em 29/10/2026:** usar média fixa por execução de cada tipo de hábito como base da estimativa.

Fonte direta: à pergunta “Para a Rotina, você quer usar uma média fixa por execução de cada hábito ou uma estimativa que varie conforme a duração informada?”, a pessoa usuária respondeu “acho melhor uma média fixa”.

Essa escolha definiu a base da estimativa; a tabela específica foi aprovada posteriormente em D10. O local do cálculo continua pendente. D07 permanece: o valor diário apresentado representa o total do hábito no dia.

**INCERTO — regra derivada proposta:** obter o total diário do hábito multiplicando sua média por execução pela quantidade de ocorrências distribuídas naquele dia, uma única vez. O total diário já pronto não deve receber uma segunda multiplicação pela mesma quantidade. Fontes da dedução: D04, D07 e a escolha acima. Precisão, arredondamento e tratamento de referência ausente continuam pendentes.

**Referência encontrada e posteriormente aprovada em D10:** a tabela `_HABIT_LITERS_PER_SESSION` da [IA][ai-habit-tools] usa banho longo=80 L, lavar quintal=150 L, lavar roupa=100 L, regar plantas=15 L, lavar carro=200 L e lavar louça=20 L por execução. O arquivo não cita uma fonte para os valores; sua adoção na Rotina decorre da decisão explícita da pessoa usuária registrada abaixo.

## D10 — Tabela inicial de litros por execução aprovada

**Decidido pela pessoa usuária em 29/10/2026:** usar a tabela existente na IA como estimativa inicial da Rotina.

Fonte direta: após receber a tabela com os seis valores e a pergunta “Usamos essa tabela como estimativa inicial ou você prefere outra referência?”, a pessoa usuária respondeu “pode usar essa tabela mesmo”.

| Tipo de hábito | Estimativa por execução |
| --- | --- |
| Banho longo | 80 L |
| Lavar quintal | 150 L |
| Lavar roupa | 100 L |
| Regar plantas | 15 L |
| Lavar carro | 200 L |
| Lavar louça | 20 L |

Fonte técnica dos valores: `_HABIT_LITERS_PER_SESSION` nas [ferramentas da IA][ai-habit-tools]. A decisão aprova parâmetros de estimativa; esses valores não são medições da conta nem os números ilustrativos do Figma. D07 continua definindo o resultado exibido como total diário do hábito.

**PENDENTE:** local do cálculo, representação no contrato ampliado, precisão/arredondamento e tratamento de tipo de hábito sem valor na tabela. A aprovação desta tabela não altera o OpenAPI nem autoriza implementação nesta etapa documental.

## D11 — Horário individual por execução

**Decidido pela pessoa usuária em 29/10/2026:** permitir que a pessoa informe um horário para cada execução do hábito, inclusive quando ele acontece mais de uma vez no mesmo dia.

Fonte direta: à pergunta sobre dois horários separados (por exemplo, 7h e 20h) ou apenas um horário para o hábito no dia, a pessoa usuária respondeu “ele pode informar um horário para cada execução”. Os horários do exemplo não são valores predefinidos.

**Capacidade atual:** o [OpenAPI](openapi.json) só oferece um `startTime` e um `endTime` por registro de hábito, sem coleção de horários por execução ou por dia. **INCERTO — consequência técnica:** a representação futura precisa relacionar os horários às execuções distribuídas em D04 para preservar essa informação ao salvar e consultar. A estrutura de campos ainda será definida; não foi alterado o OpenAPI.

**Atualização em D12:** horário obrigatório em cada execução. **PENDENTE:** se cada execução terá somente início ou intervalo início/fim, duração, formato/fuso, meia-noite e tratamento de horários iguais/sobrepostos. O cálculo continua baseado na média fixa escolhida em D09/D10; horário individual não aprova cálculo por duração.

## D12 — Horário obrigatório em cada execução

**Decidido pela pessoa usuária em 29/10/2026:** é obrigatório informar horário para cada execução do hábito.

Fonte direta: à pergunta “Informar o horário será obrigatório ou será possível salvar execuções sem horário?”, a pessoa usuária respondeu “tem que informar o horario”. Essa definição complementa D11; não define ainda se é necessário somente início ou início e fim.

O [OpenAPI atual](openapi.json) continua declarando `startTime` e `endTime` opcionais e únicos por registro. A obrigatoriedade por execução é requisito para o formulário e o contrato ampliado, sem alteração do schema nesta etapa. Falta definir como tratar hábitos já cadastrados sem horário durante a transição; não foram escolhidos horários padrão.

## D13 — Adiar decisões restantes e fechar o plano

**Decidido pela pessoa usuária em 29/10/2026:** encerrar a rodada de perguntas agora, manter as definições restantes como **INCERTO** e concluir o plano documental da feature.

Fonte direta: “a gente pode tomar essas decisoes depois marque como incerto e vamos fechar o plano”.

As decisões já registradas permanecem válidas. As pendências abaixo e os trechos anteriores identificados como PENDENTE devem ser lidos como **INCERTO — definição adiada**, não como requisito aprovado nem como permissão para escolher silenciosamente um comportamento. O [plano](plan.md) associa essas definições às etapas dependentes. Nenhuma nova resposta é exigida para concluir esta documentação; a implementação continua fora do escopo desta etapa.

## D14 — Execução autorizada somente da etapa 0

**Decidido pela pessoa usuária em 29/10/2026:** executar somente a etapa 0 do [plano](plan.md#etapa-0--confirmar-o-contrato-por-recorte), sem commit e sem alterar arquivos fora desse recorte. Fonte direta: “Implemente APENAS a etapa 0 do plano” e “não faça o commit: eu reviso o diff antes”. Esta autorização atualiza o limite documental de D01/D13 apenas para essa etapa; não autoriza código Android nem alterações no backend.

À pergunta sobre uma fonte oficial do contrato ampliado ou definição conjunta, a pessoa usuária respondeu “Vamos definir juntos agora”. As respostas seguintes especificam requisitos acordados para a ampliação, sem comprovar implementação ou publicação. O `openapi.json` só será atualizado com fonte oficial, conforme a etapa 0. O commit previsto continua `docs: definir contrato confirmado para o recorte inicial`, sem execução nesta entrega.

**Estado da etapa:** especificação acordada do recorte inicial de leitura concluída em D15–D23, com exemplos em [api-contract.md](api-contract.md#etapa-0--especificação-acordada-do-recorte-de-leitura--29102026). Distribuição, horários, legado, projeção, cálculo e catálogo foram definidos. Implementação/publicação do backend, correspondência do catálogo no ambiente e snapshot oficial ampliado continuam sem comprovação. Regras exclusivas de escrita continuam pendentes para as etapas dependentes. Esta conclusão não declara integração real nem autoriza a etapa 1.

## D15 — Somente início obrigatório por execução

**Decidido pela pessoa usuária em 29/10/2026:** cada execução terá apenas horário de início, sem horário de fim obrigatório nem duração a informar. Fonte direta: à pergunta “Cada execução terá apenas um horário de início (por exemplo, 07:00) ou um intervalo com início e fim (07:00–07:20)?”, a pessoa usuária respondeu “Apenas horário de início”. A pergunta esclareceu que a estimativa continua baseada na média fixa por execução aprovada em D09/D10.

Esta decisão resolve a alternativa início versus intervalo em D11/D12/P03. Não define formato de transporte, precisão, fuso, horários iguais ou tratamento de registros anteriores. Não deduzir duração, fim ou consumo por tempo a partir desse início. O par opcional `startTime`/`endTime` no OpenAPI continua descrevendo o contrato anterior por registro, sem comprovar suporte a horários individuais.

## D16 — Distribuição representada pelos horários de cada dia

**Decidido pela pessoa usuária em 29/10/2026:** representar a distribuição como lista de dias únicos, cada um com pelo menos um horário de execução; a soma dos horários deve ser igual à frequência semanal. Fonte direta: à pergunta com essa estrutura e validação, incluindo o exemplo de frequência cinco com dois horários na segunda e três na quarta, a pessoa usuária respondeu “Sim, usar essa estrutura e validação”.

A quantidade diária é o tamanho da lista de horários daquele dia. Não manter quantidade independente que possa divergir dessa lista, repetir IDs de dias ou duplicar registros para representar execuções. Esta decisão resolve a estrutura conceitual e a igualdade antes inferida em D04; não comprova suporte publicado nem define sozinha os nomes dos campos JSON. A associação com o formato de horário está em D17.

## D17 — Hora local em HH:mm

**Decidido pela pessoa usuária em 29/10/2026:** horários no formato de 24 horas `HH:mm`, de `00:00` a `23:59`, sem segundos nem conversão automática de fuso. Fonte direta: à pergunta com essas condições e o exemplo de `07:00` continuar aparecendo como `07:00`, a pessoa usuária respondeu “Sim, hora local em HH:mm”.

O valor representa hora local da rotina, não instante UTC nem duração. D15 mantém somente início por execução. Horários iguais e legado ficaram pendentes neste ponto da conversa e foram resolvidos posteriormente em D22 e D19. Esta decisão não escolhe o fuso de “hoje” do calendário em P07 nem altera o formato não especificado do OpenAPI anterior.

## D18 — Cálculo diário no mobile e ausência de referência

**Decidido pela pessoa usuária em 29/10/2026:** calcular no mobile com a tabela de D10; total diário do hábito = litros por execução × número de horários naquele dia; total do dia = soma dos totais diários dos hábitos. Para tipo sem referência, mostrar estimativa indisponível e total incompleto, sem substituir por zero. Os seis valores atuais geram litros inteiros, sem arredondamento. Fonte direta: à pergunta que apresentou essas regras, a pessoa usuária respondeu “Sim, adotar essas regras”.

Esta decisão resolve local, fórmula, unidade, ausência de referência e precisão para a tabela inicial em P02. Duas execuções de banho longo representam 80 L por execução e 160 L no resumo do hábito naquele dia; não multiplicar os 160 L novamente pela quantidade. Esses números são exemplos aritméticos com a estimativa aprovada, não medições da conta. `estimatedLiters` do contrato anterior não tem unidade temporal confirmada e não substitui esse cálculo. Referências fracionárias futuras exigem nova definição de precisão; não fazem parte da tabela atual.

## D19 — Legado preservado com agenda incompleta

**Decidido pela pessoa usuária em 29/10/2026:** hábitos antigos sem distribuição ou horários completos permanecem identificados como “agenda incompleta”, fora das linhas por horário, sem inventar dias/horários. Enquanto existirem, a agenda e o total serão sinalizados como incompletos, inclusive se não houver outras execuções no dia. Fonte direta: à pergunta com essas condições, a pessoa usuária respondeu “Sim, preservar e sinalizar como incompletos”.

Não reconstruir a distribuição dividindo a frequência entre os dias, copiando o horário único do contrato anterior ou criando horários padrão. Uma coleção com esses registros não comprova dia livre nem total zero. A decisão define o tratamento de leitura e não executa migração, atualização ou exclusão dos dados antigos. A apresentação visual do aviso será implementada na etapa correspondente.

## D20 — Semana padrão projetada em qualquer data

**Decidido pela pessoa usuária em 29/10/2026:** aplicar a semana padrão a qualquer data, inclusive passada. Fonte direta: à escolha entre data de início inclusiva com repetição sem fim e aplicar a semana padrão a qualquer data, a pessoa usuária selecionou “Aplicar a semana padrão a qualquer data, inclusive passada”.

Para o recorte de leitura, selecionar a distribuição correspondente ao dia da semana da data consultada, sem início/fim de vigência. A agenda é projeção da rotina cadastrada; não é histórico de execução, medição ou comprovação de hábito realizado no passado. Não acrescentar `startDate` nem reconstruir versões históricas. Esta decisão resolve a projeção de leitura em P04; semântica de PUT/DELETE, série versus ocorrência e seus efeitos continua para os recortes de escrita.

## D21 — Catálogo fechado para o recorte inicial

**Decidido pela pessoa usuária em 29/10/2026:** limitar os tipos aos seis do catálogo local, IDs 1 a 6, na ordem banho longo, lavar quintal, lavar roupa, regar plantas, lavar carro e lavar louça; dias de 1=segunda até 7=domingo. Fonte direta: à pergunta com esse mapeamento e a ressalva de que a correspondência no banco publicado continua sem comprovação, a pessoa usuária respondeu “Sim, usar apenas esse catálogo”.

Não criar tipo/nome/categoria livre nesse recorte. Fonte local dos IDs: [registration_catalog.json][catalog]. A estimativa usa o mapeamento explícito desses tipos à tabela de D10, sem correspondência por aproximação textual. Esta é a especificação desejada; não comprova IDs válidos no banco publicado. Tipo recebido sem referência não vira zero, conforme D18.

## D22 — Horários distintos dentro do mesmo hábito/dia

**Decidido pela pessoa usuária em 29/10/2026:** impedir horários repetidos para o mesmo hábito no mesmo dia; permitir que hábitos diferentes tenham o mesmo horário. Fonte direta: à pergunta com o exemplo de dois banhos às `07:00` na segunda versus banho e regar plantas às `07:00`, a pessoa usuária respondeu “Sim, impedir repetição no mesmo hábito/dia”.

Não há intervalo/duração para avaliar sobreposição. Essa regra não resolve a duplicidade de registros do mesmo tipo na conta, nem substitui o critério de conflito do POST/PUT em P05.

## D23 — Payload de leitura ampliado acordado

**Decidido pela pessoa usuária em 29/10/2026:** aprovar os nomes e a estrutura de leitura `schedule`, `dayOfWeek` e `startTimes`, com `daysOfWeek` e `frequency` coerentes com a lista. Fonte direta: após o exemplo JSON com duas execuções na segunda e três na quarta, à pergunta sobre sua aprovação como especificação para o backend, preservando o OpenAPI e sem declarar publicação, a pessoa usuária respondeu “Sim, aprovar esse payload de leitura”.

`schedule` associa cada dia único à lista de inícios individuais. `daysOfWeek` representa exatamente os dias dessa lista, sem repetição; `frequency` corresponde ao total de horários. Os exemplos completos e as regras de projeção/estimativa estão em [api-contract.md](api-contract.md#etapa-0--especificação-acordada-do-recorte-de-leitura--29102026). Sem `schedule` completo, aplicar D19, sem inferir distribuição pelos campos antigos.

Preservar as rotas existentes de listagem/detalhe, sem parâmetros de data/mês inventados. O contrato acordado ainda precisa ser implementado e confirmado pelo backend; o snapshot oficial local permanece anterior. POST/PUT/DELETE e seus exemplos de releitura serão especificados nos recortes de escrita, conforme a etapa 0, sem presumir que um request de escrita deva copiar este response.

## Pendências — estado após a etapa 0

| ID | Decisão/definição necessária | Origem |
| --- | --- | --- |
| P01 | **ESPECIFICADA para leitura em D16/D19/D23:** distribuição recuperável por `schedule`, dias únicos com horários, coerência com dias/frequência e legado incompleto. **PENDENTE DE BACKEND:** implementação/publicação e OpenAPI oficial. **INCERTO para escrita:** schemas e demais validações de POST/PUT | [OpenAPI](openapi.json), D16/D19/D23 |
| P02 | **RESOLVIDA para leitura em D18/D19:** mobile, média × quantidade diária e soma da agenda, litros inteiros sem arredondamento; referência/agenda ausente deixa estimativa indisponível/total incompleto. Suporte publicado não comprovado | D07–D10/D18/D19, [OpenAPI](openapi.json), [briefing](briefing.md) |
| P03 | **ESPECIFICADA para leitura em D15/D17/D19/D22:** apenas início obrigatório, hora local `HH:mm` de `00:00` a `23:59`, sem segundos/conversão de fuso/duração; sem repetição dentro do hábito/dia; legado incompleto sem horários inventados. Suporte publicado não comprovado | D15/D17/D19/D22, [OpenAPI](openapi.json) |
| P04 | **RESOLVIDA para leitura em D20:** semana padrão em qualquer data passada/futura, sem vigência nem histórico de execução. **INCERTO para escrita:** semântica e efeitos de editar/excluir série ou ocorrência | D20, [OpenAPI](openapi.json), [briefing](briefing.md) |
| P05 | **INCERTO:** contrato real de erros por campo, limites, omissão no PUT e critério de duplicidade | [OpenAPI](openapi.json), [ApiErrors][errors] |
| P06 | **ESPECIFICADA em D21:** apenas seis tipos locais, IDs 1–6, dias 1–7, sem tipos livres. **PENDENTE DE BACKEND:** comprovar correspondência no ambiente; aprovação não comprova banco/deploy | D21, [catálogo][catalog], [AUTH_MOBILE.md][auth-doc], [OpenAPI](openapi.json) |
| P07 | **INCERTO:** interação da faixa semanal, seletor de mês, semanas de fronteira, marcações, limites de navegação, fuso de “hoje” e acesso a adicionar na agenda preenchida | [agenda](design/habitos.png), [vazio](design/habitosvazio.png), [briefing](briefing.md) |
| P08 | **INCERTO:** layout de criar/editar/distribuir, estado de salvamento, descarte/retorno e erro com nova tentativa sem perder o formulário | [design/](design/), [briefing](briefing.md), [MainSectionFragment][fragment] |
| P09 | **INCERTO:** fonte e escopo de “Meta ideal”, percentual/meta máxima, alerta de pico, “Ajustar automático” e “Ignorar”; não foi aprovada exclusão nem inclusão funcional desses itens | [agenda](design/habitos.png), [alerta](design/habitosAlerta.png), [detalhes](design/detalhesHabito.png), [OpenAPI](openapi.json) |
| P10 | **INCERTO:** composição de tablet/paisagem e escolha do calendário; nenhuma dependência nova foi escolhida | [briefing](briefing.md), [design/](design/), [build][build] |
| P11 | **INCERTO:** política para resultado incerto de mutação/timeout e atualização após mudanças feitas por outro fluxo/dispositivo; não há idempotency key/versionamento publicado | [OpenAPI](openapi.json), [SectionsViewModel][sections]; contexto do chatbot em [decisions.md](../chatbot/decisions.md), D08 |

## Opções de calendário e responsividade ainda não aprovadas

O app já usa `MaterialDatePicker` no [SignupProfileFragment][profile] com tema em [selection_styles.xml][selection-styles]. Isso é seleção em diálogo, sem componente existente de agenda semanal/horária na Rotina. A dependência Material e o RecyclerView já estão declarados no [build][build]. Fontes adicionais: [ApiService][api], [MainSectionFragment][fragment], [catálogo de dependências][versions].

**INCERTO — opções técnicas:** compor faixa semanal e agenda com Views/RecyclerView existentes, usando MaterialDatePicker para escolher datas; usar o `CalendarView` do Android para calendário padrão com adaptação visual; ou avaliar biblioteca externa de calendário/agenda. As duas primeiras não exigem uma nova biblioteca de calendário. A terceira exigiria avaliação de compatibilidade, licença, manutenção e acessibilidade antes de escolha. Fontes das capacidades base: [MaterialDatePicker oficial](https://developer.android.com/reference/com/google/android/material/datepicker/MaterialDatePicker), [CalendarView oficial](https://developer.android.com/reference/android/widget/CalendarView), [build][build]. Não há recomendação de pacote/versão nem decisão tomada.

**INCERTO — proposta de composição:** coluna com largura limitada e rolagem nas telas estreitas; em tablet/paisagem, coluna central limitada ou calendário e agenda lado a lado conforme espaço disponível, sem escalar a captura de celular. É preciso aprovar composição, largura e ponto de troca. O requisito aprovado é funcionar nas duas orientações sem cortes, não adotar duas colunas. Fontes: [briefing](briefing.md), [design/](design/); precedente de largura limitada no [layout do chat][chat-layout], sem transformá-lo em padrão universal.

Sete alvos independentes de 48dp exigem ao menos 336dp úteis. **INCERTO — consequência geométrica para a proposta:** em 360dp com margens de 16dp, os 328dp úteis não acomodam sete alvos de 48dp lado a lado; será necessário decidir margens menores, rolagem ou outra apresentação acessível. Os pixels das imagens não definem dp. Fontes do requisito: [briefing](briefing.md); margens existentes: [dimens.xml][dimens].

[api]: ../../app/src/main/java/com/aula/delta_mobile_app/services/api/ApiService.java
[auth]: ../../app/src/main/java/com/aula/delta_mobile_app/repository/AuthRepository.java
[retrofit]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/RetrofitClient.java
[interceptor]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/SessionInterceptor.java
[network]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/NetworkConfig.java
[errors]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/ApiErrors.java
[fragment]: ../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/MainSectionFragment.java
[sections]: ../../app/src/main/java/com/aula/delta_mobile_app/viewmodel/SectionsViewModel.java
[models]: ../../app/src/main/java/com/aula/delta_mobile_app/model/ApiModels.java
[reference-repository]: ../../app/src/main/java/com/aula/delta_mobile_app/repository/AbnormalConsumptionRepository.java
[catalog]: ../../app/src/main/assets/registration_catalog.json
[auth-doc]: ../../devdocs/fluxodavi/AUTH_MOBILE.md
[build]: ../../app/build.gradle.kts
[versions]: ../../gradle/libs.versions.toml
[profile]: ../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/SignupProfileFragment.java
[selection-styles]: ../../app/src/main/res/values/selection_styles.xml
[chat-layout]: ../../app/src/main/res/layout/fragment_chat.xml
[dimens]: ../../app/src/main/res/values/dimens.xml
[ai-habit-tools]: <C:/Users/joaopsouza-ieg/OneDrive - Instituto J&F/Área de Trabalho/2° SÉRIE/INTER/delta-artificial-intelligence/app/tools/habits/tools.py>
[ai-schemas]: <C:/Users/joaopsouza-ieg/OneDrive - Instituto J&F/Área de Trabalho/2° SÉRIE/INTER/delta-artificial-intelligence/app/schemas.py>
