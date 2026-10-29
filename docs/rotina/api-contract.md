# Contrato da API de Rotina

## Escopo e fontes — 28/10/2026

Análise estática do [OpenAPI local](openapi.json), OpenAPI 3.1.0, título `Delta API`, versão declarada `1.0.0`, comparado com o checkout Android. A versão declarada não identifica um commit do backend. Não foram chamados endpoints Delta, executados testes Android ou alterados fontes nesta etapa. Exemplos abaixo são ilustrativos, não capturas HTTP nem comprovação de aceitação pelo servidor. Origem do escopo: pedido da pessoa usuária nesta conversa; regras de evidência: [AGENTS.md](../../AGENTS.md).

Fontes complementares: [briefing](briefing.md), [decisões desta conversa](decisions.md), [ApiService][api], [ApiModels][models], [SectionsViewModel][sections], [MainSectionFragment][fragment], [catálogo local][catalog] e [documentação de autenticação][auth-doc]. As afirmações de contrato nas seções seguintes vêm de `paths` e `components.schemas` do [OpenAPI](openapi.json); quando o arquivo é omisso, isso é indicado expressamente. **INCERTO** identifica inferência ou proposta, nunca regra confirmada do backend.

**Atualização de decisão em 29/10/2026:** a pessoa usuária aprovou considerar a ampliação da API para receber, persistir e devolver a distribuição de ocorrências por dia. O formato exato ainda não foi definido e o OpenAPI local não foi alterado. As tabelas e exemplos abaixo continuam documentando o contrato disponível, sem antecipar campos da ampliação. Fonte: [D06](decisions.md#d06-ampliação-da-api-para-a-distribuição-por-dia).

**Definições de produto em 29/10/2026:** os litros estimados representam o total do hábito no dia e serão calculados usando média fixa por execução de cada tipo de hábito, com a tabela inicial da IA aprovada em D10. O local do cálculo continua pendente; mobile foi indicado pela pessoa usuária como hipótese provável. O OpenAPI não documenta fórmula ou unidade temporal; regra diária, precisão e representação do valor na ampliação ainda precisam ser definidos. Fontes: [D07–D10](decisions.md).

**Horários por execução, definidos em 29/10/2026:** cada execução do hábito deverá ter horário individual obrigatório, inclusive várias no mesmo dia. O contrato atual só tem um par opcional `startTime`/`endTime` por hábito; estrutura por execução e uso de início ou intervalo permanecem pendentes. Fontes: [D11](decisions.md#d11-horário-individual-por-execução), [D12](decisions.md#d12-horário-obrigatório-em-cada-execução) e [OpenAPI](openapi.json).

**Encerramento do planejamento em 29/10/2026:** por solicitação da pessoa usuária, as definições ainda pendentes ficam como **INCERTO — decisão adiada**. A análise do contrato atual não foi substituída por um contrato futuro presumido. O [plano](plan.md) registra a sequência de implementação e as definições necessárias em cada etapa, sem novas perguntas nesta rodada. Fonte: [D13](decisions.md#d13-adiar-decisões-restantes-e-fechar-o-plano).

## Etapa 0 — Especificação acordada do recorte de leitura — 29/10/2026

A pessoa usuária autorizou somente a etapa 0 do [plano](plan.md#etapa-0--confirmar-o-contrato-por-recorte), sem commit, e escolheu definir o contrato conjuntamente. Esta seção registra as novas confirmações; as tabelas e exemplos do OpenAPI nas demais seções continuam representando o contrato anterior. Não foi fornecido um snapshot oficial ampliado nem verificado o serviço publicado nesta execução. Fonte: [D14](decisions.md#d14--execução-autorizada-somente-da-etapa-0).

**Estado:** especificação do recorte inicial de leitura concluída com as respostas D15–D23. Ela resolve as definições necessárias a modelos/testes de leitura com serviço substituído; não comprova compatibilidade para integração real. As referências anteriores a estrutura, fórmula, início versus intervalo, legado, vigência e catálogo ainda pendentes são históricas e ficam substituídas, somente nesses pontos, pelas confirmações desta seção. Regras exclusivas de escrita continuam adiadas. O `openapi.json` permanece intacto até existir fonte oficial ampliada.

**Confirmado em D15:** cada execução terá apenas horário de início obrigatório. Não exigir fim nem duração, nem calcular consumo pelo tempo. A estimativa permanece baseada na média fixa por execução aprovada em D09/D10. Essa decisão substitui somente a alternativa início versus intervalo anteriormente pendente; não altera os campos do OpenAPI anterior. Fonte: [D15](decisions.md#d15--somente-início-obrigatório-por-execução).

**Confirmado em D16:** distribuição como lista de dias únicos, cada dia com lista não vazia de horários; a quantidade diária é o tamanho dessa lista e a soma deve ser igual à frequência semanal. Não repetir IDs de dias nem duplicar hábitos para representar execuções. Nomes dos campos de transporte ainda não foram definidos. Fonte: [D16](decisions.md#d16--distribuição-representada-pelos-horários-de-cada-dia).

**Confirmado em D17:** início em hora local no formato de 24 horas `HH:mm`, entre `00:00` e `23:59`, sem segundos nem conversão automática de fuso. O valor não representa um instante UTC nem duração; `07:00` continua `07:00` na agenda. Fonte: [D17](decisions.md#d17--hora-local-em-hhmm).

**Confirmado em D18:** cálculo no mobile com a tabela inicial de D10. Total diário do hábito = litros por execução × número de horários no dia; total do dia = soma desses totais, uma única vez. Exemplo aritmético: duas execuções de banho longo, de 80 L cada, resultam em 160 L no resumo do hábito, não 320 L. São estimativas, não medições. Os seis valores atuais produzem litros inteiros, sem arredondamento. Tipo sem referência fica com estimativa indisponível e total incompleto; não usar zero nem o `estimatedLiters` anterior, cuja unidade temporal não está confirmada, para preencher essa lacuna. Fonte: [D18](decisions.md#d18--cálculo-diário-no-mobile-e-ausência-de-referência).

**Confirmado em D19:** preservar hábitos antigos sem distribuição ou horários completos, identificados como “agenda incompleta” e fora das linhas por horário. Não inferir quantidade por dia nem copiar horário único para múltiplas execuções. Enquanto houver esses registros, agenda e total permanecem incompletos, mesmo sem outras execuções no dia; não mostrar vazio confirmado/zero como se a coleção fosse completa. Não há migração ou escrita autorizada. Fonte: [D19](decisions.md#d19--legado-preservado-com-agenda-incompleta).

**Confirmado em D20:** projetar a semana padrão em qualquer data, inclusive passada, pelo dia da semana. Não exigir início/fim de vigência nem acrescentar data ao payload. Essa agenda representa planejamento com a rotina cadastrada, não histórico de hábitos executados ou de consumo medido. Efeitos de edição/exclusão continuam para os recortes de escrita. Fonte: [D20](decisions.md#d20--semana-padrão-projetada-em-qualquer-data).

**Confirmado em D21/D22:** tipos limitados aos seis do catálogo local, com os IDs abaixo; dias de 1=segunda a 7=domingo. Não permitir tipos livres. Horários do mesmo hábito no mesmo dia não podem se repetir; hábitos diferentes podem compartilhar horário. Não há duração para avaliar sobreposição. A correspondência desses IDs no banco publicado permanece sem comprovação. Fontes: [D21/D22](decisions.md), [catálogo local][catalog].

| `habitId` | Tipo de hábito | Litros estimados por execução |
| --- | --- | --- |
| 1 | Banho longo | 80 L |
| 2 | Lavar quintal | 150 L |
| 3 | Lavar roupa | 100 L |
| 4 | Regar plantas | 15 L |
| 5 | Lavar carro | 200 L |
| 6 | Lavar louça | 20 L |

Fonte dos valores: D10; adoção do catálogo: D21. Não inferir IDs de tipos desconhecidos por semelhança de nome. Tipo sem referência segue D18, sem estimativa inventada.

### Estrutura acordada das respostas de leitura

**Confirmado em D23:** manter `GET /delta/habits/{userId}` como coleção completa, sem query de dia/mês, e `GET /delta/habits/{userId}/{id}` como detalhe do registro. Preservar identidade da conta real e conferir o dono. Acrescentar a estrutura abaixo ao objeto de hábito retornado; não confundir o ID do registro com o ID do catálogo. Esta é especificação acordada para a ampliação, não descrição do OpenAPI oficial anterior nem evidência de response HTTP real.

| Campo | Tipo/semântica no recorte ampliado |
| --- | --- |
| `id`, `userId`, `habitId` | Inteiros de identidade do registro, dono e tipo, já existentes |
| `frequency` | Inteiro de execuções semanais, igual à soma dos tamanhos de `startTimes` |
| `daysOfWeek` | Lista de dias únicos de 1 a 7, exatamente os dias de `schedule`; a ordem não define cronologia |
| `schedule` | Lista de dias únicos com pelo menos um início por dia; necessária para agenda completa |
| `schedule[].dayOfWeek` | Inteiro de 1=segunda a 7=domingo, correspondente a `daysOfWeek` |
| `schedule[].startTimes` | Lista não vazia de strings `HH:mm`, válidas de `00:00` a `23:59`, sem repetição dentro desse hábito/dia |

`habitName`, `habitCategory` e `habitDescription` continuam metadados do registro conforme o contrato anterior, sem inventar nome/categoria livres. Não definir nova obrigatoriedade desses metadados nesta etapa. `startTime`, `endTime` e `estimatedLiters` antigos não representam a distribuição nem determinam a projeção/cálculo novo. Sua remoção ou política de escrita não é definida por este recorte.

A lista de horários é a fonte da quantidade diária; não acrescentar um contador paralelo. O app seleciona o dia da semana e ordena os inícios cronologicamente para a agenda, sem depender da ordem recebida no JSON. Não há campos de data, duração, execução concluída, medição, imóvel ou total diário calculado pelo servidor nesta ampliação acordada. Fonte das regras: D15–D23; a ordenação cronológica já está prevista na etapa 4 do [plano](plan.md).

### Exemplo de listagem ampliada — especificado, sem deploy comprovado

Resposta desejada de `GET /delta/habits/42`, status `200`. IDs de conta/registro e horários são ilustrativos; os tipos e litros seguem o catálogo aprovado. A coleção é array, sem envelope. O exemplo permite duas execuções do mesmo hábito na segunda e três na quarta, e outro hábito no mesmo horário de segunda.

```json
[
  {
    "id": 105,
    "userId": 42,
    "habitId": 1,
    "habitName": "Banho longo",
    "frequency": 5,
    "daysOfWeek": [1, 3],
    "schedule": [
      {"dayOfWeek": 1, "startTimes": ["07:00", "20:00"]},
      {"dayOfWeek": 3, "startTimes": ["08:00", "14:00", "21:00"]}
    ]
  },
  {
    "id": 106,
    "userId": 42,
    "habitId": 4,
    "habitName": "Regar plantas",
    "frequency": 1,
    "daysOfWeek": [1],
    "schedule": [
      {"dayOfWeek": 1, "startTimes": ["07:00"]}
    ]
  }
]
```

Fonte: estrutura aprovada em D23 e regras D15–D22. Resultado esperado da projeção desse exemplo, sem hábitos incompletos adicionais:

| Dia selecionado | Execuções | Resumo por hábito | Total estimado do dia |
| --- | --- | --- | --- |
| Segunda-feira | Banho às 07:00 e 20:00; plantas às 07:00 | Banho 160 L; plantas 15 L | 175 L |
| Quarta-feira | Banho às 08:00, 14:00 e 21:00 | Banho 240 L | 240 L |
| Terça-feira | Nenhuma | Vazio confirmado | 0 L |

Cada linha por execução mostra sua estimativa individual (banho 80 L, plantas 15 L); o resumo diário de banho não deve ser repetido como valor de cada execução. Semana padrão é projeção e não histórico de realização, inclusive para datas passadas. Coleção válida `[]`, sem registros incompletos, também permite vazio confirmado e zero; corpo nulo/falha de consulta não permite essa conclusão. Fontes: D18–D20, tratamento existente de body nulo em [SectionsViewModel][sections], [plano](plan.md), [AGENTS.md](../../AGENTS.md).

### Exemplo de detalhe ampliado — mesma distribuição da coleção

Resposta desejada de `GET /delta/habits/42/105`, status `200`, sem envelope. O detalhe usa o ID do registro, não `habitId`. Seus horários e frequência correspondem ao primeiro objeto da listagem:

```json
{
  "id": 105,
  "userId": 42,
  "habitId": 1,
  "habitName": "Banho longo",
  "frequency": 5,
  "daysOfWeek": [1, 3],
  "schedule": [
    {"dayOfWeek": 1, "startTimes": ["07:00", "20:00"]},
    {"dayOfWeek": 3, "startTimes": ["08:00", "14:00", "21:00"]}
  ]
}
```

Fonte: D23. A igualdade entre os exemplos especifica leitura sem perda da distribuição; não prova gravação, persistência ou releitura de um POST/PUT executado.

### Compatibilidade, pendências e verificação do recorte

Registros antigos sem `schedule` completo continuam legíveis como cadastro, com agenda/total incompletos conforme D19. Não atribuir automaticamente uma ocorrência a cada `daysOfWeek`, nem usar `frequency` para repartir execuções, nem propagar o antigo `startTime`. Dias/frequência/horários incoerentes não constituem agenda completa válida; não corrigir silenciosamente a distribuição recebida. D16/D22/D23 definem as invariantes, sem definir novo schema de erro HTTP por campo.

**Ainda dependente do backend:** implementar e publicar essa estrutura, confirmar IDs do catálogo no ambiente e fornecer OpenAPI oficial ampliado. A aprovação conjunta é especificação, sem confirmação técnica do backend. Não substituir o snapshot local por schema produzido manualmente. Nenhum endpoint autenticado foi chamado nesta etapa.

**INCERTO — adiado para escrita:** schemas de POST/PUT, limites adicionais, omissão/nulos, conflito entre registros, mapa real de erros, efeitos de edição/exclusão e resultado incerto de mutação (P01/P04/P05/P11). Definir exemplos de escrita e releitura antes de suas implementações, sem presumir que os requests copiem o response de leitura. Interação do calendário/formulário, layout e metas/alertas continuam em P07–P10. Esta entrega não resolve essas pendências nem implementa controles de UI.

**Como verificar a especificação:** validar sintaxe dos JSONs; conferir dias únicos, faixas/horários distintos, correspondência de `daysOfWeek`, soma igual à frequência e igualdade do detalhe com o objeto da coleção; recalcular 160 L + 15 L = 175 L na segunda e 240 L na quarta. Ao implementar modelos/testes da etapa 1, acrescentar legado, referência ausente, vazio e fronteiras de calendário. Integração real exige responses oficiais correspondentes e verificação de persistência nos recortes de escrita.

**Verificação aplicável agora:** revisão documental, conferência dos exemplos e `git diff --check`. Sem build/teste Android, mudança de tela ou commit para este recorte documental, conforme o plano.

**Executado em 29/10/2026:** os dois exemplos ampliados foram parseados com `ConvertFrom-Json` e conferidos por script PowerShell local: dias únicos na faixa 1–7, correspondência de dias, horários válidos/distintos, soma igual à frequência, detalhe idêntico ao primeiro item da coleção e totais de 175 L/240 L. Os IDs foram comparados ao catálogo local, sem acesso ao banco. `git diff --check` passou. Essa checagem documental não é teste de classes Java, rede, persistência ou tela.

## Transporte, autenticação e ponto de integração

- O OpenAPI declara servidor `https://delta-api-postgres.onrender.com` e segurança global `bearerAuth`, HTTP Bearer/JWT. Nenhuma das cinco operações de hábitos sobrescreve essa segurança. Requests de escrita usam `application/json`; respostas com conteúdo declaram `*/*`. Fonte: [OpenAPI](openapi.json), `servers`, `security`, `components.securitySchemes.bearerAuth` e `paths./delta/habits/*`.
- No app, a base efetiva vem de `BuildConfig.DELTA_API_URL`, com `/` final e overrides por build; o padrão coincide com o servidor documentado. Reutilizar a instância de `AuthRepository.getInstance(context).api()`, Retrofit/Gson e o interceptor existentes. Não criar outro cliente nem acrescentar Bearer manualmente na feature. Fontes: [README][readme], [build][build], [RetrofitClient][retrofit], [NetworkConfig][network], [AuthRepository][auth], [SessionInterceptor][interceptor].
- `userId` é parâmetro de caminho, não campo do request de hábito; a integração atual obtém a identidade por `auth.currentProfile().id`, que consulta `/delta/auth/me`. `id` identifica o registro de hábito do usuário; `habitId` referencia o tipo de hábito. A implementação atual usa `id` para editar/excluir e confere `userId` no detalhe antes dessas mutações. Fontes: [OpenAPI](openapi.json), [SectionsViewModel][sections], [AuthRepository][auth].
- Os cinco métodos já existem em `ApiService`: `listUserHabits`, `getUserHabit`, `createUserHabit`, `updateUserHabit`, `deleteUserHabit`. Não há endpoint novo a declarar para esse CRUD. Os DTOs Android ainda não representam todos os campos do OpenAPI, conforme a seção de divergências. Fontes: [ApiService][api], [ApiModels][models].
- Atualmente, o acesso HTTP dos hábitos fica em `SectionsViewModel`, por `auth.resource()`, sem repositório dedicado de hábitos. **INCERTO — proposta técnica, não decisão aprovada:** concentrar essas operações em um novo `RoutineRepository`, no pacote `repository`, injetando `AuthRepository` por construtor e reutilizando o mesmo `ApiService`, para seguir a separação documentada e exemplificada por Consumo anormal. Não foi criado nenhum arquivo de implementação. Fontes: [SectionsViewModel][sections], [README][readme], [AbnormalConsumptionRepository][reference-repository].

## Operações disponíveis

Todos os parâmetros de caminho abaixo são obrigatórios, `integer/int32`; não há mínimo declarado. Os nomes `userId`, `id` e `habitId` não são intercambiáveis. Fonte: parâmetros e schemas do [OpenAPI](openapi.json).

| Operação | Request | Sucesso | Falhas explicitamente declaradas |
| --- | --- | --- | --- |
| Listar todos os hábitos da conta | `GET /delta/habits/{userId}`, sem body | `200`, array de `UserHabitIO` | Nenhuma nesta operação |
| Consultar um registro | `GET /delta/habits/{userId}/{id}`, sem body | `200`, `UserHabitIO` | `404`: hábito do usuário não encontrado |
| Criar | `POST /delta/habits/{userId}`, body obrigatório `CreateUserHabitRequest` | `201`, `UserHabitIO` | `400`: dados inválidos; `404`: hábito ou dia da semana não encontrado; `409`: hábito já cadastrado para o usuário |
| Editar | `PUT /delta/habits/{userId}/{id}`, body obrigatório `UpdateUserHabitRequest` | `200`, `UserHabitIO` | `400`: dados inválidos; `404`: hábito do usuário, hábito ou dia da semana não encontrado; `409`: hábito já cadastrado para o usuário |
| Excluir | `DELETE /delta/habits/{userId}/{id}`, sem body | `204`, sem conteúdo declarado | `404`: hábito do usuário não encontrado, sem conteúdo declarado |

Fonte de todas as linhas: [OpenAPI](openapi.json), os dois caminhos `/delta/habits/{userId}` e `/delta/habits/{userId}/{id}`. A ausência de códigos como `401`, `403`, `429` e `500` nessa tabela não comprova que não possam ocorrer; a segurança global exige Bearer, e o app já trata erros HTTP além dos listados. Fontes: [OpenAPI](openapi.json), [ApiErrors][errors], [SessionInterceptor][interceptor].

### Listar por dia ou mês: não há operação publicada

O único GET de coleção recebe apenas `userId`. Não declara query de data, mês, intervalo, fuso, paginação ou ordenação. Não há coleção de ocorrências, endpoint de resumo diário/mensal ou marcações do calendário. Fonte: inventário de `paths` e parâmetros de `GET /delta/habits/{userId}` no [OpenAPI](openapi.json).

Portanto, não existe request/response documentado específico para um dia ou mês. **Decisão em 29/10/2026:** a API será ampliada para persistir a distribuição por dia, conforme [D06](decisions.md#d06-ampliação-da-api-para-a-distribuição-por-dia). **INCERTO — alternativas ainda a decidir:** projetar as ocorrências no app a partir da coleção ampliada ou recebê-las já expandidas pelo backend. A aprovação da distribuição não define consultas por data/mês. Não acrescentar parâmetros imaginários ao GET. Fontes da lacuna: [OpenAPI](openapi.json); necessidade: [briefing](briefing.md) e [D03/D04/D06](decisions.md).

Exemplo ilustrativo da consulta existente, com IDs fictícios:

```http
GET /delta/habits/42
Authorization: Bearer <access_token>
```

Exemplo de `200` como array de `UserHabitIO`; valores de texto, IDs, horários e litros não são dados reais. **INCERTO:** o formato de horário usado no exemplo não é garantido pelo schema.

```json
[
  {
    "id": 105,
    "userId": 42,
    "habitId": 1,
    "habitName": "Banho longo",
    "habitCategory": "Higiene e Cuidados",
    "habitDescription": "Descrição ilustrativa do hábito",
    "frequency": 5,
    "startTime": "07:00:00",
    "endTime": "07:20:00",
    "estimatedLiters": 45,
    "daysOfWeek": [1, 2, 3, 4, 5]
  }
]
```

Fonte da estrutura: [OpenAPI](openapi.json), `UserHabitIO` e resposta `200` da listagem. `[]` é compatível com o schema de sucesso; isso não documenta como resolver um dia sem ocorrências a partir de um hábito recorrente. Corpo ausente não deve ser confundido com `[]`; o código atual trata body nulo como erro. Fontes: [SectionsViewModel][sections], [OpenAPI](openapi.json).

### Consultar detalhe

```http
GET /delta/habits/42/105
Authorization: Bearer <access_token>
```

No `200`, retorna um objeto `UserHabitIO`, como o objeto dentro do array anterior, sem envelope. O schema não contém data agendada, ID de ocorrência ou duração. Fonte: [OpenAPI](openapi.json), GET de detalhe e `UserHabitIO`.

### Criar

```http
POST /delta/habits/42
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "habitId": 1,
  "frequency": 5,
  "daysOfWeek": [1, 2, 3, 4, 5],
  "startTime": "07:00:00",
  "endTime": "07:20:00",
  "estimatedLiters": 45
}
```

Sucesso: `201`, objeto `UserHabitIO` com a estrutura do exemplo de detalhe/listagem. O request mínimo pelos campos `required` é `{"habitId":1,"frequency":5,"daysOfWeek":[1,2,3,4,5]}`. Ambos são exemplos estruturais; não comprovam IDs válidos, regra de frequência, formato de hora ou cálculo de litros no ambiente publicado. Fonte: [OpenAPI](openapi.json), `CreateUserHabitRequest`, POST e `UserHabitIO`.

### Editar

```http
PUT /delta/habits/42/105
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "habitId": 1,
  "frequency": 3,
  "daysOfWeek": [1, 3, 5],
  "startTime": "08:00:00",
  "endTime": "08:15:00",
  "estimatedLiters": 30
}
```

Sucesso: `200`, objeto `UserHabitIO`, por exemplo com `id=105`, `userId=42` e os valores editados. `UpdateUserHabitRequest` tem os mesmos campos e obrigatoriedade do create; não há PATCH declarado. **INCERTO:** não está definido se a omissão dos opcionais em PUT preserva ou apaga valores existentes. O formulário atual envia somente `habitId`, `frequency` e `daysOfWeek`; isso não comprova preservação dos demais campos no backend atualizado. Fontes: [OpenAPI](openapi.json), [ApiModels][models], [SectionsViewModel][sections].

### Excluir

```http
DELETE /delta/habits/42/105
Authorization: Bearer <access_token>
```

Sucesso: `204`, sem JSON declarado. O contrato exclui um registro de hábito do usuário; não oferece parâmetro para excluir só a ocorrência de uma data, todas as futuras ou uma exceção de recorrência. A confirmação antes de excluir é requisito do app, não outro endpoint. Fontes: [OpenAPI](openapi.json), [briefing](briefing.md).

## Campos de criação e edição

| Campo | Tipo | Obrigatoriedade no schema | Restrições explícitas e lacunas |
| --- | --- | --- | --- |
| `habitId` | `integer/int32` | Obrigatório | Sem mínimo ou catálogo/enum no OpenAPI; não é nome livre |
| `frequency` | `integer/int32` | Obrigatório | Sem mínimo, máximo, unidade ou distribuição documentada no OpenAPI; a pessoa usuária definiu vezes por semana em D03 |
| `daysOfWeek` | Array de `integer/int32` | Obrigatório | `minItems=1`; sem enum dos itens, faixa, `maxItems` ou `uniqueItems` |
| `startTime` | `string` | Opcional | Sem `format`, regex, exemplo, fuso ou precisão |
| `endTime` | `string` | Opcional | Mesmas lacunas; sem regra de comparação com início ou travessia de meia-noite |
| `estimatedLiters` | `number` | Opcional | OpenAPI sem mínimo, máximo, precisão, default, cálculo ou unidade temporal; D07–D10 definem total diário com média fixa e tabela inicial; regra diária e local pendentes |

Fonte da tabela: [OpenAPI](openapi.json), `CreateUserHabitRequest` e `UpdateUserHabitRequest`. Opcional significa ausente do array `required`; os tipos não incluem `null`, portanto não se deve tratar aceitação explícita de `null` como documentada. Não há `default` nesses campos.

**Diferença para o requisito futuro:** D12 exige horário obrigatório para cada execução; a opcionalidade dos horários na tabela acima descreve apenas o schema atual. A representação por execução e a decisão entre somente início ou intervalo ainda precisam entrar no contrato ampliado. Fonte: [D11/D12](decisions.md).

O `UserHabitIO` declara `id`, `userId`, `habitId`, `frequency` como `integer/int32`; `habitName`, `habitCategory`, `habitDescription`, `startTime`, `endTime` como `string`; `estimatedLiters` como `number`; `daysOfWeek` como array de `integer/int32`. Não declara nenhum campo como `required`, nem enum para categoria. Não há envelope, total ou metadados de paginação. Fonte: [OpenAPI](openapi.json), `UserHabitIO` e respostas das operações.

## Datas, enums e catálogo

- Não existe campo de data nos três schemas de hábitos. Não há formato de data de agendamento para documentar. Os horários são strings sem `format`; não afirmar que `HH:mm`, `HH:mm:ss`, offset ou ISO-8601 completo sejam o contrato confirmado. O fato de outros schemas da API usarem `format: date` não define datas de Rotina. Fonte: [OpenAPI](openapi.json).
- Nenhum enum de hábito, categoria ou recorrência é declarado nesses schemas. Não há `DAILY`, `WEEKLY`, RRULE ou enum de frequência. Fonte: [OpenAPI](openapi.json), schemas de hábitos.
- O asset do app mapeia dias como `1=segunda`, `2=terça`, `3=quarta`, `4=quinta`, `5=sexta`, `6=sábado`, `7=domingo`. Isso é catálogo local, não enum garantido pelo OpenAPI. Hábitos locais: `1=Banho longo`, `2=Lavar quintal`, `3=Lavar roupa`, `4=Regar plantas`, `5=Lavar carro`, `6=Lavar louça`. Fontes: [registration_catalog.json][catalog], [SignupOptions][options].
- A documentação de autenticação informa que esses IDs vieram de inserts fornecidos e precisam corresponder ao banco. O OpenAPI analisado não fornece endpoint de catálogo de tipos de hábito/dias. **INCERTO:** a correspondência com o ambiente atual não foi validada nesta etapa. Fontes: [AUTH_MOBILE.md][auth-doc], [OpenAPI](openapi.json).

## Recorrência: requisito confirmado e capacidade publicada

**Confirmado pela pessoa usuária nesta conversa:** `frequency` é quantas vezes o hábito acontece na semana; `daysOfWeek` indica os dias em que acontece. Quando houver cinco ocorrências entre segunda e quarta, deve aparecer um formulário para a pessoa distribuir as cinco entre esses dois dias. Fontes: respostas à análise em 28/10/2026, transcritas em [D03/D04](decisions.md).

**Capacidade publicada:** a API transporta uma frequência global e uma lista de IDs de dias. Não define um campo de quantidade por dia, múltiplos intervalos por dia, ocorrências concretas, início/fim de vigência, exceções, data única, fuso ou histórico de versões da rotina. Não há resposta já expandida por dia ou mês. Fonte: [OpenAPI](openapi.json), caminhos e schemas de hábitos.

**Decidido em 29/10/2026:** ampliar a API para receber, persistir e devolver a distribuição exigida. **PENDENTE:** definir estrutura, validações e compatibilidade com hábitos existentes. Não se assume que duplicar IDs em `daysOfWeek` represente quantidade ou que múltiplos registros do mesmo `habitId` sejam aceitos. A ausência de `uniqueItems` não define semântica para repetições; POST/PUT ainda declaram conflito de hábito já cadastrado. Fonte do contrato atual: [OpenAPI](openapi.json), schemas e respostas `409`; requisito e aprovação: [D04/D06](decisions.md).

**Requisito adicional de D11/D12:** exigir horário individual para cada execução. **INCERTO — consequência técnica:** a ampliação precisa representar esses horários obrigatórios e sua associação aos dias/execuções; os campos atuais de início/fim do registro inteiro não documentam essa associação. Não se assumiu lista de novos campos, formato, duração ou valores padrão para registros anteriores sem horário. Fontes: [D11/D12](decisions.md), [OpenAPI](openapi.json).

**INCERTO — alternativas para expansão:** o app pode projetar ocorrências a partir da distribuição devolvida pela API ampliada, ou o backend pode devolver ocorrências já expandidas. D06 aprova persistir a distribuição, mas não escolhe quem resolve a recorrência. O OpenAPI atual não fornece algoritmo suficiente para isso. Também faltam decisões sobre alterações retroativas e exclusão da série versus ocorrência. Fontes: [OpenAPI](openapi.json), [briefing](briefing.md), [decisões](decisions.md).

**Decidido em D07–D10:** os litros estimados são o total diário calculado do hábito, com média fixa por execução e tabela inicial aprovada; regra diária e local de cálculo continuam pendentes. **INCERTO — regra derivada proposta:** calcular o total diário como média por execução multiplicada pela quantidade de ocorrências daquele dia. Multiplicar esse total diário pronto novamente pela quantidade contaria o consumo duas vezes. A representação dos valores por dia na API ampliada, precisão e tratamento de referências/estimativas ausentes também continuam pendentes. O OpenAPI atual oferece um único `estimatedLiters` por registro e não confirma essa semântica no backend. Fontes: [D04 e D07–D10](decisions.md), [OpenAPI](openapi.json).

**INCERTO — proposta para o total da agenda:** somar os totais diários dos hábitos que ocorrem no dia selecionado, depois de definir como obter cada valor e tratar ausências. Não substituir consumo planejado por telemetria diária do imóvel; o briefing pede total dos hábitos. Fontes: [briefing](briefing.md), [D07](decisions.md#d07-litros-estimados-como-total-diário-do-hábito), [HOME_API.md][home-api].

### Evidência local de cálculo fora da API de Rotina

O repositório local da IA contém `estimate_habit_water_impact`, com valores fixos por execução em `_HABIT_LITERS_PER_SESSION` e cálculo de totais semanal e mensal pela frequência. Não recebe distribuição por dia, duração ou vazão; não cita no arquivo a fonte dos valores de referência. Não é um endpoint de cálculo de Rotina documentado no OpenAPI analisado. Fontes: [ferramentas de hábitos da IA][ai-habit-tools], [schemas do chat da IA][ai-schemas], [OpenAPI da Rotina](openapi.json). A leitura não comprova deploy. Em [D10](decisions.md#d10-tabela-inicial-de-litros-por-execução-aprovada), a pessoa usuária aprovou a tabela como estimativa inicial da Rotina: banho longo 80 L, lavar quintal 150 L, lavar roupa 100 L, regar plantas 15 L, lavar carro 200 L e lavar louça 20 L por execução. São parâmetros de estimativa, não medições. O local do cálculo permanece pendente.

## Erros de validação e reaproveitamento do tratamento

O OpenAPI descreve `400` como dados inválidos e `409` como duplicidade. Nas falhas `400/404/409` de POST/PUT e no `404` do GET de detalhe, referencia o próprio `UserHabitIO` como corpo, sem definir schema de erro, campos inválidos, códigos de negócio ou exemplo de mensagem. DELETE `404` não declara conteúdo. Não há erro `422` declarado nas operações de hábitos. Fonte: respostas em [OpenAPI](openapi.json).

Portanto, **não há formato utilizável de erro por campo nem exemplo real de response de validação comprovado**. Não inventar `errors`, `fieldErrors`, `detail` ou garantir `message`. A declaração de `UserHabitIO` em erro é uma lacuna do contrato; não comprova qual corpo chega em execução. Fonte: [OpenAPI](openapi.json).

O app já tem `ApiErrors.from(response)` e `ApiErrors.network()`, com mensagens por status, leitura de `Retry-After` em `429` e leitura filtrada de `message` exclusivamente em `400/409`. Esse parser não extrai mapa de erros por campo. Um JSON como `{"message":"Confira os dados informados."}` exemplifica um formato que o parser consegue ler nesses status, **não** um response confirmado de hábitos. Fonte: [ApiErrors][errors].

Reutilizar `ScreenState` e avisos existentes, mantendo validação associada ao campo quando houver regra confirmada. Atenção: `displayMessage()` retorna vazio no estado ERROR; o detalhe fica em `message`. O fluxo atual de Rotina passa `displayMessage()` ao aviso e fecha o diálogo imediatamente após disparar o salvamento. **INCERTO — consequência da leitura:** erros podem ficar sem texto e o rascunho não tem garantia de restauração após falha/rotação. Isso precisa ser tratado na implementação futura para cumprir o briefing, sem mudar silenciosamente o contrato global de `ScreenState`. Fontes: [ScreenState][state], [ScreenStateTest][state-test], [MainSectionFragment][fragment], [StatusNoticeView][notice], [briefing](briefing.md).

## Divergências atuais do Android

| Ponto | Evidência |
| --- | --- |
| Create/Update só modelam `habitId`, `frequency`, `daysOfWeek` | [ApiModels][models] |
| Response não modela `habitCategory`, `startTime`, `endTime`, `estimatedLiters` | [ApiModels][models] versus `UserHabitIO` no [OpenAPI](openapi.json) |
| Rotina mostra a coleção da conta, sem selecionar dia/mês, sem horário ou total | `renderRoutine` em [MainSectionFragment][fragment] e `SectionData.habits` em [SectionData][section-data] |
| CRUD já usa a sessão existente, verifica dono antes de editar/excluir e recarrega lista após sucesso | [SectionsViewModel][sections] |
| UI exige frequência positiva e ao menos um dia; não tem distribuição por dia | [MainSectionFragment][fragment], [SectionsViewModel][sections] |

O schema não impõe frequência positiva, embora o app já valide assim; não apresentar essa validação local como limite documentado do backend. Nenhuma adaptação de DTO ou fluxo foi implementada nesta análise. Fontes: [OpenAPI](openapi.json), [SectionsViewModel][sections]; escopo autorizado nesta conversa.

## Confirmações restantes após a especificação do recorte de leitura

1. **PENDENTE DE BACKEND:** implementar/publicar a estrutura de leitura acordada em D23 e fornecer OpenAPI oficial ampliado. O snapshot local ainda não contém `schedule`.
2. **PENDENTE DE BACKEND:** comprovar correspondência dos seis tipos e dias do catálogo de D21 no ambiente publicado.
3. **INCERTO para escrita:** schemas de POST/PUT, limites adicionais, omissão de opcionais/nulos, critério de duplicidade entre registros e erros reais por campo. A regra de horários repetidos de D22 não resolve conflito entre cadastros.
4. **INCERTO para escrita:** efeitos da edição/exclusão de série ou ocorrência e política de resultado incerto/timeout, incluindo mudanças por outro fluxo/dispositivo.
5. **INCERTO para UI:** calendário e fuso de “hoje”, formulário, tablet/paisagem e fontes/ações de metas, percentuais e alertas (P07–P10).

Distribuição, horário de início `HH:mm`, legado incompleto, projeção da semana padrão em qualquer data e cálculo diário no mobile foram especificados em D15–D23; deixam de ser decisões de leitura em aberto. Isso não elimina as lacunas técnicas do OpenAPI anterior descritas acima nem comprova deploy. Fontes: [decisões](decisions.md), [OpenAPI](openapi.json).

O [plano](plan.md) indica quando resolver as pendências restantes. Nenhuma etapa posterior foi implementada ou autorizada nesta entrega, e nenhum critério de aceite de tela foi marcado como concluído.

[readme]: ../../README.md
[api]: ../../app/src/main/java/com/aula/delta_mobile_app/services/api/ApiService.java
[models]: ../../app/src/main/java/com/aula/delta_mobile_app/model/ApiModels.java
[sections]: ../../app/src/main/java/com/aula/delta_mobile_app/viewmodel/SectionsViewModel.java
[fragment]: ../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/MainSectionFragment.java
[catalog]: ../../app/src/main/assets/registration_catalog.json
[options]: ../../app/src/main/java/com/aula/delta_mobile_app/repository/SignupOptions.java
[auth-doc]: ../../devdocs/fluxodavi/AUTH_MOBILE.md
[auth]: ../../app/src/main/java/com/aula/delta_mobile_app/repository/AuthRepository.java
[retrofit]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/RetrofitClient.java
[network]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/NetworkConfig.java
[interceptor]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/SessionInterceptor.java
[build]: ../../app/build.gradle.kts
[errors]: ../../app/src/main/java/com/aula/delta_mobile_app/services/network/ApiErrors.java
[state]: ../../app/src/main/java/com/aula/delta_mobile_app/model/ScreenState.java
[state-test]: ../../app/src/test/java/com/aula/delta_mobile_app/model/ScreenStateTest.java
[notice]: ../../app/src/main/java/com/aula/delta_mobile_app/view/StatusNoticeView.java
[reference-repository]: ../../app/src/main/java/com/aula/delta_mobile_app/repository/AbnormalConsumptionRepository.java
[home-api]: ../../devdocs/fluxodavi/HOME_API.md
[section-data]: ../../app/src/main/java/com/aula/delta_mobile_app/model/SectionData.java
[ai-habit-tools]: <C:/Users/joaopsouza-ieg/OneDrive - Instituto J&F/Área de Trabalho/2° SÉRIE/INTER/delta-artificial-intelligence/app/tools/habits/tools.py>
[ai-schemas]: <C:/Users/joaopsouza-ieg/OneDrive - Instituto J&F/Área de Trabalho/2° SÉRIE/INTER/delta-artificial-intelligence/app/schemas.py>
