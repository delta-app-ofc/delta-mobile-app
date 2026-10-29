# Dashboard — referência dash.png

Referência: [dash.png](../../docs/references/dash.png). Tela independente de Custos, acessada pelo ícone do cifrão. A Home original continua no ícone de casa e como destino inicial; preserva autenticação, seleção de imóvel, consumo, custo e os cinco destinos do menu.

## Estrutura e aparência
- [x] Fundo #222222, rolagem, margens laterais de 25 dp e espaço final para o menu flutuante.
- [x] Cabeçalho com residência, nome real do imóvel, notificações e configurações.
- [x] Card em degradê azul, fonte de consumo, atualização, seletor Diário/Semanal/Mensal, consumo e comparação.
- [x] Cards de custo previsto e meta mensal lado a lado, com ícones e barras arredondadas.
- [x] Histórico de seis meses em m³, linha branca, último ponto azul e rótulos mensais.
- [x] Mostrar o valor numérico acima de cada ponto disponível, em formato brasileiro; reservar espaço superior e limitar a largura do rótulo ao mês.
- [x] Seção Alertas com três cartões e cores vermelha, amarela e ciano.
- [x] Diagnóstico com três métricas e botão vermelho.
- [x] Preservar menu flutuante com cinco ações e Home elevada. O conteúdo pode rolar totalmente acima dele.
- [x] Ícones vetoriais de alerta, pressão e meta; áreas interativas de pelo menos 48 dp; descrições acessíveis do gráfico.
- [ ] Foto da residência: a API não fornece imagem; usa ícone de casa como alternativa. Não recortar a foto de exemplo e atribuí-la ao imóvel do usuário.

## Comportamentos e dados
- [x] Nome e leituras pertencem ao imóvel selecionado; manter troca de imóvel nas configurações.
- [x] Seletor altera consumo e comparação: hoje/ontem, últimos 7 dias/7 anteriores, mês até hoje/mesmos dias do mês anterior.
- [x] Histórico permanece mensal, como indicado pelo subtítulo, com lacunas para meses sem leituras.
- [x] Consultar consumo a cada cinco minutos apenas com o dashboard iniciado; cancelar agendamento ao sair.
- [x] Exibir horário da consulta concluída. Não afirmar que a API diária é um sensor em tempo real.
- [x] Manter custo mensal real do backend e formatação brasileira.
- [x] Conectar custo, metas, alertas, diagnóstico e configurações aos destinos existentes; sino rola até os alertas.
- [x] Preservar carregamento inicial, tratamento de erro e nova tentativa, zero para consulta bem-sucedida sem registros e traços para indisponibilidade.
- [x] Reutilizar estado de monitoramento existente, isolado por imóvel; não confundir ausência de telemetria com ausência de vazamentos.
- [ ] Conectar meta mensal e percentual: contrato não disponível. Barra permanece neutra, com estado textual indisponível.
- [ ] Conectar fechamento da conta e progresso: não há contrato de ciclo de cobrança.
- [ ] Conectar fluxo por cômodo, duração, consumo elevado e pressão: API atual não fornece esses dados.
- [ ] Conectar detecção nas últimas 24h, litros/dia e custo/mês dos vazamentos: sem fonte confirmada. Não extrapolar volume observado para uma projeção diária/mensal.
- [ ] Conectar foto real da residência quando houver fonte disponível.

## Diferenças necessárias em relação à imagem
- Valores 187 L, R$142,80, 72%, 86 L, R$41 e 2 são exemplos da referência, não dados de produção.
- Sem fonte confirmada, textos indicam indisponibilidade e métricas usam traços. Nunca anunciar vazamento, pressão instável ou meta atingida com dados fictícios.
- Campos sem informação na imagem não são reconstruídos como fatos.
- Áreas de toque foram ampliadas e textos podem crescer; o menu não fica fixo sobre o segundo alerta como ocorre na captura.

## Navegação corrigida
- [x] Restaurar o layout e o controlador originais da Home.
- [x] Abrir `DashboardFragment` pelo destino `costsFragment`, associado ao cifrão.
- [x] Usar carregamento próprio do dashboard, sem depender do estado exclusivo da Home.
- [x] Preservar histórico de custos em `costsHistoryFragment`, acessível pelo card de custo previsto.

## Validação
- [x] Executar seis testes JUnit com sucesso (`OK (6 tests)`): filtro de imóvel/data/valor, virada de ano, períodos mensais equivalentes, histórico com lacunas, retorno vazio e fevereiro.
- [x] Compilar e vincular todos os recursos XML com AAPT2 usando dependências locais.
- [x] Compilar todos os arquivos Java do aplicativo com javac e recursos gerados.
- [ ] Concluir build Gradle e testes integrados: ambiente retorna `Unable to establish loopback connection` antes de compilar.
- [ ] Conferir captura Android contra a referência 393 × 1219, telas menores/maiores e fonte ampliada em dispositivo/emulador. O ADB também falhou ao iniciar a conexão local neste ambiente.
- [ ] Validar integrações de telemetria e metas quando os contratos forem disponibilizados.
