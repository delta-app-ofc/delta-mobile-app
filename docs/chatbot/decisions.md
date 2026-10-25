# Decisões do fluxo do chatbot

## Objetivo e fontes

Registrar as decisões de produto e integração antes de escrever código. Uma decisão de comportamento não significa que a API já consiga atendê-la.

- **Fonte das decisões:** respostas da pessoa usuária aos itens E.1–E.10 nesta conversa, em 25/10/2026.
- **Fonte técnica:** [contrato da API](api-contract.md), baseado no commit remoto `af5d6c1dafb10abd6897180e3e270402693c2a2b` do chatbot.
- **Referências de produto:** [briefing](briefing.md) e [imagens do design](design/).
- As propostas ainda não aceitas são identificadas como **PENDENTE**. Deduções técnicas são marcadas como **INCERTO**.
- Nesta etapa, a autorização é documentar decisões. A implementação ainda não foi autorizada.

## Decisões registradas — 25/10/2026

### D01 — URL e identidade da conta

**Decidido:** usar `https://delta-artificial-intelligence.onrender.com` como endereço do serviço de IA. O `user_id` corresponde ao ID retornado por `/delta/auth/me` da API principal.

**Pendente de verificação:** a pessoa usuária não sabe qual revisão está publicada nem se existe gateway/autenticação externa. Essas respostas não equivalem a confirmar ausência de autenticação no ambiente publicado.

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

**Decidido:** o chatbot apenas orienta; não deve salvar hábitos.

**Bloqueio técnico a resolver:** na versão analisada, `/chat` disponibiliza ao agente a ferramenta `create_habit`, que executa escrita no banco. Não existe no request um parâmetro de modo somente leitura. A interface mobile, sozinha, não remove essa ferramenta do servidor. Ver [contrato](api-contract.md#consumo-metas-clima-e-hábitos) e [schemas da API][schemas].

**INCERTO:** a revisão publicada pode diferir da analisada; a capacidade de escrita no ambiente de integração ainda precisa ser verificada.

**PENDENTE — proposta ainda não aceita:** exigir que o serviço bloqueie a escrita antes de habilitar o envio real pelo app, preservando o requisito de somente orientação. Enquanto o backend permanecer sem ajuste, o fluxo visual e a preparação do mobile podem ser planejados, mas o envio real ficaria pendente desse requisito. Esta proposta não autoriza editar o backend nem implementar o mobile agora.

**Pendente adicional:** a resposta não define se o acompanhamento de metas inclui criação/edição ou somente consulta da meta existente.

Fonte: resposta E.8, em conjunto com E.7.

### D09 — Dados climáticos

**Decidido:** o chatbot deve considerar condições climáticas atuais e previsões futuras, além do histórico.

**Bloqueio técnico a resolver:** as ferramentas analisadas consultam histórico meteorológico e explicitam que ele não representa previsão futura. O atendimento ao requisito exige confirmar se o serviço publicado já oferece essas fontes ou definir como serão disponibilizadas. Ver [contrato](api-contract.md#consumo-metas-clima-e-hábitos).

**Pendente:** fonte, localização, atualização e comportamento quando esses dados estiverem indisponíveis. Não foi autorizado inventar clima, tratar histórico como previsão ou acrescentar integração climática ao mobile.

Fonte: resposta E.9.

### D10 — Requisição em andamento, erros e teclado

**Decidido:** sair da tela não deve cancelar a chamada em andamento. Incluir uma apresentação de erro que ainda não existe nas referências. Manter o menu oculto enquanto o teclado estiver aberto.

**Pendente:** permitir ou bloquear envios simultâneos; apresentação e texto do erro; ação de nova tentativa; tratamento da resposta recebida enquanto a tela estiver fora de exibição. O comportamento da chamada ao trocar de chat também depende de D05.

**Limite técnico conhecido:** a API não tem deduplicação de envios. **INCERTO — risco inferido:** repetir uma chamada após timeout pode duplicar operações. Ver [contrato](api-contract.md#post-chat--enviar-mensagem).

Fonte: resposta E.10. “Não tem que fechar a chamada” foi registrado como manutenção da requisição ao sair da tela; essa frase não foi tratada como resposta sobre envios simultâneos.

## Ordem para resolver as pendências

Esta ordem organiza a conversa; não representa decisões já aceitas sobre as soluções.

1. **Garantir que o chatbot não salve hábitos:** compatibilizar D08 com a manutenção do backend em D07; verificar a capacidade do ambiente publicado.
2. **Viabilidade de clima atual e futuro:** compatibilizar D09 com as fontes disponíveis e com D07.
3. **Ambiente publicado:** conferir contrato/revisão acessível e requisitos de autenticação, sem inferi-los das respostas “não sei”.
4. **Ciclo da conversa:** comportamento do botão de novo chat, encerramento com falha e requisições em andamento.
5. **Envio e falhas:** concorrência, mensagem de erro, nova tentativa e entrega de respostas fora da tela.
6. **Continuidade e metas:** critérios de aceite diante do contexto limitado e operações permitidas para metas.
7. **Apresentação:** sugestões iniciais, histórico alternativo, botão de novo chat e formatação do texto.

Vamos resolver uma pendência por vez e atualizar este arquivo a cada decisão.

[schemas]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/schemas.py
