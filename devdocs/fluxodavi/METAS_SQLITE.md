# Metas por residência no SQLite

O pedido mais recente substitui a restrição anterior de implementar metas sem SQLite.

- `SQLiteGoalStore` cria `delta_monthly_goals.db`, tabela `monthly_goals`, com chave composta `(user_id, property_id, month)` e litros armazenados como texto decimal para preservar a precisão de `BigDecimal`.
- `MonthlyGoalsViewModel` e `MonthlyGoalCard` usam o mesmo armazenamento SQLite. Consultas e gravações ocorrem fora da thread principal; respostas antigas do card são descartadas.
- A primeira criação do banco importa as chaves válidas de `delta_monthly_goals` em SharedPreferences. As preferências antigas são mantidas como origem de migração; novas gravações usam somente SQLite. Reabrir o banco não repete a importação nem sobrescreve metas editadas.
- A tela continua vinculada à residência atual. O repositório confirma usuário e vínculo com o imóvel antes de consultar ou salvar uma meta.
- O banco está excluído de backup e transferência, seguindo o isolamento já aplicado ao armazenamento anterior. SQLite não sincroniza com a API ou com outros aparelhos.

Fontes: [SQLiteGoalStore.java](../../app/src/main/java/com/aula/delta_mobile_app/repository/SQLiteGoalStore.java), [MonthlyGoalsViewModel.java](../../app/src/main/java/com/aula/delta_mobile_app/viewmodel/MonthlyGoalsViewModel.java), [MonthlyGoalCard.java](../../app/src/main/java/com/aula/delta_mobile_app/view/MonthlyGoalCard.java), [MonthlyGoalsRepository.java](../../app/src/main/java/com/aula/delta_mobile_app/repository/MonthlyGoalsRepository.java).

Os testes instrumentados em `MonthlyGoalStorageInstrumentedTest` cobrem reabertura, separação por usuário/imóvel/mês e migração sem sobrescrever edições posteriores. Sua execução no Android permanece pendente: o Wrapper falhou antes das tarefas com `Unable to establish loopback connection`. Compilação isolada não substitui essa execução.
