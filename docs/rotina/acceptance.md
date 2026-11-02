# Evidências e aceite da Rotina

Registro da etapa 10 em 30/10/2026, sobre o commit base `0070863`. Este documento separa regressão com serviço controlado, verificação visual e integração com backend. Ausência de evidência permanece pendência, não aprovação implícita.

## Ambiente observado

| Item | Valor |
| --- | --- |
| Build Android | debug, Java 11 no código e JDK 21.0.11 no Gradle |
| Aparelho | Samsung SM-S921B físico, Android 16 / API 36 |
| Tela física | 1080 × 2340 px, 480 dpi |
| Largura calculada | 360dp em retrato e 780dp em paisagem |
| Escala de fonte | 1,0× |
| Backend documentado | snapshot OpenAPI 3.1.0, Delta API 1.0.0 |
| Workaround do host | `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=./app/build/nonexistent-domain-dir` para a conexão local do Gradle |

O OpenAPI versionado ainda descreve o contrato antigo de hábitos. Ele não comprova que `schedule`, `scheduleVersions` e as políticas de escrita acordadas nas etapas anteriores estejam publicados.

Compatibilidade visual adotada: registros antigos com `daysOfWeek`, mas sem distribuição de horários, aparecem somente nos dias declarados. O `startTime` legado é exibido quando válido; sem ele, o cartão mostra “Sem horário”. Eles não são transformados em execuções nem entram no total diário, porque isso exigiria inventar a distribuição ausente.

## Verificações executadas

| Verificação | Resultado | Alcance |
| --- | --- | --- |
| `:app:assembleDebug :app:testDebugUnitTest` após adequação visual da agenda | Passou; APK debug gerado e 378 testes JVM sem falhas | Recursos XML, adapter da linha do tempo e regressão JVM |
| `:app:compileDebugAndroidTestJavaWithJavac :app:testDebugUnitTest` | Passou; 378 testes JVM, sem falhas ou ignorados | Compilação da primeira versão da instrumentação e regressão JVM |
| `RoutineStateInstrumentedTest` no SM-S921B | Passou; 2 testes | Serviço manual controlado, relógio fixo, sem produção |
| Criação durante request + rotação | Passou | Dia, rascunho e request único preservados; sucesso recarrega só a coleção |
| Exclusão, cancelar e rotação durante request | Passou | Cancelar não escreve; alvo e request único são preservados; sucesso recarrega a coleção |
| Editor simplificado — compilação, build e regressão JVM | Passou | APK debug gerado, testes instrumentados compilados e 379 testes JVM sem falhas |
| `RoutineFormInstrumentedTest` simplificado no SM-S921B | Passou; 2 testes | Dia e horário preservados no giro; salvar valida automaticamente; descarte preserva o rascunho até confirmação |
| `:app:lintDebug` | Falhou com 7 erros preexistentes fora da Rotina | `LeakDetectionFragment`, `SignupViewModel`, `fragment_hourly_analysis.xml` e quatro usos de tint em `fragment_dashboard.xml` |
| Suíte instrumentada completa | Não executada | Evitada porque os testes de autenticação documentados limpam a sessão do app de teste |

A última repetição da classe do formulário não foi autorizada pelo ambiente. Por isso o ajuste final dessa classe não é apresentado como compilado ou aprovado. A instalação feita pelos testes instrumentados também não equivale a uma instalação de aceite do APK para uso normal.

## Matriz visual e responsiva

| Cenário | Evidência | Estado |
| --- | --- | --- |
| Celular ~360dp, retrato, fonte 1,0× | Execução instrumentada no SM-S921B | Parcial: fluxo e rotação controlados; sem revisão manual completa |
| Celular ~360dp, paisagem (~780dp de largura) | Rotação real do editor | Dia, horário, frequência derivada e resumo preservados; revisão visual manual ainda pendente |
| Celular ~412dp | Nenhum dispositivo/emulador executado | Pendente |
| Janela larga a partir de 800dp | Nenhum dispositivo/emulador executado | Pendente |
| Tablet 10" | Nenhum dispositivo/emulador executado | Pendente |
| Fonte 1,3× e 2,0× | Não executado | Pendente |
| TalkBack e ordem de foco | Não executado manualmente | Pendente |
| Alvos de toque de 48dp | Recursos e testes existentes não substituem medição visual | Pendente |
| Calendário em fronteiras de mês/ano/bissexto | Cobertura JVM das regras; sem captura manual nesta etapa | Lógica coberta, visual pendente |

As capturas produzidas pela instrumentação foram gravadas no diretório externo privado do APK de teste. O runner removeu o pacote ao terminar e os arquivos não ficaram disponíveis para inclusão no repositório. Nenhuma captura foi fabricada ou reaproveitada de outra etapa.

## Integração com backend

Nenhuma chamada à API de produção foi usada como fixture. Logo, leitura, POST, PUT e DELETE com o contrato ampliado continuam sem aceite de integração.

A evidência externa disponível é o erro informado durante uso manual ao cadastrar um tipo já existente: PostgreSQL `23505`, restrição `uq_tb_user_habit_user_habit`. Isso confirma que o backend publicado rejeita mais de um registro do mesmo tipo por usuário, mas não define sozinho a semântica de conflito nem comprova suporte ao payload ampliado. A política acordada para esse caso deve ser reconciliada com o backend em trabalho posterior.

Para aceitar integração ainda são necessários backend versionado compatível, conta e catálogo de teste autorizados e execução controlada de leitura/criação/edição/exclusão. Resultado incerto deve continuar sem reenvio automático.

## Critérios do briefing

| Grupo | Estado em 30/10/2026 |
| --- | --- |
| Modelos, projeção semanal, estimativas e validação | Coberto por testes JVM |
| Agenda, detalhe e CRUD coordenado | Coberto com doubles; integração real pendente |
| Estado em rotação e requests em andamento | Coberto pela nova instrumentação |
| Formulário e descarte | Coberto no fluxo simplificado; horários usam seletor Material, sem campos livres de quantidade/frequência |
| Responsividade, fonte ampliada e acessibilidade | Parcial no aparelho de 360dp; demais larguras e inspeções pendentes |
| Elementos de meta ideal, pico e ações automáticas da referência P09 | Fora do aceite; contrato e decisão continuam pendentes |

## Conclusão

A regressão lógica controlada da Rotina está aprovada no aparelho disponível. O aceite visual completo e o aceite de integração não estão concedidos. As pendências acima precisam de evidência própria antes de marcar a feature como aceita para entrega final.
