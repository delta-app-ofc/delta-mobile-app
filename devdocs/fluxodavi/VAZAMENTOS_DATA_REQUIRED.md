# Integração pendente — Detecção de vazamentos

O usuário confirmou em 17/10/2026 que os endpoints ainda não existem. A consulta ao Swagger público no mesmo dia não encontrou rotas de eventos, telemetria, análise horária ou configuração de alertas.

## O que está integrado

- Perfil e sessão autenticada existentes.
- Imóveis associados ao usuário por `/delta/user-properties/{userId}`.
- Medidores publicados em `/delta/device`, filtrados pelo imóvel autorizado e por `isActive`.
- Tela de detecção acessível em Consumo, com o imóvel preservado.
- Análises por horário e consumo anormal como destinos próprios, preservando o contexto.

A flag `isActive` informa cadastro ativo; não é tratada como prova de que o sensor está online.

## O que não está integrado

Fluxo atual, referência normal, eventos e seus status, volume acumulado, medições por horário, detecção em tempo real e alteração das preferências de alertas. O app não cria registros fictícios e não substitui fluxo instantâneo por consumo agregado diário.

O gráfico vazio contém apenas seus eixos. A interface não mostra 2,0 L/min, 600 L, “Alerta ativo” ou “Em investigação” sem receber um evento real. Há apenas um cartão de indisponibilidade quando não existe fonte de eventos, sem duplicar cartões de exemplo.

## Contrato a fornecer

| Informação | Definição necessária |
|---|---|
| Origem e autenticação | Rotas publicadas; uso da sessão atual; escopo do imóvel/medidor; exemplos de sucesso e erro. |
| Eventos | ID; imóvel; medidor; status e transições permitidas; início e fim; período consultado; ordenação. |
| Fluxo | Valor atual em L/min; data/hora da medição; referência normal e sua janela; idade máxima aceitável. |
| Série temporal | Timestamp com fuso/offset; valor e unidade; lacunas explícitas; regra para distinguir atraso, offline e fluxo zero. |
| Gráfico | Confirmar que o eixo vertical representa L/min. O título sugere fluxo, mas a imagem não contém unidade vertical. |
| Volume acumulado | Litros acumulados no evento; se aproximado ou exato; caso calculado, regra de integração de fluxo ao longo do tempo. |
| Detecção | Critério de vazamento e consumo anormal; limites; motor responsável; evitar algoritmo presumido no app. |
| Atualização | WebSocket, SSE ou consultas periódicas; intervalo; janela; reconexão; sinalização de dados antigos. |
| Recomendações | Prioridades e orientações contextuais; ações permitidas por status. |
| Alertas | Consultar/alterar preferência; escopo (evento/medidor/imóvel); duração; persistência; reativação; regras de confirmação. |
| Análises | Agregação por horário e registros anormais, filtros e paginação quando aplicável. |

## Preparação no app

`LeakMonitoringData` é um modelo de domínio da interface, não um DTO de servidor publicado. Seu método `monitored` valida a associação de eventos aos medidores ativos e pode receber dados da integração futura. Nenhum fluxo de produção chama esse método com dados de demonstração.

`LeakMonitoringRepository` usa somente as rotas reais de perfil, imóveis e dispositivos e retorna uma indisponibilidade explícita de telemetria. `LeakMonitoringViewModel` protege contra respostas atrasadas de outra seleção e reaproveita a consulta entre a tela e suas análises. Não existe polling, assinatura de sensor ou serviço em segundo plano simulando monitoramento.

O renderizador do gráfico usa timestamps reais, preserva medições de zero e quebra a linha em amostras ausentes/inválidas. Sua unidade planejada é L/min, a confirmar no contrato. Volume nunca é obtido somando taxas de fluxo.

“Desativar alertas” abre um diálogo informando a indisponibilidade; não altera preferências locais ou no servidor. As outras duas ações abrem orientações gerais de inspeção. O segundo botão usa o rótulo “Ver consumo anormal registrado”, cuja parte final está parcialmente encoberta na referência.

## Validação ainda necessária

Build Gradle completo, verificação visual em emulador/aparelho e testes autenticados com telemetria real. A implementação visual e os estados de indisponibilidade não equivalem a um detector de vazamentos funcional sem essa integração.
