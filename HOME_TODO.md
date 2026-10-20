# Home — implementação da referência enviada

Referência: [home.png](docs/references/home.png). Os números e o nome da imagem são exemplos visuais; a tela usa a conta autenticada e os dados reais.

## Estrutura e aparência

- [x] Reorganizar a Home na ordem da imagem: cabeçalho, chamada, informações gerais, EH e metas, recomendações e relação temperatura × litros.
- [x] Reproduzir o fundo escuro com iluminação suave inferior, azuis dos cartões, bordas, cantos e divisórias.
- [x] Ajustar espaçamentos, alinhamentos e hierarquia de tamanhos; permitir rolagem e crescimento dos textos.
- [x] Preservar o nome real do usuário, avatar, alertas e configurações.
- [x] Manter ícones de alertas e configurações legíveis em botões de 48 dp.
- [x] Retirar o seletor de imóvel do corpo da Home; preservar a troca de imóvel nas configurações.
- [x] Retirar ranking, mês de referência do ranking, hábitos, variação mensal, histórico por data e botão de medidor do corpo da Home. O medidor continua nas configurações.

## Informações gerais

- [x] Cartão horizontal com três colunas e divisórias: consumo hoje, consumo este mês e custo previsto.
- [x] Incluir ícones de gota/alerta, calendário e dinheiro.
- [x] Carregar consumo pelo imóvel associado à conta e calcular totais reais do dia e mês.
- [x] Consultar custo usando litros convertidos para m³, com moeda brasileira e sem valor fictício.

## Indicador EH e metas

- [x] Trocar o mostrador de posição no ranking por um indicador EH com escala /100, anel arredondado em gradiente e classificação.
- [x] Preparar o anel para pontuações reais entre 0 e 100; sem pontuação, mostrar traços e anel neutro.
- [x] Reproduzir o cartão de meta mensal com área de percentual, barra arredondada e “Ver mais”.
- [x] Reproduzir o cartão de sugestão de meta com título e “Ver mais”.
- [ ] Conectar EH, classificação, meta mensal, percentual e sugestão aos dados do backend. Contrato ainda ausente: ver HOME_DATA_REQUIRED.md.

## Recomendações

- [x] Cartão azul com lâmpada e “Para ficar dentro da meta”.
- [x] Linhas separadas com rótulos à esquerda e valores à direita: limite diário, redução e economia equivalente em banhos.
- [x] Aplicar verde ao limite diário, rosa à redução e branco à equivalência.
- [x] Remover o parágrafo extra de recomendações pendentes que não existe na referência.
- [ ] Conectar recomendações reais e regras de equivalência em banhos. Não inventar limites ou fórmulas.

## Gráfico

- [x] Exibir “Relação temperatura x litros”, eixos em °C e litros e suporte a curva branca com pontos azuis.
- [x] Manter o gráfico de consumo por data nas outras telas; não usá-lo como substituto do gráfico de temperatura na Home.
- [x] Tratar a ausência de série com eixos e traço neutro, sem desenhar uma curva fictícia.
- [ ] Conectar a série real de temperatura e litros após receber contrato e período de agregação.

## Menu

- [x] Preservar as cinco ações e a navegação existentes.
- [x] Substituir o fundo rasterizado por superfície desenhada com bordas arredondadas e rebaixo central.
- [x] Manter Home circular e elevada, sem quadrado preto.
- [x] Arredondar os ícones e corrigir o cifrão com desenho vetorial, traços contínuos e pontas arredondadas.

## Carregamento e validação

- [x] Ocultar Home e menu durante o carregamento inicial e ao trocar de imóvel.
- [x] Aguardar perfil, imóveis, consumo e custo dependente antes de revelar o conteúdo.
- [x] Permitir estados de erro e nova tentativa sem carregamento infinito.
- [x] Invalidar respostas de custo referentes à seleção anterior ou a leituras substituídas.
- [x] Executar cinco testes unitários da decisão de exibir a Home: todos passaram via JUnit direto.
- [x] Compilar e vincular recursos XML com AAPT2 usando dependências locais: sucesso.
- [x] Compilar os arquivos Java alterados com javac e símbolos gerados pelo AAPT2: sucesso.
- [ ] Concluir build Gradle e testes integrados. Ambiente falha com “Unable to establish loopback connection”, inclusive sem daemon e com outra instalação Java.
- [ ] Conferir captura real em emulador/dispositivo contra a referência em diferentes dimensões. Ainda não validado visualmente no aplicativo.
- [ ] Após receber os dados ausentes, incluir suas consultas na condição de carregamento e validar os números em uma conta real.

## Diagnóstico de dados vazios (17/10/2026)

- [x] Revalidar o Swagger público: campos de imóveis, consumo diário e custo correspondem aos DTOs do app.
- [x] Mover a mensagem de erro/ausência de leitura para logo abaixo de Informações gerais, onde fica visível sem chegar ao final da página.
- [x] Diferenciar resposta vazia, leituras sem volume/data válida, registros somente de meses anteriores e ausência de leitura de hoje; mostrar 0 L e custo zero em consultas bem-sucedidas sem consumo, preservando erros como indisponíveis.
- [ ] Verificar o status e a resposta autenticada de consumo para o imóvel da conta reportada. O usuário confirmou que a aba Consumo também está vazia; isso ainda não distingue retorno vazio de falha na requisição.

- [x] Aplicar a regra solicitada: sem registros no mês após uma consulta bem-sucedida, mostrar 0 L e custo previsto R$0; sem registros hoje, mostrar 0 L.
- [x] Preservar a distinção entre consumo zerado e consulta indisponível.

## Apresentação de avisos

- [x] Remover o aviso de ausência de leituras da Home: períodos sem registros mostram zero sem mensagem adicional.
- [x] Usar um componente único de aviso com fundo, borda, ícone e espaçamento para erros e estados que exigem ação.
- [x] Aplicar o componente em Home, Consumo/Custos/Rotina, login, cadastro, configuração de conta e leitor QR.
- [x] Ocultar completamente o componente quando não existir mensagem.
- [x] Manter mensagens de validação vinculadas aos campos e os avisos existentes em diálogos/Toasts.
