# delta-app-mobile
Repositório central de desenvolvimento do aplicativo móvel do delta.

Configuração e detalhes da integração: [Autenticação mobile](AUTH_MOBILE.md).

## Organização do app

- `services/api/ApiService`: métodos de autenticação, cadastro, recursos Delta e consulta de CEP. É uma interface porque o Retrofit gera a implementação.
- `services/network/RetrofitClient`: cria os clientes HTTP. O cliente Delta renova a sessão; o cliente BrasilAPI consulta CEP sem enviar tokens Delta.
- `repository`: repositórios, contratos de acesso e regras de cadastro.
- `model`: objetos enviados e recebidos pelas APIs, dados das telas e estados compartilhados.
- `services/session`: armazenamento de tokens e controle da sessão.
- `viewmodel`: estado e ações das telas.
- `view`: activities, fragments e componentes visuais.
- `res`: layouts, estilos, textos e imagens.

Para adicionar uma rota, declare o método em `ApiService` e use o serviço no repositório responsável. Os repositórios Delta obtêm a instância por `AuthRepository.getInstance(context).api()`; a consulta de CEP usa `RetrofitClient.getCepService()`.

Os XMLs usam indentação de quatro espaços e um atributo por linha em elementos com vários atributos. O Java mantém chamadas, argumentos e condições compactos, sem comentários no código.

Validação local: `gradlew.bat testDebugUnitTest assembleDebug`.
