# Contrato da API do chatbot

## Objetivo e versão analisada

Registrar o contrato HTTP e o comportamento implementado para orientar a integração do chatbot no app mobile, incluindo os detalhes que o OpenAPI não descreve.

- Análise realizada em **25/10/2026**, com base na `main` remota do `delta-artificial-intelligence`, commit **`af5d6c1dafb10abd6897180e3e270402693c2a2b`**. Os links de fonte deste documento estão fixados nessa revisão.
- O checkout local da API estava no commit `b6e8aee1e30a0d36f861981db59001c05ca015fd`, 59 commits atrás da revisão analisada. A comparação foi feita sem atualizar o checkout. [Comparação das versões][versions]
- Referência complementar: [OpenAPI local](openapi.json). Seus campos de request/response de chat coincidem com os [schemas analisados][schemas], mas as respostas de sessões não têm estrutura definida no OpenAPI e `chart_data` é um objeto livre.
- Este documento resulta de leitura de código. **Não houve chamada aos endpoints, validação de produção ou execução de testes.**

Os exemplos JSON abaixo são **ilustrativos**, com IDs, datas, mensagens e números fictícios. Não são capturas de respostas reais nem valores para fixar no app. As estruturas vêm dos arquivos citados. Deduções e pontos ainda sem confirmação são marcados como **INCERTO**.

## Ambiente, transporte e autenticação

A URL base de integração ainda precisa ser informada. O OpenAPI local não declara `servers`; o registro de rotas da aplicação usa `/chat`, `/sessions/...` e `/health`, sem prefixo `/delta`. Não presumir que o endereço da API principal do mobile seja também o endereço da IA. [OpenAPI local](openapi.json), [main.py][main]

`POST /chat` recebe JSON e responde com um único JSON ao término do processamento. Não há streaming, SSE ou WebSocket nesse fluxo. [chat.py][chat], [main.py][main]

**Não há autenticação HTTP implementada nesse FastAPI:** as rotas não validam JWT/Bearer nem vinculam `user_id` a uma identidade autenticada. O ID é recebido do cliente. A validação de propriedade de uma sessão compara seu dono com o `user_id` informado na requisição; essa comparação não comprova a identidade de quem chama. [main.py][main], [chat.py][chat], [sessions.py][sessions], [session.py][session-memory]

**INCERTO:** pode existir autenticação em um gateway externo ou no ambiente publicado. Também precisa ser confirmado se `user_id` corresponde exatamente ao ID retornado por `/delta/auth/me` da API principal.

## Endpoints de chat e sessões

Estes são os endpoints disponíveis para envio e recuperação de conversas. O momento de acionar `close` ainda é uma decisão de produto; sua presença aqui não define encerramento automático ao sair da tela. [sessions.py][sessions], [long_term.py][long-term]

| Método e caminho | Entrada | Resposta de sucesso |
| --- | --- | --- |
| `POST /chat` | Body JSON: `user_id`, `message`, `session_id` opcional | `response`, `agent_used`, `session_id`, `chart_data` |
| `GET /sessions/{user_id}` | `user_id` no caminho | Objeto com a lista `sessions` |
| `GET /sessions/{session_id}/history` | `session_id` no caminho e `user_id` na query | Objeto com a lista `messages` |
| `POST /sessions/{session_id}/close` | `session_id` no caminho e `user_id` na query; sem body | `status` e `session_id` |

Fontes: [schemas.py][schemas], [chat.py][chat], [sessions.py][sessions].

### POST /chat — enviar mensagem

Request: `POST {BASE_URL}/chat`, com `Content-Type: application/json`.

| Campo | Tipo | Regra implementada |
| --- | --- | --- |
| `user_id` | Inteiro, obrigatório | O guardrail exige valor maior que zero. |
| `message` | String, obrigatória | Entre 1 e 2.000 caracteres; somente espaços também é rejeitado. |
| `session_id` | String ou `null`, opcional | Ausente, `null` ou string vazia gera um UUID. Uma string não vazia é usada como recebida, sem validação de formato UUID no schema. |

Fontes: [schemas.py][schemas], [input.py][input-guard], [chat.py][chat].

Body de exemplo para uma conversa nova:

```json
{
  "user_id": 42,
  "message": "Quanto vou consumir neste mês?",
  "session_id": null
}
```

Response de exemplo — HTTP 200:

```json
{
  "response": "Não há dados suficientes para estimar o consumo deste mês.",
  "agent_used": "forecast",
  "session_id": "52d5a619-7c18-4d66-8d27-693372901b81",
  "chart_data": null
}
```

Body de exemplo para continuar a mesma conversa, reutilizando o ID retornado:

```json
{
  "user_id": 42,
  "message": "E qual é a minha meta diária cadastrada?",
  "session_id": "52d5a619-7c18-4d66-8d27-693372901b81"
}
```

Response de exemplo — HTTP 200:

```json
{
  "response": "Você não tem uma meta diária cadastrada.",
  "agent_used": "forecast",
  "session_id": "52d5a619-7c18-4d66-8d27-693372901b81",
  "chart_data": null
}
```

As respostas textuais e o roteamento dos exemplos não são garantidos para essas perguntas: são ilustrações de payloads válidos. `response`, `agent_used` e `session_id` são strings obrigatórias na resposta; `chart_data` aceita objeto ou `null`. [schemas.py][schemas], [router.py][router]

Depois de gerar e validar a resposta, a rota grava a mensagem **original** do usuário como `human` e a resposta final como `assistant`, em duas operações separadas. A anonimização da entrada não substitui o texto original salvo no histórico. Não há endpoint dedicado para criar uma sessão vazia. [chat.py][chat], [session.py][session-memory], [main.py][main]

Não há identificador de requisição nem mecanismo de deduplicação nesse caminho. **INCERTO — risco inferido:** repetir um envio após timeout pode duplicar mensagens ou operações executadas pelo agente; uma falha entre as duas gravações pode deixar um turno incompleto. [chat.py][chat], [session.py][session-memory]

### GET /sessions/{user_id} — listar conversas

Request de exemplo: `GET {BASE_URL}/sessions/42`.

Representação JSON dos parâmetros **somente para documentação**; a requisição HTTP não possui body:

```json
{
  "path": {
    "user_id": 42
  }
}
```

Response de exemplo — HTTP 200:

```json
{
  "sessions": [
    {
      "session_id": "52d5a619-7c18-4d66-8d27-693372901b81",
      "created_at": "2026-10-25T15:00:00+00:00"
    },
    {
      "session_id": "ecc929ec-937a-4991-b6d3-ac07cd7cd792",
      "created_at": "2026-10-24T18:30:00+00:00"
    }
  ]
}
```

Sem sessões, a resposta é:

```json
{
  "sessions": []
}
```

Cada item contém apenas `session_id` e `created_at`. A consulta exclui `_id` e não retorna título, resumo, última mensagem, ícone, agente, data de atualização ou status de encerramento. Não há paginação nem ordenação explícita; a ordem usada no exemplo não constitui uma garantia. [sessions.py][sessions], [session.py][session-memory]

As datas são gravadas como `datetime` em UTC. **INCERTO:** o formato exato serializado no HTTP não foi validado em execução; o exemplo representa a data em ISO 8601 com offset `+00:00`. [session.py][session-memory]

### GET /sessions/{session_id}/history — recuperar mensagens

Request de exemplo: `GET {BASE_URL}/sessions/52d5a619-7c18-4d66-8d27-693372901b81/history?user_id=42`.

Representação JSON dos parâmetros **somente para documentação**; não enviar como body:

```json
{
  "path": {
    "session_id": "52d5a619-7c18-4d66-8d27-693372901b81"
  },
  "query": {
    "user_id": 42
  }
}
```

Response de exemplo — HTTP 200:

```json
{
  "messages": [
    {
      "role": "human",
      "content": "Quanto vou consumir neste mês?",
      "timestamp": "2026-10-25T15:00:00+00:00"
    },
    {
      "role": "assistant",
      "content": "Não há dados suficientes para estimar o consumo deste mês.",
      "timestamp": "2026-10-25T15:00:00.010000+00:00"
    }
  ]
}
```

Sessão inexistente, sem mensagens ou pertencente a outro `user_id` resulta em lista vazia:

```json
{
  "messages": []
}
```

Não há distinção pública entre esses casos. Cada mensagem gravada por `/chat` contém `role` (`human` ou `assistant`), `content` e `timestamp`. Não contém ID de mensagem, `chart_data` ou `agent_used`. O histórico devolve o array armazenado, sem paginação nem nova ordenação por timestamp. [sessions.py][sessions], [session.py][session-memory], [chat.py][chat]

`timestamp` registra o momento da gravação, depois da geração da resposta, e não o instante em que o usuário tocou em enviar. As datas são gravadas em UTC; **INCERTO:** sua representação exata no JSON HTTP não foi verificada, conforme a ressalva do exemplo de listagem. [chat.py][chat], [session.py][session-memory]

### POST /sessions/{session_id}/close — gerar memória da conversa

Request de exemplo: `POST {BASE_URL}/sessions/52d5a619-7c18-4d66-8d27-693372901b81/close?user_id=42`.

Representação JSON dos parâmetros **somente para documentação**; esse POST não recebe body JSON:

```json
{
  "path": {
    "session_id": "52d5a619-7c18-4d66-8d27-693372901b81"
  },
  "query": {
    "user_id": 42
  }
}
```

Response de exemplo — HTTP 200:

```json
{
  "status": "ok",
  "session_id": "52d5a619-7c18-4d66-8d27-693372901b81"
}
```

O endpoint lê o histórico validado, gera um resumo com LLM e o salva no Qdrant. **Não marca a sessão como encerrada, não a exclui e não impede novos envios.** O resumo não é devolvido nessa resposta nem na listagem de sessões. [sessions.py][sessions], [long_term.py][long-term], [session.py][session-memory]

Se o histórico validado estiver vazio, a função de resumo retorna sem gravar; a rota ainda responde com `status: "ok"`. Exceções durante o fechamento retornam HTTP 500 com `detail` textual. [sessions.py][sessions], [long_term.py][long-term]

## Endpoint técnico opcional

### GET /health — verificar resposta da aplicação

Este endpoint existe, mas **seu uso pelo mobile ainda não foi definido**. Ele apenas retorna um valor fixo; não realiza checks de banco ou de provedor LLM. [health.py][health]

Request: `GET {BASE_URL}/health`, sem parâmetros e sem body. Representação JSON documental:

```json
{
  "path": {},
  "query": {}
}
```

Response — HTTP 200:

```json
{
  "status": "ok"
}
```

## Valores de agent_used

O campo é uma string, não um enum de agente único. O registro atual contém: [router.py][router]

| Nome | Responsabilidade descrita no roteador |
| --- | --- |
| `forecast` | Previsão de consumo, conta estimada, tendência e risco de meta |
| `leak` | Indícios de vazamento, consumo anormal e alertas |
| `habits` | Hábitos, impacto hídrico e clima relacionado a hábitos ao ar livre |
| `rag` | Explicações baseadas na documentação oficial do Delta |

Sem agentes selecionados, o workflow usa `unknown`. Com vários agentes, une os nomes por vírgula e espaço, por exemplo `habits, forecast` ou `rag, leak`. A ordem vem da classificação e a lista não é deduplicada. `agent_used` registra os agentes **selecionados**, mesmo que algum deles falhe. A rota também possui fallback defensivo de string vazia caso a chave não exista no estado. [router.py][router], [workflow.py][workflow], [chat.py][chat]

`ConsumptionAgent` existe como implementação independente, mas não está registrado no roteador de `/chat` nessa revisão. [consumption.py][consumption], [router.py][router]

## Formato real de chart_data

`chart_data` é `null` ou o resultado de **`plotly.graph_objects.Figure.to_dict()`**: séries em `data` e configuração em `layout`. Não existe DTO específico nem versão de contrato para esses gráficos. [charts.py][charts], [schemas.py][schemas]

| Origem | Série e eixos | Configuração adicional |
| --- | --- | --- |
| `forecast` | `scatter`, `mode="lines+markers"`; datas em `x`, litros em `y` | Pode adicionar projeção mensal e custo em `layout.annotations`. |
| `leak` | `bar`; datas em `x`, litros das janelas anômalas em `y` | Barras vermelhas e título de consumo anômalo. |
| `habits` | `bar`, `orientation="h"`; frequências em `x`, nomes em `y` | Texto de frequência, como `3x/sem`, e altura conforme a quantidade de hábitos. |

Fonte: [charts.py][charts].

Exemplo ilustrativo de `/chat` com gráfico de hábitos. **O objeto Plotly foi reduzido aos campos relevantes**; não representa todos os defaults que `to_dict()` pode incluir, como o template de layout:

```json
{
  "response": "Estes são os hábitos cadastrados e suas frequências semanais.",
  "agent_used": "habits",
  "session_id": "52d5a619-7c18-4d66-8d27-693372901b81",
  "chart_data": {
    "data": [
      {
        "type": "bar",
        "orientation": "h",
        "x": [3, 1],
        "y": ["Banho Longo", "Lavar Roupa"],
        "marker": {
          "color": "#2196F3"
        },
        "text": ["3x/sem", "1x/sem"],
        "textposition": "auto"
      }
    ],
    "layout": {
      "title": {
        "text": "Frequência semanal dos hábitos"
      },
      "xaxis": {
        "title": {
          "text": "Vezes por semana"
        }
      },
      "yaxis": {
        "title": {
          "text": "Hábito"
        }
      },
      "height": 250,
      "margin": {
        "l": 150,
        "r": 20,
        "t": 40,
        "b": 40
      }
    }
  }
}
```

Limites implementados:

- O gráfico depende das ferramentas chamadas e de seus resultados; selecionar um agente não garante gráfico.
- A previsão pode produzir figura apenas com anotação, sem série histórica.
- Os construtores de previsão e vazamento usam a parte da data de `window_started_at` e o valor de cada janela, sem uma agregação diária explícita nesses construtores.
- Com vários agentes, o workflow entrega somente o primeiro gráfico não vazio entre os resultados bem-sucedidos.
- O histórico não persiste `chart_data`; reabrir uma sessão não recupera seus gráficos por esse endpoint.

Fontes: [charts.py][charts], [workflow.py][workflow], [session.py][session-memory].

## Erros e resultados sem dados

### HTTP 422 — validação de schema

O OpenAPI descreve `detail` como lista de erros, com `loc`, `msg` e `type`; também admite `input` e `ctx`. Exemplo ilustrativo para `message` vazia: [OpenAPI local](openapi.json), [schemas.py][schemas]

```json
{
  "detail": [
    {
      "type": "string_too_short",
      "loc": ["body", "message"],
      "msg": "String should have at least 1 character",
      "input": "",
      "ctx": {
        "min_length": 1
      }
    }
  ]
}
```

### HTTP 422 — guardrail de entrada

O guardrail usa outra estrutura: `detail` é uma **string**, e não a lista descrita no OpenAPI. Exemplo para uma mensagem composta somente de espaços: [chat.py][chat], [input.py][input-guard]

```json
{
  "detail": "Mensagem vazia."
}
```

Também existem rejeições por `user_id` inválido e conteúdo bloqueado, usando esse mesmo formato textual. [input.py][input-guard]

### HTTP 500 — validação de saída ou fechamento

Exemplo de erro explicitamente previsto pela validação da resposta: [chat.py][chat], [output.py][output-guard]

```json
{
  "detail": "Resposta vazia do agente."
}
```

`/close` retorna `detail` com o texto da exceção quando falha. Não existe um schema específico de erro para essa rota. Outras exceções de `/chat` não são convertidas por um handler próprio nessa implementação; não há garantia neste contrato de que todo erro 500 tenha o JSON acima. [sessions.py][sessions], [chat.py][chat], [main.py][main]

### HTTP 200 — indisponibilidade expressa como mensagem

Se todos os especialistas falharem ou não produzirem resposta aproveitável, o workflow gera uma mensagem de indisponibilidade. Se as etapas seguintes concluírem, ela pode ser retornada como HTTP 200: [workflow.py][workflow], [chat.py][chat]

```json
{
  "response": "Não foi possível obter uma resposta neste momento. Tente novamente.",
  "agent_used": "forecast",
  "session_id": "52d5a619-7c18-4d66-8d27-693372901b81",
  "chart_data": null
}
```

Não há campo estruturado na resposta HTTP que diferencie essa situação de uma resposta normal. Falta de dados também pode ser explicada no texto da resposta. Os estados e avisos internos dos agentes não fazem parte de `ChatResponse`. [schemas.py][schemas], [workflow.py][workflow], [forecast/tools.py][forecast-tools]

## Contexto e capacidades que não aparecem no OpenAPI

### Continuidade de conversa

O contexto recupera até seis mensagens recentes; o roteador utiliza até quatro delas e até duas memórias relevantes. Entretanto, a execução dos especialistas chama `agent.run(question)` somente com a pergunta atual. O runtime inicia a geração com o prompt do sistema e essa pergunta, sem o histórico da sessão. O RAG também recebe somente a pergunta atual. [context.py][context], [router.py][router], [workflow.py][workflow], [_runtime.py][runtime], [rag.py][rag]

**INCERTO — consequência inferida:** perguntas como “Quero uma versão mais resumida” podem não reformular corretamente a resposta anterior. Recuperar uma sessão para exibição no app não resolve, por si só, essa limitação da geração.

Com vários resultados bem-sucedidos, o workflow concatena as respostas usando `\n\n---\n\n`. A API entrega uma string única, sem blocos tipados por agente. [workflow.py][workflow]

### Consumo, metas, clima e hábitos

- **Consumo e metas:** as ferramentas consultam histórico, meta diária existente e calculam projeção/risco. Não há ferramenta para criar ou editar a meta nesse conjunto. O request HTTP não recebe `property_id`; a previsão residencial consulta dados pelo usuário, enquanto o caminho organizacional pode resolver unidades pelo nome informado ao agente. [schemas.py][schemas], [forecast/tools.py][forecast-tools]
- **Clima:** `get_weather_history` consulta histórico meteorológico no MongoDB por localização, com janela limitada a 1–30 dias. O resultado explicita que não representa previsão futura. A localização é obtida por `get_user_location`, documentada como a localização do primeiro imóvel vinculado. [habits/tools.py][habits-tools], [db_postgres.py][postgres]
- **Rotina:** o agente pode criar hábitos residenciais chamando `fn_create_user_habit`. Assim, `/chat` pode executar escrita de dados, mesmo sem um endpoint HTTP separado de cadastro no serviço de IA. [habits/tools.py][habits-tools], [db_postgres.py][postgres]
- **Fontes documentais:** o RAG acrescenta “Fontes consultadas” ao texto. Embora existam fontes estruturadas internamente, o schema HTTP não possui um campo `sources`. [rag.py][rag], [workflow.py][workflow], [schemas.py][schemas]

## Pontos ainda a confirmar para a integração

1. URL base, revisão efetivamente publicada, autenticação externa e correspondência entre o ID da conta mobile e `user_id`.
2. Escopo por conta ou por imóvel selecionado, já que o request não possui `property_id`.
3. Fonte de títulos, prévias e ícones do histórico, ausentes na listagem atual.
4. Momento de chamar `/close`, política de reenvio e tratamento de mensagens simultâneas.
5. Exibição dos gráficos, persistência ao reabrir conversas e suporte visual a Markdown/fontes.
6. Ajuste do contexto enviado aos especialistas para perguntas dependentes de respostas anteriores.
7. Se criar hábitos pelo chat exige confirmação na interface, e se o escopo de metas inclui edição.
8. Se o histórico climático atende ao requisito ou se também será necessária previsão meteorológica.

Essas decisões permanecem abertas; este documento não presume suas respostas. As lacunas técnicas correspondentes estão descritas e referenciadas nas seções anteriores.

[versions]: https://github.com/delta-app-ofc/delta-artificial-intelligence/compare/b6e8aee1e30a0d36f861981db59001c05ca015fd...af5d6c1dafb10abd6897180e3e270402693c2a2b
[main]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/main.py
[schemas]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/schemas.py
[chat]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/routes/chat.py
[sessions]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/routes/sessions.py
[health]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/routes/health.py
[input-guard]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/guardrails/input.py
[output-guard]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/guardrails/output.py
[router]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/graph/router.py
[workflow]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/graph/workflow.py
[context]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/graph/context.py
[session-memory]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/memory/session.py
[long-term]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/memory/long_term.py
[charts]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/tools/charts.py
[habits-tools]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/tools/habits/tools.py
[forecast-tools]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/tools/forecast/tools.py
[postgres]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/data/db_postgres.py
[consumption]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/agents/consumption.py
[runtime]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/agents/_runtime.py
[rag]: https://github.com/delta-app-ofc/delta-artificial-intelligence/blob/af5d6c1dafb10abd6897180e3e270402693c2a2b/app/agents/rag.py
