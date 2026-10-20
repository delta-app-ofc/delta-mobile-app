# Dados pendentes — Análise por horário

A tela e os renderizadores foram criados com base em `docs/references/analise-por-horario.png`. O contexto vem do imóvel/evento selecionado no monitoramento. As APIs existentes só validam imóveis e medidores; não fornecem os indicadores horários.

`HourlyAnalysisData` é um modelo de domínio, não um DTO de backend publicado. A interface usa `unavailable()` até existir uma fonte confirmada. Nenhum número da referência é usado como medição da conta.

## Necessário para conectar

- Consumo de hoje/ontem por intervalo, em litros, com datas, fuso, limites e qualidade de cada medição.
- Resolver a inconsistência visual entre “4h em 4h” e rótulos de 0h, 5h, 10h, 15h, 20h, 23h.
- Regra de intensidade comparável entre os dias, incluindo ausência e intervalos futuros.
- Lista de picos: intervalo, volume, classificação e origem da descrição de atividade. Não inferir banho ou refeições de um volume agregado.
- Vazão de pico, referência comparativa e regra do percentual. Referência zero não produz divisão por zero.
- Estimativas de equipamentos, origem da identificação, intervalo de simultaneidade e total estimado. Não igualar estimativa ao valor medido artificialmente.
- Título real do cartão inferior, atualmente coberto pelo menu na referência.
- Política de atualização, cache, reconexão e validade dos dados.

Com resposta bem-sucedida que confirme consumo zero, o modelo preserva 0 e intensidade mínima. Fonte ausente, dado inválido, período futuro e falha de consulta não são convertidos em zero. Uma lista de picos vazia com fonte disponível é diferente de fonte indisponível.

O renderizador de linhas aceita equipamentos do modelo. As categorias Chuveiro/Torneira cozinha/Máquina de lavar só são campos de referência no estado sem fonte, com valores “—”. Não há detecção de equipamentos implementada.

A flag de disponibilidade do modelo só deve ser ativada após confirmar as fontes de todos os blocos da resposta. Se as futuras APIs forem independentes, integrar estados por bloco antes de tratar uma lista vazia de picos como ausência confirmada.
