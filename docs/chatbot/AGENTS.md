# AGENTS.md — Feature de chatbot

## Escopo e referências

Este arquivo contém somente regras específicas do chatbot e se aplica a `docs/chatbot/`. As convenções gerais de stack, arquitetura, rede, erros, UI, recursos, validação e colaboração ficam no [AGENTS.md da raiz](../../AGENTS.md). Sua localização não estende automaticamente as instruções ao código de `app/`.

Leia as fontes da feature nesta ordem:

1. [Briefing](briefing.md) e [referências visuais](design/).
2. [Contrato analisado](api-contract.md) e [OpenAPI](openapi.json).
3. [Decisões e pendências](decisions.md).

O briefing define os critérios de aceite; `decisions.md` registra os ajustes aprovados. Para questões não descritas no OpenAPI, consulte o contrato e os arquivos da API na revisão ali identificada. Fonte: [contrato](api-contract.md), [decisões](decisions.md).

## Etapa atual

- Na etapa registrada em 25/10/2026, a implementação do chatbot aguarda autorização explícita. O backend permanece sem ajustes por enquanto. Fonte: [objetivo e D07](decisions.md).
- Registre respostas sobre o fluxo em [decisions.md](decisions.md), mantendo os identificadores D01–D10 e as pendências específicas.
- O bloqueio de escrita de hábitos como pré-requisito para habilitar o envio real ainda é proposta pendente; não está aprovado. Fonte: [D08](decisions.md).

## Regras de produto já decididas

Consulte o texto completo e as pendências de cada decisão em [decisions.md](decisions.md):

- **D01–D02:** serviço de IA em `https://delta-artificial-intelligence.onrender.com`; `user_id` corresponde ao ID de `/delta/auth/me`; contexto da conta toda. Revisão publicada e autenticação externa seguem sem confirmação.
- **D03–D04:** conteúdo e interação das sugestões iniciais seguem pendentes. Usar ícones predefinidos no Figma e apresentação alternativa do histórico; desconsiderar “Ver todos”. Não inventar títulos ou prévias como se viessem da API.
- **D05:** entrar sempre pela apresentação inicial e incluir um botão de novo chat, que inicia outra conversa e encerra a anterior no fluxo do app. Tratamento de falhas e requisições em andamento ainda precisa de decisão.
- **D06:** gráficos ficam fora da entrega atual. Isso não altera o contrato existente de `chart_data`.
- **D07–D08:** manter o backend sem ajustes por enquanto; o chatbot deve apenas orientar, sem salvar hábitos. A capacidade de escrita do backend analisado é uma incompatibilidade a resolver, não algo que a interface garanta impedir.
- **D09:** considerar clima atual e previsão futura, além de histórico. Não apresentar histórico climático como previsão nem afirmar que a fonte já atende ao requisito sem verificação.
- **D10:** sair da tela não cancela a chamada; prever uma apresentação de erro adicional às referências. Concorrência e nova tentativa continuam pendentes. O tratamento geral de menu/teclado está no AGENTS.md da raiz.

Envio de imagens, áudios, arquivos e notificações push estão fora do escopo, conforme o [briefing](briefing.md).

## Limites específicos da integração

- Não presuma que `close` apaga ou bloqueia uma sessão: a implementação analisada gera memória da conversa. Fonte: [contrato de sessões](api-contract.md).
- O histórico chega ao roteador, mas não à geração dos especialistas. **INCERTO:** pedidos como “Quero uma versão mais resumida” podem não operar sobre a resposta anterior. Fonte: [continuidade no contrato](api-contract.md) e [D07](decisions.md).
- A interface não remove a ferramenta `create_habit` do servidor. Não trate ausência de botão de salvar como garantia de somente orientação. Fonte: [contrato](api-contract.md) e [D08](decisions.md).
- A entrega futura deve atender aos tamanhos do briefing: celular pequeno (~360dp), celular grande (~412dp) e tablet (~800dp), em retrato e paisagem. Os textos fixos devem ficar em `strings.xml`. Esses são critérios desta feature. Fonte: [briefing](briefing.md).

## Manutenção da documentação da feature

- Ao revisar `api-contract.md`, mantenha a revisão da API, fontes e exemplos JSON ilustrativos identificados. Para GETs e POSTs sem body, documente parâmetros de caminho/query separadamente do body. Fonte: [contrato atual](api-contract.md).
- Preserve o que foi aprovado em `decisions.md`; ao substituir uma decisão, registre a alteração e as pendências afetadas. Fonte: [objetivo e decisões](decisions.md).
