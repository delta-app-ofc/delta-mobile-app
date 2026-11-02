# Delta · Aplicativo Android

![Android](https://img.shields.io/badge/Android-API%2030%2B-34A853?logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-11%20source-007396?logo=openjdk&logoColor=white)
![Android Gradle Plugin](https://img.shields.io/badge/AGP-9.4.1-34A853?logo=android&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.6.0-02303A?logo=gradle&logoColor=white)
![Android SDK](https://img.shields.io/badge/compile%20%2F%20target%20SDK-36-3DDC84?logo=android&logoColor=white)

Do primeiro login à leitura do hidrômetro, o app Delta reúne no Android fluxos de cadastro, acompanhamento de consumo, análise de vazamentos e conversa com o chatbot. A interface usa Java e Views em XML; os dados chegam pelas APIs do projeto por serviços e repositórios dedicados.

**Tags:** `android` · `java` · `android-views` · `retrofit` · `monitoramento-de-agua` · `consumo` · `vazamentos` · `chatbot`

## Sumário

- [Visão geral](#visão-geral)
- [Stack e requisitos](#stack-e-requisitos)
- [Funcionalidades no código](#funcionalidades-no-código)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Configuração da API](#configuração-da-api)
- [Build, lint e testes](#build-lint-e-testes)
- [Documentação relacionada](#documentação-relacionada)

## Visão geral

| Camada | Implementação |
|---|---|
| Plataforma | Aplicativo Android nativo, módulo Gradle `:app`; `minSdk 30` (Android 11), `compileSdk 36` e `targetSdk 36` |
| Linguagem e interface | Java com compatibilidade de fonte/bytecode 11; layouts, navegação e estilos em XML/Views |
| Build | Android Gradle Plugin `9.4.1` e Gradle Wrapper `9.6.0` |
| Rede | Retrofit/Gson e OkHttp; clientes separados para API Delta e consulta de CEP |
| Estado da interface | `ViewModel`/`LiveData`, repositórios e `ScreenState<T>` para estados de carregamento, sucesso e erro |
| Sessão | Armazenamento de tokens com AES-GCM e Android Keystore |
| Código QR | CameraX e ML Kit Barcode; a câmera é opcional no dispositivo |

Java 11 descreve a compatibilidade do código compilado. O daemon do Gradle está configurado para JDK 21 em `gradle/gradle-daemon-jvm.properties`.

## Funcionalidades no código

- **Conta e cadastro:** login e fluxo de cadastro em etapas, incluindo endereço, imóvel e hábitos.
- **Imóvel e hidrômetro:** consulta de CEP, seleção de imóvel e vinculação de dispositivo por leitura de QR code ou digitação manual do código.
- **Consumo:** telas de início e dashboard, histórico, análise por horário e visualização de consumo anormal.
- **Vazamentos:** telas de detecção e acompanhamento com seus modelos de estado e visualizações.
- **Chatbot:** interface de conversa com modelos, repositório e cliente de API próprios.
- **Sessão e navegação:** rotas de autenticação e da área principal em grafos Android Navigation.

A presença de uma tela ou modelo documenta a implementação local; ela, por si só, não garante disponibilidade do serviço remoto ou do dado solicitado.

## Stack e requisitos

- JDK 21 para o daemon do Gradle, conforme a configuração do Wrapper.
- Android SDK Platform 36.
- Android SDK Build Tools compatíveis instalados pela ferramenta Android.
- Um aparelho ou emulador Android é necessário para executar a aplicação; os testes instrumentados também precisam de um dispositivo/emulador.

Dependências centrais declaradas incluem AndroidX AppCompat `1.8.0`, Material Components `1.14.0`, Lifecycle `2.11.0`, Navigation `2.10.1`, Retrofit `2.12.0`, Gson `2.14.0`, OkHttp `4.12.0`, CameraX `1.5.3` e ML Kit Barcode `17.3.0`. A lista completa e as versões ficam em `gradle/libs.versions.toml` e `app/build.gradle.kts`.

## Estrutura do projeto

O código Android fica em `app/src/main/java/com/aula/delta_mobile_app/`.

| Caminho | Responsabilidade |
|---|---|
| `services/api/` | Interfaces Retrofit para serviços Delta e chatbot |
| `services/network/` | Clientes HTTP, configuração, interceptores e tratamento de erros |
| `services/session/` | Armazenamento de tokens e estado da sessão |
| `repository/` | Acesso às APIs e regras de consulta/cadastro |
| `model/` | DTOs, modelos de tela e estados compartilhados |
| `viewmodel/` | Estado e ações que as telas observam |
| `view/activities/` e `view/fragments/` | Atividades, fragments e componentes visuais |
| `app/src/main/res/` | Layouts, navegação, textos, dimensões, estilos e imagens |
| `app/src/main/assets/` | Catálogo local usado pelo fluxo de cadastro |
| `app/src/test/` | Testes locais executáveis na JVM |
| `app/src/androidTest/` | Testes instrumentados que rodam em Android |
| `docs/chatbot/` e `devdocs/fluxodavi/` | Contratos, decisões, critérios de aceite e notas de integração |

### Organização das chamadas de API

Declare novas rotas Delta em `services/api/ApiService` e consuma-as no repositório responsável. Os repositórios Delta obtêm a instância por `AuthRepository.getInstance(context).api()`; `RetrofitClient.getApiService(context)` delega à mesma instância. A consulta de CEP usa `RetrofitClient.getCepService()`, cliente independente que não envia tokens Delta.

Mantenha as chamadas de rede fora da thread principal. Para detalhes de autenticação, sessão, configuração de URL e contrato de cadastro, consulte [Autenticação mobile](devdocs/fluxodavi/AUTH_MOBILE.md).

## Configuração da API

O build permite sobrescrever a base da API Delta por propriedades Gradle. A URL deve terminar com barra; releases exigem HTTPS.

- Debug: `-PdeltaApiUrl=https://HOST/`
- Release: `-PdeltaReleaseApiUrl=https://HOST/`

Para um backend local no emulador Android, a documentação de autenticação mostra `-PdeltaApiUrl=http://10.0.2.2:8080/`. Não coloque credenciais, tokens ou chaves no README nem em arquivos versionados.

## Build, lint e testes

Os comandos abaixo usam o Wrapper do repositório. No PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:testDebugUnitTest
```

Para rodar os testes instrumentados em um aparelho ou emulador conectado:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

O comando combinado de validação local também pode ser executado assim:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Os testes de unidade ficam em `app/src/test`; os instrumentados ficam em `app/src/androidTest`. A existência das suítes não representa uma execução recente nem um resultado de CI.

## Documentação relacionada

- [Guia de autenticação e sessão](devdocs/fluxodavi/AUTH_MOBILE.md)
- [Contrato da API do chatbot](docs/chatbot/api-contract.md)
- [Critérios de aceite do chatbot](docs/chatbot/acceptance.md)
- [Organização Delta no GitHub](https://github.com/delta-app-ofc)
- [delta-handbook](https://github.com/delta-app-ofc/delta-handbook) — documentação central do projeto

