# TO-DO — Detecção de vazamentos

Referência visual: [funcionalidade-vazamento.png](../../docs/references/funcionalidade-vazamento.png).

Escopo: implementar a tela Android da referência e suas integrações, preservando a navegação e o menu existentes. A interface, navegação e estados de indisponibilidade estão implementados. A integração de telemetria e preferências aguarda os endpoints que o usuário confirmou ainda não possuir. Valores da imagem são exemplos visuais, não dados fixos da conta.

## 1. Entrada e navegação

- [x] Criar um destino próprio para Detecção de vazamentos, sem substituir toda a tela de Consumo.
- [x] Transformar o cartão de detecção já existente em Consumo em acesso à nova tela.
- [x] Usar o imóvel selecionado e o medidor associado à conta autenticada; preservar a seleção ao navegar e retornar.
- [x] Preservar o menu flutuante com os cinco ícones atuais, cifrão corrigido, traços arredondados e Home circular elevada.
- [x] Manter a indicação de Consumo como seção ativa nesta tela e permitir retorno pelo botão do sistema.
- [x] Reservar espaço inferior de rolagem para que o menu não impeça acesso ao gráfico e aos botões.

## 2. Aparência geral

- [x] Reproduzir o fundo escuro e a iluminação suave em tons azul/cinza da referência.
- [x] Implementar o título “Detecção de vazamentos” e o subtítulo “Monitoramento em tempo real do fluxo hídrico”.
- [x] Ajustar tipografia, pesos, cores, margens e distâncias entre cabeçalho, cartões, gráfico, ações e botões.
- [x] Usar recursos vetoriais nítidos para os ícones, preservando as formas mostradas.
- [x] Permitir adaptação a telas estreitas e fonte ampliada, sem cortar títulos, unidades ou ações.

## 3. Carrossel de alertas

- [x] Criar a faixa horizontal de cartões azuis, arredondados, com o próximo cartão parcialmente visível, conforme a imagem.
- [x] Permitir rolagem horizontal para acessar todos os eventos retornados.
- [x] Exibir “Fluxo atual”, valor em L/min e referência de fluxo normal com sua faixa de horário.
- [x] Formatar números em português, como “2,0 L / min”, com a mesma hierarquia visual da referência.
- [x] Exibir o status em cápsula clara: “Alerta ativo” em vermelho e “Em investigação” em amarelo.
- [x] Vincular cada cartão ao evento e ao medidor correspondentes; não duplicar um evento para preencher o carrossel.
- [x] Manter status e dados coerentes entre o cartão selecionado, o gráfico e as ações.
- [ ] Definir, com o contrato do backend, ordenação, seleção inicial e demais status possíveis.
- [x] Tratar ausência de eventos com um estado visual dentro do componente, sem texto solto ou alerta fictício.

## 4. Gráfico da madrugada

- [x] Reproduzir “Fluxo durante a madrugada” e “Volume acumulado no evento ~ 600L”, substituindo o volume pelo valor real.
- [x] Desenhar gráfico de área com preenchimento cinza translúcido, contorno cinza azulado e linha de base horizontal.
- [x] Exibir os horários 00h, 03h e 06h para o período mostrado na referência; adaptar os rótulos ao período real quando necessário.
- [x] Posicionar os pontos por horário real, inclusive quando houver intervalos diferentes entre medições.
- [ ] Confirmar a grandeza e unidade vertical do gráfico: fluxo em L/min ou volume em litros. A imagem não explicita essa unidade.
- [x] Usar volume acumulado informado pela API ou uma regra de integração confirmada; não somar valores de fluxo como se fossem litros.
- [x] Preservar a diferença entre a janela do gráfico (00h–06h) e a referência normal indicada nos cartões (01h–06h), conforme os dados recebidos.
- [x] Tratar lacunas de telemetria e períodos sem medições sem desenhar uma curva inventada.

## 5. Ações recomendadas

- [x] Reproduzir o cartão azul “Ações recomendadas”, com cantos arredondados e divisórias claras.
- [x] Incluir “Verificar registro geral”, com ícone de controle e cápsula “Urgente” em vermelho.
- [x] Incluir “Inspecionar tubulações”, com lupa e cápsula “Recomendado” em amarelo.
- [x] Incluir “Desativar alertas”, com sino silenciado e cápsula “Opcional” em azul.
- [x] Definir o conteúdo de orientação/checklist de cada ação e abrir os detalhes em componente próprio.
- [x] Não apresentar “Verificar registro geral” como fechamento remoto de válvula sem que exista essa funcionalidade.
- [ ] Definir se desativar alertas afeta um evento, um medidor ou todo o imóvel, e por quanto tempo.
- [ ] Implementar a alteração de alertas com estado persistido, retorno de sucesso/erro e opção de reativação conforme o contrato aprovado.
- [ ] Desabilitar ações durante seu envio e evitar requisições duplicadas.

## 6. Botões de detalhes

- [x] Reproduzir os dois botões com fundo cinza, borda clara, cantos arredondados, texto à esquerda e seta à direita.
- [x] Implementar “Ver análise por horário”, preservando imóvel, evento e período selecionados no destino.
- [ ] Implementar o botão de consumo anormal e confirmar seu rótulo completo: o menu encobre parte do texto na imagem.
- [x] Definir os destinos e o conteúdo das duas análises antes de considerá-las concluídas; não criar botões sem ação.

## 7. Dados e integração — dependências

O contrato local atual possui consumo agregado por dia (`consumption-daily/{propertyId}`), mas não fornece telemetria intradiária, eventos de vazamento ou preferências de alertas. Agregados diários não comprovam fluxo em tempo real.

- [ ] Receber endpoints e exemplos de resposta para eventos, fluxo atual, medições por horário, análises e configuração de alertas.
- [ ] Receber IDs de evento, imóvel e medidor; status; início/fim; última atualização; fluxo atual; fluxo normal; janela de normalidade; volume acumulado e série temporal.
- [ ] Receber a regra que caracteriza vazamento/consumo anormal e os limites de alerta. Não inferir essa regra dos números da imagem.
- [ ] Definir se a detecção é feita no backend ou no dispositivo; não implementar algoritmo presumido no app.
- [ ] Definir origem do monitoramento: WebSocket, SSE ou consultas periódicas; intervalo, prazo de validade e política de reconexão.
- [ ] Receber as unidades, o fuso dos horários e o significado de valores ausentes; normalizar sem alterar o significado dos dados.
- [ ] Definir recomendações, prioridade e quais mudanças de status são permitidas ao usuário.
- [ ] Criar DTOs, repositório e ViewModel a partir do contrato recebido, com a autenticação existente.
- [x] Descartar respostas atrasadas de outro imóvel/evento e impedir atualização depois de encerrar a tela.
- [ ] Suspender o monitoramento quando a tela ficar inativa e atualizar ao retomá-la, conforme o mecanismo escolhido.

## 8. Carregamento, ausência de dados e mensagens

- [x] Mostrar carregamento antes de revelar a primeira versão completa da tela, aguardando os dados necessários.
- [ ] Nas atualizações em tempo real, atualizar os valores sem piscar a tela ou reiniciar continuamente o carregamento.
- [ ] Exibir 0 para períodos confirmados sem consumo. Não representar sensor offline, telemetria ausente ou falha de consulta como fluxo zero.
- [ ] Exibir ausência de medidor, falta de eventos, perda de conexão e dados desatualizados em componentes visuais próprios.
- [x] Nunca inserir mensagens de erro, diagnóstico ou instrução de nova tentativa como texto solto na página.
- [x] Reutilizar `StatusNoticeView`, diálogos ou barras de aviso para feedback necessário; ocultar componentes vazios.
- [ ] Manter ações de nova tentativa e reativação acessíveis, sem depender somente da cor das cápsulas.

## 9. Validação e conclusão

- [ ] Comparar a tela em execução com a imagem: cores, formas, ícones, tamanhos, espaçamento e sobreposição do menu.
- [ ] Verificar rolagem vertical/horizontal, telas menores, fonte ampliada e leitores de tela; garantir áreas de toque adequadas.
- [ ] Testar conversão e formatação de L/min e litros, timestamps, fuso e volume acumulado segundo a regra confirmada.
- [ ] Testar nenhum evento, um evento, vários eventos, cada status, fluxo zero e medidor sem sinal.
- [ ] Testar erro inicial, perda e retorno da conexão, dados antigos e respostas fora de ordem.
- [ ] Testar troca de imóvel/evento, retorno de outra tela, rotação e restauração do estado sem duplicar assinaturas de monitoramento.
- [ ] Testar ações recomendadas, persistência das preferências e os dois destinos de análise.
- [ ] Executar build, verificações de recursos e testes apropriados; registrar quaisquer limitações do ambiente.
- [ ] Validar em uma conta com telemetria real antes de declarar o monitoramento em tempo real concluído.

## Informações a solicitar para iniciar a integração

Endpoints e JSONs reais; regra de detecção; unidade do gráfico; estratégia de atualização; conteúdo das orientações; comportamento de desativar/reativar alertas; telas de destino das análises e rótulo completo do segundo botão.

## Resultado desta implementação — 17/10/2026

- Entrada: Consumo → Detecção de vazamentos, disponível mesmo quando a consulta de consumo não retorna leituras.
- Novos destinos: detecção, análise por horário e consumo anormal registrado. O último rótulo foi adotado a partir da parte visível da imagem; confirmação final permanece pendente.
- Orientações: registro geral e tubulações abrem diálogos. Desativar alertas informa indisponibilidade e não grava uma preferência fictícia.
- Integração existente: perfil, vínculos do imóvel e dispositivos ativos, com validação de propriedade e estado separado para ausência de medidor.
- Modelo e renderizadores preparados para eventos reais; os cartões selecionam eventos por toque. O gráfico não integra taxas como volume nem une lacunas explícitas.
- Não há polling, WebSocket, SSE, detector local, alerta ativo fictício ou fluxo preenchido com 2,0 L/min.
- `StatusNoticeView` apresenta erros e estados que exigem ação; nenhum diagnóstico aparece como texto solto.
- Validação: recursos compilados e vinculados com AAPT2; arquivos Java alterados compilados com javac usando esses símbolos; 20 testes JVM passaram, sendo oito do novo modelo.
- Gradle: `testDebugUnitTest assembleDebug` tentado e bloqueado por “Unable to establish loopback connection”. Compilação separada não equivale a APK instalado nem teste em execução.
- Pendentes: revisão visual em aparelho, testes de interface/lifecycle em execução, endpoints e regras de telemetria, detecção, análises e preferências de alertas. Itens dependentes do backend continuam desmarcados.

Detalhamento para a próxima integração: [VAZAMENTOS_DATA_REQUIRED.md](VAZAMENTOS_DATA_REQUIRED.md).

## Entrada direta pelo menu

- [x] O ícone da gota abre diretamente Detecção de vazamentos.
- [x] Ao tocar na gota a partir de uma análise, abrir a tela principal de detecção, sem restaurar o detalhe anterior.
- [x] Manter o destaque do ícone nos destinos de detecção e análise e atualizar sua descrição de acessibilidade.
