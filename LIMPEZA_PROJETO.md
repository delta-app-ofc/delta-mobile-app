# Limpeza do projeto

## Arquivos removidos

- `app/src/test/java/com/aula/delta_mobile_app/ExampleUnitTest.java` — teste de exemplo gerado pelo Android Studio; só verificava `2 + 2` e não exercitava código do app.
- `app/src/androidTest/java/com/aula/delta_mobile_app/ExampleInstrumentedTest.java` — teste de exemplo gerado pelo Android Studio; só verificava o `applicationId`, já coberto pela configuração do módulo.

## Auditoria realizada

Foram examinadas as fontes Java, os testes, os recursos Android, o manifest, a navegação, as configurações Gradle e a documentação do repositório. Os arquivos Android restantes têm referências em código, XML, manifest ou configuração, ou documentam contratos, requisitos e decisões das funcionalidades existentes. Não foram removidos arquivos apenas por estarem fora do caminho principal da Home.

Também foram preservados arquivos locais de configuração e saídas/caches ignorados pelo Git, como `.env`, `local.properties`, `.gradle` e `build`. Eles podem conter configuração da máquina ou acelerar o desenvolvimento; a auditoria não tratou esses itens como lixo descartável.

## Limite da verificação

A busca de referências foi estática e não prova ausência de uso por reflexão, nome dinâmico ou ferramenta externa. A tarefa Gradle `:app:lintDebug` não iniciou: o Wrapper recebeu `FileNotFoundException` com acesso negado ao lock em `%USERPROFILE%\\.gradle\\wrapper\\dists`. Portanto, a lista acima registra somente os arquivos removidos com uso comprovadamente irrelevante; não afirma que lint ou build confirmaram a ausência de outros candidatos.
