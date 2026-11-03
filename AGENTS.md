# AGENTS.md — delta-mobile-app

## Escopo e fontes

Este arquivo reúne convenções gerais do repositório mobile. Respeite as instruções explícitas da pessoa usuária e o escopo da tarefa; consulte os `AGENTS.md` mais específicos nos diretórios envolvidos. **Origem:** solicitação explícita nesta conversa, em 25/10/2026; não há arquivo anterior para esta regra.

Cada orientação técnica cita sua origem. **INCERTO** identifica convenção inferida, recomendação ou escolha sem definição formal nas docs; não a apresente como política já consolidada. Versões são as declaradas no checkout, não uma recomendação de atualização. **Origem:** solicitação explícita da pessoa usuária nesta tarefa.

## Stack e versões declaradas

| Área | Configuração | Origem |
| --- | --- | --- |
| Plataforma | Android nativo; módulo `:app`; namespace/applicationId `com.aula.delta_mobile_app` | [app/build.gradle.kts][app-build], [settings.gradle.kts][settings] |
| Linguagem | Fontes Java; compatibilidade de fonte/bytecode Java 11; build em Kotlin DSL | [app/build.gradle.kts][app-build], [AbnormalConsumptionFragment.java][abnormal-fragment], [build.gradle.kts][root-build] |
| SDK | `minSdk=30` (Android 11), `compileSdk=36`, `targetSdk=36` | [app/build.gradle.kts][app-build] |
| Build | AGP `9.4.1`, Gradle Wrapper `9.6.0`; AndroidX habilitado | [libs.versions.toml][versions], [gradle-wrapper.properties][wrapper], [gradle.properties][gradle-properties] |
| UI | XML/Views; AppCompat `1.8.0`, Material `1.14.0`, Activity/Activity KTX `1.13.0`, ConstraintLayout `2.2.2`, SplashScreen `1.2.0` | [libs.versions.toml][versions], [app/build.gradle.kts][app-build] |
| Estado/navegação | Lifecycle ViewModel/LiveData `2.11.0`; Navigation Fragment/UI `2.10.1` | [app/build.gradle.kts][app-build], [libs.versions.toml][versions] |
| Rede | Retrofit/converter Gson `2.12.0`, Gson `2.14.0`, OkHttp/logging interceptor `4.12.0` | [libs.versions.toml][versions] |
| Câmera/QR | CameraX `1.5.3`, ML Kit Barcode `17.3.0` | [libs.versions.toml][versions] |
| Testes | JUnit `4.13.2`, AndroidX Test JUnit `1.3.0`, Espresso `3.7.0`; runner `AndroidJUnitRunner` | [libs.versions.toml][versions], [app/build.gradle.kts][app-build] |

Java 11 é a compatibilidade do código, não uma declaração do JDK de execução do Gradle. O app não configura Compose nem aplica plugin Kotlin Android. Não descreva dependências disponíveis como tecnologia obrigatoriamente usada por todas as telas. [app/build.gradle.kts][app-build], [libs.versions.toml][versions]

## Arquitetura e pacotes

As fontes principais ficam em `app/src/main/java/com/aula/delta_mobile_app/`. Respeite a divisão documentada no [README.md][readme]:

| Pacote/diretório | Responsabilidade | Origem adicional |
| --- | --- | --- |
| `services/api` | Interfaces Retrofit e rotas | [ApiService.java][api] |
| `services/network` | Clientes HTTP, Gson, configuração, interceptor e erros | [RetrofitClient.java][retrofit], [ApiErrors.java][api-errors] |
| `services/session` | Contrato e armazenamento de sessão/tokens | [SessionStore.java][session-store], [TokenStore.java][token-store] |
| `repository` | Acesso aos serviços, contratos de ações e regras de cadastro/consulta | [AuthRepository.java][auth-repository], [AbnormalConsumptionRepository.java][abnormal-repository] |
| `model` | Payloads de API, modelos de domínio e estados compartilhados | [ApiModels.java][api-models], [ScreenState.java][screen-state] |
| `viewmodel` | Estado e ações das telas; coordenação dos resultados | [AbnormalConsumptionViewModel.java][abnormal-vm], [HomeViewModel.java][home-vm] |
| `view` | Activities, Fragments e Views próprias; subpacotes `activities`/`fragments` | [MainActivity.java][main-activity], [AbnormalConsumptionFragment.java][abnormal-fragment], [StatusNoticeView.java][notice] |
| `res` e `assets` | Layouts, navegação, estilos, textos, imagens e catálogo local | [fragment_home.xml][home-layout], [main_navigation.xml][main-navigation], [registration_catalog.json][catalog] |

- Mantenha HTTP no repositório e apresentação/observação no Fragment, seguindo a divisão existente. **INCERTO:** “MVVM” é uma descrição inferida dessa organização; o README não formaliza um framework arquitetural. [README.md][readme], [AbnormalConsumptionRepository.java][abnormal-repository], [AbnormalConsumptionViewModel.java][abnormal-vm], [AbnormalConsumptionFragment.java][abnormal-fragment]
- Diferencie DTO publicado de modelo preparado para uma fonte futura. Um modelo ou renderizador não comprova endpoint nem integração funcional. [ANALISE_HORARIO_DATA_REQUIRED.md][hourly-data], [VAZAMENTOS_DATA_REQUIRED.md][leak-data]

## Nomenclatura e formatação

- Use UTF-8, espaços, indentação de quatro espaços, quebra final de linha e remoção de espaços finais conforme `.editorconfig`; Markdown tem exceção à remoção de espaços finais. XML com vários atributos usa um atributo por linha. [.editorconfig][editorconfig], [README.md][readme]
- Java mantém chamadas, argumentos e condições compactos, sem comentários no código, conforme a orientação expressa do README. [README.md][readme]
- **INCERTO — convenção observada:** classes em `PascalCase`, métodos/campos em `camelCase`, constantes em `UPPER_SNAKE_CASE` e identificadores de código em inglês. Não há guia formal de nomenclatura geral. [AbnormalConsumptionViewModel.java][abnormal-vm], [NetworkConfig.java][network-config], [ApiService.java][api]
- **INCERTO — convenção observada:** classes usam sufixos `Fragment`, `Activity`, `ViewModel`, `Repository`; layouts usam `fragment_*`/`activity_*`; recursos e IDs usam `snake_case` com prefixo da tela. Destinos de navegação existentes usam também IDs como `homeFragment`; não os renomeie para uniformizar essa descrição. [AbnormalConsumptionFragment.java][abnormal-fragment], [fragment_abnormal_consumption.xml][abnormal-layout], [main_navigation.xml][main-navigation]

## Rede, sessão e contratos

- Declare novas rotas Delta em `ApiService`; o repositório responsável obtém o serviço por `AuthRepository.getInstance(context).api()`. `RetrofitClient.getApiService(context)` delega à mesma instância. CEP usa `RetrofitClient.getCepService()`, cliente BrasilAPI independente. [README.md][readme], [RetrofitClient.java][retrofit]
- Execute rede fora da thread principal. `AuthRepository.resource()` usa executor de uma thread e verifica sessão/geração; ViewModels retornam à main thread com `Handler`. CEP usa `Call.enqueue()`. Não suponha que todo callback de repositório já esteja na main thread. [AUTH_MOBILE.md][auth-doc], [AuthRepository.java][auth-repository], [AbnormalConsumptionViewModel.java][abnormal-vm], [PostalCodeRepository.java][postal-repository]
- A base vem de `BuildConfig.DELTA_API_URL`; overrides: `-PdeltaApiUrl=...` em debug e `-PdeltaReleaseApiUrl=...` em release. A URL termina em `/`; release exige HTTPS. Ao reutilizar a configuração, preserve os timeouts de conexão/escrita de 30 s e leitura de 90 s. [app/build.gradle.kts][app-build], [NetworkConfig.java][network-config], [AUTH_MOBILE.md][auth-doc]
- Preserve o isolamento de credenciais: Bearer apenas na origem/prefixo Delta protegidos, com exclusões das rotas públicas; refresh com cliente público, trava compartilhada e no máximo uma repetição da requisição protegida. Falha de rede não equivale a refresh rejeitado. [SessionInterceptor.java][interceptor], [SessionInterceptorTest.java][interceptor-test]
- Use identidade real e valide vínculo com a conta/imóvel antes de apresentar dados; não fixe IDs de usuários/imóveis. Datas dos DTOs são strings ISO-8601 e dinheiro usa `BigDecimal`. [HOME_API.md][home-api], [AbnormalConsumptionRepository.java][abnormal-repository], [ApiModels.java][api-models], [AUTH_MOBILE.md][auth-doc]
- Confirme o contrato antes de acrescentar requests/fallback. A conclusão do cadastro usa `complete-registration`, sem sequência alternativa de POSTs que possa duplicar registros. [AUTH_MOBILE.md][auth-doc], [ApiService.java][api]

## Erros e estados da UI

- Reutilize `ScreenState<T>`: `IDLE`, `LOADING`, `SUCCESS`, `ERROR`, com `data`, `message`, `code` e `retryAt`; `busy()` indica somente `LOADING`. Observe LiveData com `getViewLifecycleOwner()` nos Fragments. [ScreenState.java][screen-state], [AbnormalConsumptionFragment.java][abnormal-fragment]
- HTTP malsucedido passa por `ApiErrors.from()` nos exemplos de repositório; conexão usa `ApiErrors.network()`. Há tradução por código e `Retry-After` para 429. O parser de negócio aceita `message` apenas em 400/409 e filtra detalhes técnicos; não é um parser universal para qualquer serviço. [ApiErrors.java][api-errors], [AbnormalConsumptionRepository.java][abnormal-repository]
- Atenção: `displayMessage()` retorna vazio em `ERROR`, preservando o detalhe em `message`. Isso é coberto por teste; passar esse helper a um aviso não exibe o erro. Não altere silenciosamente esse contrato ao reutilizar a estrutura. [ScreenState.java][screen-state], [ScreenStateTest.java][screen-state-test]
- Use `StatusNoticeView`, diálogos ou componentes de aviso para erros/estados que exigem ação; não insira diagnóstico ou nova tentativa como texto solto. Aviso vazio fica `GONE`; validação de campo permanece associada ao campo. [HOME_TODO.md][home-todo], [VAZAMENTOS_TODO.md][leak-todo], [StatusNoticeView.java][notice]
- Preserve seleção/estado durante a vida do ViewModel; evite consultas equivalentes em andamento e descarte respostas antigas de outra seleção. O exemplo usa `generation`, `cleared` e limpeza em `onCleared()`. Isso não comprova persistência após morte do processo. [AbnormalConsumptionViewModel.java][abnormal-vm], [AUTH_MOBILE.md][auth-doc]
- Diferencie zero confirmado, resposta vazia bem-sucedida, lacuna e falha conforme o modelo da tela; não generalize “ausência = zero”. Na Home, aguarde as consultas necessárias se resolverem em sucesso ou erro, sem loading infinito. [CONSUMO_ANORMAL_DATA_REQUIRED.md][abnormal-data], [ANALISE_HORARIO_DATA_REQUIRED.md][hourly-data], [HOME_API.md][home-api], [HomeLoadState.java][home-load]
- O carregamento comum usa `AuthFlowFragment.showLoading()`/`LoadingView`; o shell também coordena a Home. Use a coordenação da tela, sem presumir que todos os fluxos dependam do loading da Home. [AuthFlowFragment.java][auth-flow], [MainActivity.java][main-activity], [DASHBOARD_TODO.md][dashboard-todo]

## ViewBinding, navegação e DI

- **ViewBinding:** `viewBinding=true` está habilitado, mas as telas verificadas usam `findViewById` e `Fragment(R.layout...)`; não há uso de classes geradas `*Binding` nas fontes Java consultadas. Não o descreva como padrão obrigatório já adotado. **INCERTO:** não há convenção documentada para migração ou ciclo de vida de bindings; explicite a escolha se isso entrar no escopo. [app/build.gradle.kts][app-build], [LoginFragment.java][login-fragment], [AbnormalConsumptionFragment.java][abnormal-fragment]
- **Navegação:** use os grafos e destinos/argumentos existentes. Fragments usam `NavHostFragment`/`NavController`; `AuthFlowFragment` oferece helpers/animações; `MainActivity` controla menu, back stack salvo e restauração. Não presuma Safe Args: não há plugin configurado, e o código usa `Bundle`/argumentos diretamente. [auth_navigation.xml][auth-navigation], [main_navigation.xml][main-navigation], [AuthFlowFragment.java][auth-flow], [MainActivity.java][main-activity], [app/build.gradle.kts][app-build]
- **DI:** composição manual com singleton `AuthRepository` usando application context, dependências por construtor nos repositórios e `ViewModelProvider` nas telas. Não há Hilt/Dagger/Koin nas dependências declaradas. **INCERTO:** continuar essa composição é a extensão mais próxima dos exemplos, não uma proibição formal de frameworks. [AuthRepository.java][auth-repository], [AbnormalConsumptionRepository.java][abnormal-repository], [PostalCodeRepository.java][postal-repository], [AbnormalConsumptionFragment.java][abnormal-fragment], [app/build.gradle.kts][app-build]

## XML responsivo, recursos e acessibilidade

- Mantenha rolagem e crescimento de textos, sem cortar valores, unidades e ações com fonte ampliada. Os exemplos usam `ScrollView`/`fillViewport`, conteúdo vertical `wrap_content` e distribuição por `0dp`/peso. São exemplos de composição, não obrigação de usar o mesmo container em todas as telas. [HOME_TODO.md][home-todo], [CONSUMO_ANORMAL_TODO.md][abnormal-todo], [fragment_home.xml][home-layout], [fragment_abnormal_consumption.xml][abnormal-layout]
- Reserve espaço inferior para o conteúdo rolar acima do menu flutuante. Preserve o tratamento central de barras/IME: `MainActivity` oculta o menu com o teclado e o manifest usa `adjustResize`. [VAZAMENTOS_TODO.md][leak-todo], [fragment_abnormal_consumption.xml][abnormal-layout], [MainActivity.java][main-activity], [AndroidManifest.xml][manifest]
- **INCERTO — convenção observada:** layout em `dp`, texto em `sp`, margens lógicas `Start`/`End` e `@dimen`/`@style` para recursos compartilhados. Há medidas/cores literais; não existe sistema completo de tokens nem proibição geral documentada desses literais. [fragment_signup_account.xml][signup-layout], [home_styles.xml][home-styles], [dimens.xml][dimens]
- `activity_horizontal_margin` vale 16dp na base, 48dp em `values-w600dp`, 200dp em `values-w1240dp` e 48dp em `values-land`. São qualificadores `w`, não `sw`, e só afetam Views que referenciam esse recurso. Há telas com paddings próprios. **INCERTO:** não há composição universal de tablet ou largura máxima documentada; esses arquivos não comprovam responsividade de todas as telas. [dimens.xml][dimens], [w600dp][dimens-600], [w1240dp][dimens-1240], [land][dimens-land], [fragment_home.xml][home-layout]
- **INCERTO — orientação de verificação solicitada nesta tarefa:** conferir celular/tablet em retrato/paisagem, sem inventar matriz oficial de larguras. As docs gerais pedem telas menores/maiores, fonte ampliada e aparelho/emulador, mas não fixam matriz global. [DASHBOARD_TODO.md][dashboard-todo], [VAZAMENTOS_TODO.md][leak-todo]; origem adicional: pedido explícito da pessoa usuária nesta conversa.
- Preserve áreas interativas de pelo menos 48dp, descrições acessíveis e acesso a dados dos gráficos sem depender só da cor; exclua elementos decorativos da leitura quando apropriado aos exemplos. [DASHBOARD_TODO.md][dashboard-todo], [fragment_abnormal_consumption.xml][abnormal-layout], [AbnormalConsumptionChart.java][abnormal-chart]
- **INCERTO — organização observada:** textos fixos dos XMLs usam `@string`, estilos são agrupados por tela e o tema é Material 3 DayNight sem ActionBar. Java ainda contém textos literais; não afirme que a base inteira está sem hardcode. [fragment_abnormal_consumption.xml][abnormal-layout], [abnormal_strings.xml][abnormal-strings], [home_styles.xml][home-styles], [themes.xml][themes], [LoginFragment.java][login-fragment]

## Proibições sustentadas pelas fontes

- Não use valores, nomes, fotos ou curvas da referência visual como dados reais; não invente fontes, endpoints, limiares de detecção, alertas ou fórmulas para funções sem contrato. [HOME_TODO.md][home-todo], [DASHBOARD_TODO.md][dashboard-todo], [VAZAMENTOS_DATA_REQUIRED.md][leak-data]
- Não transforme ausência de telemetria em garantia de ausência de vazamento; não reconstrua horários/equipamentos a partir do consumo diário; não some vazão como volume. [VAZAMENTOS_DATA_REQUIRED.md][leak-data], [ANALISE_HORARIO_DATA_REQUIRED.md][hourly-data]
- Não exponha tokens, senhas ou headers de autenticação em logs nem substitua armazenamento cifrado por texto puro. Preserve AES-GCM/Android Keystore, exclusões de backup e salvamento de estado desabilitado nas senhas. Não envie credenciais internas entre serviços pelo app. [AUTH_MOBILE.md][auth-doc], [TokenStore.java][token-store], [LoginFragment.java][login-fragment], [backup_rules.xml][backup], [data_extraction_rules.xml][extraction]
- **INCERTO — orientação inferida das exclusões:** não versione segredos, chaves, dumps, logs e saídas de build ignoradas. `.gitignore` não constitui sozinho uma política completa de segurança. [.gitignore][gitignore]
- Não declare integração real, equivalência visual, APK instalado ou testes executados apenas com leitura de código ou compilação separada javac/AAPT2. Registre o executado e seus limites. [AUTH_MOBILE.md][auth-doc], [CONSUMO_ANORMAL_DATA_REQUIRED.md][abnormal-data]

## Build, lint e testes

Use o Wrapper. Estes comandos são documentados; o prefixo `.\` explicita a invocação local no PowerShell. [README.md][readme], [AUTH_MOBILE.md][auth-doc], [gradle-wrapper.properties][wrapper]

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

- Validação combinada documentada: `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`; o README também usa `testDebugUnitTest assembleDebug`. Instrumentação exige aparelho/emulador de teste e limpa a sessão do app de teste. [README.md][readme], [AUTH_MOBILE.md][auth-doc], [AuthStateInstrumentedTest.java][auth-test]
- Testes JVM ficam em `app/src/test`; instrumentados em `app/src/androidTest`. Há rede substituída e testes de modelo/fluxo; os testes de autenticação documentados não chamam produção. [SessionInterceptorTest.java][interceptor-test], [ScreenStateTest.java][screen-state-test], [AuthStateInstrumentedTest.java][auth-test], [AUTH_MOBILE.md][auth-doc]
- As docs registram falhas históricas de loopback Java/Gradle e ausência de dispositivo; não as apresente como falha atual sem executar o comando. Compilação direta não substitui build/lint/instrumentação. [AUTH_MOBILE.md][auth-doc]
- O workflow de PR chama verificações compartilhadas da organização e não declara tarefas Gradle locais. Não afirme que testes Android rodaram no CI sem evidência da execução. [.github/workflows/trigger_actions.yml][workflow]

## Feature de referência

**INCERTO — recomendação, não molde oficial:** use **Consumo anormal** para entender domínio → repositório → ViewModel → Fragment/layout. Ela demonstra consulta real, validação do imóvel, estados, nova tentativa, seleção de período e descarte de respostas atrasadas. Confira testes/limites antes de copiar; o comportamento de `displayMessage()` descrito acima também afeta esse exemplo. [AbnormalConsumptionRepository.java][abnormal-repository], [AbnormalConsumptionViewModel.java][abnormal-vm], [AbnormalConsumptionFragment.java][abnormal-fragment], [fragment_abnormal_consumption.xml][abnormal-layout], [AbnormalConsumptionDataTest.java][abnormal-test], [CONSUMO_ANORMAL_DATA_REQUIRED.md][abnormal-data]

Para consultas dependentes, consulte a prontidão inicial da Home; para rede/sessão, a autenticação. Não generalize comportamento de uma tela para todas as demais. [HOME_API.md][home-api], [HomeLoadState.java][home-load], [AUTH_MOBILE.md][auth-doc]

## Como trabalhar com a pessoa usuária

**Origem de todo este bloco:** instruções diretas da pessoa usuária nesta conversa, em 25/10/2026. Não há arquivo de origem anterior para essas preferências; elas não são deduções de código.

- Trabalhe uma etapa por vez e explique o porquê, relacionando cada ação ao problema que resolve.
- Respeite a etapa e os arquivos autorizados. Pedido de análise/documentação não autoriza antecipar implementação. Aprovação de uma solução não resolve automaticamente outras pendências.
- Faça uma pergunta por vez quando faltar decisão necessária, priorizando bloqueios. Distinga requisito aprovado, capacidade atual e proposta.
- Cite arquivos nas afirmações técnicas e marque **INCERTO** nas deduções. Se a fonte não existir, informe isso; não invente padrões.

[readme]: README.md
[app-build]: app/build.gradle.kts
[root-build]: build.gradle.kts
[settings]: settings.gradle.kts
[versions]: gradle/libs.versions.toml
[wrapper]: gradle/wrapper/gradle-wrapper.properties
[gradle-properties]: gradle.properties
[editorconfig]: .editorconfig
[api]: app/src/main/java/com/aula/delta_mobile_app/services/api/ApiService.java
[retrofit]: app/src/main/java/com/aula/delta_mobile_app/services/network/RetrofitClient.java
[network-config]: app/src/main/java/com/aula/delta_mobile_app/services/network/NetworkConfig.java
[api-errors]: app/src/main/java/com/aula/delta_mobile_app/services/network/ApiErrors.java
[interceptor]: app/src/main/java/com/aula/delta_mobile_app/services/network/SessionInterceptor.java
[token-store]: app/src/main/java/com/aula/delta_mobile_app/services/session/TokenStore.java
[session-store]: app/src/main/java/com/aula/delta_mobile_app/services/session/SessionStore.java
[auth-repository]: app/src/main/java/com/aula/delta_mobile_app/repository/AuthRepository.java
[abnormal-repository]: app/src/main/java/com/aula/delta_mobile_app/repository/AbnormalConsumptionRepository.java
[postal-repository]: app/src/main/java/com/aula/delta_mobile_app/repository/PostalCodeRepository.java
[api-models]: app/src/main/java/com/aula/delta_mobile_app/model/ApiModels.java
[screen-state]: app/src/main/java/com/aula/delta_mobile_app/model/ScreenState.java
[abnormal-vm]: app/src/main/java/com/aula/delta_mobile_app/viewmodel/AbnormalConsumptionViewModel.java
[home-vm]: app/src/main/java/com/aula/delta_mobile_app/viewmodel/HomeViewModel.java
[home-load]: app/src/main/java/com/aula/delta_mobile_app/viewmodel/HomeLoadState.java
[main-activity]: app/src/main/java/com/aula/delta_mobile_app/view/activities/main/MainActivity.java
[abnormal-fragment]: app/src/main/java/com/aula/delta_mobile_app/view/fragments/AbnormalConsumptionFragment.java
[login-fragment]: app/src/main/java/com/aula/delta_mobile_app/view/fragments/login/LoginFragment.java
[auth-flow]: app/src/main/java/com/aula/delta_mobile_app/view/fragments/AuthFlowFragment.java
[notice]: app/src/main/java/com/aula/delta_mobile_app/view/StatusNoticeView.java
[abnormal-chart]: app/src/main/java/com/aula/delta_mobile_app/view/AbnormalConsumptionChart.java
[home-layout]: app/src/main/res/layout/fragment_home.xml
[abnormal-layout]: app/src/main/res/layout/fragment_abnormal_consumption.xml
[signup-layout]: app/src/main/res/layout/fragment_signup_account.xml
[auth-navigation]: app/src/main/res/navigation/auth_navigation.xml
[main-navigation]: app/src/main/res/navigation/main_navigation.xml
[catalog]: app/src/main/assets/registration_catalog.json
[home-styles]: app/src/main/res/values/home_styles.xml
[abnormal-strings]: app/src/main/res/values/abnormal_strings.xml
[themes]: app/src/main/res/values/themes.xml
[dimens]: app/src/main/res/values/dimens.xml
[dimens-600]: app/src/main/res/values-w600dp/dimens.xml
[dimens-1240]: app/src/main/res/values-w1240dp/dimens.xml
[dimens-land]: app/src/main/res/values-land/dimens.xml
[manifest]: app/src/main/AndroidManifest.xml
[backup]: app/src/main/res/xml/backup_rules.xml
[extraction]: app/src/main/res/xml/data_extraction_rules.xml
[auth-doc]: devdocs/fluxodavi/AUTH_MOBILE.md
[home-api]: devdocs/fluxodavi/HOME_API.md
[home-todo]: devdocs/fluxodavi/HOME_TODO.md
[dashboard-todo]: devdocs/fluxodavi/DASHBOARD_TODO.md
[abnormal-data]: devdocs/fluxodavi/CONSUMO_ANORMAL_DATA_REQUIRED.md
[abnormal-todo]: devdocs/fluxodavi/CONSUMO_ANORMAL_TODO.md
[hourly-data]: devdocs/fluxodavi/ANALISE_HORARIO_DATA_REQUIRED.md
[leak-data]: devdocs/fluxodavi/VAZAMENTOS_DATA_REQUIRED.md
[leak-todo]: devdocs/fluxodavi/VAZAMENTOS_TODO.md
[interceptor-test]: app/src/test/java/com/aula/delta_mobile_app/services/network/SessionInterceptorTest.java
[screen-state-test]: app/src/test/java/com/aula/delta_mobile_app/model/ScreenStateTest.java
[abnormal-test]: app/src/test/java/com/aula/delta_mobile_app/model/AbnormalConsumptionDataTest.java
[auth-test]: app/src/androidTest/java/com/aula/delta_mobile_app/AuthStateInstrumentedTest.java
[gitignore]: .gitignore
[workflow]: .github/workflows/trigger_actions.yml
