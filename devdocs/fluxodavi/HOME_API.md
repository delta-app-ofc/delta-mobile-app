# Dados da Home

> Atualização de escopo: por solicitação explícita da pessoa usuária, o indicador EH foi removido do mobile. Metas agora usam [SQLite por residência](METAS_SQLITE.md). A solicitação posterior de calcular recomendações foi implementada com estimativas locais descritas em [METAS_CALCULOS.md](METAS_CALCULOS.md); essas fórmulas são escolhas desta implementação, não um contrato de indicadores publicado pela API.

O usuário vem de `GET /delta/auth/me`; os imóveis vêm de `GET /delta/user-properties/{userId}`. Não há ID fixo no aplicativo.

| Informação exibida | Fonte |
|---|---|
| Nome | /delta/auth/me |
| Consumo hoje e no mês | /delta/analytics/consumption-daily/{propertyId} |
| Custo previsto | /delta/property/{propertyId}/watercost?consumptionM3=...&referenceDate=... |

Os totais usam as datas das leituras e o mês atual. Litros são convertidos para m³ ao consultar o custo. A seleção anterior é mantida apenas quando pertence à conta; sem seleção, usa-se o primeiro imóvel válido. A escolha de imóvel está nas configurações, sem seletor no corpo da Home.

`HomeRepository` acessa perfil e custo pelo cliente autenticado; `WaterViewModel` fornece imóveis e leituras; `HomeViewModel` calcula os totais e invalida respostas de custo antigas. `HomeLoadState` exige o fim das requisições necessárias antes de revelar a tela. Falhas encerram o carregamento com mensagem e nova tentativa, sem simular dados.

Ranking, hábitos e variação mensal não são mais consultados pela Home, pois esses blocos não pertencem à referência. As demais telas mantêm seus próprios dados.

A série de temperatura ainda precisa de dados reais associados ao consumo. Metas e recomendações locais seguem os documentos acima. As referências antigas a EH, metas e recomendações sem fonte em [HOME_DATA_REQUIRED.md](HOME_DATA_REQUIRED.md) e [HOME_TODO.md](HOME_TODO.md) registram a etapa anterior.
