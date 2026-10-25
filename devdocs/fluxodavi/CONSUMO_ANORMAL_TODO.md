# TO-DO — Consumo anormal registrado

Referência: [consumo-anormal.png](../../docs/references/consumo-anormal.png).

Escopo: criar a interface da imagem no botão “Ver consumo anormal registrado”, da tela Detecção de vazamentos. A interface dedicada e os históricos disponíveis foram implementados; os itens sem marcação dependem de contrato, regras de negócio ou validação no aparelho. Os números e conclusões da imagem não são dados reais da conta.

## 1. Entrada e navegação

- [x] Aproveitar o destino `leakAbnormalFragment`, já conectado ao botão correto em Detecção de vazamentos.
- [x] Substituir a apresentação provisória de `LeakAnalysisFragment` por uma tela dedicada de consumo anormal.
- [ ] Preservar imóvel, medidor, evento/anomalia selecionados, período e fuso quando disponíveis no contrato.
- [x] Implementar a seta de voltar no topo e o retorno pelo sistema para a detecção, restaurando seu contexto.
- [x] Preservar o menu flutuante e seus ícones arredondados, cifrão corrigido e Home circular elevada.
- [x] Manter a gota selecionada nesta tela e abrindo diretamente Detecção de vazamentos.
- [x] Permitir rolagem e reservar espaço para que o menu não impeça acesso às abas, gráfico ou cartão inferior.

## 2. Aparência geral

- [x] Reproduzir fundo escuro, iluminação suave e composição vertical da referência.
- [ ] Implementar a seta branca, o título “Consumo anormal - Detectado” e o subtítulo da imagem no estado de detecção confirmada.
- [x] Exibir “A IA identificou comportamento fora do padrão” somente se houver detecção real atribuída a esse mecanismo; não anunciar uma IA inexistente.
- [x] Definir título/subtítulo neutros para ausência de anomalia, fonte indisponível ou erro, sem apresentar uma detecção fictícia.
- [ ] Reproduzir tipografia, margens, alinhamentos, azuis, vermelho dos alertas, bordas e cantos arredondados.
- [ ] Usar ícones vetoriais para retorno, alerta, relógio e gráfico de barras.
- [ ] Adaptar textos e componentes a telas estreitas e fonte ampliada sem cortar valores ou unidades.

## 3. Resumo da anomalia

- [ ] Reproduzir o cartão azul em gradiente com símbolo de alerta vermelho dentro de um círculo claro.
- [ ] Mostrar nível da anomalia e descrição de persistência retornados pela fonte, como o exemplo “Três dias consecutivos acima do padrão”.
- [x] Não fixar “Anomalia nível 2” nem concluir severidade sem regra confirmada.
- [x] Criar linhas com rótulos à esquerda, valores à direita e divisórias: média histórica, consumo detectado, variação e causa identificada.
- [ ] Formatar média e consumo em L/dia, preservando a unidade real do contrato.
- [ ] Reproduzir a cápsula clara de variação com texto vermelho e sinal quando aplicável.
- [ ] Reproduzir a cápsula da causa, exibindo “Desconhecida” apenas se a anomalia existir e sua causa for de fato desconhecida.
- [x] Distinguir causa desconhecida de ausência completa de diagnóstico/dados.
- [ ] Confirmar o cálculo da variação: na imagem, 300 L/dia e 500 L/dia não correspondem a +117% pela fórmula usual de aumento relativo; essa combinação não deve ser copiada como dado ou regra.
- [x] Tratar referência zero e valores ausentes sem divisão por zero ou percentual inventado.
- [ ] Mostrar 0 em métricas de consumo quando a fonte confirmar ausência de consumo; não criar uma anomalia zerada para substituir ausência de eventos.

## 4. Linha dos últimos sete dias

- [x] Reproduzir o cartão escuro com borda fina e título “Linha dos últimos 7 dias”.
- [ ] Desenhar a curva/linha vermelha do consumo, a linha-base e a referência histórica tracejada em cinza.
- [ ] Posicionar o rótulo da média e a anotação “anomalia →” conforme a série e o evento reais.
- [x] Não desenhar a anotação de anomalia sem detecção confirmada nem copiar o formato da linha de exemplo.
- [ ] Usar sete dias de calendário no fuso correto, incluindo o dia de referência conforme a regra aprovada; não confundir sete dias com sete últimos registros.
- [x] Posicionar pontos pela data real, ordenar e tratar registros repetidos segundo o contrato.
- [x] Distinguir dia confirmado com consumo 0 de dia sem medição; não unir lacunas como se houvesse dados contínuos.
- [x] Ajustar a escala aos valores reais e à média, tratando séries zeradas ou constantes sem divisão por zero.
- [x] Identificar unidades e datas por acessibilidade e detalhe ao tocar, sem inserir parágrafos de diagnóstico soltos.

## 5. Comparação de períodos

- [x] Reproduzir “Comparação de Períodos” e “Análise de variações no consumo”.
- [x] Criar as abas/cápsulas Semanal, Mensal e Anual, com seleção azul preenchida e demais opções escuras com borda azul clara.
- [x] Iniciar em Semanal conforme a referência e preservar a seleção ao voltar ou girar a tela.
- [x] Fazer cada aba atualizar título, intervalos, barras, totais e comparação do mesmo imóvel; não alterar somente o destaque visual.
- [ ] Definir limites de semana, mês e ano, data de referência e fuso conforme a regra de produto/backend.
- [x] Definir comparação de períodos atuais parciais com períodos anteriores, evitando comparar uma semana incompleta a uma completa sem identificação.
- [x] Descartar respostas atrasadas de uma aba anterior e impedir requisições duplicadas.

## 6. Gráfico de barras comparativo

- [x] Reproduzir o cartão escuro arredondado, borda fina, ícone de relógio e título do consumo no período.
- [ ] Confirmar o selo “4h em 4h”: ele aparece na referência do gráfico semanal, mas sua relação com agregados semanais/mensais/anuais não está definida.
- [ ] Criar quatro barras, com períodos anteriores em tons de azul e período atual em vermelho no estado de anomalia confirmado.
- [ ] Para Semanal, reproduzir os rótulos S-4, S-3, S-2 e Atual da imagem, após confirmar quais semanas representam. Não presumir que S-4 é a semana imediatamente anterior.
- [x] Definir os rótulos correspondentes de Mensal e Anual com os períodos reais.
- [x] Escalar altura por volume em litros, com escala comum entre barras da mesma visualização.
- [x] Exibir total anterior, total atual e variação com formatação brasileira, sem fixar 1.800 L ou 2.100 L.
- [ ] Confirmar o significado de “Anterior”: período imediatamente anterior, média dos anteriores ou outro agregado.
- [x] Usar vermelho e destaque de anomalia conforme a classificação real; não sinalizar consumo anormal somente porque há uma barra atual.
- [ ] Tratar séries incompletas, valores zero, fonte indisponível e referência zero de forma distinta.
- [x] Fornecer descrição/detalhe acessível de cada barra com período, total e eventual comparação.

## 7. Dias com maior consumo

- [x] Reproduzir o cartão azul em gradiente, ícone, título “Dias com maior consumo”, divisórias e cápsulas claras à direita.
- [ ] Renderizar o agrupamento por dia da semana a partir de uma análise real, sem fixar Domingos ou +35%.
- [ ] Definir amostra, período, referência e regra da comparação “vs média”.
- [ ] Renderizar a relação com temperatura apenas com dados de temperatura associados ao mesmo imóvel/período.
- [x] Não inferir “Dias >30°C” a partir de consumo ou data, nem fixar +52% vs 20°C.
- [ ] Definir faixas de temperatura, referência e amostra mínima antes de mostrar correlações/percentuais.
- [ ] Manter as cores da referência: variação de dias em vermelho e comparação térmica em amarelo/oliva, com rótulos legíveis sem depender somente da cor.
- [x] Tratar ausência de base estatística em componente próprio, sem apresentar relação causal não comprovada.

## 8. Dados e integração

O destino de navegação já existe. O contrato atual publica consumo diário e variação mensal, que precisam ser avaliados para os históricos reais. Não existe fonte confirmada para diagnóstico de anomalia por IA, severidade, causa, temperatura ou todos os comparativos da referência. O consumo diário isolado não comprova essas conclusões.

- [ ] Receber endpoints e JSONs para o diagnóstico: anomalia, nível, status, persistência, período, causa e origem da detecção.
- [ ] Receber média histórica e consumo detectado com unidade, janela de cálculo e fórmula da variação.
- [ ] Verificar se `consumption-daily/{propertyId}` fornece cobertura suficiente para os sete dias e comparações, usando a resposta completa em vez do histórico truncado da Home.
- [ ] Avaliar `monthly-variation/{propertyId}` para os comparativos mensais, sem assumir cobertura para semanas ou anos.
- [ ] Receber contratos ou regras aprovadas para agregações semanais, mensais e anuais e alinhamento de períodos.
- [ ] Receber análise por dia da semana e dados/estatísticas de temperatura quando disponíveis.
- [ ] Validar identidade, imóvel, medidor, evento, unidades, datas, fuso e qualidade da medição.
- [x] Criar modelos, repositório e ViewModel conforme o contrato real, reutilizando autenticação e fontes existentes quando adequadas.
- [x] Não criar endpoints presumidos, pontuações, causas, classificações ou respostas de IA simuladas.
- [ ] Definir atualização, validade, cache, reconexão e comportamento ao trocar imóvel, evento ou aba.

## 9. Carregamento e mensagens

- [x] Usar um único carregamento inicial enquanto o conjunto necessário é preparado; não encadear telas de loading.
- [x] Não exibir o conteúdo sendo preenchido bloco a bloco no primeiro acesso.
- [ ] Nas trocas de aba, usar feedback dentro do componente de comparação, preservando o restante da tela e evitando saltos.
- [ ] Mostrar 0 quando uma fonte existente confirmar período sem consumo; manter indisponível quando não houver fonte, medição ou ocorrer erro.
- [x] Distinguir “nenhuma anomalia detectada” de “não foi possível consultar o diagnóstico”.
- [x] Nunca apresentar erro, diagnóstico técnico ou instrução de nova tentativa como texto solto entre seções.
- [x] Reutilizar `StatusNoticeView`, cards, diálogos ou barras de aviso para feedback necessário; ocultar componentes vazios.
- [x] Preservar títulos, legendas e rótulos da referência como partes de seus componentes e não adicionar avisos desnecessários.

## 10. Testes e conclusão

- [ ] Comparar capturas com a referência: ícones, gradientes, bordas, cápsulas, tipografia, espaçamento e menu.
- [ ] Testar entrada pelo botão correto, voltar, menu ativo, rolagem e preservação do contexto.
- [ ] Testar anomalia confirmada, nenhuma anomalia, diagnóstico indisponível, severidade e causa desconhecida.
- [x] Testar percentuais com referência positiva, zero e ausente, sinal, arredondamento e unidades.
- [x] Testar sete dias de calendário, lacunas, duplicidades, séries zeradas e registros fora de ordem.
- [ ] Testar as três abas, limites de período, períodos parciais e respostas atrasadas após troca de seleção.
- [ ] Testar agrupamentos por dia da semana e temperatura somente com uma base de dados válida.
- [ ] Testar falha inicial, nova tentativa, cache, reconexão, rotação e recriação do processo.
- [ ] Validar acessibilidade dos gráficos e cápsulas, fontes ampliadas e diferentes dimensões de tela.
- [ ] Executar build e testes apropriados e verificar instalação no Android 11 ou superior, registrando limitações do ambiente.
- [ ] Validar os números e a origem do diagnóstico com uma conta real antes de declarar a integração concluída.

## Informações pendentes

Contrato do diagnóstico; significado de nível/persistência/causa; origem real da IA; fórmula de variação; cobertura dos históricos; regras e rótulos dos comparativos; significado de “4h em 4h” no gráfico; fontes e critérios das análises por dia da semana e temperatura.

## Execução e validação — 17/10/2026

Implementados layout dedicado, navegação, gráficos acessíveis, abas com agregação local e consumo diário completo do imóvel autenticado. Trocar abas é imediato, sem requisição ou novo loading. Diagnóstico e correlações permanecem indisponíveis, sem valores simulados.

Recursos compilados/vinculados com AAPT2 e todas as fontes Java compiladas com javac: OK. JUnit: 35 testes aprovados, incluindo 8 de agregação de consumo anormal. Build Gradle/instalação e comparação visual no Android 11 continuam pendentes devido ao bloqueio de loopback do ambiente já identificado.

Regras provisórias, dados necessários e limitações: [CONSUMO_ANORMAL_DATA_REQUIRED.md](CONSUMO_ANORMAL_DATA_REQUIRED.md). Itens marcados indicam implementação; não substituem validação visual ou integração de diagnóstico.
