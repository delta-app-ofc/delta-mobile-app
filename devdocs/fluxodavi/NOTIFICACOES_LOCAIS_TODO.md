# TO-DO — Notificações locais

## Objetivo e limite do MVP

Implementar lembretes locais para os horários cadastrados na tela Rotina. O app agenda os avisos no próprio aparelho; a entrega não depende de Firebase nem de uma chamada ao servidor no instante do aviso.

O MVP não detecta vazamento, consumo elevado ou alteração remota do hidrômetro enquanto o app está fechado. Esses eventos dependem de telemetria e de um mecanismo do servidor para avisar o aparelho. Não criar alertas com base em consumo estimado ou em dados incompletos.

## Estado atual do projeto

- A tela Rotina e os horários por dia da semana existem. Os modelos também representam versões de agenda com datas de vigência: `RoutineData`, `ApiModels.UserHabitResponse` e `ApiModels.UserHabitScheduleVersionResponse`.
- `RoutineRepository` salva alguns horários localmente em `RoutineScheduleStore`; os lembretes mantêm uma projeção própria para reagendar após reinício do aparelho.
- A implementação local usa canal e receivers Android, `POST_NOTIFICATIONS` no Android 13+ e acesso `SCHEDULE_EXACT_ALARM` nas versões que o exigem.
- O catálogo de dependências não declara WorkManager.
- Referências: `app/src/main/java/com/aula/delta_mobile_app/model/RoutineData.java`, `app/src/main/java/com/aula/delta_mobile_app/model/ApiModels.java`, `app/src/main/java/com/aula/delta_mobile_app/repository/RoutineRepository.java`, `app/src/main/java/com/aula/delta_mobile_app/repository/RoutineScheduleStore.java`, `app/src/main/AndroidManifest.xml`, `gradle/libs.versions.toml`.

## Decisões de produto antes de implementar

- [x] Lembrete no horário de cada hábito da Rotina.
- [x] Usar alarme exato; o Android pode exigir acesso especial a “Alarmes e lembretes”.
- [x] Começar desligado e pedir ativação explícita pelo usuário em Configurações.
- [x] Mostrar nome curto do hábito e abrir a Rotina; a notificação usa visibilidade privada na tela bloqueada.
- [x] Usar uma preferência geral para o MVP; preferências por hábito ficam fora do escopo atual.
- [x] Não bloquear o app se a permissão for negada; explicar a situação e oferecer as configurações do Android para notificações.

## 1. Contrato dos lembretes

- [x] Usar apenas hábitos cuja agenda esteja completa e válida; agendas incompletas não geram horários inventados.
- [x] Criar um lembrete para cada horário cadastrado, sem inferir consumo ou execução.
- [x] Respeitar `effectiveFrom` e `effectiveUntil` das versões de agenda.
- [x] Enviar no horário cadastrado, sem antecipar o aviso.
- [x] Usar o fuso horário local do aparelho e reagendar após mudança de hora/fuso.
- [x] Ao tocar, abrir a tela Rotina.

## 2. Permissão e interface

- [x] Declarar `POST_NOTIFICATIONS` e solicitar a permissão quando a pessoa ativa os lembretes, no Android 13 ou superior.
- [x] Criar canal de notificação com nome e descrição; o sistema controla som e vibração.
- [x] Acrescentar preferência em Configurações para ativar/desativar e consultar o estado das permissões.
- [x] Não bloquear a Rotina se a pessoa negar uma permissão.
- [x] Abrir a Rotina ao tocar e não incluir endereço ou consumo no aviso.

## 3. Agendamento e arquitetura

- [x] Implementar serviço separado da interface para criar, atualizar e cancelar lembretes locais.
- [x] Usar `AlarmManager.setExactAndAllowWhileIdle`; alarmes exatos exigem permissão especial em versões compatíveis e WorkManager não é usado para um horário de relógio.
- [x] Usar identificador estável por conta, hábito, dia, horário e versão da agenda.
- [x] Persistir preferência e regras locais suficientes para reconstruir alarmes após reinício; não usar esses dados como consumo.
- [x] Reconciliar depois de carregar a Rotina e após mudanças confirmadas de agenda.
- [x] Cancelar horários antigos ao substituir agenda e remover horários de hábitos excluídos.
- [x] Restaurar após reinício, atualização do app e mudança de hora/fuso.
- [x] Cancelar alarmes e limpar regras ao sair da conta.
- [x] Agendar somente a próxima ocorrência de cada regra; não acumular avisos vencidos.
- [x] A notificação usa dados locais; rede só é consultada para atualizar a agenda, não no instante do disparo.

## 4. Estados e falhas

- [x] Diferenciar preferência desligada e permissões negadas; a tela mostra orientação para restaurar permissões.
- [x] Se a consulta falhar, manter as regras confirmadas já salvas.
- [ ] Se não houver agenda completa, não criar lembrete para aquele hábito e informar isso na interface da Rotina quando for relevante.
- [x] Usar identificadores estáveis, cancelar alarmes anteriores e limpar dados ao trocar/sair da conta.

## 5. Validação antes de concluir

- [ ] Validar solicitação e recusa de permissão em Android 13 ou superior e comportamento em versões anteriores suportadas.
- [ ] Validar recebimento com app em primeiro plano, em segundo plano e após fechar e reabrir o app.
- [ ] Validar reinício, mudança de fuso/horário, agenda atualizada, hábito excluído, logout e troca de conta.
- [ ] Validar horário inexistente ou malformado, agenda incompleta, versões efetivas, notificações duplicadas e avisos vencidos.
- [ ] Validar toque na notificação e leitura acessível do conteúdo.
- [ ] Executar build e verificações/testes apropriados; registrar os limites do ambiente e não declarar entrega sem validação em aparelho.

## Fontes Android

- [Permissão e canais de notificação](https://developer.android.com/develop/ui/views/notifications/time-sensitive)
- [Agendar alarmes e restrições para alarmes exatos](https://developer.android.com/develop/background-work/services/alarms)
- [Quando usar WorkManager ou AlarmManager](https://developer.android.com/develop/background-work/background-tasks/persistent)

## Fora deste MVP

- Push remoto para alertas de telemetria, vazamento ou consumo: requer contrato/serviço no backend e integração de entrega, por exemplo Firebase Cloud Messaging.
- Detecção local de vazamento, consulta periódica ao servidor ou processamento contínuo em segundo plano.
- Regras de economia de água, metas ou mensagens baseadas em valores estimados.

## Implementação atual — 02/11/2026

- A preferência “Lembretes da rotina” está em Configurações e começa desligada.
- O app pede a permissão de notificações no Android 13+ e o acesso especial a alarmes exatos quando necessário.
- Hábitos com agenda válida geram notificações nos horários cadastrados; versões futuras de agenda, fuso local e vigência são respeitados.
- Os alarmes são atualizados depois da consulta bem-sucedida da Rotina e após reinício, atualização do app e mudança de horário/fuso. Sair da conta cancela e limpa os lembretes.
- O toque leva à tela Rotina. As regras locais são excluídas de backup e transferência de aparelho.
- Validação pendente: `:app:assembleDebug` foi tentado em 02/11/2026 e falhou antes da compilação com `Unable to establish loopback connection`. Não foi possível validar em aparelho; testes não foram executados.
