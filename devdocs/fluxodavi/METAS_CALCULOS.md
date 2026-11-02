# Recomendações da meta mensal

Origem: solicitação explícita de calcular as metas e atualizar os valores quando o consumo mudar. As fórmulas abaixo são escolhas desta implementação; não representam indicadores publicados pelo backend.

Fontes: meta atual da residência no SQLite; consumo acumulado do mês em `HomeDashboard.monthLiters`; data local do aparelho. Nenhuma tarifa ou equivalência com banho/equipamento foi acrescentada.

- Limite diário: `max(meta - consumo, 0) / dias restantes`, incluindo hoje. Arredondamento para baixo em duas casas evita recomendar mais do que o saldo.
- Projeção mensal: `consumo / dia do mês × quantidade de dias no mês`. É uma estimativa pelo ritmo acumulado, sujeita à disponibilidade/atualização das leituras diárias e ao dia atual ainda não encerrado.
- Economia estimada em litros/mês: `max(projeção - meta, 0)`.
- Redução necessária: `economia / projeção × 100`, ou zero quando a projeção é zero.
- Se o consumo acumulado ultrapassar a meta, o título indica isso e o limite diário é zero. A projeção não significa que consumo já ocorrido possa ser desfeito.

Sem meta ou com consumo indisponível (`null`), os valores ficam indisponíveis. Resposta vazia bem-sucedida segue o contrato já existente de `HomeDashboard`: consumo zero; falha ou carregamento não vira zero.

`MonthlyGoalCard` observa os totais da Home, relê a meta no retorno à tela e invalida resultados antigos de outra residência/conta. `HomeFragment` consulta o consumo ao entrar e a cada 60 segundos enquanto estiver RESUMED. `WaterViewModel` evita consultas concorrentes, respeita Retry-After, descarta respostas antigas e não publica novamente uma lista inalterada no mesmo dia. A atualização periódica preserva o conteúdo durante a consulta, sem reabrir o loading geral.

Fontes: [MonthlyGoalGuidance.java](../../app/src/main/java/com/aula/delta_mobile_app/model/MonthlyGoalGuidance.java), [MonthlyGoalCard.java](../../app/src/main/java/com/aula/delta_mobile_app/view/MonthlyGoalCard.java), [HomeFragment.java](../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/HomeFragment.java), [WaterViewModel.java](../../app/src/main/java/com/aula/delta_mobile_app/viewmodel/WaterViewModel.java).

Validação: cinco testes específicos cobrem alteração do consumo, consumo zero, meta ultrapassada, fim do mês/ano bissexto e dados ausentes/inválidos. As classes e recursos foram compilados isoladamente; a consulta periódica e a apresentação ainda precisam ser conferidas no aparelho.
