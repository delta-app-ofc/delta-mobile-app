# Sonar e testes Android

O workflow `Validação Sonar` compila o APK debug, executa a suíte JVM e gera cobertura JaCoCo antes de analisar o mesmo SHA. Roda em PRs e pushes para `develop` e `main`, além da execução manual. Não precisa de emulador; testes instrumentados continuam sendo uma validação separada.

## Executar localmente

Use o JDK 21 e o Android SDK 36 configurados no ambiente:

```powershell
.\gradlew.bat --no-daemon :app:assembleDebug :app:createDebugUnitTestCoverageReport :app:prepareSonarLibraries
python scripts/verify_sonar_reports.py
python -m unittest discover -s scripts -p 'test_*.py'
```

Os testes existentes ficam em `app/src/test`. O verificador exige pelo menos um teste executado, nenhum teste com falha, erro ou ignorado e um XML JaCoCo com linhas cobertas. Os testes Python verificam a rejeição de relatórios ausentes, vazios e malsucedidos; não substituem testes do aplicativo.

## Relatórios e análise

O artefato `sonar-reports` preserva classes compiladas, dependências JAR, JUnit e cobertura. O scanner compartilhado restaura esses arquivos em `app/build`. O escopo de análise inclui todas as fontes de `app/src/main`, sem excluir telas ou regras de negócio da cobertura.

O check `Delta / Política Sonar` usa a política compartilhada da organização: cobertura mínima de 70% no código novo instrumentável; problemas graves novos de segurança bloqueiam, confiabilidade exige revisão adicional e manutenibilidade avisa. O gate padrão do Sonar não substitui essa política.

## Configuração no GitHub e no Sonar

Importe `delta-app-ofc/delta-mobile-app` no SonarQube Cloud e desative Automatic Analysis. Configure `SONAR_ORGANIZATION`, `SONAR_PROJECT_KEY`, `SONAR_TOKEN`, `SONAR_POLICY_TOKEN` e preserve `DELTA_ORG_AUTOMATION_TOKEN`, conforme a documentação compartilhada em `delta-app-ofc/.github/docs/sonar.md`. Os padrões de organização/projeto são `delta-app-ofc` e `delta-app-ofc_delta-mobile-app`.

Execute a análise da branch de destino para estabelecer o baseline antes de avaliar cobertura nova. PRs para `develop` e análises dessa branch dependem do suporte do plano Sonar a branches adicionais; se a conta restringir análises à branch principal, é necessário ajustar o plano/configuração no Sonar. Não se deve declarar `develop` como `main` para contornar essa restrição.

Os eventos `workflow_run` dos workflows de política precisam estar na branch padrão do GitHub para serem disparados. Uma PR que adiciona estes arquivos apenas à `develop` não ativa esse evento na `main`. O check de testes Android e a análise podem rodar na PR de implantação; a ativação completa da política exige que os wrappers confiáveis também cheguem à branch padrão.

Depois de validar as execuções reais, acrescente `Testes e relatórios Android` e `Delta / Política Sonar` aos checks obrigatórios, preservando as proteções existentes. Esta alteração não muda proteção de branches nem faz merge ou deploy.

Referências: [cobertura Android](https://developer.android.com/studio/test/coverage-report), [análise Java no Sonar](https://docs.sonarsource.com/sonarqube-cloud/analyzing-source-code/languages/java).
