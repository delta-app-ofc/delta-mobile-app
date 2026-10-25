# Auditoria do login — 14/10/2026

| Item da lista | Implementação e evidência |
|---|---|
| Layout XML da tela de login | `app/src/main/res/layout/fragment_login.xml` |
| Activity de login | `view/activities/login/LoginActivity.java` hospeda `LoginFragment`; mantém a arquitetura Activity + Fragments existente. |
| LoginRequestDTO | `model/AuthModels.Credentials`, com `email` e `password`. Teste do contrato JSON. |
| LoginResponseDTO | `model/AuthModels.Tokens`, com access/refresh token e seus prazos. Teste de desserialização. |
| Endpoint de login | `services/api/AuthApiService.login`: POST `delta/auth/login`. Teste da anotação Retrofit. |
| AuthRepository | Retrofit em executor, armazenamento de sessão, consulta de perfil e associação a imóvel. |
| LoginViewModel | `viewmodel/LoginViewModel.java`, com LiveData e entrega no thread principal. |
| Integração Activity/ViewModel | `LoginFragment` obtém o ViewModel com escopo de `LoginActivity`. |
| Validação dos campos | E-mail válido até 255 caracteres, senha preenchida até 256 caracteres. Sem envio ao servidor quando inválidos. |
| Loading | Estado LOADING observado pela tela, diálogo existente, campos e ações desabilitados. Toques repetidos bloqueados. |
| Credenciais inválidas | HTTP 401 mantém login com mensagem de erro; nova tentativa disponível. HTTP 503 tem mensagem distinta. |
| Persistência de sessão | Android Keystore + SharedPreferences criptografadas e excluídas do backup. A tela agora verifica automaticamente a sessão persistida ao abrir. |
| Navegação para cadastro | A ação de cadastro abre `WelcomeFragment`; conta autenticada sem imóvel segue para `SignupHabitsFragment`. |
| Navegação após autenticação | Perfil e associação são verificados antes de decidir: conta concluída abre `MainActivity`; conta incompleta abre o complemento. |
| Testar fluxo de login | 11 testes JVM do fluxo e 4 do contrato/erros adicionados. Testes reais no aparelho e login contra o servidor ainda pendentes. |

## Correções desta auditoria

- Retomada automática da sessão salva, com uma verificação por ViewModel para evitar repetição na rotação.
- Falha temporária na verificação permite tentar novamente sem digitar senha quando a sessão continua válida.
- Cancelamento invalida a sessão e ignora respostas atrasadas. O Repository já usa geração para impedir que um login cancelado grave tokens depois do logout.
- Senha removida da memória e do campo ao concluir autenticação ou cancelar.
- `AuthActions` permite testar `LoginFlow`, usado pelo ViewModel, com respostas controladas sem chamar produção.

## Validação executada

- Todas as fontes Java de produção compilaram diretamente com SDK Android 36 e as dependências locais.
- 29 testes JVM passaram, incluindo login, contrato, renovação de sessão, consumo, CEP e catálogo.
- Runner Gradle bloqueado por `Unable to establish loopback connection`, antes da compilação.
- ADB identificou um aparelho conectado. O APK com estas mudanças não foi instalado nem testado nele; não foram usados tokens fictícios na sessão do usuário nem feitas chamadas de login com credenciais reais.

Os testes JVM usam um gateway controlado para testar as transições do fluxo. Eles não comprovam a resposta do servidor, a criptografia no dispositivo ou a navegação visual. Há testes instrumentados de persistência e loading no projeto, mas não foram executados nesta auditoria.

## Restauração de sessão sem exibir o login — 17/10/2026

- [x] Iniciar a verificação de sessão salva antes de permitir renderizar o formulário.
- [x] Ocultar o formulário durante a renovação e verificação do cadastro.
- [x] Manter loading e formulário oculto na decisão de abrir a Home, inclusive durante o estado IDLE emitido por consume().
- [x] Permitir a exibição do formulário após falha ou cancelamento da restauração.
- [x] Impedir cancelamento da sessão quando a transição autenticada já foi iniciada.
- [x] Executar os 16 testes JVM do fluxo de login, incluindo restauração sem estado IDLE entre renovação e roteamento: passaram.
- [x] Adicionar asserção instrumentada de formulário oculto durante loading e visível após erro.
- [ ] Executar os testes instrumentados e conferir Splash → loading → Home no aparelho; ambiente Gradle segue com limitação de loopback.

## Loading único na entrada automática — 17/10/2026

- [x] Splash envia a sessão salva diretamente à MainActivity, sem passar pela LoginActivity/LoginFragment.
- [x] Remover o carregamento automático dos grafos nos layouts; cada Activity define o destino quando a decisão está pronta.
- [x] Usar a mesma instância de LoadingView para renovar sessão, verificar cadastro e aguardar os dados da Home.
- [x] Manter o loader visível enquanto qualquer fase estiver pendente e revelar a Home somente ao concluir o conjunto.
- [x] Preservar LoginViewModel durante rotação para não repetir a renovação de sessão.
- [x] Encaminhar sessão ausente/expirada ou falha ao login com a restauração automática desativada para aquela entrada, evitando repetição de loadings.
- [x] Encaminhar cadastro incompleto diretamente à etapa de complemento, sem abrir o login.
- [ ] Conferir o fluxo e a continuidade da animação em aparelho, incluindo sessão válida, expirada, falha de rede, rotação e cadastro incompleto.
