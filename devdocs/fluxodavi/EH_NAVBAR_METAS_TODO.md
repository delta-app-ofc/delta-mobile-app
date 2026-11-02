# Mobile — remover EH, corrigir navbar e criar metas

Atualização de escopo: o pedido posterior autorizou SQLite para metas por residência. A implementação atual e os limites de validação estão em [METAS_SQLITE.md](METAS_SQLITE.md). As referências a “sem SQLite” abaixo registram a entrega anterior.

## Escopo e situação

Origem: pedido explícito da pessoa usuária nesta conversa: remover EH, corrigir a navbar e implementar uma tela simples de metas sem SQLite. O planejamento foi seguido nesta entrega. As marcações abaixo distinguem implementação e verificações executadas de aceite em aparelho ainda pendente.

Requisitos aprovados:

- Remover o indicador EH, pois ele não faz parte do produto.
- Corrigir a navegação pelos ícones da navbar.
- Permitir consultar e editar a meta mensal em uma tela simples.
- Não usar SQLite para metas.

Ficam fora deste TO-DO: API de clima, notificações, auditoria geral dos requisitos acadêmicos, alterações da Ana sem referência identificada e roteiro completo da feira. Os testes abaixo cobrem os três itens deste escopo.

## 1. Remover o indicador EH

Diagnóstico inicial: a Home contém o mostrador e o inicializa sem pontuação. Há textos de EH e escala `/100` nos recursos. Fontes: [HomeFragment.java][home-fragment], [fragment_home.xml][home-layout], `HomeEfficiencyView.java` (classe removida nesta entrega) e [home_strings.xml][home-strings].

- [x] Remover o mostrador EH e sua inicialização na Home.
- [x] Reorganizar o bloco para manter a meta mensal sem deixar espaço vazio ou coluna reservada ao indicador.
- [x] Remover os textos, descrições acessíveis, escala e referências exclusivos de EH.
- [x] Verificar usos antes de excluir `HomeEfficiencyView` e outros recursos exclusivos do indicador.
- [x] Atualizar as pendências de EH em [HOME_TODO.md](HOME_TODO.md), [HOME_DATA_REQUIRED.md](HOME_DATA_REQUIRED.md) e [HOME_API.md](HOME_API.md), registrando a decisão atual sem reescrever o histórico como se nunca tivesse existido.

Critérios de aceite:

- [x] A Home não apresenta EH, pontuação `/100`, classificação nem aviso de EH indisponível.
- [ ] O cartão de meta continua acessível, legível e sem recortes com fonte ampliada.
- [x] Consumo e custo continuam usando os dados existentes, sem alteração de fórmulas por causa da remoção.

## 2. Corrigir a navbar

Diagnóstico inicial: o shell liga os cinco ícones aos destinos e combina back stack salvo com restauração de estado. O menu é ocultado com teclado, restauração de sessão e alguns fluxos de detalhe. Isso não comprova a causa dos problemas relatados. **INCERTO:** a origem de cada falha depende de reprodução no aparelho. Fontes: [MainActivity.java][main-activity], [activity_main.xml][main-layout] e [main_navigation.xml][navigation].

- [ ] Reproduzir cada falha antes de alterar a navegação; registrar tela de origem, ícone tocado, resultado atual e resultado esperado.
- [ ] Testar os ícones de Vazamentos, Custos, Início, Chat e Rotina a partir das abas e de suas telas internas.
- [x] Corrigir os casos em que o toque não navega ou a restauração mantém o usuário em uma tela diferente da esperada.
- [ ] Conferir retorno à Home e troca de aba a partir de Configurações, informações pessoais/residenciais, Ranking e Metas.
- [x] Diferenciar menu intencionalmente oculto de falha: revisar os fluxos de QR, configuração de conta, teclado e restauração de sessão.
- [x] Revisar sobreposições de loading, estado selecionado dos ícones e áreas de toque.
- [x] Preservar os avisos de alterações não salvas e as proteções durante salvamento; não contornar esses controles para fazer o ícone responder.
- [x] Evitar destinos duplicados com toques repetidos e manter um comportamento coerente do botão Voltar.
- [x] Manter a navbar sem ícone de ranking; o ranking já possui acessos fora dela.

Critérios de aceite:

- [ ] Cada combinação de origem e ícone testada chega ao destino esperado ou apresenta um bloqueio explícito de edição/salvamento.
- [ ] O destaque visual identifica a aba correspondente, inclusive em telas internas.
- [ ] Loading, teclado, rotação e retorno de outra aba não deixam controles permanentemente bloqueados.
- [ ] O botão Voltar não entra em ciclos nem acumula cópias da mesma tela.

## 3. Implementar a tela de metas sem SQLite

Diagnóstico inicial: `monthlyGoalsFragment` usa a tela genérica, que mostra uma mensagem de indisponibilidade. Home e Dashboard já possuem ações que abrem esse destino. Não há rota de metas em [ApiService.java][api]. Isso descreve a interface do app neste checkout, sem afirmar que nenhum outro serviço possa oferecer metas. Fontes: [main_navigation.xml][navigation], [MainSectionFragment.java][section-fragment], [HomeFragment.java][home-fragment] e [DashboardFragment.java][dashboard-fragment].

### Definições adotadas nesta implementação

- Usar `SharedPreferences` para persistência local, com acesso encapsulado em repositório. O pedido de executar este TO-DO adota essa alternativa proposta; ela não se torna uma convenção obrigatória para o restante do projeto.
- Identificar a meta por usuário autenticado, imóvel e mês/ano para evitar mistura entre contas, unidades e períodos.
- A meta é um limite mensal em litros, maior que zero, com até duas casas decimais. Aceitar ponto ou vírgula decimal sem separador de milhar. São escolhas desta implementação; não descrevem um contrato de backend. Não calcular percentuais ou recomendações nesta entrega.
- Informar na tela que a meta é salva somente neste aparelho, sem sincronização com a web. As preferências desta função ficam excluídas do backup/transferência do Android.
- Usar uma composição simples: imóvel, mês de referência, meta atual, ação de editar e confirmação de salvamento. A referência da web ainda precisa ser identificada para reproduzir detalhes visuais.

### Implementação

- [x] Consolidar as definições acima, especialmente persistência, unidade e identificação por período.
- [x] Criar uma tela dedicada de metas, reaproveitando o destino `monthlyGoalsFragment` e os acessos existentes.
- [x] Exibir o imóvel e o mês/ano aos quais a meta pertence; impedir salvamento sem identidade e imóvel válidos.
- [x] Permitir consultar, cadastrar e editar a meta mensal.
- [x] Validar valor vazio, formato numérico, separador decimal, zero e negativos conforme a regra consolidada; associar erros ao campo.
- [x] Salvar e reler o valor confirmado pelo repositório sem SQLite; não exibir sucesso apenas por atualizar um texto na tela.
- [x] Preservar o formulário durante rotação e solicitar descarte quando houver alterações não salvas.
- [x] Exibir estados claros para ausência de meta, ausência de imóvel e falha de leitura/salvamento.
- [x] Atualizar os cartões de meta da Home e do Dashboard após salvar e ao reabrir essas telas.
- [x] Não exibir progresso por consumo nesta entrega. O cartão mostra o limite mensal em litros; nenhuma cobertura de telemetria ou fórmula de percentual é presumida.
- [x] Não inventar sugestões, equivalências, limites recomendados ou endpoints para preencher os cartões existentes.
- [x] Garantir rolagem acima do menu, teclado sem cobrir ações, toque mínimo de 48dp e textos em recursos.

Critérios de aceite:

- [ ] A pessoa consegue definir uma meta, fechar a edição e consultar o valor salvo para o mesmo imóvel e mês.
- [ ] Reiniciar o app preserva a meta se a persistência local proposta for adotada.
- [x] Outra conta, outro imóvel ou outro mês não recebe a meta incorreta.
- [ ] Cancelar a edição não modifica a meta confirmada.
- [x] Não há dependência, banco, tabela ou acesso SQLite introduzido para esta função.
- [ ] Home, Dashboard e tela de metas mostram o mesmo valor para o mesmo contexto.

## Validação e encerramento

Ordem sugerida: remover EH → corrigir navbar → implementar metas → executar a regressão conjunta.

- [x] Executar `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` a partir da raiz do repositório.
- [x] Cobrir e executar testes isolados das regras de validação, persistência e isolamento das metas.
- [ ] Executar os novos testes instrumentados de navegação e persistência no aparelho, além da reprodução dos problemas relatados.
- [ ] Testar em aparelho/emulador: navegação entre abas, Voltar, loading, teclado, rotação, ausência de meta/imóvel, edição, cancelamento e reabertura do app.
- [ ] Conferir celular/tablet em retrato/paisagem e fonte ampliada, sem presumir uma matriz global oficial.
- [x] Registrar comandos executados, resultados e limitações. O histórico recente inclui falha de loopback do Java/Gradle; verificar novamente antes de apresentá-la como bloqueio atual.
- [x] Registrar evidências e somente então marcar os itens implementados e validados como concluídos.


## Resultado da execução e limites

- EH: mostrador, inicialização, escala e recursos exclusivos removidos; o cartão de meta ocupa a largura disponível na Home.
- Navbar: `MainNavigation.open()` leva ao destino raiz do ícone sem restaurar uma pilha com telas internas; toques no destino atual não acrescentam cópias. O destaque inclui telas internas. O shell rejeita pedidos de loading de uma View que não seja a tela atual. A edição residencial não permite abandonar uma gravação em andamento.
- Metas: tela dedicada com seleção de imóvel e mês, edição/cancelamento, validação e confirmação de gravação. O repositório confirma identidade e vínculo antes de ler/gravar e rejeita sobrescrita de uma meta alterada desde a abertura. O ViewModel conserva o rascunho durante sua vida e descarta respostas de sessão antiga.
- Home e Dashboard: leem o mesmo armazenamento para o usuário/imóvel e mês atual; atualizam ao receber o contexto e ao retomar a tela. Não há barra ou percentual fictício de meta.
- Testes isolados: **26 aprovados**, incluindo `MonthlyGoalTest`, `MonthlyGoalsRepositoryTest`, `SectionDataTest` e regressão `RankingRepositoryTest`, com serviços substituídos. Não houve gravação em API de produção.
- Compilação suplementar: as 121 fontes Java principais e os novos testes instrumentados compilaram com dependências do cache e `R` auxiliar; todos os recursos passaram por `aapt2 compile`. Essas verificações não substituem build/lint Android nem comprovam equivalência visual.
- Wrapper: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` foi executado, mas falhou antes das tarefas com `Unable to establish loopback connection`. Tentativas com seletor alternativo e daemon isolado também falharam; nenhuma configuração global de Java/Gradle foi alterada.
- Preparados e compilados, porém **não executados**: `MainNavigationInstrumentedTest` e `MonthlyGoalStorageInstrumentedTest`. O segundo usa as preferências do pacote de teste, sem limpar metas/sessão do app da pessoa usuária.
- Permanecem pendentes: reprodução/aceite visual no aparelho, rotação/teclado/fontes ampliadas, celular/tablet em retrato/paisagem, instalação do APK desta entrega e verificações completas do Wrapper. Não registrar esses itens como concluídos com base nos testes isolados.

Fontes da implementação: [MonthlyGoalsFragment.java](../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/MonthlyGoalsFragment.java), [MonthlyGoalsRepository.java](../../app/src/main/java/com/aula/delta_mobile_app/repository/MonthlyGoalsRepository.java), [MonthlyGoalsViewModel.java](../../app/src/main/java/com/aula/delta_mobile_app/viewmodel/MonthlyGoalsViewModel.java), [MonthlyGoalCard.java](../../app/src/main/java/com/aula/delta_mobile_app/view/MonthlyGoalCard.java) e [MainNavigation.java](../../app/src/main/java/com/aula/delta_mobile_app/view/activities/main/MainNavigation.java).

[home-fragment]: ../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/HomeFragment.java
[home-layout]: ../../app/src/main/res/layout/fragment_home.xml
[home-strings]: ../../app/src/main/res/values/home_strings.xml
[main-activity]: ../../app/src/main/java/com/aula/delta_mobile_app/view/activities/main/MainActivity.java
[main-layout]: ../../app/src/main/res/layout/activity_main.xml
[navigation]: ../../app/src/main/res/navigation/main_navigation.xml
[section-fragment]: ../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/MainSectionFragment.java
[dashboard-fragment]: ../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/DashboardFragment.java
[api]: ../../app/src/main/java/com/aula/delta_mobile_app/services/api/ApiService.java
