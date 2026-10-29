# Plano de implementação do chatbot

## Status, escopo e fontes

**Plano de 25/10/2026. Este documento não autoriza por si só a implementação das etapas.** A execução documental da etapa 0 foi autorizada posteriormente e preparada sem commits para revisão. Após essa entrega, a pessoa usuária autorizou os dois commits previstos para a etapa 0. As etapas 1–10 permanecem sem autorização de implementação.

O plano segue o [AGENTS.md geral](../../AGENTS.md), o [AGENTS.md da feature](AGENTS.md), o [briefing](briefing.md), as [decisões](decisions.md), o [contrato analisado](api-contract.md) e as [referências visuais](design/). Quando a lista de pendências do contrato divergir de uma decisão posterior, usar `decisions.md` para o requisito aprovado; conferir a API para a capacidade técnica.

Cada etapa entrega uma parte compilável e verificável, sem deixar classes quebradas esperando a próxima. **Independência significa poder revisar e testar a entrega isoladamente após suas dependências mínimas; não significa executar todas em qualquer ordem.** As etapas de UI podem avançar enquanto as dependências externas são resolvidas. Não inserir respostas fictícias no fluxo de produção: dados controlados ficam somente em testes. Fonte da restrição: [AGENTS.md geral](../../AGENTS.md).

Todos os novos nomes e desenhos técnicos abaixo são **propostas**, não padrões já existentes. **INCERTO:** conveniência de RecyclerView, composição visual de tablet e divisão exata das classes deve ser confirmada durante a revisão da etapa; o código atual não estabelece essas escolhas como convenção obrigatória.

### Caminhos usados nas tabelas

Os caminhos são relativos à raiz do repositório mobile:

- `J/` = `app/src/main/java/com/aula/delta_mobile_app/`.
- `R/` = `app/src/main/res/`.
- `T/` = `app/src/test/java/com/aula/delta_mobile_app/`.
- `I/` = `app/src/androidTest/java/com/aula/delta_mobile_app/`.

Arquivos propostos ainda não existem. Os links de fontes no fim do documento apontam somente para referências existentes.

## Commits planejados

Por solicitação da pessoa usuária nesta conversa, cada etapa terá **no mínimo dois commits próprios, coesos e com alterações reais**, incluindo as etapas documentais 0 e 10. Os dois commits de cada etapa estão descritos abaixo: **22 commits planejados no mínimo** para as etapas 0–10.

- As mensagens são propostas para cada etapa e dependem de autorização de execução. A autorização posterior para os commits 0.1/0.2 limita-se à etapa 0 documental; não libera implementação ou commits das etapas seguintes.
- Cada commit deve compilar e passar pelas verificações aplicáveis ao seu conteúdo. Incluir dependências, recursos e tipos necessários no mesmo commit, sem deixar referências quebradas para o seguinte.
- Não usar commits vazios, alterações cosméticas sem necessidade ou testes deliberadamente falhando para alcançar o mínimo. Se uma pendência impedir o segundo commit, a etapa continua pendente.
- O primeiro commit entrega uma unidade funcional ou documental; o segundo completa outra unidade relevante da mesma etapa, conforme as divisões propostas. Testes essenciais podem acompanhar ambos; a divisão não autoriza deixar comportamento novo sem verificação até o segundo commit.
- Podem existir commits adicionais para ajustes necessários. Não contar commits de etapas anteriores, merges ou um mesmo commit em duas etapas para atingir o mínimo.

Essa divisão é uma proposta de execução. As decisões de produto e dependências externas do restante do plano continuam abertas.

## Limites já definidos

- Java, XML/Views, Retrofit/OkHttp/Gson, ViewModel/LiveData e composição manual, aproveitando a arquitetura atual. Manter `findViewById` nesta feature como proposta de menor mudança; ViewBinding está habilitado, mas não há adoção nas telas consultadas. Alternativa: ViewBinding oferece acesso tipado, porém introduziria uma convenção e um ciclo de vida de binding que precisam ser discutidos. Fontes: [regras gerais](../../AGENTS.md), [build do app][build], [Fragment de referência][reference-fragment].
- Conta toda; URL da IA `https://delta-artificial-intelligence.onrender.com`; ID vindo do perfil real; entrada sempre pela apresentação inicial; ação de novo chat; histórico alternativo com ícones do Figma; sem “Ver todos”; sem gráficos nesta entrega. Fontes: D01–D06 em [decisions.md](decisions.md).
- Conforme as revisões de D08/D09 na etapa 0, o chat pode gravar dados pelas ferramentas disponíveis no backend e a primeira entrega pode usar histórico climático; clima atual/futuro fica para depois. O backend permanece sem ajustes por enquanto; sair da tela mantém a chamada e o menu fica oculto com teclado. Fontes: D07–D10 em [decisions.md](decisions.md).
- Textos fixos da feature em recursos de string, sem anexos de imagem/áudio/arquivo nem notificações push. Fonte: [briefing](briefing.md).

## Pendências que condicionam as etapas

Não são perguntas para responder todas agora. Resolver uma por vez, começando pelos bloqueios; registrar respostas em `decisions.md`. Nenhuma sugestão nesta tabela está aprovada pelo fato de aparecer no plano.

| ID | Pendência e responsável | O que condiciona |
| --- | --- | --- |
| P01 | **RESOLVIDA na etapa 0:** a pessoa usuária permitiu gravações pelas ferramentas disponíveis no backend. A proposta de exigir modo somente leitura foi descartada. Revisão efetivamente publicada segue em P03. | Deixa de bloquear a etapa 8 pelo requisito de escrita. Não autoriza operações além do plano nem POSTs na etapa 0. |
| P02 | **RESOLVIDA quanto ao escopo da primeira entrega:** aceitar histórico climático; adiar condições atuais e previsão futura. Origem/localização/período e disponibilidade dos dados históricos ainda precisam de evidência na integração. | Não exige nova capacidade climática no backend para a primeira entrega. O uso de histórico real continua sendo critério de aceite na etapa 10. |
| P03 | **PARCIALMENTE VERIFICADA:** `/health` e `/openapi.json` responderam 200 sem credenciais, e o OpenAPI é igual ao local. **Responsável pelo ambiente:** confirmar revisão Git e autenticação/gateway das operações de chat/sessões, não verificadas pelos dois GETs. Não enviar Bearer da API principal sem confirmação. | Configuração definitiva da etapa 2 e testes reais das etapas 7–10. |
| P04 | **Pessoa usuária:** definir sugestões iniciais, textos e ação de toque; apresentação/ordem do histórico e associação de ícones. A API não oferece título/prévia nem ordenação garantida. | Acabamento da etapa 5 e apresentação do histórico na etapa 7. |
| P05 | **Pessoa usuária:** definir posição/rótulo do novo chat, retomada da conversa corrente, falha de `close` e novo chat durante envio. Distinguir retorno pelo menu de recriação por rotação. | Transições completas das etapas 4, 5 e 9. |
| P06 | **Pessoa usuária:** decidir envios simultâneos, composição/texto do erro e nova tentativa quando não sabemos se o servidor processou a mensagem. **Proposta:** um envio por conversa em andamento, sem retry automático. | Política da etapa 8 e casos de concorrência nas etapas 4/9. |
| P07 | **Pessoa usuária + backend:** definir aceite de perguntas dependentes do contexto anterior enquanto os especialistas recebem só a pergunta atual. **INCERTO:** “Quero uma versão mais resumida” pode não reformular a resposta anterior. | Aceite da continuidade na etapa 10; não solucionar concatenando histórico à pergunta silenciosamente. |
| P08 | **Pessoa usuária:** definir formatação de respostas, links/fontes RAG e se acompanhar metas significa apenas consultar ou também criar/editar. **Proposta:** texto legível preservando quebras e fontes, sem execução de HTML; consultas de meta existente. | Renderização final da etapa 6 e aceite de metas na etapa 10. |

Fontes dos limites técnicos: [contrato](api-contract.md). Fontes das escolhas e pendências: [decisões](decisions.md). A confirmação de correspondência de `user_id` já foi dada em D01; não perguntá-la novamente.

## Sequência e dependências

| Etapa | Entrega pequena | Dependência mínima |
| --- | --- | --- |
| 0 | Verificar ambiente e registrar decisões bloqueantes | Acesso ao ambiente e respostas dos responsáveis; nenhuma implementação |
| 1 | DTOs do contrato | Contrato documentado |
| 2 | Cliente de IA e adaptação de erro | 1; P03 para configuração real |
| 3 | Repositório com identidade vinculada à conta | 1–2 |
| 4 | Estado e fluxo testável | 3 por interface; respostas falsas só nos testes |
| 5 | Entrada e layout responsivo inicial | Pode começar sem 1–4; P04/P05 para acabamento |
| 6 | Componentes da conversa e estados visuais | 4–5; P08 para formatação final |
| 7 | Listar e abrir histórico | 3–6; P03/P04 para integração/apresentação finais |
| 8 | Enviar mensagem e receber resposta | 3–6; P01 resolvida; resolver P03/P06 para ativação real |
| 9 | Novo chat e encerramento da conversa anterior | 7–8; P05 e política de concorrência |
| 10 | Aceite integrado e matriz de dispositivos | Entregas anteriores e pendências aplicáveis resolvidas |

Etapas 7 e 8 são entregas separadas sobre a mesma base; nenhuma depende do endpoint principal da outra. Nenhuma etapa pressupõe alteração do repositório da API. Se isso se tornar necessário, será outra tarefa autorizada.

## Etapa 0 — Fechar o que impede integração real

**Resultado:** saber com qual serviço podemos integrar e quais critérios continuam bloqueados, antes de tratar a feature como pronta.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo do commit |
| --- | --- | --- |
| 0.1 | `docs: confere contrato publicado do chatbot` | Atualizar `api-contract.md` com evidências da inspeção do serviço, diferenças e limites da verificação; conferir links e exemplos afetados. |
| 0.2 | `docs: registra decisões bloqueantes da integração do chatbot` | Atualizar `decisions.md` com respostas efetivamente recebidas e ajustar neste plano somente dependências afetadas. Registrar pendências que continuam abertas. |

Os dois commits exigem conteúdo novo comprovado; não registrar decisão como aprovada nem repetir texto apenas para atingir a quantidade.

| Arquivo a alterar futuramente | Responsabilidade |
| --- | --- |
| `docs/chatbot/api-contract.md` | Registrar respostas do contrato publicado, data, revisão identificável e diferenças da análise anterior. |
| `docs/chatbot/decisions.md` | Registrar as respostas sobre P01–P08, uma por vez, sem promover propostas a aprovação. |

**Abordagem e alternativas:** começar por inspeção de `GET /health` e `GET /openapi.json`, se disponíveis; `/health` não comprova bancos/LLM funcionando. Confirmar autenticação e capacidades com o responsável pelo serviço, pois o OpenAPI não revela ferramentas internas. Testar diretamente `/chat` não é uma consulta somente leitura: pode persistir conversas e executar ferramentas. O custo desta etapa é depender de respostas externas, mas evita construir o fluxo com uma capacidade presumida. Fonte: [contrato](api-contract.md).

**Como verificar:** comparar os schemas publicados com os exemplos locais; identificar o que foi de fato comprovado e o que permaneceu **INCERTO**. Nenhum build é necessário para uma alteração documental; verificar links e exemplos JSON. Nenhuma chamada é executada por este plano.

**O que você aprende:** contrato público, implementação e deploy podem divergir; uma resposta HTTP 200 não comprova os critérios de negócio.

**Pendências após a execução:** P01 e P02 tiveram suas decisões de produto resolvidas nesta etapa. P03 permanece parcial: o contrato público foi comparado, mas revisão Git e autenticação das operações dependem do responsável pelo ambiente. Não é necessário decidir todos os detalhes visuais para começar DTOs ou layout, quando essas etapas forem autorizadas.

**Registro de execução — 25/10/2026:** os dois GETs e a comparação estão em [api-contract.md](api-contract.md#verificação-do-serviço-publicado--etapa-0); as respostas sobre escrita e clima estão em [D08/D09](decisions.md). Os grupos previstos 0.1/0.2 foram preparados no diff para revisão e seus commits foram autorizados posteriormente. A etapa 0 não está integralmente encerrada enquanto a parte externa de P03 continuar sem confirmação. O snapshot local do OpenAPI, os AGENTS.md e o código da aplicação não foram alterados.

## Etapa 1 — Representar o contrato em Java

**Resultado:** converter JSON real do contrato para objetos tipados, ainda sem acessar a rede.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo do commit |
| --- | --- | --- |
| 1.1 | `feat: adiciona modelos de mensagens do chatbot` | Criar em `ChatModels.java` os DTOs de envio e resposta de `/chat`, com exemplos de desserialização correspondentes em `ChatModelsTest.java`. |
| 1.2 | `feat: adiciona modelos de sessões do chatbot` | Completar DTOs de listagem, histórico e fechamento; ampliar os testes para listas vazias, datas e campos opcionais. |

| Arquivo proposto | Ação e responsabilidade |
| --- | --- |
| `J/model/ChatModels.java` | Criar DTOs de envio, resposta, sessão, mensagem de histórico e resposta de fechamento; manter os nomes JSON via Gson. |
| `T/model/ChatModelsTest.java` | Criar testes de desserialização nos limites do contrato: listas vazias, `null`, campos opcionais, texto com quebras e datas. |

**Abordagem e alternativas:** agrupar DTOs como já ocorre em `AuthModels`/`ApiModels` reduz arquivos pequenos; um arquivo por DTO facilita localizar tipos, mas aumenta dispersão nesta entrega. `agent_used` deve continuar string porque aceita combinações, e não enum fechado. `chart_data` pode ser ignorado na renderização; o parser deve aceitar tanto objeto quanto `null`. Datas ficam como strings de transporte; a apresentação resolve formato/fuso na etapa 7. Fontes: [modelos existentes][api-models], [contrato](api-contract.md).

**Como verificar:** executar `.\gradlew.bat :app:testDebugUnitTest --tests "com.aula.delta_mobile_app.model.ChatModelsTest"` e `.\gradlew.bat :app:assembleDebug` no PowerShell. Os exemplos são fixtures dos testes, nunca mensagens preenchidas no aplicativo.

**O que você aprende:** DTO, JSON, Gson, campos opcionais e diferença entre contrato e texto visível na tela.

**Pendências:** divergências descobertas na etapa 0 exigem revisar os DTOs. Nenhuma decisão visual ou acesso a banco é necessário.

## Etapa 2 — Cliente HTTP separado e erros compreensíveis

**Resultado:** ter os métodos Retrofit da IA e um tratamento de falha testável, sem ligar o botão de envio.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo do commit |
| --- | --- | --- |
| 2.1 | `feat: configura cliente http isolado do chatbot` | Criar `ChatbotApiService`, a configuração de URL e a fábrica no `RetrofitClient`; incluir testes de origem, parâmetros e isolamento de credenciais. |
| 2.2 | `feat: adapta erros da api do chatbot` | Criar `ChatbotErrors`, mensagens em recursos e testes das duas formas de 422, corpo inválido, rede e espera informada em 429. |

| Arquivo proposto/existente | Ação e responsabilidade |
| --- | --- |
| `J/services/api/ChatbotApiService.java` | Criar interface para `chat`, `sessions`, `history` e `close`, com caminho/query/body exatamente como no contrato. |
| `J/services/network/RetrofitClient.java` | Alterar para construir um serviço de IA com origem própria e Gson/timeouts reaproveitados; não anexar automaticamente o interceptor Delta. |
| `app/build.gradle.kts` | Acrescentar configuração proposta `CHATBOT_API_URL`, com HTTPS e `/` final; preservar a configuração da API principal. Um override de desenvolvimento será documentado ao ser definido. |
| `J/services/network/ChatbotErrors.java` | Criar adaptação de `detail` textual/lista, HTTP, timeout e falha de conexão para `ScreenState` e recursos de string; não exibir detalhes técnicos brutos. |
| `R/values/chat_strings.xml` | Criar mensagens fixas da feature, inclusive erro e indisponibilidade. |
| `T/services/network/ChatbotNetworkTest.java` | Verificar origem, método, parâmetros, JSON e ausência de credenciais Delta usando transporte substituído. |
| `T/services/network/ChatbotErrorsTest.java` | Verificar 422 nas duas formas, erro sem JSON, 429/espera quando informado e corpo inválido. |

**Abordagem e alternativas:** reutilizar Retrofit/OkHttp/Gson mantém a stack; uma interface separada deixa explícito que a IA não é uma rota do servidor Delta. Adicionar tudo ao `ApiService` único é possível, mas mistura serviços com origem e autenticação diferentes. Não reutilizar o interceptor Delta por presunção. Adaptar erro localmente evita mudar o comportamento de login/cadastro e o contrato testado de `ScreenState.displayMessage()`. O custo é uma pequena interface e um adaptador adicionais. Fontes: [RetrofitClient][retrofit], [SessionInterceptor][interceptor], [ApiErrors][api-errors], [ScreenStateTest][screen-test].

**Como verificar:** executar os testes JVM da etapa por classe e `:app:assembleDebug :app:lintDebug`. Nenhum teste chama Render. O teste de transporte deve provar que `user_id` de query não vira body nos endpoints de sessões. Não acrescentar biblioteca de mock se os substitutes existentes forem suficientes.

**O que você aprende:** base URL, interface Retrofit, serialização, timeout, isolamento de autenticação e diferenças entre erro HTTP e erro de rede.

**Pendências:** P03. Se existir autenticação externa, implementar apenas o mecanismo confirmado; segredo interno não vai no APK. Não alterar timeouts como tentativa de esconder indisponibilidade sem medir o problema.

## Etapa 3 — Repositório com dados vinculados ao usuário

**Resultado:** concentrar o acesso ao serviço fora da tela e entregar resultados sem depender do Android visual.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo do commit |
| --- | --- | --- |
| 3.1 | `feat: adiciona consultas de sessões no repositório do chatbot` | Criar `ChatActions` e `ChatRepository` com listagem/histórico e identidade recebida por construtor; incluir testes de usuário, corpo inválido e resposta vazia. |
| 3.2 | `feat: adiciona envio e fechamento no repositório do chatbot` | Acrescentar os contratos e operações de `send`/`close`, com testes de resultado, sessão retornada e falha. Não ativar chamadas reais na interface. |

No primeiro commit, o contrato contém somente as operações já implementadas; o segundo o amplia junto com a implementação e os testes, sem deixar métodos obrigatórios incompletos entre commits.

| Arquivo proposto | Ação e responsabilidade |
| --- | --- |
| `J/repository/ChatActions.java` | Criar contrato pequeno das operações necessárias à feature, para permitir implementação real e substituto de teste. |
| `J/repository/ChatRepository.java` | Criar implementação com serviço e ID real vinculados ao construir a instância; converter respostas/erros e verificar corpos obrigatórios. |
| `T/repository/ChatRepositoryTest.java` | Verificar manutenção do usuário nas chamadas, sessão retornada, respostas incompletas, histórico vazio e isolamento de callbacks da instância antiga. |

**Abordagem e alternativas:** dependências por construtor e interface seguem o exemplo `AuthActions`/`LoginFlow`; dispensam Hilt e permitem testes controlados. O ID deve vir de `AuthRepository.currentProfile()` pelo caminho assíncrono existente, não de um campo editável ou da seleção de imóvel. Para a chamada longa da IA, usar o assíncrono de Retrofit; não ocupar a única thread de `AuthRepository.resource()` com a geração da resposta. O custo é coordenar o carregamento inicial do perfil com a criação do repositório. Fontes: [AuthRepository][auth-repository], [AuthActions][auth-actions], [PostalCodeRepository][postal-repository], D01/D02 em [decisões](decisions.md).

**Como verificar:** testes JVM com `ChatbotApiService` substituído; compilação do app. Verificar que trocar imóvel não altera o escopo da conversa e que nenhuma chamada usa ID fixo. O repositório ainda não inicia requests por existir: cada operação é explícita.

**O que você aprende:** responsabilidade de um repositório, injeção manual, identidade da conta e por que uma requisição longa não deve bloquear outros recursos.

**Pendências:** P03 para verificar identidade no servidor. Vincular ID no app não substitui autenticação/controle de acesso do backend. A etapa apenas prepara `send`/`close`; não ativa operações reais na interface.

## Etapa 4 — Estado e fluxo da conversa, antes da aparência

**Resultado:** testar os estados que a tela precisa observar sem depender de respostas reais do LLM.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo do commit |
| --- | --- | --- |
| 4.1 | `feat: modela estado e fluxo testável do chatbot` | Criar `ChatMessage`, `ChatState` e `ChatFlow`, com testes de rascunho, validação e transições de envio aprovadas. |
| 4.2 | `feat: conecta fluxo do chatbot ao viewmodel` | Criar `ChatViewModel`, publicação por LiveData e composição manual; completar testes de callback atrasado e invalidação por identidade. |

| Arquivo proposto | Ação e responsabilidade |
| --- | --- |
| `J/model/ChatMessage.java` | Criar mensagem da UI com autoria, conteúdo e identidade local; separar de DTO persistido, que não tem ID de mensagem. |
| `J/model/ChatState.java` | Criar composição do estado de apresentação, rascunho, sessão e mensagens; usar `ScreenState` nas operações, sem substituir seus quatro status globais. |
| `J/viewmodel/ChatFlow.java` | Criar coordenação em Java testável, com `ChatActions`, despacho e publicação de estado recebidos por construtor, como no fluxo de login. Métodos de histórico/novo chat serão acrescentados nas etapas correspondentes. |
| `J/viewmodel/ChatViewModel.java` | Criar ponte Android/LiveData e composição manual. Escopo da Activity para a conversa sobreviver à troca de seção; observar encerramento da sessão e limpar conteúdo da conta anterior. |
| `T/viewmodel/ChatFlowTest.java` | Verificar rascunho, envio pendente, resposta tardia, validação de mensagem e invalidação após troca de identidade. |

**Abordagem e alternativas:** dividir fluxo Java e ponte Android segue `LoginFlow` e facilita testar ordem dos callbacks. Colocar tudo no Fragment diminui arquivos, mas prende rede/estado à View e dificulta a regra de manter chamadas ao sair. Colocar tudo no AndroidViewModel é possível, com testes mais dependentes do runtime Android. O estado da Activity usa mais memória durante a sessão, porém mantém o resultado sem segurar referências de Views. Fonte: [LoginFlow][login-flow], [ViewModel de referência][reference-vm], D10 em [decisões](decisions.md).

**Como verificar:** testes JVM com ações e dispatcher controlados. Incluir um teste em que o Fragment deixa de existir conceitualmente e o resultado ainda atualiza o estado, e outro em que logout invalida o resultado. Na integração Android posterior, confirmar isso com navegação/rotação reais; teste puro não comprova lifecycle Android.

**O que você aprende:** máquina de estados, observação, ciclo de vida, identidade local de mensagem e prevenção de respostas fora de ordem.

**Pendências:** P05/P06. **Proposta a aprovar:** um envio por conversa em andamento; resultados sempre pertencem à operação/sessão original. Não definir silenciosamente fila, concorrência ou retomada após morte do processo. Esta etapa não promete execução garantida se o Android encerrar o processo; WorkManager/serviço em segundo plano não está no escopo aprovado.

## Etapa 5 — Tela inicial e navegação, já responsivas

**Resultado:** abrir a apresentação inicial pelo menu atual, com layout verificável em celular e tablet, ainda sem conversar com o servidor.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo do commit |
| --- | --- | --- |
| 5.1 | `feat: cria layout responsivo de entrada do chatbot` | Criar layouts de entrada, fundo, strings, estilos e dimensões de celular/tablet/paisagem; compilar os recursos sem alterar ainda o destino de navegação. |
| 5.2 | `feat: conecta entrada do chatbot ao menu existente` | Criar `ChatFragment`, apontar `tipsFragment` para a classe existente nesse mesmo commit e verificar entrada, retorno e apresentação nas seis configurações. |

| Arquivo proposto/existente | Ação e responsabilidade |
| --- | --- |
| `J/view/fragments/ChatFragment.java` | Criar controlador dedicado, inicialmente só da apresentação; `Fragment(R.layout.fragment_chat)` e `findViewById`. |
| `R/navigation/main_navigation.xml` | Substituir somente a classe do destino `tipsFragment`; manter o ID usado pelo menu. |
| `R/layout/fragment_chat.xml` | Criar estrutura com apresentação, região de conteúdo e espaço do compositor; reservar espaço para o menu quando visível. |
| `R/layout/include_chat_landing.xml` | Criar o conteúdo da apresentação, separando-o da lista de mensagens. |
| `R/values/chat_strings.xml` | Acrescentar títulos, rótulos e descrições acessíveis. |
| `R/values/chat_styles.xml` | Criar estilos da feature, reaproveitando cores/estilos já adequados. |
| `R/values/chat_dimens.xml` | Criar dimensões específicas, sem alterar margens globais das outras telas. |
| `R/values-w600dp/chat_dimens.xml` | Criar ajustes de largura/margens para janelas maiores. |
| `R/values-land/chat_dimens.xml` | Criar ajustes necessários à altura disponível em paisagem. |
| `R/drawable/chat_surface_background.xml` | Criar fundo da feature a partir da referência, sem rasterizar a tela inteira. |

**Abordagem e alternativas:** manter o destino evita remontar o menu; o Fragment dedicado evita expandir ainda mais `MainSectionFragment`. Um layout adaptável com dimensões qualificadas reduz duplicação em relação a duas telas completas de celular/tablet. **INCERTO — proposta visual:** limitar/centralizar a coluna de conteúdo no tablet em vez de esticar bolhas e campo por toda a largura; a largura final será revista visualmente. Não criar composição de duas colunas que o produto não aprovou. Fontes: [navegação atual][navigation], [MainActivity][main-activity], [referências](design/), [regras gerais](../../AGENTS.md).

**Como verificar:** `:app:assembleDebug :app:lintDebug`; abrir pelo ícone do menu. Conferir os seis cenários de tamanho/orientação da matriz abaixo já nesta etapa. Cabeçalho, sugestões aprovadas e botão de novo chat não podem ficar sob o menu nem fora da tela. Antes de P04/P05, validar o container sem preencher sugestões ou posição final com decisões inventadas.

**O que você aprende:** Fragment dedicado, destino de navegação, composição XML, dimensões qualificadas e diferença entre largura da tela e largura útil do conteúdo.

**Pendências:** P04/P05. Os ícones do histórico serão os recursos corretos do Figma, não uma associação inventada a agentes. A integração das ações fica para as etapas seguintes.

## Etapa 6 — Mensagens, espera e erro dentro da conversa

**Resultado:** renderizar mensagens e estados da conversa com conteúdo controlado somente pelos testes.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo do commit |
| --- | --- | --- |
| 6.1 | `feat: renderiza mensagens e compositor do chatbot` | Criar adapter, bolhas, compositor e seus recursos; declarar a dependência da lista se aprovada e incluir os cenários básicos de renderização no teste instrumentado. |
| 6.2 | `feat: apresenta espera e falhas na conversa do chatbot` | Acrescentar feedback e ligação aos estados, com testes de espera, erro, texto longo, teclado e acessibilidade. Fixtures permanecem restritas aos testes. |

| Arquivo proposto/existente | Ação e responsabilidade |
| --- | --- |
| `J/view/ChatMessageAdapter.java` | Criar apresentação incremental de mensagens, sem regra de rede. |
| `R/layout/item_chat_user.xml` | Criar bolha da pessoa usuária com texto expansível e largura adequada. |
| `R/layout/item_chat_assistant.xml` | Criar bolha da resposta e leitura acessível do conteúdo. |
| `R/layout/include_chat_composer.xml` | Criar campo e ação de enviar, com limite visual/validação sem texto fixo em Java. |
| `R/layout/include_chat_feedback.xml` | Criar espera e aviso de falha/reenvio conforme a decisão; reutilizar `StatusNoticeView` para o aviso quando apropriado. |
| `R/drawable/chat_user_bubble.xml` | Criar forma/fundo da bolha de pergunta. |
| `R/drawable/chat_assistant_bubble.xml` | Criar forma/fundo da bolha de resposta. |
| `R/layout/fragment_chat.xml`, `J/view/fragments/ChatFragment.java` | Alterar para conectar os componentes ao estado observável, sem ativar rede de produção. |
| `R/values/chat_strings.xml`, `R/values/chat_styles.xml`, `R/values/chat_dimens.xml` | Acrescentar recursos da conversa e do novo estado de erro. |
| `app/build.gradle.kts`, `gradle/libs.versions.toml` | Se a proposta abaixo for aceita, declarar RecyclerView explicitamente e conferir versão compatível durante a implementação. |
| `I/ChatRenderingInstrumentedTest.java` | Criar teste de mensagem longa, quebras, espera e erro, com `ChatActions` falso injetado pela composição manual do teste. |

**Abordagem e alternativas:** **INCERTO — proposta técnica:** RecyclerView recicla Views de conversas longas; ScrollView/LinearLayout evita dependência adicional, mas mantém uma View por mensagem e torna o custo do histórico maior. A versão e a adoção do RecyclerView não estão decididas neste plano. Ele não resolve sozinho o tamanho do JSON/histórico sem paginação. A espera deve ficar dentro da conversa, conforme a imagem, e não usar o modal de configuração de conta. Erro precisa de componente próprio sem depender de `ScreenState.displayMessage()`, que oculta erros por contrato. Fontes: [referência de espera](design/telachatpergunta.png), [ScreenState][screen-state], [teste do helper][screen-test], [contrato](api-contract.md).

**Como verificar:** compilar e rodar `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.aula.delta_mobile_app.ChatRenderingInstrumentedTest` em dispositivo de teste. Testar respostas muito longas, caracteres acentuados, listas, fontes RAG, URLs longas e teclado. Dados falsos só no teste instrumentado; não criar conversa demo na entrada de produção.

**O que você aprende:** lista de itens, separação de dado/renderização, estados por mensagem e custo de renderizar histórico.

**Pendências:** P06/P08; aprovação da abordagem da lista durante a revisão. A renderização deve preservar texto e fontes; não inferir sucesso de escrita nem esconder mensagem de indisponibilidade retornada com HTTP 200.

## Etapa 7 — Histórico da conta e reabertura de conversa

**Resultado:** listar sessões reais e abrir suas mensagens, com erros e estado vazio próprios.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo do commit |
| --- | --- | --- |
| 7.1 | `feat: lista histórico de sessões do chatbot` | Criar adapter/item de sessão, ícones e rótulos aprovados; conectar listagem com vazio/erro e testes correspondentes. |
| 7.2 | `feat: reabre conversas do histórico do chatbot` | Conectar seleção e carregamento das mensagens; testar troca A→B, retorno atrasado, falha e histórico vazio sem atribuir causa não comprovada. |

| Arquivo proposto/existente | Ação e responsabilidade |
| --- | --- |
| `J/view/ChatSessionAdapter.java` | Criar linhas de histórico com ícones aprovados e rótulos baseados apenas nos dados disponíveis. |
| `R/layout/item_chat_session.xml` | Criar apresentação alternativa da sessão; sem “Ver todos”. |
| `R/drawable-nodpi/chat_history_icon_*.png` ou `R/drawable/chat_history_icon_*.xml` | Acrescentar somente os ícones necessários, conforme exportação/formato confirmado no Figma; nomes exatos definidos após identificar os recursos. |
| `J/viewmodel/ChatFlow.java`, `J/viewmodel/ChatViewModel.java` | Alterar para listar, selecionar e carregar sessão; manter estados separados da geração de resposta. |
| `J/view/fragments/ChatFragment.java`, `R/layout/include_chat_landing.xml` | Alterar para apresentar lista, vazio, falha e ação de abrir. |
| `R/values/chat_strings.xml` | Acrescentar rótulos/avisos aprovados e datas apresentadas ao usuário. |
| `T/viewmodel/ChatFlowTest.java` | Acrescentar testes de lista vazia, troca rápida A→B, resposta atrasada e falha de histórico. |

**Abordagem e alternativas:** usar somente ID/data evita buscar todas as mensagens de todas as sessões apenas para fabricar prévias. **Proposta para P04:** rótulo neutro com data e um ícone aprovado; ordenação local por data válida, se aprovada. Gerar título com LLM exigiria fonte/custo adicional e persistência que a API não oferece. Não persistir cache sensível em disco nesta proposta; ele fica na vida do ViewModel. O custo é recarregar após morte do processo. Fonte: [contrato de sessões](api-contract.md).

**Como verificar:** testes com duas sessões da mesma conta, vazio, data inválida, erro e ordem de respostas invertida. Com ambiente P03 confirmado, consultar sessões de uma conta de teste autorizada e comparar o texto reaberto com o servidor. `messages: []` não permite distinguir sessão vazia de inexistente ou de usuário diferente: a UI não deve inventar essa causa.

**O que você aprende:** listagem/detalhe, estado vazio, seleção, datas e proteção contra resposta atrasada de outra sessão.

**Pendências:** P03/P04/P05. Ícones a exportar são a única família de arquivos cujo nome final depende do recurso escolhido. Gráficos e agentes não são recuperáveis pelo histórico atual; não fabricar esses campos.

## Etapa 8 — Envio real, resultado e falhas sem perder o rascunho

**Resultado:** enviar uma mensagem na conta correta, guardar o `session_id` retornado e acrescentar a resposta somente à conversa correspondente.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo do commit |
| --- | --- | --- |
| 8.1 | `feat: integra envio de mensagens do chatbot` | Conectar compositor, fluxo e repositório com validação, sessão retornada e política de falha aprovada; incluir testes de envio, duplo toque e erro. P01 foi resolvida; ativação real continua condicionada a P03/P06. |
| 8.2 | `feat: preserva resposta do chatbot ao sair da tela` | Completar coordenação no escopo da Activity, quando necessária, e os testes instrumentados de saída/retorno/rotação; proteger o resultado contra logout e anexação em outra conversa. |

O primeiro commit já respeita a separação de estado/rede da View definida na etapa 4. O segundo completa a integração de lifecycle e sua verificação, sem introduzir cancelamento temporário ao sair da tela.

| Arquivo a alterar | Responsabilidade |
| --- | --- |
| `J/viewmodel/ChatFlow.java`, `J/viewmodel/ChatViewModel.java` | Conectar envio ao repositório; validar conteúdo; coordenar mensagem pendente/sucesso/falha, sessão e retorno tardio. |
| `J/view/fragments/ChatFragment.java`, `R/layout/include_chat_composer.xml`, `R/layout/include_chat_feedback.xml` | Ligar toque/ação do teclado aos comandos; aplicar a política de erro/reenvio aprovada. |
| `J/view/activities/main/MainActivity.java` | Coordenar entrada na seção e escopo da Activity quando necessário, preservando o menu/IME existente e as outras telas. |
| `R/values/chat_strings.xml` | Acrescentar validação de mensagem e feedback final aprovado. |
| `T/viewmodel/ChatFlowTest.java` | Acrescentar casos de duplo toque, falha antes/depois da criação da sessão, 422/500, timeout e logout antes da resposta. |
| `I/ChatLifecycleInstrumentedTest.java` | Criar teste com resposta atrasada controlada: sair, voltar, rotacionar e receber o resultado sem duplicar envio. |

**Abordagem e alternativas:** usar o resultado completo de `/chat`, sem simular streaming. Na primeira mensagem, omitir/enviar `null` em `session_id`; usar a string retornada nas seguintes. Não criar sessão HTTP vazia. Nenhum cancelamento ocorre só porque a View saiu de exibição; o ViewModel da Activity mantém a operação. Retry automático pode duplicar gravação, já que falta idempotência. **Proposta para P06:** manter falha/rascunho visíveis e exigir ação explícita para repetir, com tratamento da confirmação de resultado incerto a definir. Fonte: [contrato](api-contract.md), D05/D10 em [decisões](decisions.md).

**Como verificar:** testes JVM/instrumentados primeiro. Depois de P03/P06 resolvidos, usar conta de teste e enviar pergunta autorizada; verificar request, resposta, continuidade do ID e histórico. A permissão de escrita já foi definida em P01/D08; verificar os efeitos reais das ferramentas somente no cenário de teste autorizado. Repetir com rede desligada, resposta lenta, saída da seção e rotação. Não imprimir conteúdo de conversa ou tokens para depurar; preferir inspeção local com dados de teste e estados/códigos sem conteúdo sensível.

**O que você aprende:** requisição assíncrona de ponta a ponta, validação de entrada, correlação de callbacks, timeout e diferença entre perder a conexão e o servidor não ter processado.

**Pendências:** P03/P06 para ativação real e P07/P08 para o aceite de conteúdo. P01 permite as gravações suportadas pelo backend; P02 aceita histórico climático nesta entrega. A disponibilidade e o uso dos dados reais continuam sujeitos à verificação; não preencher lacunas com prompts ocultos ou dados inventados.

## Etapa 9 — Novo chat e encerramento explícito

**Resultado:** o botão aprovado troca de conversa de forma previsível e trata o resultado de `close` sem apagar o histórico.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo do commit |
| --- | --- | --- |
| 9.1 | `feat: coordena novo chat e fechamento da sessão anterior` | Implementar no fluxo/ViewModel as transições aprovadas, incluindo falta de sessão, falha, concorrência e testes de correlação entre sessões. |
| 9.2 | `feat: adiciona ação de iniciar novo chat` | Conectar o botão e seus recursos ao fluxo já implementado; ampliar a instrumentação para cliques repetidos, entrada inicial e histórico preservado. |

| Arquivo a alterar | Responsabilidade |
| --- | --- |
| `J/viewmodel/ChatFlow.java`, `J/viewmodel/ChatViewModel.java` | Acrescentar a transição de novo chat e fechamento associado à sessão anterior; aplicar a política aprovada de falha/concorrência. |
| `J/view/fragments/ChatFragment.java`, `R/layout/fragment_chat.xml`, `R/layout/include_chat_landing.xml` | Ligar o botão na posição aprovada, estados de espera e feedback da transição. |
| `R/values/chat_strings.xml` | Rótulo/descrição acessível do botão e mensagens de falha de encerramento. |
| `T/viewmodel/ChatFlowTest.java` | Acrescentar casos sem sessão anterior, fechamento com sucesso/falha, clique repetido e retorno tardio da conversa anterior. |
| `I/ChatLifecycleInstrumentedTest.java` | Acrescentar teste da troca de conversa em dispositivo e da entrada sempre pela apresentação inicial. |

**Abordagem e alternativas:** fechar somente a sessão identificada quando o usuário pede novo chat; uma nova conversa vazia é estado local, pois o servidor só cria a sessão no primeiro envio. Não chamar `close` automaticamente em `onStop`/rotação: isso confundiria saída visual com a ação explícita e geraria resumos duplicados. Se `close` falhar, continuar ou manter a anterior são políticas diferentes e dependem de P05. Fonte: [contrato de close](api-contract.md), D05/D10 em [decisões](decisions.md).

**Como verificar:** contar requests no serviço substituído: zero `close` em rotação/saída e no máximo um por acionamento válido; confirmar o ID correto. Novo envio cria outra sessão sem sobrescrever as anteriores. Voltar ao histórico continua permitindo abrir a conversa antiga, pois `close` não a exclui. Testar resposta da antiga chegando depois da troca: nunca anexá-la na nova.

**O que você aprende:** sessão no servidor versus tela local, transições com múltiplas operações e semântica de encerramento sem exclusão.

**Pendências:** P05 e política de P06; também definir como retomar a conversa atual a partir da apresentação inicial. Não presumir que “sempre apresentação inicial” significa fechar ou descartar uma conversa em andamento.

## Etapa 10 — Verificação final do briefing

**Resultado:** produzir evidência do que passou, do que falhou e do que continua pendente, sem confundir tela funcional com resposta de negócio validada.

**Commits planejados — mínimo 2:**

| Ordem | Mensagem sugerida | Conteúdo do commit |
| --- | --- | --- |
| 10.1 | `feat: completa cenários de aceite do chatbot` | Completar testes instrumentados relevantes de lifecycle, acessibilidade e cenários da matriz; incluir fixtures/recursos de teste necessários. Não criar teste redundante apenas para gerar commit. |
| 10.2 | `docs: registra validação do chatbot em celular e tablet` | Criar `acceptance.md` com resultados reais; atualizar briefing e, somente se houver evidência/decisão nova, contrato e decisões. Manter critérios bloqueados sem marca de conclusão. |

Se os testes planejados já estiverem completos antes desta etapa, o primeiro commit poderá registrar a primeira parte das evidências reais em `acceptance.md`, com mensagem `docs: registra verificações automatizadas do chatbot`; o segundo acrescentará matriz visual e aceite de negócio. A adaptação preserva duas entregas úteis sem inventar testes ou execução.

| Arquivo a criar/alterar | Responsabilidade |
| --- | --- |
| `docs/chatbot/acceptance.md` | Criar registro de comandos, aparelhos/AVDs, dimensões, capturas, cenários e resultados reais por critério. |
| `docs/chatbot/briefing.md` | Marcar critérios somente depois de evidência correspondente; manter pendentes os não atendidos. |
| `docs/chatbot/api-contract.md` | Atualizar somente divergências comprovadas durante a integração. |
| `docs/chatbot/decisions.md` | Registrar decisões finais/revisões de escopo explicitamente aceitas. |
| `I/ChatRenderingInstrumentedTest.java`, `I/ChatLifecycleInstrumentedTest.java` | Completar os cenários relevantes de acessibilidade, lifecycle e navegação. |

Se a validação encontrar defeito, corrigir o arquivo da feature responsável em uma etapa identificada, sem usar esta fase como autorização para refatorar o app inteiro. Fonte: [regras gerais](../../AGENTS.md).

**Abordagem e alternativas:** combinar testes determinísticos com execução real e comparação visual. Um teste com LLM falso prova o fluxo do app; não prova uso dos dados da conta, clima ou qualidade das dicas. Uma única resposta real satisfatória não prova todos os erros/lifecycles. Evidência manual complementa os testes, mas não os substitui. Fontes: [briefing](briefing.md), [limites documentados do app][auth-doc].

**Como verificar:** executar build, lint e suíte JVM; em seguida instrumentação nos dispositivos de teste e os cenários reais aprovados. Registrar a saída e distinguir falha de código de limitação do ambiente. Não assumir que o erro histórico de loopback continua ocorrendo hoje. Comandos de referência: [AGENTS.md geral](../../AGENTS.md).

**O que você aprende:** critérios de aceite verificáveis, regressão, diferença entre teste de UI e integração, e como relatar limites sem declarar conclusão indevida.

**Pendências:** P03/P07/P08 e evidências de integração com dados reais para o aceite de negócio completo. P01/P02 foram resolvidas como requisitos, não como provas de funcionamento. Se algum requisito vigente ficar de fora, registrar explicitamente uma entrega parcial aprovada; não marcar todo o briefing como concluído.

## Matriz de celular, tablet e acessibilidade

A matriz vem do [briefing](briefing.md). Executá-la nas etapas visuais e repetir a verificação final depois das mudanças de comportamento:

| Perfil | Retrato | Paisagem | Cenários mínimos |
| --- | --- | --- | --- |
| Celular pequeno, largura de referência ~360dp | Obrigatório | Obrigatório | Entrada, histórico, campo com teclado, resposta longa, erro, novo chat |
| Celular grande, largura de referência ~412dp | Obrigatório | Obrigatório | Mesmos cenários, várias mensagens e retorno à seção |
| Tablet de 10", largura de referência ~800dp | Obrigatório | Obrigatório | Coluna com largura revista, botões alcançáveis, teclado, histórico e conversa sem espaço estranho |

Ao girar, registrar a largura/altura realmente disponíveis em dp: não forçar a largura de retrato como se fosse a de paisagem. **INCERTO — proposta de validação adicional:** Android 11/API 30 e pelo menos uma versão recente disponível, para cobrir o mínimo do app e diferenças de insets/teclado. Fonte do mínimo: [build do app][build].

Em cada perfil, verificar: fonte normal e ampliada, TalkBack, toque de 48dp, rótulos de ações, contraste, texto sem cortes, URLs/linhas extensas, compositor acessível, última mensagem e última linha do histórico acima do menu. Confirmar que o menu some com o teclado e volta quando apropriado. Não aprovar responsividade apenas porque existem qualificadores `w600dp`/`land`. Fontes: [regras gerais](../../AGENTS.md), [MainActivity][main-activity], [briefing](briefing.md).

## Rastreabilidade dos critérios de aceite

| Critério | Etapas que preparam | Evidência para concluir |
| --- | --- | --- |
| Enviar e receber mensagem | 1–4, 6, 8 | Request real autorizado, resposta e sessão coerentes; testes de erro/lifecycle. |
| Respostas usam consumo da conta | 3, 8, 10 | Conta de teste com dados conhecidos; comparar período/escopo e conteúdo da resposta com a fonte autorizada. Sem registros, explicar ausência sem inventar medição. |
| Clima considerado | 0, 8, 10 | Demonstrar uso de histórico climático real, origem/localização/período e tratamento de ausência, conforme D09 revisada; condições atuais e previsão futura ficam para depois. |
| Dicas de consumo consciente | 6, 8, 10 | Revisar respostas reais e sua coerência; distinguir dica geral de afirmação baseada em medição. |
| Acompanhamento de metas | 0, 8, 10 | Testar conta com meta e sem meta conforme escopo aprovado em P08; não inventar um cadastro de metas no mobile. |
| Layout conforme referência e mudanças aprovadas | 5–7, 9–10 | Comparar capturas com as cinco imagens e as decisões sobre novo chat, histórico alternativo, ausência de “Ver todos” e erro adicional. |
| Sem texto fixo hardcoded | 2, 5–9, 10 | Revisar recursos XML/Java da feature e lint; texto vindo da API/usuário permanece dinâmico. |
| Celular/tablet, retrato/paisagem | 5–10 | Evidência dos seis cenários da matriz, com teclado e conteúdo longo. |
| Gravações pelas ferramentas disponíveis | 0, 8, 10 | Conforme D08 revisada, permitir o que o backend disponibiliza; verificar efeitos na conta de teste autorizada, sem inventar ferramenta ou prometer capacidade não comprovada. Não exigir modo somente leitura. |
| Continuidade e novo chat | 4, 8–10 | Mesma sessão entre envios; novo ID após ação explícita; limites de contexto tratados conforme P07, sem prometer geração que o backend não entrega. |

Fonte dos critérios: [briefing](briefing.md) e [decisões](decisions.md). A análise anterior não executou esses testes; este plano também não registra nenhum deles como aprovado.

## Como executar uma etapa comigo

Antes de implementar uma etapa: confirmar seu escopo e resolver somente as pendências que a afetam. Na entrega: explicar os arquivos alterados, o motivo da escolha, como verificar e o resultado real dos checks. Revisar essa etapa antes de avançar para outra; não transformar autorização deste plano em autorização de implementação. Fonte: [AGENTS.md geral](../../AGENTS.md).

[build]: ../../app/build.gradle.kts
[api-models]: ../../app/src/main/java/com/aula/delta_mobile_app/model/ApiModels.java
[retrofit]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/RetrofitClient.java
[interceptor]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/SessionInterceptor.java
[api-errors]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/ApiErrors.java
[screen-state]: ../../app/src/main/java/com/aula/delta_mobile_app/model/ScreenState.java
[screen-test]: ../../app/src/test/java/com/aula/delta_mobile_app/model/ScreenStateTest.java
[auth-repository]: ../../app/src/main/java/com/aula/delta_mobile_app/repository/AuthRepository.java
[auth-actions]: ../../app/src/main/java/com/aula/delta_mobile_app/repository/AuthActions.java
[postal-repository]: ../../app/src/main/java/com/aula/delta_mobile_app/repository/PostalCodeRepository.java
[login-flow]: ../../app/src/main/java/com/aula/delta_mobile_app/viewmodel/LoginFlow.java
[reference-fragment]: ../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/AbnormalConsumptionFragment.java
[reference-vm]: ../../app/src/main/java/com/aula/delta_mobile_app/viewmodel/AbnormalConsumptionViewModel.java
[navigation]: ../../app/src/main/res/navigation/main_navigation.xml
[main-activity]: ../../app/src/main/java/com/aula/delta_mobile_app/view/activities/main/MainActivity.java
[auth-doc]: ../../devdocs/fluxodavi/AUTH_MOBILE.md
