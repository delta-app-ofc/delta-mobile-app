# AGENTS.md — Feature de chatbot

## Escopo e referências

Este arquivo contém somente regras específicas do chatbot e se aplica a `docs/chatbot/`. As convenções gerais de stack, arquitetura, rede, erros, UI, recursos, validação e colaboração ficam no [AGENTS.md da raiz](../../AGENTS.md). Sua localização não estende automaticamente as instruções ao código de `app/`.

Leia as fontes da feature nesta ordem:

1. [Briefing](briefing.md) e [referências visuais](design/).
2. [Contrato analisado](api-contract.md) e [OpenAPI](openapi.json).
3. [Decisões e pendências](decisions.md).

O briefing define os critérios de aceite; `decisions.md` registra os ajustes aprovados. Para questões não descritas no OpenAPI, consulte o contrato e os arquivos da API na revisão ali identificada. Fonte: [contrato](api-contract.md), [decisões](decisions.md).

## Etapa atual

- A implementação foi autorizada nas etapas registradas em [decisions.md](decisions.md). Em 26/10/2026, a pessoa usuária também autorizou testar a integração publicada com o contrato disponível; o backend permanece sem ajustes por enquanto. Fonte: [D01 e D07](decisions.md).
- Registre respostas sobre o fluxo em [decisions.md](decisions.md), mantendo os identificadores D01–D10 e as pendências específicas.
- A pessoa usuária permitiu que o chat registre hábitos se o backend oferecer essa ferramenta. Fonte: [D08](decisions.md).

## Regras de produto já decididas

Consulte o texto completo e as pendências de cada decisão em [decisions.md](decisions.md):

- **D01–D02:** serviço de IA em `https://delta-artificial-intelligence.onrender.com`; `user_id` corresponde ao ID de `/delta/auth/me`; contexto da conta toda. Revisão publicada e autenticação externa seguem sem confirmação.
- **D03–D04:** conteúdo e interação das sugestões iniciais seguem pendentes. Usar ícones predefinidos no Figma e apresentação alternativa do histórico; desconsiderar “Ver todos”. Não inventar títulos ou prévias como se viessem da API.
- **D05:** entrar sempre pela apresentação inicial; mostrar “Novo chat” e “Voltar ao início do chatbot” somente durante a conversa. Voltar preserva sessão, mensagens e rascunho; “Retomar conversa” reabre o conteúdo guardado. Novo chat encerra a conversa anterior no fluxo do app, conforme [decisions.md](decisions.md).
- **D06:** gráficos ficam fora da entrega atual. Isso não altera o contrato existente de `chart_data`.
- **D07–D08:** manter o backend sem ajustes por enquanto; o chat pode registrar hábitos quando o backend permitir.
- **D09:** aceitar histórico climático na primeira entrega; clima atual e previsão futura ficam para depois. Não apresentar histórico como previsão.
- **D10:** sair da tela não cancela a chamada; permitir um envio por vez, preservar o rascunho e exigir nova tentativa manual após falha. O tratamento geral de menu/teclado está no AGENTS.md da raiz.

Envio de imagens, áudios, arquivos e notificações push estão fora do escopo, conforme o [briefing](briefing.md).

## Limites específicos da integração

- Não presuma que `close` apaga ou bloqueia uma sessão: a implementação analisada gera memória da conversa. Fonte: [contrato de sessões](api-contract.md).
- O histórico chega ao roteador, mas não à geração dos especialistas. **INCERTO:** pedidos como “Quero uma versão mais resumida” podem não operar sobre a resposta anterior. Fonte: [continuidade no contrato](api-contract.md) e [D07](decisions.md).
- A interface não remove a ferramenta `create_habit` do servidor. Não trate ausência de botão de salvar como garantia de somente orientação. Fonte: [contrato](api-contract.md) e [D08](decisions.md).
- A entrega futura deve atender aos tamanhos do briefing: celular pequeno (~360dp), celular grande (~412dp) e tablet (~800dp), em retrato e paisagem. Os textos fixos devem ficar em `strings.xml`. Esses são critérios desta feature. Fonte: [briefing](briefing.md).

## Manutenção da documentação da feature

- Ao revisar `api-contract.md`, mantenha a revisão da API, fontes e exemplos JSON ilustrativos identificados. Para GETs e POSTs sem body, documente parâmetros de caminho/query separadamente do body. Fonte: [contrato atual](api-contract.md).
- Preserve o que foi aprovado em `decisions.md`; ao substituir uma decisão, registre a alteração e as pendências afetadas. Fonte: [objetivo e decisões](decisions.md).
