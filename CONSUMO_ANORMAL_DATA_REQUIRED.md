# Consumo anormal — dados e limites

A tela abre pelo botão “Ver consumo anormal registrado”. Usa listUserProperties e consumptionDaily do imóvel associado ao usuário autenticado. Não há endpoints novos presumidos.

## Disponível

- Linha dos últimos sete dias de calendário, usando a resposta diária completa. Datas ordenadas; duplicados iguais deduplicados; duplicados conflitantes viram lacunas.
- Abas Semanal, Mensal e Anual: quatro períodos consecutivos; totais registrados em janelas de igual duração. Semana começa segunda-feira; referência é a data local do aparelho. No mês, o menor número de dias entre as quatro janelas limita todas elas. A data de corte aparece no cartão; tocar abre os intervalos completos.
- Anterior significa o período imediatamente anterior. Variação = (atual - anterior) / anterior × 100, arredondada; denominador zero não gera percentual.
- Resposta vazia bem-sucedida mostra consumo 0. Falha, fonte ausente ou resposta sem registros válidos não vira 0. Lacunas diárias em resposta não vazia permanecem indisponíveis. Barras somam somente registros disponíveis: são volumes registrados, sem garantia de cobertura integral.
- Seleção de aba preservada; cache durante a vida do ViewModel; nova tentativa após erro; respostas antigas descartadas ao trocar imóvel. Não há atualização em tempo real.

## Fontes necessárias

Diagnóstico real de anomalia (identificador, imóvel/medidor, evento, status, nível, persistência, origem IA), média histórica e consumo detectado com suas janelas, causa identificada, cobertura e qualidade da série, fuso do imóvel, análises por dia da semana e temperatura associada ao período. Sem essas fontes, os cartões mostram indisponibilidade e não afirmam detecção.

Precisamos também confirmar regras de semana/mês/ano, comparação parcial, cobertura de consumptionDaily e significado dos rótulos da referência. monthlyVariation não foi combinado às séries diárias porque seu alinhamento e cobertura não estão confirmados.

## Decisões para não apresentar conclusões falsas

Título neutro enquanto não houver diagnóstico. Linha e barras azuis; vermelho e linha histórica exigem diagnóstico confirmado. O exemplo 500 versus 300 equivale a +67%, não +117%. Não foram copiados os números demonstrativos da imagem.

Rótulos cronológicos S-3, S-2, S-1, Atual (e M-/A- nas demais abas); o exemplo omite S-1. Selo Por semana/Por mês/Por ano em vez de 4h em 4h, que não descreve esses agregados. Estas decisões precisam de validação de produto antes de fechar os itens correspondentes do to-do.

## Validação

AAPT2 e compilação de todas as fontes Java passaram. 35 testes JUnit passaram, incluindo 8 específicos desta agregação. Aparência, navegação em dispositivo, TalkBack, fontes ampliadas, instalação Android 11 e integração com conta real permanecem pendentes. O Gradle completo segue bloqueado pela falha de conexão loopback do ambiente.
