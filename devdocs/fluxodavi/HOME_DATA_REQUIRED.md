# Dados necessários para concluir a Home

A referência está em `docs/references/home.png`. Valores como 92, 87%, 218 L/dia, -82 L/dia e aproximadamente 4 banhos/dia não foram fixados na conta do usuário.

## Já disponíveis e conectados

| Informação | Fonte atual |
|---|---|
| Nome e identidade autenticada | GET /delta/auth/me |
| Imóveis pertencentes ao usuário | GET /delta/user-properties/{userId} |
| Consumo diário e total do mês | GET /delta/analytics/consumption-daily/{propertyId} |
| Custo previsto | GET /delta/property/{propertyId}/watercost, com consumptionM3 e referenceDate |

A Home respeita o imóvel já selecionado quando ele pertence à conta; caso contrário, usa o primeiro imóvel válido retornado. A troca continua disponível em Configurações → Selecionar imóvel. Quando a consulta termina com sucesso, dias e meses sem leituras mostram 0 L; com consumo mensal zero, o custo previsto mostra R$0 sem consultar tarifa. Erros e carregamento permanecem indisponíveis, sem serem convertidos em zero.

## Enviar endpoints, exemplos de resposta e regras destes campos

| Bloco | Campos e definições necessários |
|---|---|
| Indicador EH | Pontuação de 0 a 100; classificação textual (ex.: Excelente!); usuário/imóvel; período de referência; limites das classificações se calculadas no app. |
| Meta mensal | Meta em litros; mês; percentual de progresso; definição do percentual (consumo utilizado, economia ou outro indicador); comportamento acima de 100%; identificação do imóvel. |
| Sugestão de meta | Valor sugerido, unidade, período e conteúdo de detalhes a exibir em “Ver mais”. |
| Limite diário | Limite em litros/dia; data/período de referência; regra de cálculo se não vier pronto do servidor. |
| Redução necessária | Litros/dia; convenção de sinal (a imagem usa valor negativo); comparação utilizada; comportamento quando a conta já está dentro da meta. |
| Economia em banhos | Quantidade de banhos/dia ou litros por banho, duração e vazão de referência, regra de arredondamento e eventual aproximação. |
| Temperatura × litros | Série de pares temperatura em °C e consumo em litros; data/período; imóvel; fonte da temperatura; agregação de leituras que tenham a mesma temperatura. |
| Alertas | Endpoint, contagem de não lidos e conteúdo/ação ao tocar; atualmente a ação informa indisponibilidade. |

O endpoint `residential-efficiency-ranking` entrega posição (`rankEfficiency`) e percentil (`consumptionPercentRank`), não uma pontuação EH definida. A Home não converte esses campos em 92/100 sem uma regra confirmada.

## Estado atual

Os blocos visuais existem, mas EH, metas e recomendações exibem “—”; a barra de meta fica neutra. O gráfico mostra somente os eixos e o estado vazio até existir uma série válida. O renderizador do gráfico aceita pontos reais de temperatura e litros e o indicador aceita pontuação e classificação, mas suas fontes não estão conectadas porque não constam no contrato local.

Ao receber o contrato, conectar os dados ao ViewModel, validar identidade e período e aguardar essas novas requisições antes de revelar a Home. Não criar rotas presumidas ou dados de demonstração no fluxo autenticado.
