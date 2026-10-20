# Auditoria do cadastro — 14/10/2026

| Item da lista | Implementação e evidência |
|---|---|
| Layout XML da tela de cadastro | `res/layout/fragment_signup_profile.xml` até `fragment_signup_address.xml`. |
| CadastroActivity | O cadastro usa `view/activities/login/LoginActivity.java` e Fragments, conforme a arquitetura existente. Não foi criada uma segunda Activity com a mesma função. |
| Campos obrigatórios | Nome, nascimento, e-mail, senha, confirmação, nome/tipo/classificação do imóvel e CEP com cidade, estado e região tarifária configurada. Telefone, área e hábitos são opcionais. |
| CadastroRequestDTO | `AuthModels.CreateAccount` para criação e `AuthModels.Completion` para complemento. Contrato JSON testado. |
| CadastroResponseDTO | `AuthModels.Tokens` para autenticação e `AuthModels.Completed` para confirmação final. |
| Endpoint de cadastro | `AuthApiService.create`: POST `delta/auth/create-account`; `complete`: POST `delta/auth/complete-registration`. Testes do contrato Retrofit. |
| Cadastro no AuthRepository | Criação grava sessão com segurança; complemento faz uma única chamada com endereço, imóvel e hábitos. |
| CadastroViewModel | `viewmodel/SignupViewModel.java`, com estados separados para criação, CEP e conclusão. |
| Integração Activity/ViewModel | Os Fragments usam `ViewModelProvider(requireActivity())` pelo helper `registration()`. |
| Validação dos campos | `SignupValidation`, executada pelo ViewModel, valida nome, nascimento estrito, idade mínima de 18 anos, telefone, credenciais, tipo/classificação e área. CEP e hábitos mantêm validação específica. |
| Estado de loading | Criação e conclusão exibem o diálogo existente e desabilitam campos/ações durante a chamada. |
| Erros de cadastro | Estados ERROR exibidos na etapa correspondente; tratamento de 400, 401, 403, 409, 429, falhas de rede e servidor. Novas tentativas preservam os dados e respeitam Retry-After. |
| Login ↔ cadastro | Login abre Welcome e as etapas de cadastro; Welcome/etapas iniciais permitem voltar ao login. Login de conta incompleta retoma o complemento. |
| Navegação após conclusão | Só ocorre após confirmação `completed=true`, abrindo `MainActivity` com Home e menu fixo. |
| Testar fluxo completo | Fluxo de criação/complemento validado em testes JVM com gateway controlado. Teste visual e integrado ao servidor no aparelho continua pendente. |

## Correções desta auditoria

- Loading da criação da conta, que antes só desabilitava os campos, agora também exibe o diálogo de loading.
- Senha composta apenas por espaços é rejeitada.
- Área opcional aceita espaços ao redor de números válidos, com a mesma normalização na validação e no payload.
- `SignupFlow`, efetivamente usado pelo ViewModel, permite verificar erros, repetição de chamadas, retomada, conclusão pendente e respostas atrasadas sem acesso à produção.

## Verificação executada

- Todas as fontes Java de produção compilaram diretamente com SDK Android 36 e dependências locais.
- 49 testes JVM passaram. Foram adicionados 20 testes de fluxo, validação e contrato do cadastro; os 29 anteriores também passaram.
- Não foram criadas contas de teste na produção nem alterada a sessão do aparelho conectado.
- O Gradle falhou na auditoria anterior com `Unable to establish loopback connection`, impedindo gerar e instalar o APK atualizado pelo fluxo normal. A compilação direta confirma o código Java, mas não substitui a execução das telas, o Android Keystore e o servidor no aparelho.

Testes novos: `SignupFlowTest`, `SignupValidationTest` e `RegistrationContractTest`, sob `app/src/test/java/com/aula/delta_mobile_app/`.
