# Autenticação mobile

O fluxo reutiliza `LoginActivity`, `auth_navigation.xml`, os cinco `Cadastro000–004Fragment` e os layouts existentes. `AuthFlowFragment` fornece somente helpers de UI. O rascunho pertence ao `SignupViewModel` compartilhado pela Activity; login usa `LoginViewModel`. Ambos acessam o único `AuthRepository`, que executa Retrofit em um executor. CEP usa `PostalCodeRepository` e o cliente BrasilAPI independente.

## Ambiente

Debug e release apontam por padrão para `https://delta-api-postgres.onrender.com/`, tanto no emulador quanto no aparelho físico. É possível sobrescrever com `-PdeltaApiUrl=http://10.0.2.2:8080/` em desenvolvimento ou `-PdeltaReleaseApiUrl=https://HOST/` em release. A URL deve terminar em `/`; release exige HTTPS. O timeout de leitura é de 90 segundos para permitir a inicialização da instância no Render.

## Cadastro e sessão

A segunda tela inicia `POST delta/auth/create-account` com dados pessoais e credenciais. É possível preencher o complemento enquanto essa chamada termina. Os estados de criação, CEP e conclusão são separados e sobrevivem à rotação. A conclusão usa exclusivamente `POST delta/auth/complete-registration`; sem posts separados de endereço/imóvel/hábitos. Navegação para `HomeFragment` ocorre após `completed=true`. A área inicial autenticada mantém acesso às telas existentes de hidrômetro, separadas do payload de cadastro.

`TokenStore` cifra tokens e perfil com AES-GCM, IV por gravação e chave do Android Keystore. SharedPreferences `delta_session` são excluídas de backup e transferência. SHA-256 hexadecimal do refresh original fica em uma chave separada. Renovação envia o original decifrado e preserva seu prazo e digest. Não há logs de conteúdo ou headers de autenticação. Senhas ficam apenas na memória do ViewModel e nos campos com salvamento de estado desabilitado; são removidas após autenticação bem-sucedida.

`SessionInterceptor` adiciona Bearer somente às rotas protegidas da origem Delta. Renovação usa cliente público sem interceptor e uma trava compartilhada. Chamadas concorrentes reutilizam o token renovado; requisições são repetidas no máximo uma vez. Falhas de rede preservam sessão; refresh rejeitado/expirado limpa a sessão. Geração impede que resposta anterior ao logout restaure tokens. Logout é local, sem inventar revogação no servidor.

Ao entrar/reiniciar, o repository consulta `/auth/me` e `/user-properties/{id}`. Uma associação existente indica complemento concluído conforme a implementação de `RegistrationCompletionService`; `/me` sozinho não informa conclusão. Falhas dessa consulta não presumem conclusão. Após morte do processo, a navegação volta ao login para verificar a sessão; o complemento deve ser preenchido novamente, sem persistir rascunhos de outro usuário.

## Catálogo e regiões

`app/src/main/assets/registration_catalog.json` contém IDs dos inserts fornecidos pelo usuário: hábitos 1–6 e dias 1–7, com chaves explícitas independentes da posição visual. Se o banco foi carregado com sequências diferentes, atualize o arquivo com os IDs reais. Não existe endpoint de catálogo no backend inspecionado.

Regiões configuradas para SP: São Paulo → 1, Lins → 2, Presidente Prudente → 3, Adamantina/Pirapozinho → 4, Bragança Paulista → 5. O mapeamento das cidades que dão nome às regiões deriva dos comentários e nomes do dataload fornecido. Outras cidades, inclusive as demais da Grande SP, exigem confirmação da abrangência tarifária antes de ampliar a configuração. Localidades não mapeadas são bloqueadas com explicação.

O dataload contém endereços de exemplo em São Paulo ligados às regiões 2–5, contradizendo os comentários de regiões tarifárias. Esses endereços não são usados como regra geográfica. Há também constraint SQL de nascimento com idade mínima de 18 anos, enquanto o DTO de autenticação exige apenas data passada: o mobile segue o DTO e registra a divergência para alinhamento do contrato.

## Validação e pendências

Testes JVM cobrem 401 concorrentes, original de refresh, máximo de retries, falha de rede, logout durante renovação, hosts públicos/externos, digest e resolução de catálogo. Testes instrumentados cobrem Android Keystore/SharedPreferences, renovação sem rotação, limpeza e validações do ViewModel. Não fazem chamadas à produção.

Comandos normais: `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`. Instrumentação: `gradlew.bat :app:connectedDebugAndroidTest` com aparelho/emulador de teste. Os testes instrumentados limpam a sessão do aplicativo de teste.

A captura Swagger fornecida confirma `/delta/auth/register`, `/delta/auth/login`, `/delta/auth/refresh` e `/delta/auth/me`, com os DTOs esperados. O JSON atualizado em `http://localhost:8080/v3/api-docs` também confirma `/delta/auth/create-account` e `/delta/auth/complete-registration`. Os DTOs foram cruzados com `AuthController`, `AuthSwagger`, `CompleteRegistrationRequest` e `RegistrationCompletionService` dos fontes locais. Não há fallback automático de criação para evitar posts duplicados.

Não há endpoint específico de status de conclusão: a detecção usa a associação a imóveis conforme o serviço existente. A integração dos recursos autenticados está descrita abaixo. Organização continua opcional no cadastro. Os endpoints de organizações estão disponíveis na camada de API; não existe formulário de organização nas telas atuais.

## Verificações anteriores à conexão com o Render

- Compilação direta de todas as fontes Java e classes geradas, com o classpath real exportado pelo Gradle e Android SDK 36: passou.
- 11 testes JVM executados diretamente por JUnit: passaram (renovação concorrente, limite de repetição, rede, logout, isolamento de hosts, CEP atrasado/incompleto, catálogo e SHA-256).
- `processDebugResources`, geração de bindings, recursos de navegação e manifests pelo Gradle: passaram.
- Build/APK, testes pelo runner Gradle e lint completos: não confirmados. O Java do ambiente falha ao abrir a conexão local do worker (`Unable to establish loopback connection`, `UnixDomainSockets.connect`). Também foram tentadas execuções sem daemon e sem cache de configuração.
- Instrumentação e verificação visual/ponta a ponta: não executadas; `adb devices` não encontrou aparelho/emulador conectado.

## Arquivos e conexão

| Camada | Arquivos reais principais |
|---|---|
| UI | `view/fragments/SignupProfileFragment.java` até `SignupAddressFragment.java`, `view/fragments/login/LoginFragment.java`, `AuthFlowFragment.java`, `HomeFragment.java` |
| Estado | `viewmodel/SignupViewModel.java`, `viewmodel/LoginViewModel.java`, `model/ScreenState.java` |
| Dados | `repository/AuthRepository.java`, `repository/PostalCodeRepository.java`, `repository/SignupOptions.java`, `model/AuthModels.java` |
| Sessão/rede | `services/session/TokenStore.java`, `services/session/SessionStore.java`, `services/network/SessionInterceptor.java`, `RetrofitClient.java`, `NetworkConfig.java`, `services/api/AuthApiService.java` |
| Recursos | `res/layout/fragment_login.xml`, `fragment_signup_property.xml`, `fragment_home.xml`, `res/navigation/auth_navigation.xml`, `res/values/strings.xml`, regras de backup e `assets/registration_catalog.json` |

Os arquivos Java acima ficam sob `app/src/main/java/com/aula/delta_mobile_app/`. Os stubs antigos `LoginRepository`/`LoginDataSource`, que retornavam um usuário fictício, foram substituídos pelo único repository real. As alterações locais anteriores nas telas e câmera foram preservadas.

## Integração com o Render — 11/10/2026

O Swagger publicado em https://delta-teste-provi.onrender.com/v3/api-docs respondeu HTTP 200 e foi conferido contra os controllers locais. O contrato contém create-account, login, refresh, me e complete-registration. O módulo region não possui controller nem endpoint de catálogo publicado; o mapeamento de regiões continua no asset, com as limitações geográficas descritas acima.

ApiService e ApiModels disponibilizam os contratos de endereço, imóvel, organização, associação usuário/imóvel, hábitos, dispositivo, última conta, tarifas e todas as consultas de analytics, incluindo cálculo de custo. Datas são strings ISO-8601, números monetários usam BigDecimal. As chamadas compartilham a mesma sessão e trava de refresh da autenticação. Para obter o serviço autenticado, use RetrofitClient.getApiService(context); execute as chamadas fora da thread principal.

Na tela inicial, o usuário seleciona um dos imóveis retornados para sua conta e vê o consumo diário mais recente. Toque na mensagem de erro para tentar carregar novamente. O QR/manual envia o código textual a POST /delta/device com propertyId e isActive=true. Um código já cadastrado só é aceito se estiver ativo e associado ao imóvel escolhido. Não há transferência automática de medidor de outro imóvel. A rota /delta/internal/devices/validate é exclusiva da comunicação entre serviços e não recebe credenciais internas no app.

A tela da conta salva month no primeiro dia do mês, totalValue e m3Value para o usuário autenticado. Se já existir conta desse usuário/mês, usa PUT; caso contrário, POST. A navegação de sucesso depende da confirmação do servidor. WaterViewModel mantém os estados durante rotação e os limpa no encerramento da sessão. Latitude e longitude retornadas pela consulta de CEP agora seguem no complemento de cadastro.

Verificação desta atualização: compilação direta de todas as fontes Java com as dependências Android reais; 5 testes JVM da classe existente SessionInterceptorTest passaram; comparação das declarações Retrofit com o Swagger publicado. A geração de APK pelo Gradle permanece limitada pela falha ambiental "Unable to establish loopback connection", também observada com os JDKs 17, 21 e 25. Nenhuma conta, dispositivo ou fatura de teste foi criado na API de produção. O fluxo autenticado completo ainda precisa de verificação em aparelho com uma conta real.
