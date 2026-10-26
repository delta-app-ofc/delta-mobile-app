# Chatbot

## Objetivo
Ajudar o usuário do aplicativo com dicas de consumo de água, acompanhamento
de metas e identificação de possíveis vazamentos, promovendo consumo
consciente a partir dos dados do próprio usuário e de dados climáticos.

## Critérios de aceite

Estado revisto na etapa 10, em 26/10/2026: ver [evidências e limites do aceite](acceptance.md).
Os critérios não assinalados continuam pendentes; testes com respostas simuladas
não comprovam integração nem qualidade das respostas reais. As revisões de escopo
aprovadas estão em [decisions.md](decisions.md): histórico climático nesta entrega
(D09), ferramentas de escrita permitidas pelo backend (D08), histórico alternativo
e ausência de “Ver todos” (D04), novo chat e retomada (D05). Sugestões iniciais
continuam pendentes em D03.

- [ ] O usuário consegue enviar uma mensagem e receber a resposta do chatbot
- [ ] As respostas consideram os dados de consumo do usuário
- [ ] As respostas consideram dados climáticos (ex.: calor, chuva)
- [ ] O chatbot dá dicas de consumo consciente
- [ ] O chatbot ajuda a acompanhar metas de consumo
- [ ] Seguir os layouts das telas, conforme o Figma
- [x] Sem texto fixo de apresentação hardcoded na feature (recursos em
  `app/src/main/res/values/chat_strings.xml`; auditoria descrita no aceite)
- [ ] O layout funciona em celular e tablet, retrato e paisagem, sem
  elementos cortados, esticados demais ou com espaço vazio estranho
- [x] Testado nos perfis de referência: celular pequeno (~360dp), celular grande
  (~412dp) e tablet (~800dp), em retrato/paisagem, com fonte normal/ampliada,
  por emulador. Isso comprova os tamanhos em dp, sem comprovar três aparelhos
  físicos ou a diagonal física de 10"; matriz e capturas no [aceite](acceptance.md).

## Fora do escopo
- envio de imagens
- envio de audios
- envio de arquivos
- notificações push
