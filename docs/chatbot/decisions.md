# Decisões do fluxo do chatbot

## Objetivo e fontes

Registrar as decisões de produto e integração antes de escrever código. Uma decisão de comportamento não significa que a API já consiga atendê-la.

- **Fonte das decisões:** respostas da pessoa usuária aos itens E.1–E.10 nesta conversa, em 25/10/2026.
- **Fonte técnica:** [contrato da API](api-contract.md), baseado no commit remoto `af5d6c1dafb10abd6897180e3e270402693c2a2b` do chatbot.
- **Referências de produto:** [briefing](briefing.md) e [imagens do design](design/).
- As propostas ainda não aceitas são identificadas como **PENDENTE**. Deduções técnicas são marcadas como **INCERTO**.
- Nesta etapa, a autorização é documentar decisões. A implementação ainda não foi autorizada.
- **Atualização na execução da etapa 0, em 25/10/2026:** a pessoa usuária revisou D08 e D09 nas respostas registradas abaixo. As versões anteriores foram preservadas como histórico, sem continuar valendo como requisito. A etapa 0 documental foi inicialmente preparada sem commits para revisão; depois, a pessoa usuária autorizou os dois commits propostos. A autorização não abrange código Android/backend nem etapas posteriores.

## Decisões registradas — 25/10/2026

### D01 — URL e identidade da conta

**Decidido:** usar `https://delta-artificial-intelligence.onrender.com` como endereço do serviço de IA. O `user_id` corresponde ao ID retornado por `/delta/auth/me` da API principal.

**Verificado na etapa 0:** `GET /health` e `GET /openapi.json` responderam HTTP 200 sem credenciais. O OpenAPI publicado é estruturalmente igual ao arquivo local e declara `Delta AI`, versão `0.1.0`. Fontes e horários: [registro da verificação](api-contract.md#verificação-do-serviço-publicado--etapa-0).

**Ainda pendente:** revisão Git publicada e autenticação/gateway das operações de chat/sessões. A pessoa usuária havia respondido “não sei” sobre esses pontos; os dois GETs não identificam o commit nem comprovam ausência de proteção nas outras operações.

Fonte: resposta E.1. A ausência de autenticação HTTP no código analisado está descrita no [contrato](api-contract.md#ambiente-transporte-e-autenticação); não comprova a configuração externa do Render.

### D02 — Escopo dos dados

**Decidido:** o chatbot considera a conta toda, sem restringir a conversa ao imóvel selecionado no app.

Fonte: resposta E.2. O request atual não recebe `property_id`, conforme o [contrato](api-contract.md#post-chat--enviar-mensagem).

### D03 — Sugestões iniciais

**PENDENTE:** a pessoa usuária ainda não definiu o conteúdo dos cartões iniciais, se serão fixos ou personalizados, nem se o toque envia a pergunta ou apenas preenche o campo.

Fonte: resposta E.3. Referência visual: [tela inicial](design/telainicialchatbot.png).

### D04 — Apresentação do histórico

**Decidido:** usar os ícones predefinidos no Figma e uma apresentação alternativa compatível com os dados disponíveis. Desconsiderar o acesso “Ver todos” nesta entrega.

**Pendente:** definir a regra dos rótulos e da associação entre ícones e conversas. Não foi decidido gerar títulos, buscar todas as mensagens para extrair prévias ou criar metadados locais.

Fonte: resposta E.4. A listagem atual retorna somente `session_id` e `created_at`, conforme o [contrato](api-contract.md#get-sessionsuser_id--listar-conversas).

### D05 — Entrada e nova conversa

**Decidido:** ao entrar no chatbot, apresentar sempre a tela inicial. Acrescentar um botão de iniciar novo chat, mesmo sem essa ação nas referências do Figma. Ao acioná-lo, iniciar uma nova conversa e encerrar a anterior.

**Pendente:** posição e rótulo final do botão; comportamento se o encerramento falhar; comportamento ao iniciar outro chat enquanto uma resposta está sendo processada; definição de como acessar a conversa corrente a partir da apresentação inicial.

**Limite técnico:** o endpoint `close` gera memória da conversa, mas não marca a sessão como fechada nem impede novos envios. A decisão de encerrar a conversa no fluxo do app não altera essa semântica do servidor. Ver [contrato](api-contract.md#post-sessionssession_idclose--gerar-memória-da-conversa).

Fonte: resposta E.5.

### D06 — Gráficos

**Decidido:** gráficos ficam fora da entrega atual. A exibição e a recuperação dos gráficos ao reabrir conversas não serão requisitos desta etapa.

**Pendente:** a resposta não define como renderizar Markdown, links ou as referências textuais produzidas pelo RAG.

Fonte: resposta E.6. O formato e a persistência de `chart_data` permanecem documentados no [contrato](api-contract.md#formato-real-de-chart_data).

### D07 — Backend e continuidade da conversa

**Decidido:** o backend não será ajustado por enquanto.

**Limite técnico conhecido:** na versão analisada, o histórico chega ao roteador, mas os especialistas recebem somente a pergunta atual. **INCERTO — consequência inferida:** pedidos dependentes da resposta anterior, como “Quero uma versão mais resumida”, podem não funcionar corretamente. Ver [contrato](api-contract.md#continuidade-de-conversa).

**Pendente:** definir como esse limite afeta o aceite e a apresentação do fluxo. A decisão de manter o backend não autoriza simular continuidade nem prometer o comportamento da referência.

Fonte: resposta E.7.

### D08 — Orientação sem salvar hábitos

**Decisão anterior — substituída em 25/10/2026:** o chatbot apenas orientaria, sem salvar hábitos. O título desta seção foi mantido para preservar as referências existentes; o requisito vigente está no próximo parágrafo.

**Decidido agora:** o chat pode registrar dados pelas ferramentas que o backend disponibiliza. Não exigir modo somente leitura nem bloquear a integração pelo simples fato de existir ferramenta de escrita. A pessoa usuária autorizou essa mudança de requisito ao responder à pendência P01 na execução da etapa 0.

Fonte da revisão: resposta nesta conversa em 25/10/2026 — “pode deixar o chat registrar, se o backend permite nao tme problema, pode deixar escrever mesmo”.

**P01 resolvida quanto ao requisito de escrita:** a proposta anterior de bloquear ferramentas no backend antes de habilitar o envio real foi descartada. D07 continua valendo: não ajustar o backend por enquanto. Permitir gravações no uso futuro do chat não autoriza executar POSTs nesta etapa documental nem acrescentar telas/operações além do plano.

**Limite técnico conhecido:** a revisão analisada disponibiliza `create_habit`, mas não há parâmetro de modo somente leitura no request. **INCERTO:** as ferramentas efetivamente presentes no deploy continuam sem confirmação, pois o OpenAPI público não as descreve. Ver [contrato](api-contract.md#consumo-metas-clima-e-hábitos) e [schemas da API][schemas].

**Pendente adicional:** a resposta não define se o acompanhamento de metas inclui criação/edição ou somente consulta da meta existente.

Fonte histórica: resposta E.8, em conjunto com E.7; a revisão acima prevalece sobre a restrição original.

### D09 — Dados climáticos

**Decisão anterior — adiada para entrega posterior:** considerar condições climáticas atuais e previsões futuras, além do histórico.

**Decidido agora:** aceitar histórico climático na primeira entrega; deixar condições atuais e previsão futura para depois. Fonte: resposta à P02 nesta conversa em 25/10/2026 — “Aceitar histórico climático na primeira entrega; deixar clima atual/futuro para depois”.

**P02 resolvida quanto ao escopo da primeira entrega:** ela não depende da inclusão de clima atual/futuro no backend. Isso não comprova funcionamento ou disponibilidade dos dados históricos: o aceite ainda exige verificar a resposta com uma fonte histórica real. A versão analisada consulta histórico meteorológico e informa que não é previsão. Ver [contrato](api-contract.md#consumo-metas-clima-e-hábitos).

**Ainda pendente:** evidência de origem, localização, período e disponibilidade do histórico usado pelo serviço, além da apresentação da ausência de dados. Não tratar histórico como condição atual nem previsão. Não foi autorizada uma integração climática nova no mobile.

Fonte histórica: resposta E.9; a revisão acima substitui a obrigatoriedade de clima atual/futuro na primeira entrega.

### D10 — Requisição em andamento, erros e teclado

**Decidido:** sair da tela não deve cancelar a chamada em andamento. Incluir uma apresentação de erro que ainda não existe nas referências. Manter o menu oculto enquanto o teclado estiver aberto.

**Pendente:** permitir ou bloquear envios simultâneos; apresentação e texto do erro; ação de nova tentativa; tratamento da resposta recebida enquanto a tela estiver fora de exibição. O comportamento da chamada ao trocar de chat também depende de D05.

**Limite técnico conhecido:** a API não tem deduplicação de envios. **INCERTO — risco inferido:** repetir uma chamada após timeout pode duplicar operações. Ver [contrato](api-contract.md#post-chat--enviar-mensagem).

Fonte: resposta E.10. “Não tem que fechar a chamada” foi registrado como manutenção da requisição ao sair da tela; essa frase não foi tratada como resposta sobre envios simultâneos.

## Ordem para resolver as pendências

Esta ordem organiza a conversa; não representa decisões já aceitas sobre as soluções.

1. **Ambiente publicado — P03:** o contrato HTTP foi comparado, mas faltam revisão em execução e confirmação da autenticação/gateway nas operações de chat/sessões. Encaminhar a verificação ao responsável pelo ambiente; não repetir à pessoa usuária perguntas às quais ela já respondeu “não sei”.
2. **Ciclo da conversa — P05:** botão de novo chat, retomada, falha de encerramento e requisições em andamento.
3. **Envio e falhas — P06:** concorrência, mensagem de erro, nova tentativa e entrega de respostas fora da tela.
4. **Continuidade e metas — P07/P08:** critérios de aceite diante do contexto limitado e operações permitidas para metas.
5. **Apresentação — P04/P08:** sugestões iniciais, histórico alternativo e formatação do texto.
6. **Aceite climático:** verificar o uso de histórico real na integração futura; clima atual/futuro fica para depois, conforme a revisão de D09.

Vamos resolver uma pendência por vez e atualizar este arquivo a cada decisão.

## Registro de execução da etapa 0 — 25/10/2026

- **Grupo previsto 0.1:** verificação pública realizada e registrada em `api-contract.md`; sem alteração do snapshot `openapi.json`.
- **Grupo previsto 0.2:** P01 e P02 respondidas pela pessoa usuária; D08/D09 revisadas acima, com o histórico preservado. Somente as dependências afetadas foram ajustadas em `plan.md`.
- **Situação:** parte verificável da etapa 0 executada; a etapa não está integralmente encerrada enquanto P03 depender da confirmação externa descrita acima. P04–P08 continuam abertas para as etapas correspondentes.
- Nenhuma chamada a `/chat`, sessões ou `close` e nenhum código Android/backend alterado. Os grupos 0.1/0.2 foram apresentados sem commits para revisão; em seguida, a pessoa usuária autorizou registrá-los nos dois commits previstos. Isso não encerra P03 nem autoriza iniciar a etapa 1.
- Os resumos anteriores de D08/D09 em outras referências, inclusive no AGENTS.md da feature, descrevem o requisito antigo. Estas respostas explícitas e as decisões atualizadas prevalecem; esses outros arquivos não foram alterados por estarem fora do escopo da etapa 0.

[schemas]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/schemas.py
