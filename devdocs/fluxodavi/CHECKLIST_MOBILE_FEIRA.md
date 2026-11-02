# Checklist mobile — situação atual

Implementado significa alteração presente no código. Não equivale a aceite completo no aparelho. Esta revisão não executou novos testes nem recebeu a lista oficial de requisitos acadêmicos ou as alterações da Ana.

| Pedido | Situação | O que falta |
| --- | --- | --- |
| Remover indicador EH | Implementado | Regressão visual geral junto às demais telas. |
| Tela simples de meta mensal, com consulta e edição | Implementado | Validar edição, cancelamento, teclado, troca do mês e reabertura no aparelho. Vinculada à residência atual. |
| Corrigir ícones e navegação da navbar | Parcial | Rotas corrigidas e falha do card compartilhado Home/Dashboard corrigida. Executar a matriz de navegação e Voltar em aparelho. |
| Remover ranking da navbar | Implementado | Navbar usa consumo/vazamentos, custos, Home, chat e rotina. Ranking permanece acessível nas configurações. |
| Aplicar alterações da Ana no protótipo | Pendente de referência | Receber o protótipo atualizado ou a lista de alterações e comparar com o mobile. |
| SQLite e demais requisitos mobile | Parcial | Metas usam SQLite por usuário/residência/mês, com migração das metas antigas. Validar persistência/migração no Android e revisar a rubrica oficial. Notificações ainda não implementadas. |
| API de clima | Pendente | Integrar dados reais; o gráfico temperatura × litros não está populado. |
| Testar e mapear todos os casos da feira | Pendente | Montar o roteiro completo, executar e registrar resultados. Os testes pontuais realizados não equivalem à homologação completa. |
| Identificar partes sem dados | Revisão inicial feita | Lista abaixo; atualizar após revisão de todas as telas. |

## Partes sem integração/dados confirmados

- Notificações: o botão da Home mostra aviso de indisponibilidade; não há serviço Android de notificações implementado nas fontes revisadas.
- Clima e relação temperatura × litros: a Home inicializa o gráfico sem pontos; não há cliente de clima nas fontes revisadas.
- Monitoramento de vazamentos: a fonte atual retorna telemetria indisponível; eventos, vazão e diagnósticos não devem ser apresentados como detecção real.
- Fechamento da conta e progresso do custo previsto: o Dashboard mantém o texto de indisponibilidade e barra em zero; o custo previsto é um dado separado.
- Alteração de senha: a tela informa indisponibilidade na API.
- Rejeição do endereço/CEP de São Paulo relatada: causa exata ainda não confirmada. O salvamento do nome da residência foi separado da validação do endereço.

## Roteiro ainda a executar

- [ ] Login, restauração de sessão e logout confirmado.
- [ ] Todos os ícones da navbar a partir de cada aba e tela interna; Voltar e toques repetidos.
- [ ] Configurações, edição/cancelamento/salvamento de dados pessoais e residenciais; falhas de CEP e teclado.
- [ ] Foto: câmera/galeria, envio, reentrada na Home, falha de rede e isolamento entre contas.
- [ ] Metas: edição/cancelamento, persistência após reiniciar, migração, meses e residências diferentes.
- [ ] Cálculos: consumo zero, aumento do consumo, meta atingida/ultrapassada e virada do mês.
- [ ] Consumo, custo, histórico, ranking e ausência de dados; respostas de erro e rede lenta/offline.
- [ ] Rotação, fonte ampliada, tela pequena/grande e conteúdo acima da navbar/teclado.
- [ ] Conferência item a item da rubrica oficial e do protótipo atualizado.

## Fontes

- [Remoção de EH e navegação](EH_NAVBAR_METAS_TODO.md).
- [SQLite de metas](METAS_SQLITE.md), [cálculos](METAS_CALCULOS.md) e [Cloudinary](PHOTO_CLOUDINARY.md).
- [HomeFragment.java](../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/HomeFragment.java), [MainNavigation.java](../../app/src/main/java/com/aula/delta_mobile_app/view/activities/main/MainNavigation.java), [activity_main.xml](../../app/src/main/res/layout/activity_main.xml).
- [LeakMonitoringRepository.java](../../app/src/main/java/com/aula/delta_mobile_app/repository/LeakMonitoringRepository.java), [fragment_dashboard.xml](../../app/src/main/res/layout/fragment_dashboard.xml), [PersonalInformationFragment.java](../../app/src/main/java/com/aula/delta_mobile_app/view/fragments/PersonalInformationFragment.java).
