# Rotina

## Objetivo
Permitir que o usuário visualize, em um calendário, os hábitos de cada
dia e quanto de água cada hábito consome, e gerencie esses hábitos
(adicionar, editar e excluir), para entender o impacto da própria
rotina no consumo.

## Critérios de aceite

### Calendário e visualização
- [ ] O usuário vê o calendário no formato definido no Figma e
  consegue selecionar um dia
- [ ] O dia atual vem selecionado ao abrir a tela
- [ ] O usuário navega entre meses
- [ ] Ao selecionar um dia, aparece a lista de hábitos daquele dia
- [ ] Cada hábito mostra a quantidade de água que gasta (com unidade)
- [ ] O dia selecionado mostra o total de água dos hábitos
- [ ] Dia sem hábitos mostra o estado vazio do Figma

### Gerenciar hábitos
- [ ] O usuário consegue adicionar um hábito
- [ ] O usuário consegue editar um hábito existente
- [ ] O usuário consegue excluir um hábito, com confirmação antes
- [ ] Os campos do formulário são validados, com mensagem de erro
  clara (campo obrigatório, valor inválido)
- [ ] Após adicionar, editar ou excluir, a lista e o total do dia
  são atualizados sem precisar reabrir a tela
- [ ] Falha de rede ao salvar ou excluir mostra erro e permite tentar
  de novo, sem perder o que o usuário digitou

### Estados e qualidade
- [ ] Existem estados de carregando e erro (com tentar de novo)
- [ ] O dia selecionado e o formulário em edição se mantêm ao girar a tela
- [ ] Sem texto hardcoded (tudo em strings.xml)
- [ ] Layout funciona em celular e tablet, retrato e paisagem, sem
  elementos cortados ou esticados
- [ ] Testado em ~360dp, ~412dp e tablet 10" (~800dp)
- [ ] Teclado aberto não cobre os campos do formulário
- [ ] Acessibilidade: contentDescription, toque mínimo de 48dp

## Fora do escopo
- Marcar hábito como concluído
