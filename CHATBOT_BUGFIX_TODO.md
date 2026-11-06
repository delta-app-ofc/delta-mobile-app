# TO-DO — Bug-fix de conexão e contrato do chatbot

## Estado da confirmação

- [x] Confirmar o significado da mensagem exibida: `ChatbotErrors` converte respostas HTTP 401 e 403 no texto “Não foi possível autorizar o acesso ao assistente”.
- [x] Consultar o host `https://delta-artificial-intelligence-mvo1.onrender.com/openapi.json`: responde HTTP 200 e corresponde ao OpenAPI anexado nesta solicitação.
- [x] Identificar que `https://delta-artificial-intelligence.onrender.com` publica outro contrato legado; não é o host solicitado para este app.
- [x] Confirmar no OpenAPI `mvo1` a autenticação Bearer e as rotas `/chat`, `/sessions`, `/sessions/{session_id}/messages` e `/sessions/{session_id}/close`.
- [x] Comparar os DTOs: `ChatRequest` aceita `message` e `session_id`; `ChatResponse` acrescenta `suggestions` e `pending_confirmation`; sessões/histórico/fechamento usam o modelo já suportado pelo app.

## Correção aplicada

- [x] Configurar o host `https://delta-artificial-intelligence-mvo1.onrender.com/`.
- [x] Alinhar as rotas e remover o `user_id` explícito; o backend deriva a identidade do Bearer JWT.
- [x] Reativar o access token Delta no header Bearer do chatbot e renovação via API Delta, com no máximo uma repetição após 401.
- [x] Mapear `suggestions` e `pending_confirmation` no DTO `ChatResponse`.

## Verificação

- [x] Atualizar testes de URL e contrato do host `mvo1` e adicionar teste do access token Bearer.
- [x] Executar `:app:testDebugUnitTest` depois do alinhamento com mvo1 — passou.
- [x] Executar `:app:assembleDebug` depois do alinhamento com mvo1 — passou; APK atualizado em `app/build/outputs/apk/debug/app-debug.apk`.
- [ ] Validar conversa real e histórico numa conta de teste no app instalado.

## Referências no checkout

- `app/src/main/java/com/aula/delta_mobile_app/viewmodel/ChatViewModel.java`
- `app/src/main/java/com/aula/delta_mobile_app/services/network/RetrofitClient.java`
- `app/src/main/java/com/aula/delta_mobile_app/services/network/ChatbotAuthInterceptor.java`
- `app/src/main/java/com/aula/delta_mobile_app/services/network/SessionInterceptor.java`
- `app/src/main/java/com/aula/delta_mobile_app/model/ChatModels.java`
- `app/src/test/java/com/aula/delta_mobile_app/services/network/ChatbotNetworkTest.java`
