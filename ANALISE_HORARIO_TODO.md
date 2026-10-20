# TO-DO — Análise por horário

Referência visual: [analise-por-horario.png](docs/references/analise-por-horario.png).

Escopo: criar a tela da imagem no destino do botão “Ver análise por horário”, dentro de Detecção de vazamentos. A interface dedicada está implementada. A integração dos indicadores aguarda os contratos de telemetria ainda inexistentes. Os números, horários e descrições da imagem são exemplos visuais, não dados fixos do usuário.

## 1. Navegação e contexto

- [x] Aproveitar o destino `leakHourlyFragment`, já ligado ao botão em Detecção de vazamentos.
- [x] Substituir a apresentação provisória de análise por esta interface específica, sem alterar a tela de consumo anormal.
- [x] Separar o layout/controlador da análise horária quando necessário, pois hoje os dois destinos compartilham `LeakAnalysisFragment`.
- [ ] Concluir a preservação de imóvel, medidor, evento selecionado, data de referência e fuso: imóvel e evento já são preservados; os demais metadados horários aguardam o contrato.
- [x] Implementar a seta de voltar no topo e o retorno pelo sistema, restaurando a seleção anterior.
- [x] Preservar o menu flutuante existente, com ícones arredondados, cifrão corrigido, Home circular elevada e gota selecionada.
- [x] Manter a gota abrindo diretamente Detecção de vazamentos.
- [x] Garantir rolagem para que o menu não bloqueie os valores e itens do cartão inferior.

## 2. Aparência e cabeçalho

- [x] Reproduzir fundo escuro e iluminação suave azul/cinza da referência.
- [x] Incluir a seta branca de retorno com área de toque adequada.
- [x] Implementar “Análise por horário” e “Distribuição de consumo ao longo do dia” com tamanhos e cores da imagem.
- [x] Reproduzir margens, espaçamento vertical, cantos, bordas, tipografia e alinhamento dos componentes.
- [x] Usar ícones vetoriais nítidos, incluindo relógio e informação.
- [x] Adaptar a composição a telas estreitas e fonte ampliada sem cortar informações.

## 3. Heatmap diário

- [x] Reproduzir o cartão em gradiente azul com cantos arredondados.
- [x] Incluir o ícone de relógio, “Heatmap diário” e a cápsula escura “4h em 4h”.
- [x] Tratar a cápsula como indicação de intervalo; não inventar interação de filtro que a referência não define.
- [x] Criar duas linhas de seis células, reproduzindo cores, dimensões, cantos e espaçamento da imagem.
- [x] Posicionar “Hoje”, “Ontem” e os rótulos de horário conforme a referência, mantendo associação acessível a cada linha.
- [ ] Aplicar intensidade azul: mais escuro significa maior consumo, com escala consistente entre hoje e ontem.
- [ ] Definir os limites e a normalização de cor pelo contrato ou regra de produto; não gerar intensidade aleatória.
- [x] Expor o período e o consumo de cada célula por toque e acessibilidade, em tooltip, diálogo ou componente próprio.
- [x] Incluir a legenda com ícone “Mais escuro = maior consumo”, vinculada visual e semanticamente ao heatmap.
- [ ] Confirmar a divisão temporal: a cápsula indica quatro horas, mas os rótulos da imagem são 0h, 5h, 10h, 15h, 20h e 23h. Não usar esses rótulos como limites de agregação sem uma definição.
- [ ] Definir se hoje inclui apenas intervalos concluídos e como apresentar períodos futuros.
- [x] Tratar intervalo confirmado sem consumo como 0 e cor mínima; ausência de medição como indisponível, distinguível de zero.
- [ ] Definir datas pelo fuso da medição e tratar mudança de dia e dias de duração diferente, sem comparar janelas desalinhadas.

## 4. Picos identificados

- [x] Implementar o título “Picos identificados”.
- [x] Reproduzir os cartões escuros com borda fina cinza e cantos arredondados.
- [x] Posicionar intervalo e período do dia em destaque à esquerda e volume em litros à direita.
- [x] Exibir descrição do contexto abaixo do intervalo e cápsula clara de classificação sob o volume.
- [x] Reproduzir as variantes “Pico alto” e “Pico mais alto” com texto vermelho.
- [x] Renderizar a lista de picos reais; não fixar dois picos nem os valores 45 L e 80 L.
- [ ] Definir critérios de detecção, classificação, ordenação e agrupamento de picos, respeitando intervalos de durações diferentes.
- [x] Usar descrições de atividade fornecidas por fonte confirmada; não concluir que ocorreu banho, café ou preparo de refeições apenas pelo consumo total.
- [ ] Confirmar o texto do pico noturno: a imagem associa “19h–21h - Noite” a uma descrição que contém “Banho matinal”. Não transformar esse exemplo em descrição automática.
- [x] Tratar nenhum pico com estado visual próprio; não criar um cartão de pico fictício com volume zero.

## 5. Uso simultâneo — vazão registrada

- [x] Implementar o título “Uso simultâneo”.
- [x] Reproduzir o cartão azul acinzentado com “Vazão registrada(pico)”, a linha de comparação e o valor à direita.
- [x] Exibir a vazão real de pico em L/min, separada do volume em litros dos cartões anteriores.
- [ ] Carregar a referência comparativa e o percentual de aumento com regra de cálculo confirmada.
- [x] Reproduzir a hierarquia visual do exemplo “Média anormal: 8L/min + 213%”, sem fixar os números.
- [ ] Confirmar o significado de “Média anormal”: média histórica, referência normal ou outro indicador.
- [x] Tratar referência zero/ausente sem divisão por zero ou percentual inventado.
- [x] Diferenciar pico medido de soma estimada de equipamentos.

## 6. Uso simultâneo — equipamentos

- [x] Reproduzir o cartão inferior azul, divisórias, cápsulas claras, nomes à esquerda e vazões à direita.
- [ ] Confirmar o título do cartão: ele está encoberto/desfocado pelo menu na referência, então não presumir seu texto final.
- [x] Renderizar linhas de equipamentos conforme os dados recebidos. Chuveiro, torneira da cozinha e máquina de lavar são os exemplos da imagem.
- [x] Usar o símbolo de aproximação quando o valor for estimado, conforme a origem dos dados.
- [x] Reproduzir “Total estimado:” com o valor total alinhado à direita.
- [ ] Definir a fonte da estimativa e se a identificação dos equipamentos é medida, configurada pelo usuário ou inferida pelo backend.
- [x] Não derivar uso simultâneo ou identificar aparelhos com base somente no consumo agregado diário ou em hábitos cadastrados.
- [ ] Somar apenas vazões pertencentes ao mesmo intervalo simultâneo, com unidades consistentes e regra de arredondamento confirmada.
- [x] Não ajustar estimativas artificialmente para igualar o pico registrado.
- [x] Não exibir os exemplos ~10, ~6 e ~9 L/min nem o total 25 L/min como dados reais.

## 7. Dados e integração

O app já preserva o destino e o contexto do evento, mas o backend atual não fornece as medições intradiárias e análises desta tela. O usuário confirmou que os endpoints de telemetria ainda não existem. O endpoint de consumo diário não permite reconstruir este heatmap, detectar horários de pico ou identificar equipamentos simultâneos.

- [ ] Receber endpoints e JSONs para consumo por intervalo, picos, comparação de vazão e uso simultâneo.
- [ ] Receber imóvel, medidor, evento, data de referência, fuso, início/fim de cada intervalo, unidade e estado de qualidade da medição.
- [ ] Receber os dados de hoje e ontem com janelas comparáveis, incluindo a distinção entre zero, período futuro, dado ausente e falha.
- [ ] Receber as regras da escala do heatmap e as definições dos intervalos.
- [ ] Receber critérios de classificação de picos e origem de suas descrições.
- [ ] Receber a referência de vazão e a regra do percentual comparativo.
- [ ] Receber a origem das estimativas de equipamentos e do total simultâneo.
- [ ] Definir atualização, validade dos dados, reconexão e política de cache.
- [ ] Criar modelos, repositório e ViewModel conforme o contrato real, usando a autenticação existente.
- [ ] Validar propriedade dos dados e descartar respostas atrasadas de outro imóvel, evento ou dia.
- [ ] Preservar seleção e estado em rotação/retorno, sem duplicar requisições ou assinaturas.

## 8. Carregamento e estados da interface

- [x] Usar um único carregamento inicial enquanto os dados necessários são preparados, sem encadear telas de loading.
- [x] Não exibir o conteúdo se preenchendo bloco por bloco no primeiro acesso.
- [ ] Atualizar dados posteriores sem piscar a tela ou reiniciar o loading de toda a página.
- [x] Quando a fonte existir e confirmar período sem consumo, mostrar 0 nos indicadores aplicáveis.
- [x] Quando o contrato/fonte não existir ou a consulta falhar, manter o campo indisponível; não transformar falta de telemetria em zero.
- [x] Nunca mostrar mensagens de erro, diagnóstico ou nova tentativa como texto solto na página.
- [x] Usar `StatusNoticeView`, cards, diálogos ou barras de aviso para feedback necessário e ocultá-los quando vazios.
- [x] Manter títulos, rótulos e a legenda do heatmap como partes dos componentes da referência; não adicionar parágrafos de diagnóstico entre seções.
- [x] Permitir nova tentativa em erros e indicar dados antigos em componente próprio, preservando o contexto.

## 9. Validação

- [ ] Comparar capturas do app com a referência em diferentes tamanhos de tela: cores, células, ícones, bordas, tipografia e alinhamento.
- [ ] Conferir voltar, entrada pelo botão correto, menu ativo, rolagem e acesso ao fim do cartão inferior.
- [ ] Testar hoje/ontem, virada do dia, fuso, intervalo parcial e lacunas de medição.
- [ ] Testar heatmap uniforme, todos os intervalos zerados, intensidades diferentes e períodos indisponíveis.
- [ ] Testar nenhum pico, um pico, vários picos, classificação e dados de outro imóvel/medidor.
- [ ] Testar unidades, referência zero, percentuais, aproximações, arredondamento e total simultâneo.
- [ ] Testar erros, reconexão, cache, respostas fora de ordem, rotação e retorno à tela anterior.
- [ ] Validar acessibilidade sem depender apenas da cor, com leitura do intervalo e consumo de cada célula.
- [ ] Executar build e testes apropriados; registrar limitações do ambiente e verificar a instalação no Android 11 ou superior.
- [ ] Validar números em uma conta com telemetria real antes de declarar concluída a integração.

## Informações pendentes para começar a integração

Contratos da API; intervalos reais do heatmap; unidade e qualidade dos registros; regra de intensidade; detecção/classificação dos picos; origem das atividades; referência do percentual; origem das estimativas de equipamentos; título encoberto do cartão inferior.

## Entrega visual — 17/10/2026

- Novo `HourlyAnalysisFragment` e layout dedicado, mantendo o destino `leakHourlyFragment` do botão existente. A apresentação de consumo anormal segue em seu destino anterior.
- Cabeçalho com seta, heatmap de duas linhas, legenda, picos, vazão de pico e cartão de equipamentos implementados.
- Campos sem fonte permanecem indisponíveis. O estado vazio de picos é um cartão próprio, sem gerar dois eventos fictícios.
- Hoje/ontem mantêm células ausentes distintas de medições reais de zero; células futuras também têm estado próprio.
- As células abrem detalhes em diálogo. Mensagens de erro usam `StatusNoticeView` e botão de nova tentativa.
- As três categorias de equipamento da referência são rótulos de campos ainda sem estimativa; não representam aparelhos detectados ou ligados na residência.
- O título inferior foi adotado como “Estimativa por equipamento”, pois o original está encoberto. Sua confirmação continua pendente.
- O selo “4h em 4h” e os rótulos 0h/5h/10h/15h/20h/23h foram mantidos da referência somente na apresentação. Não existe agregação baseada nesses rótulos.
- Intensidade de consumo só usa uma classificação recebida no modelo de domínio. Não há escala automática presumida para dados reais.
- O modelo aceita valores de picos e estimativas do futuro backend; volume e vazão têm campos separados. O total estimado não é ajustado ao pico medido.
- A comparação percentual implementa a relação matemática entre pico e referência quando ambos existem; o significado da referência e a regra final de produto ainda precisam ser confirmados antes da integração.
- Não foram criadas rotas, simulação de consumo ou inferências a partir dos agregados diários.
- Pendentes: endpoints, regras e dados reais; confirmação dos textos/intervalos; build completo e avaliação visual em aparelho.

## Verificação desta etapa

- [x] Recursos compilados e vinculados com AAPT2 usando as dependências locais.
- [x] Todas as fontes Java do app compiladas separadamente com javac e os símbolos de recursos atualizados.
- [x] 27 testes JVM passaram, incluindo sete novos testes para ausência de fonte, zero medido, futuro, volume inválido, percentual, posição das células e intensidade desconhecida.
- [ ] Concluir build Gradle, teste instrumentado e comparação visual no Android 11/aparelho. O ambiente continua com a limitação de loopback do Gradle registrada nas etapas anteriores.
