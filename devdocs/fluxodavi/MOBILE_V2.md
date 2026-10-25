# Implementação mobile V2

Referência: arquivo Figma Delta, página `3:2` (protótipo mobile), seção `955:2886` (protótipo V2). Consulta em 15/10/2026.

API utilizada: `https://delta-api-postgres.onrender.com/`. As rotas foram verificadas no Swagger público `/v3/api-docs`. Nenhuma escrita foi executada na API de produção durante o desenvolvimento.

## Alterações desta etapa

- Home: fundo `#1B1D1E`, título e espaçamentos ajustados; navegação flutuante sobre o conteúdo, com espaço inferior para acessar os últimos controles. Ícones, fundo do menu e ilustração de rotina exportados do Figma e guardados em `drawable-nodpi`.
- Consumo: seleção de imóvel do usuário, totais de hoje e do mês, gráfico e últimas leituras via `/delta/analytics/consumption-daily/{propertyId}`. Respostas de outra seleção são descartadas pelo `WaterViewModel` existente.
- Custos: previsão via `/delta/property/{propertyId}/watercost`, referência, tarifa, volume em m³, histórico de contas filtrado pelo usuário e acesso ao formulário existente para cadastrar/atualizar a conta de água.
- Ranking de unidades: consulta a `/delta/analytics/property-ranking`, filtrada pelos imóveis associados ao usuário. Mostra posição, consumo, custo e consumo por área retornados pela API, com acesso pela tela de Custos.
- Rotina: consulta, criação, edição e exclusão confirmada de hábitos por `/delta/habits/{userId}`. Os IDs vêm do catálogo já existente. Frequência e dias são validados, e a propriedade do registro é conferida antes de editar ou excluir.
- Menu: Consumo, Custos, Início, Gluh Helper e Rotina. Os cartões de meta da Home abrem um destino próprio. O menu fica oculto no leitor QR e no formulário de conta de água.

## Limites e trabalho restante

Esta etapa não conclui todas as telas do Figma nem garante equivalência visual integral. As novas telas usam componentes Android nativos e dados reais; a validação visual no aparelho ainda está pendente.

O Swagger consultado não publica endpoints para chat, histórico/feedback do chat, metas mensais, indicador EH de 0–100, notificações, alertas de vazamento, fluxo em tempo real, temperaturas ou eventos com horário/duração. Essas funções aparecem como indisponíveis; não há respostas de chatbot, alertas ou pontuações simuladas. Hábitos existentes não são convertidos em eventos com horário inventado.

As telas de adição de unidade e configurações pessoais/residenciais do Figma também não foram implementadas nesta etapa. O backend remoto não foi modificado: o trabalho integra o app aos endpoints publicados.

## Verificação

- Recursos Android compilados e ligados diretamente com `aapt2`, usando recursos completos das bibliotecas AndroidX/Material já existentes no ambiente.
- Todas as fontes Java do app compiladas diretamente com `javac`, com R gerado a partir dos recursos atualizados.
- 13 testes JVM passaram: `SectionDataTest`, `HomeDashboardTest` e `HomeUserMetricsTest`. Incluem filtro por usuário, ordenação de contas, nulos e cálculos da home.
- Gradle (`assembleDebug`, `testDebugUnitTest`, `lintDebug`) bloqueado por erro de loopback do Java, inclusive com o JBR do Android Studio. Compilação direta não substitui build, lint ou teste de instalação completos.
- Nenhum APK novo foi instalado, e não houve teste autenticado de ponta a ponta.

## Detecção de vazamentos — atualização de 17/10/2026

A tela dedicada e os dois destinos de análise agora são acessíveis por Consumo. A interface usa a referência `docs/references/funcionalidade-vazamento.png`, valida imóvel/medidor pela API existente e apresenta indisponibilidade explícita de telemetria. Monitoramento em tempo real e configuração de alertas aguardam endpoints; não existem valores ou eventos simulados. Veja `VAZAMENTOS_TODO.md` e `VAZAMENTOS_DATA_REQUIRED.md`.
