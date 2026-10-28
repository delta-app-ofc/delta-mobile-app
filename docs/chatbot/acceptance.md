# Validação do chatbot — etapa 10

**Atualização posterior, 26/10/2026:** a pessoa usuária liberou provisoriamente o envio e o fechamento de sessões com o contrato publicado disponível. O bloqueio P03 descrito abaixo registra o estado **durante a etapa 10**, antes dessa decisão; não é mais o estado do código de produção. A confirmação da revisão/autenticação do serviço e o teste real com conta autenticada continuam pendentes. Fontes: [D01 revisada](decisions.md), [ChatViewModel][vm].

**Atualização posterior, 27/10/2026:** a decisão D05 foi revisada: a apresentação inicial não mostra “Novo chat”; durante a conversa há “Novo chat” e uma ação para voltar ao início sem apagar o conteúdo. As capturas e a comparação visual abaixo continuam sendo evidência histórica da etapa 10, anterior a essa revisão. Fontes: [D05 revisada](decisions.md), [Fragment][fragment].

## Situação do aceite

**Data:** 26/10/2026, America/Sao_Paulo. **Base:** `53cbf6cd6f5fb7a9a8c9fa4b405bb646a1846d8e`, acrescida da correção localizada, dos testes e documentos desta etapa, ainda sem commits. A alteração preexistente de `local.properties` foi preservada.

Este é um registro de validação técnica parcial, **não uma aprovação final do produto nem uma aprovação de redução do briefing**. As operações publicadas de envio e fechamento continuam bloqueadas por P03. Não foram chamadas operações de chat, sessões ou ferramentas da API publicada nesta etapa; a instrumentação utilizou exclusivamente respostas e contas simuladas, com rede desabilitada no emulador. Fontes: [D01/D05](decisions.md), [composição padrão do ViewModel][vm], [fixtures de renderização][rendering], [fixtures de lifecycle][lifecycle].

Não há decisão nova de produto nesta etapa. Mantêm-se as decisões explícitas mais recentes de [decisions.md](decisions.md), inclusive D08/D09 revisadas; os resumos históricos do AGENTS.md da feature não substituem essas decisões. Não foi alterado o contrato da API porque não houve evidência nova de integração real.

## Organização nos dois grupos previstos

1. **10.1 — `feat: completa cenários de aceite do chatbot`:** alterações nos dois testes instrumentados previstos e correção localizada no Fragment, conforme a previsão de corrigir defeitos encontrados nesta etapa. Acrescentados testes da semântica acessível e dos alvos de toque de histórico, compositor, nova tentativa, novo chat e retomada; reforçadas a verificação do fim da resposta longa e a remoção do diálogo após logout. As capturas registram no log as dimensões disponíveis do Fragment em dp e a escala da fonte. Fontes: [renderização][rendering], [lifecycle][lifecycle], [Fragment][fragment].
2. **10.2 — `docs: registra validação do chatbot em celular e tablet`:** este registro, as capturas de evidência vinculadas abaixo e a atualização dos critérios do [briefing](briefing.md). Contrato e decisões permanecem sem alterações. Nenhum commit foi realizado.

## Ambiente e limites

| Item | Ambiente usado |
| --- | --- |
| Host | Windows, PowerShell, JDK `21.0.11`; código do app mantém compatibilidade Java 11 |
| Execução Gradle | Wrapper do repositório; `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=./app/build/nonexistent-domain-dir` no host desta validação |
| AVD | `ChatbotStage5`, iniciado sem janela e sem snapshots, somente serial `emulator-5580` |
| Imagem | Android 16/API 36, imagem SDK `android-36.1/google_apis_playstore/x86_64`; 2 núcleos, 2048 MB |
| Região do emulador | `en-rUS`, fuso `GMT`; datas localizadas refletem essa configuração, não o fuso da pessoa usuária |
| Perfis | Overrides de tamanho e densidade: seis resoluções, densidade 160 dpi, fontes `1.0` e `1.3` |
| Isolamento | Modo avião, Wi-Fi e dados desabilitados; nenhum dispositivo pessoal utilizado |
| Host das telas | `LoginActivity` de teste; renderização injeta `ChatFlow`, lifecycle usa NavHost e ViewModel com conta substituída |
| O que não foi exercitado | Menu real da MainActivity autenticada, servidor/LLM/dados reais, dispositivo físico, Android/API 30, morte do processo, leitura manual com TalkBack |

Fontes do ambiente: `app/build/chat-stage5-avds/ChatbotStage5.avd/config.ini`; `adb -s emulator-5580 shell getprop ro.build.version.release` retornou `16`, e `ro.build.version.sdk` retornou `36`; `am get-config` identificou `en-rUS` e `getprop persist.sys.timezone` retornou `GMT`; [build do módulo][build]; [hosts dos testes][rendering] e [lifecycle][lifecycle]. O Play Store image inclui TalkBack (`com.google.android.marvin.talkback`), verificado por `pm list packages talkback`, mas isso não comprova uso ou leitura correta.

Os perfis simulam áreas em dp; **não são três aparelhos físicos nem comprovam diagonal física de 10 polegadas**. A reserva de espaço inferior é verificada no Fragment; o menu real e sua ocultação/restauração com IME exigem verificação adicional no shell autenticado. A lógica está em [MainActivity][main], mas sua leitura não equivale a teste do shell.

## Comandos e verificações automatizadas

Comandos executados na raiz do repositório:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21.0.11'
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=./app/build/nonexistent-domain-dir'
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug --no-configuration-cache
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks --no-configuration-cache
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --no-configuration-cache
```

| Verificação | Resultado e evidência local |
| --- | --- |
| Build do app e APK de testes | Passou, inclusive após a correção; `app/build/chat-stage10-final-build.log` e tarefas `assembleDebug`/`assembleDebugAndroidTest` em `app/build/chat-stage10-corrected-checks.log` |
| Suíte JVM | 217 testes, zero falhas/erros; execução forçada passou em `app/build/chat-stage10-jvm.log`, relatórios XML em `app/build/test-results/testDebugUnitTest/` |
| Instrumentação final | 40 testes passaram após a correção: 17 de renderização e 23 de lifecycle; `app/build/chat-stage10-corrected-instrumentation.log`. Os três casos focados em paisagem/fonte ampliada também passaram em `app/build/chat-stage10-corrected-landscape.log` |
| Matriz | 12 perfis × 9 cenários = 108 execuções, todas aprovadas após a correção; `app/build/chat-stage10-preview/matrix/results.json` e `tests.log` de cada perfil |
| Lint | Falhou com 7 erros e 190 avisos, antes e depois da correção; `app/build/chat-stage10-checks.log`, `app/build/chat-stage10-corrected-checks.log` e `app/build/reports/lint-results-debug.html`. Nenhum dos sete erros está em arquivo do chatbot |
| Auditoria de textos fixos | Não encontrados textos fixos de apresentação fora de recursos nos layouts, Fragment e adapters da feature; ver auditoria abaixo |

O comando combinado termina com falha por lint; isso não é falha de compilação nem reprovação dos testes JVM. A execução forçada da suíte JVM foi feita separadamente para não apresentar resultado em cache como nova execução. Não foi executada a suíte instrumentada inteira do aplicativo: o escopo foi as duas classes do chatbot.

Identificação dos APKs instalados para a validação após a correção, obtida com `Get-FileHash` (SHA-256):

- `app-debug.apk`: `72E5835D4E6B2048A29A1B0B4D6B4966BD9817CA4A7D5A99D2C1DF92147DAF95`.
- `app-debug-androidTest.apk`: `1B5BFCA34E7E51496D302C6B88BFA61630D710266E7F36F01F31602CA43D7B4C`.

### Erros de lint fora da feature

| Arquivo | Linha | Diagnóstico |
| --- | --- | --- |
| `app/src/main/java/com/aula/delta_mobile_app/view/fragments/LeakDetectionFragment.java` | 79 | `SuspiciousIndentation` |
| `app/src/main/java/com/aula/delta_mobile_app/viewmodel/SignupViewModel.java` | 177 | `SuspiciousIndentation` |
| `app/src/main/res/layout/fragment_hourly_analysis.xml` | 81 | `Orientation` |
| `app/src/main/res/layout/fragment_dashboard.xml` | 236, 273, 310, 345 | quatro `UseAppTint` |

Fonte: relatório XML/HTML do lint desta execução. Os arquivos não foram corrigidos por estarem fora da etapa. A existência de 190 avisos também impede descrever o projeto como livre de avisos; este registro não constitui triagem completa deles.

### Falha observada e repetição

A primeira execução instrumentada teve uma falha entre 40 testes: `keyboardRemovesOnlyTheChatMenuClearanceAndKeepsTouchTarget` não confirmou IME visível em até 5 segundos. O diagnóstico mostrou botão de envio de 89×48 px e Fragment de 360×616 px; a falha foi na condição de abertura do teclado. Evidência: `app/build/chat-stage10-instrumentation.log`.

O mesmo teste passou isoladamente, sem alteração do app, em `app/build/chat-stage10-keyboard-reproduction.log`. **INCERTO:** atraso de inicialização/foco/IME é uma explicação possível; a causa não foi comprovada. A repetição completa e a matriz finais passaram, mas devem ser lidas junto com essa ocorrência, sem apagar o resultado inicial.

### Correção encontrada na matriz

Os testes novos de alvo de toque reproduziram uma falha em `640x360`, com fontes `1.0` e `1.3`: mesmo após rolar até o fim, o retângulo visível da ação de nova tentativa não mantinha 48dp de altura. A primeira repetição completa no perfil-base já tinha passado com 40 testes, mostrando que esse defeito dependia da área disponível; evidências iniciais preservadas em `app/build/chat-stage10-preview/matrix-before-fix/` e `app/build/chat-stage10-final-instrumentation.log`.

**Correção localizada:** `ChatFragment.updateComposer()` também compacta o compositor com teclado fechado quando o Fragment tem menos de 384dp de altura. Nesse caso remove espaçamentos verticais e limita a altura do campo a uma linha visível, mantendo seu conteúdo/rolagem e o contador. O comportamento compacto com IME muito baixo continua previsto; a reserva do menu não foi reduzida. Fonte: [Fragment][fragment]. O teste deixou de exigir exatamente quatro linhas após fechar o IME, pois isso é detalhe de implementação; continua exigindo restauração da reserva inferior e contador acessível, e o teste novo mede os alvos reais de 48dp.

Essa é a única alteração no código de produção desta etapa. Não houve refatoração de navegação, backend, contrato ou liberação de P03. A comparação visual abaixo continua parcial.

### Reprodução da instrumentação

Com o emulador de teste já iniciado e offline, use o serial explícito; os testes não precisam desbloquear P03:

```powershell
$taskAdb = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
& $taskAdb -s emulator-5580 install -r app/build/outputs/apk/debug/app-debug.apk
& $taskAdb -s emulator-5580 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $taskAdb -s emulator-5580 shell am instrument -w -r -e class com.aula.delta_mobile_app.ChatRenderingInstrumentedTest,com.aula.delta_mobile_app.ChatLifecycleInstrumentedTest com.aula.delta_mobile_app.test/androidx.test.runner.AndroidJUnitRunner
```

Fonte dos cenários e contas simuladas: [ChatRenderingInstrumentedTest][rendering], [ChatLifecycleInstrumentedTest][lifecycle]. Não rodar contra um aparelho pessoal: os testes abrem/fecham Activities do pacote de teste e manipulam seu estado.

## Matriz de celular e tablet

As medidas abaixo são largura × altura em dp do Fragment, obtidas nas capturas `entry` e `keyboard`, sem confundir o tamanho imposto à tela com a área disponível ao app. A reserva do menu e o compositor ainda ocupam parte dessa área. Fonte: `viewport.log` e `results.json` de cada perfil.

| Tela imposta (dp) | Fonte | Fragment sem IME (dp) | Fragment com IME (dp) | Resultado |
| --- | --- | --- | --- | --- |
| 360x640 | 1.0 | 360.0x616.0 | 360.0x365.0 | Passou (9/9) |
| 360x640 | 1.3 | 360.0x616.0 | 360.0x278.0 | Passou (9/9) |
| 640x360 | 1.0 | 640.0x336.0 | 640.0x136.0 | Passou (9/9) |
| 640x360 | 1.3 | 640.0x336.0 | 640.0x49.0 | Passou (9/9) |
| 412x915 | 1.0 | 412.0x891.0 | 412.0x607.0 | Passou (9/9) |
| 412x915 | 1.3 | 412.0x891.0 | 412.0x520.0 | Passou (9/9) |
| 915x412 | 1.0 | 915.0x388.0 | 915.0x175.0 | Passou (9/9) |
| 915x412 | 1.3 | 915.0x388.0 | 915.0x88.0 | Passou (9/9) |
| 800x1280 | 1.0 | 800.0x1256.0 | 800.0x984.0 | Passou (9/9) |
| 800x1280 | 1.3 | 800.0x1256.0 | 800.0x897.0 | Passou (9/9) |
| 1280x800 | 1.0 | 1280.0x776.0 | 1280.0x504.0 | Passou (9/9) |
| 1280x800 | 1.3 | 1280.0x776.0 | 1280.0x417.0 | Passou (9/9) |

Em fonte ampliada, o Gboard exibiu o aviso “Keyboard font size updated”. Em `640x360` isso deixou somente 49dp para o Fragment: a captura mostra o compositor compacto de 48dp alcançável, sem espaço útil para ler a conversa enquanto esse aviso e o IME ocupam a tela. Não se declara leitura confortável da conversa nessa condição; o aceite visual completo continua aberto. A redução foi registrada, sem remover o aviso do teclado nem editar a captura.

Os nove cenários selecionados por perfil são:

| Classe | Método | O que comprova |
| --- | --- | --- |
| Renderização | `productionEntryHasNoDemoConversationAndNoSendConnection` | Entrada padrão sem conversa fictícia e com envio bloqueado |
| Renderização | `preservesLongSelectableTextMarkdownSourcesAndUrlsWithoutOpeningLinks` | Resposta longa, URL de 500 caracteres, quebras/listas/fontes preservadas, texto selecionável, sem autoabertura, fim do texto alcançável acima do compositor |
| Renderização | `keyboardRemovesOnlyTheChatMenuClearanceAndKeepsTouchTarget` | Teclado real do emulador visível, reserva inferior removida/restaurada e envio com altura mínima |
| Renderização | `lastHistoryRowExposesOneLabeledActionAboveComposer` | Última linha do histórico alcançável, ação rotulada, sem duplicação dos textos filhos na semântica e coluna limitada |
| Renderização | `retryAndComposerExposeLabelsAndReachableTouchTargets` | Erro de rede, ação manual de nova tentativa e campo/envio alcançáveis e rotulados |
| Renderização | `longConversationRecyclesRowsAndDoesNotLeakFailedStatus` | 80 mensagens, última linha alcançável e reciclagem sem reaproveitar indicação de falha indevida |
| Lifecycle | `newChatWithoutSessionConfirmsDraftAndDoesNotCallClose` | Confirmação/cancelamento de descarte e reset local sem fechamento remoto |
| Lifecycle | `closeFailurePreservesDraftAndManualRetryShowsConfirmationAgain` | Falha de fechamento, rascunho preservado e nova tentativa confirmada manualmente |
| Lifecycle | `newChatAndResumeExposeLabeledReachableAccessibilityActions` | Saída/retorno para apresentação inicial, novo chat e retomada com ações acessíveis |

Fontes: métodos dos [testes de renderização][rendering] e [lifecycle][lifecycle]. Os demais testes das classes verificam envio único, ID de sessão, reabertura do histórico, rotação/recriação, logout/troca de conta, respostas tardias, timeout, espera e bloqueio P03 no perfil-base.

Os logs e todas as capturas locais ficam em `app/build/chat-stage10-preview/matrix/<resolução>-font-<escala>/`; `results.json` reúne os resultados, e `viewport.log` guarda a área medida nas capturas. Esses arquivos são ignorados pelo Git e podem desaparecer com limpeza de build. As capturas selecionadas abaixo ficam junto da documentação; contêm somente fixtures, nunca dados reais da conta. São cópias integrais das capturas, sem recortes ou edição.

| Captura preservada | Perfil/fonte | Evidência visível |
| --- | --- | --- |
| [Celular pequeno: falha de fechamento](acceptance/phone-small-portrait-error.png) | 360x640 / 1.3 | Aviso, rascunho preservado e nova tentativa |
| [Celular pequeno: alvo de nova tentativa](acceptance/phone-small-landscape-retry.png) | 640x360 / 1.3 | Botão inteiro após correção e campo compacto com contador |
| [Celular pequeno: teclado](acceptance/phone-small-landscape-keyboard.png) | 640x360 / 1.3 | Compositor alcançável na área de 49dp; aviso do próprio Gboard |
| [Celular grande: fim da resposta longa](acceptance/phone-large-portrait-response.png) | 412x915 / 1.3 | Últimas linhas alcançáveis acima do compositor |
| [Celular grande: retomada](acceptance/phone-large-landscape-resume.png) | 915x412 / 1.3 | Apresentação inicial com ações de novo chat/retomada |
| [Tablet: histórico](acceptance/tablet-portrait-history.png) | 800x1280 / 1.3 | Coluna limitada e histórico com data/ID/ícone neutro |
| [Tablet: retomada](acceptance/tablet-landscape-actions.png) | 1280x800 / 1.3 | Ações rotuladas e compositor no perfil largo |

As capturas em fim de lista podem mostrar apenas parte do item anterior por rolagem; os testes de texto longo verificam preservação integral e ausência de reticências no conteúdo recebido. Nos testes de renderização, `bindConversation` não conecta a ação de novo chat e esse botão aparece desabilitado; os cliques de novo chat/retomada são exercitados separadamente pelos testes de lifecycle. Nenhuma dessas imagens comprova o shell autenticado ou resposta real de negócio.

Para repetir um perfil, aplique `wm density 160`, `wm size <largura>x<altura>` e `settings put system font_scale <1.0 ou 1.3>` usando `adb -s emulator-5580 shell`. Use `am instrument -e class` com os nove métodos acima, separados por vírgula e com a forma `pacote.Classe#método`. Leia a saída completa; o retorno do processo `adb` por si só não indica sucesso dos testes. Restaure com `wm size reset`, `wm density reset` e `font_scale 1.0` ao terminar.

## Acessibilidade e textos

Os novos testes inspecionam `AccessibilityNodeInfo`: texto/descrição, foco, habilitação, ação de clique e dimensões medidas mínimas de 48dp. Verificam também alvos visíveis e última linha acima do compositor. Isso é evidência da árvore semântica e da geometria, **não substitui leitura manual e ordem de foco com TalkBack**. Fontes: [renderização][rendering], [lifecycle][lifecycle].

Foram calculados contrastes dos pares opacos declarados nos recursos, pela razão `(Lmaior + 0,05)/(Lmenor + 0,05)` com luminância sRGB linear: branco/`#3069E7` = **4,88:1**; `#234FA3`/`#E7EFFF` = **6,69:1**; branco/`#486EA6` = **5,18:1**. Fontes das cores: [colors.xml][colors], [chat_styles.xml][styles], [balão do usuário][user-bubble]. Esses cálculos não são certificação de todos os pixels/estados: fundo com brilho, transparências, estados desabilitados e foco ainda exigem auditoria completa.

A auditoria estática não encontrou `android:text`, `hint` ou `contentDescription` literais nos layouts `*chat*.xml`, nem `setText("...")`, `setHint("...")`, `setContentDescription("...")`, Toast ou Snackbar com texto fixo no Fragment/adapters do chatbot. Os recursos estão em [chat_strings.xml][strings], incluindo mensagens de erro usadas por [ChatbotErrors][errors]. Texto da API/usuário é dinâmico; strings vazias e mensagens de exceção de programação não foram confundidas com texto fixo de apresentação. Fontes: [Fragment][fragment], [adapter de mensagens][message-adapter], [adapter de sessões][session-adapter].

## Comparação com as cinco referências locais

Comparação por inspeção das imagens em [design/](design/) e das capturas da execução; não foi feita medição de equivalência pixel a pixel nem declarado aceite visual final.

| Referência | Preservado / adaptação | Limite para aceite |
| --- | --- | --- |
| [Inicial](design/telainicialchatbot.png) | Fundo escuro com brilho, título e histórico; novo chat/retomada acrescentados por D05; datas/ID/ícone neutro por D04; “Ver todos” removido por decisão | Sugestões iniciais continuam ausentes por D03, sem aprovação de exclusão definitiva |
| [Vazia](design/telachatvazia.png) | Compositor inferior e área rolável; novo botão superior previsto em D05 | Enviar usa texto em vez do ícone da imagem; revisão visual ainda pendente |
| [Pergunta](design/telachatpergunta.png) | Mensagem do usuário à direita e estado de espera | Indicador de espera é Material; cabeçalho não fica visível no modo de conversa; não há aprovação específica de equivalência desses detalhes |
| [Resposta](design/telachatresposta.png) | Usuário à direita, assistente à esquerda, balões azul/claro e texto integral | Balões atuais têm cantos arredondados sem as caudas das imagens; conteúdo é fixture, não resposta de negócio |
| [Conversa longa](design/telachatresposta2.png) | Múltiplas mensagens e rolagem | Pedidos dependentes de contexto anterior continuam sujeitos a P07; reproduzir o texto da referência não comprova continuidade do backend |

Fontes das implementações: [layouts e lógica do Fragment][fragment], [balão do usuário][user-bubble], [balão do assistente][assistant-layout], [estilos][styles]; mudanças aprovadas em [D04–D06/D10](decisions.md). As diferenças restantes são registradas para revisão, sem redesenho nesta etapa de teste/documentação.

## Rastreabilidade do briefing

| Critério | Situação | Evidência / condição restante |
| --- | --- | --- |
| Enviar e receber resposta | **Parcial** | Fluxo simulado testado; composição real bloqueada por P03, sem request real autorizado |
| Respostas usam consumo da conta toda | **Bloqueado** | Sem conta/dados reais; comparar resposta com fonte e período da conta após P03, conforme D02 |
| Clima | **Bloqueado** | D09 permite histórico na primeira entrega; falta origem/localização/período/ausência verificados com fonte real; atual/futuro adiados |
| Dicas de consumo consciente | **Bloqueado** | Fixtures não avaliam coerência de respostas reais nem distinguem medições reais de dicas gerais |
| Acompanhamento de metas | **Pendente** | P08 não define consulta versus criação/edição; nenhum efeito de ferramenta foi validado |
| Layout conforme referência | **Parcial** | Comparação acima; D03 e diferenças visuais abertas; menu real fora do host instrumentado |
| Textos fixos em recursos | **Verificado no escopo da feature** | Auditoria XML/Java acima; lint global não está limpo |
| Responsividade sem cortes/espaços indevidos | **Parcial** | Matriz simulada e alvos medidos; não comprova shell autenticado, diagonal física ou TalkBack manual |
| Testado nos três perfis em ambas orientações | **Verificado em emulador** | 12 combinações de tamanho/fonte, 108 execuções; não equivale a três dispositivos físicos nem ao aceite completo do layout |
| Continuidade e novo chat | **Parcial** | Correlação de sessão, fechamento e lifecycle simulados; sem prova real de contexto dos especialistas (P07) |
| Gravações por ferramentas permitidas | **Bloqueado** | Permitidas por D08, mas não testadas; não foi criado modo somente leitura nem prometida ferramenta ausente |

Fontes dos critérios: [briefing](briefing.md), [rastreabilidade do plano](plan.md#rastreabilidade-dos-critérios-de-aceite), [decisões](decisions.md), [limites do contrato](api-contract.md). Os checkboxes do briefing só assinalam evidências estritas; não há checkbox global de entrega concluída.

## Pendências para aceite final

1. **P03:** confirmação externa da revisão publicada e autenticação/gateway das operações; depois, autorização e ambiente de teste para integração real. Não liberar o gate nesta etapa.
2. **Dados de negócio:** conta de teste com consumo, histórico climático e metas conhecidos; validar conteúdo, escopo, ausência de dados e efeitos reais das ferramentas. Fixtures não atendem a esses critérios.
3. **P07:** critério de aceite para continuidade diante do contexto limitado dos especialistas, sem ajuste de backend por enquanto.
4. **D03/P08:** conteúdo e interação das sugestões iniciais; alcance do acompanhamento de metas.
5. **Revisão visual:** decidir sobre as diferenças listadas e testar menu/IME no shell autenticado, em todos os perfis.
6. **Acessibilidade e plataformas:** TalkBack manual, ordem/fala/feedback de espera e erro e auditoria completa de contraste. **INCERTO — cobertura adicional proposta no plano:** executar também no Android mínimo/API 30; não há imagem API 30 disponível no SDK deste host. Dispositivos físicos não foram usados, sem presumir que o briefing exija comprovar a diagonal física de um tablet em vez do perfil em dp.
7. **Lint global:** sete erros e avisos fora da feature, sem correção autorizada nesta etapa.

Não se presume aprovação de entrega parcial, liberação de P03 ou dispensa desses critérios. Fontes das pendências de produto: [decisions.md](decisions.md); limites conhecidos do backend: [api-contract.md](api-contract.md); verificações requeridas: [plan.md](plan.md).

[rendering]: ../../app/src/androidTest/java/com/aula/delta_mobile_app/ChatRenderingInstrumentedTest.java
[lifecycle]: ../../app/src/androidTest/java/com/aula/delta_mobile_app/ChatLifecycleInstrumentedTest.java
[vm]: ../../app/src/main/java/com/aula/delta_mobile_app/viewmodel/ChatViewModel.java
[build]: ../../app/build.gradle.kts
[main]: ../../app/src/main/java/com/aula/delta_mobile_app/view/activities/main/MainActivity.java
[fragment]: ../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/ChatFragment.java
[strings]: ../../app/src/main/res/values/chat_strings.xml
[styles]: ../../app/src/main/res/values/chat_styles.xml
[colors]: ../../app/src/main/res/values/colors.xml
[errors]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/ChatbotErrors.java
[message-adapter]: ../../app/src/main/java/com/aula/delta_mobile_app/view/ChatMessageAdapter.java
[session-adapter]: ../../app/src/main/java/com/aula/delta_mobile_app/view/ChatSessionAdapter.java
[user-bubble]: ../../app/src/main/res/drawable/chat_user_bubble.xml
[assistant-layout]: ../../app/src/main/res/layout/item_chat_assistant.xml
