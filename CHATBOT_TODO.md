# Gluh Helper — especificação da tela de conversa

## Objetivo e escopo

Tela de chat implementada conforme as três imagens fornecidas, incluindo campo de composição, mensagens e indicador de resposta em andamento. Este documento reúne a especificação e o registro da implementação; os itens de validação visual ainda dependem de aparelho ou emulador.

Respeitar a arquitetura, recursos, sessão, navegação e comportamentos já existentes. Não refazer outras telas, o menu ou a identidade visual global para reproduzir as referências.

## Referências visuais

- `chatbot-susgestao001.png`: composição com texto digitado, teclado aberto e menu inferior oculto.
- `chatbot-sugestao002.png`: mensagem enviada à direita, indicador de três pontos à esquerda e campo vazio acima do menu.
- `chatbot-sugestao003.png`: mensagem do usuário à direita e resposta do assistente em caixa clara à esquerda.

As imagens são referências de aparência. O teclado e sua barra de sugestões pertencem ao sistema; não devem ser desenhados no app. Os exemplos de rotina são conteúdo ilustrativo, não respostas para fixar em produção. As medidas abaixo são aproximações visuais, a validar no Android, e não valores extraídos de um arquivo de design.

## Base existente a preservar

| Item | Situação verificada e orientação |
| --- | --- |
| Plataforma | Android nativo, Java 11, layouts XML e ViewBinding habilitado. Usar as dependências atuais; não migrar para Compose ou outra plataforma. |
| Destino | `app/src/main/res/navigation/main_navigation.xml`: `tipsFragment`, com título Gluh Helper e argumento `screen=chat`. Preservar o ID para manter os acessos existentes. |
| Tela atual | `view/fragments/MainSectionFragment.java` apresenta indisponibilidade e ação “Organizar minha rotina”. Isolar o novo chat sem alterar Consumo, Custos, Rotina e demais usos desse fragment. |
| Menu | `view/activities/main/MainActivity.java`, `res/layout/activity_main.xml` e `view/FloatingMenuBackground.java` já controlam o menu compartilhado. Reutilizar os ícones, seleção e destinos existentes. |
| Teclado e insets | `MainActivity` já aplica insets das barras/IME em `main_shell` e oculta o menu quando o teclado abre. Não aplicar novamente o mesmo inset no fragment. |
| Estilos | Reutilizar a tipografia sans-serif de `Home.Body` e os recursos aplicáveis. `home_background` é atualmente `#222222`; `delta_water` é `#306BE5`. Não alterar esses tokens globalmente para aproximar uma imagem. |
| Rede e sessão | Seguir `ApiService`, repositórios, ViewModels, LiveData e autenticação existentes. Não criar cliente paralelo nem inserir credenciais no app. |
| Disponibilidade | Contrato Delta AI fornecido pelo usuário e conferido em `/openapi.json`. A integração usa `ChatApiService` no domínio próprio, separada da API principal. A indisponibilidade registrada em `MOBILE_V2.md` corresponde à etapa anterior. |

Os caminhos Java da tabela são relativos a `app/src/main/java/com/aula/delta_mobile_app/`.

## Composição da tela

### Fundo e título

- Fundo escuro contínuo, com halos azuis suaves no alto à direita e na região inferior esquerda, como nas referências. Criar recurso local do chat para esses efeitos; manter o fundo central escuro e o texto legível.
- Título branco alinhado à esquerda: “Monitore cada gota com mais precisão.”, em aproximadamente 26sp, peso regular. Usar margem horizontal de 20–26dp e espaçamento superior proporcional à área útil, já descontadas as barras do sistema.
- Na largura de referência, o título ocupa duas linhas. Permitir quebra natural em telas menores e com fonte ampliada, sem altura fixa ou corte.
- Não acrescentar outro cabeçalho visível “Gluh Helper” acima do título; manter esse nome como identificação do destino.

### Histórico e posicionamento

- Usar área rolável entre o título e o campo de composição. Com poucas mensagens, alinhar o conteúdo ao fundo da área disponível, preservando o espaço vazio visto nas referências.
- O campo fica ancorado no rodapé da área de conteúdo, separado do menu compartilhado. Reservar a altura real ocupada pelo menu e suas margens; o XML atual usa 80dp de altura e 24dp de margem inferior.
- Com teclado aberto, o menu desaparece pelo comportamento existente e o campo acompanha a área útil acima do teclado, sem salto para fora da tela ou dupla margem de IME.
- Manter a última mensagem visível ao enviar. Ao receber uma resposta, rolar automaticamente apenas se a pessoa já estiver perto do fim; preservar a leitura de mensagens anteriores.
- Não fixar coordenadas com base nas imagens de 393 × 852 px. Usar dp, sp e restrições adaptáveis, inclusive em paisagem e nas configurações de largura já presentes no projeto.

## Campo de composição e botão de envio

- Contêiner escuro/transparente com borda branca fina de aproximadamente 1dp, cantos de 12–14dp e margens laterais de 20–26dp.
- Altura mínima aproximada de 44–48dp, com expansão para até quatro linhas e rolagem interna após esse limite. Reservar espaço à direita para o botão, sem sobrepor o texto.
- Texto digitado branco, aproximadamente 16–18sp. Placeholder: “Pergunte algo ao gluh..”, conforme referência; guardar o texto em recurso de string. Usar contraste legível, sem reproduzir o cinza pouco visível da imagem.
- Botão azul arredondado com ícone de envio claro, alinhado à direita. A arte pode ter cerca de 34dp, mas a área tocável deve alcançar ao menos 48 × 48dp.
- Desabilitar envio para conteúdo vazio ou formado apenas por espaços, durante uma solicitação e quando o serviço estiver indisponível. Representar o estado desabilitado visualmente e para acessibilidade.
- Envio pelo botão e pela ação de enviar do teclado deve chamar a mesma ação do ViewModel e impedir duplicidade. Manter suporte a texto multilinha.
- Após aceitar o envio, inserir a mensagem, limpar o campo e fechar o teclado para chegar ao estado da segunda referência. Se houver falha, preservar o conteúdo enviado e permitir tentar novamente sem duplicar o balão.

## Formato das caixas de mensagem

| Propriedade | Usuário | Assistente |
| --- | --- | --- |
| Alinhamento | Direita | Esquerda |
| Largura | Ajustada ao conteúdo, limitada a aproximadamente 85% da largura útil | Ajustada ao conteúdo, até aproximadamente 95% da largura útil |
| Fundo | Gradiente horizontal do azul principal para azul mais claro | Azul muito claro, próximo de `#E8EEFF` |
| Texto | Branco, aproximadamente 16sp | Azul principal, aproximadamente 16sp |
| Cantos | Arredondados, aproximadamente 16dp | Arredondados, aproximadamente 16dp |
| Cauda | Pequena extensão no canto inferior direito | Pequena extensão no canto inferior esquerdo |
| Espaçamento | Padding horizontal de 14–20dp e vertical de 10–14dp | Padding de 14dp, com espaço para parágrafos e listas |

- As caudas devem fazer parte do fundo visual, sem cobrir mensagens vizinhas ou reduzir a área legível.
- Permitir mensagens longas, acentos, emojis e quebras de linha; não usar altura fixa nem reticências para cortar a resposta.
- A resposta da terceira referência contém um título e três sugestões com emojis. Reproduzir a estrutura visual quando houver conteúdo equivalente recebido, sem hardcode dessas sugestões.
- Renderizar texto de forma segura. Não adicionar WebView ou interpretação arbitrária de HTML para reproduzir os balões. Markdown rico só deve entrar se o contrato do produto exigir.

## Estado de carregamento da resposta

O carregamento solicitado é o indicador de digitação da segunda imagem, dentro da conversa; não é uma tela de splash nem um bloqueio global.

- Exibir três pontos pequenos à esquerda, logo após a mensagem enviada e antes do campo, com alternância suave de opacidade ou escala. Um ponto mais claro e os demais discretos reproduzem a referência.
- Usar ciclo curto e contínuo, aproximadamente 900–1200ms. Respeitar animações desativadas, mantendo indicador estático nesse caso.
- Expor “Gluh está respondendo” como descrição acessível, sem anunciar cada quadro da animação.
- Manter histórico, rolagem e navegação disponíveis. Desabilitar somente o envio duplicado; permitir edição do próximo rascunho.
- Remover o indicador em sucesso, falha, cancelamento ou indisponibilidade. Não mantê-lo indefinidamente em erros de rede.
- Parar a animação ao destruir a view e reconstruir sua apresentação a partir do estado do ViewModel ao retornar.
- Não reutilizar o overlay global de carregamento da Home para uma resposta do chat.

## Estados e transições

| Estado | Apresentação e comportamento |
| --- | --- |
| Indisponível | Informar que o Gluh Helper ainda não está conectado; envio desabilitado e ação existente para organizar a rotina disponível. Não exibir digitação fictícia. |
| Inicial disponível | Título, área de conversa vazia, campo com placeholder e menu atual. |
| Digitando | Rascunho no campo, teclado nativo aberto e menu oculto pelo shell. |
| Aguardando resposta | Balão do usuário à direita, três pontos à esquerda, campo limpo e envio bloqueado. |
| Resposta recebida | Trocar os pontos pelo balão do assistente; habilitar novo envio quando houver texto válido. |
| Falha | Retirar os pontos, manter a mensagem e informar a falha com ação de tentar novamente. Não criar uma resposta do assistente como se tivesse sido recebida. |

Não apagar histórico nem rascunho por recriação da view. Manter o estado no ViewModel e usar SavedStateHandle para o rascunho se necessário. Persistência de histórico após encerramento do processo depende de definição própria; não presumir armazenamento permanente. Navegar para outra aba não deve provocar reenvio automático. Descartar respostas de sessão/conversa que já não seja a ativa e limpar dados ao encerrar a sessão.

## Plano de implementação compatível

1. Criar `GluhChatFragment` e `fragment_gluh_chat.xml`; apontar somente o destino `tipsFragment` para o fragment dedicado e preservar a compatibilidade dos argumentos existentes. Seguir o tratamento de sessão e cor das telas atuais, incluindo `AuthFlowFragment` quando aplicável.
2. Criar recursos locais como `chat_strings.xml`, `chat_styles.xml` e fundos/ícone em `drawable`. Reutilizar tokens existentes antes de acrescentar cores ou dimensões específicas. Não adicionar fonte ou biblioteca sem necessidade.
3. Criar modelo de mensagem com ID estável, papel, conteúdo e estado de envio; criar `GluhChatViewModel` para histórico, rascunho, disponibilidade e solicitação em andamento. Seguir os padrões de estado do projeto sem alterar o comportamento global de `ScreenState` para atender apenas ao chat.
4. Separar renderização dos balões e indicador da lógica de rede. Usar as bibliotecas já instaladas; a escolha da lista deve permitir rolagem e atualização incremental sem reconstruir todo o histórico a cada quadro da animação.
5. Implementado `GluhChatRepository` com `ChatApiService` e cliente no `RetrofitClient`. O contrato confirmado usa outro domínio e não declara autenticação por token: resolver o usuário por `AuthRepository` e não encaminhar tokens Delta ao serviço de IA. Confirmar identidade antes do envio e descartar respostas de sessões invalidadas.
6. Manter indisponibilidade explícita quando a conta não puder ser resolvida e falha recuperável quando o serviço não responder. O aplicativo usa respostas reais; fixtures ficam apenas nos testes.

Seguir o README: XML com quatro espaços e um atributo por linha em elementos com vários atributos; Java com chamadas, argumentos e condições compactos e sem comentários no código.

## Critérios de aceite e verificação

- [ ] Título, fundo, alinhamento, gradiente, cantos e caudas conferidos com as três referências em aparelho/emulador Android.
- [ ] Nenhum menu duplicado; Gluh Helper mantém seleção correta e os cinco destinos continuam funcionando.
- [ ] Teclado nativo abre sem encobrir o campo; menu some e retorna corretamente, inclusive ao usar Voltar.
- [ ] Campo, histórico e menu não se sobrepõem com teclado fechado, aberto, navegação por gestos ou barras do sistema.
- [ ] Fonte ampliada, mensagens longas, várias linhas e emojis não são cortados; botão de envio tem alvo acessível e descrição “Enviar mensagem”.
- [ ] Três pontos aparecem somente durante solicitação real e terminam em sucesso ou falha; não há overlay de Home nem anúncio acessível repetitivo.
- [ ] Toques repetidos e ação do teclado não duplicam solicitações; repetição após erro não duplica mensagem.
- [ ] Rotação/recriação da view preserva rascunho e conversa em memória; callbacks não acessam views destruídas.
- [ ] Ausência de backend é explícita, sem resposta simulada em produção; a ação de Rotina permanece acessível nesse estado.
- [x] Testes de modelo cobrem envio vazio, limite Unicode, solicitação única, sucesso, falha/repetição e descarte de resposta antiga após limpeza da conversa; testes JSON verificam os nomes de campos do contrato.
- [ ] Executar `gradlew.bat testDebugUnitTest assembleDebug` e, quando aplicável, `gradlew.bat lintDebug`. Registrar bloqueios reais do ambiente sem declarar validação que não foi concluída.

## Integração entregue — 25/10/2026

- Serviço: `https://delta-artificial-intelligence.onrender.com/`; `GET /health` respondeu `{"status":"ok"}` e o contrato publicado foi conferido com o anexo.
- Envio: `POST /chat`, JSON com `user_id` inteiro, `message` de 1 a 2.000 caracteres Unicode e `session_id` opcional. A primeira mensagem omite a sessão; as seguintes usam o identificador retornado.
- Resposta: `response`, `agent_used`, `session_id` e `chart_data` opcional. O texto é exibido como texto nativo, com quebras e emojis. O campo de gráfico é decodificado como objeto livre; renderização de gráficos não faz parte desta tela.
- `GluhChatFragment` substitui somente a classe do destino `tipsFragment`; argumentos e navegação compartilhada permanecem. `ChatBackdropView`, `ChatBubbleDrawable` e `ChatTypingView` cuidam do fundo, balões e animação.
- `GluhChatViewModel` mantém conversa em memória e rascunho em `SavedStateHandle`. Ao sair da conta ou mudar a geração da sessão, cancela o envio e limpa os dados. Recriar a view não reenvia mensagens.
- A autenticação/perfil usa o cliente Delta existente. A solicitação de IA roda em executor próprio para não ocupar a fila de operações do restante do app. O cliente de IA usa HTTPS, sem tokens Delta, sem redirects e sem repetição automática de POST; timeout de leitura de 90s e timeout total da chamada de 120s.
- Falhas de validação, limite de requisições, indisponibilidade, resposta vazia e timeout encerram os três pontos e oferecem repetição manual. A repetição mantém o mesmo balão local; o contrato não oferece chave de idempotência para garantir deduplicação no servidor após falha de conexão.
- Os endpoints de listagem, histórico e fechamento de sessões não são necessários para este fluxo e não foram chamados. Seus retornos não têm esquema definido no contrato fornecido; não foram inventados modelos de histórico remoto.

### Verificação executada

- Recursos atuais compilados e ligados com AAPT2, com recursos completos das dependências AndroidX/Material locais.
- Todas as fontes Java do app compiladas diretamente com `javac`; 9 testes JVM de conversa e contrato JSON passaram.
- `gradlew.bat testDebugUnitTest assembleDebug lintDebug` foi tentado, mas o Gradle falhou com `Unable to establish loopback connection` neste ambiente. A compilação direta não substitui build, lint e instalação completos.
- Nenhum aparelho/emulador estava conectado. Validação visual, teclado, rotação e envio autenticado de ponta a ponta permanecem pendentes; nenhum ID de usuário fictício foi enviado à API para testar uma conversa.

### Investigação de falha HTTP e acesso a custos

- Por orientação posterior do usuário, o botão `$` abre exclusivamente `costsFragment` (`DashboardFragment`), correspondente à referência `dash (1).png`. A rota `costsHistoryFragment` e a renderização da antiga tela de custos foram removidas. O cartão de custo previsto abre o formulário existente da conta de água.
- A captura enviada pelo usuário mostra o erro que antes correspondia a qualquer HTTP diferente de sucesso (exceto 422/429). Isso confirma a rejeição HTTP, mas não distingue 400, 403, 500 ou outro código. Não confirma, isoladamente, indisponibilidade do provedor de IA.
- O aplicativo passa a diferenciar autorização, validação, rota, limite de requisições e erros do servidor, mostrar o código HTTP e preservar detalhes textuais curtos de erros de cliente quando apropriado. Não exibe corpos de erros 5xx nem registra mensagens, tokens ou dados de conta. O Logcat registra apenas `GluhChat: POST /chat returned HTTP <código>`.
- Nesta investigação, `/health` respondeu 200; `POST /chat` com JSON vazio respondeu 422, sem criar conversa. Esses testes não comprovam geração de respostas.
- O código público `main` de `delta-app-ofc/delta-artificial-intelligence` declara `agents_used` como lista, enquanto o OpenAPI publicado no serviço declara `agent_used` como string. O modelo mobile aceita os dois campos, sem depender deles para exibir `response`. A diferença indica revisões distintas e não foi tratada como causa comprovada da falha.
- Um aparelho foi detectado nesta investigação; o Logcat consultado não continha a causa HTTP. O Gradle continuou falhando por loopback, impedindo gerar e instalar um APK atualizado por esse fluxo. O erro real do servidor ainda depende do código HTTP da nova mensagem ou dos logs do Render referentes ao envio.

