# AGENTS.md — Contexto do delta-app-mobile

Este arquivo orienta agentes de IA e pessoas desenvolvedoras que atuam neste repositório. Leia estas instruções antes de alterar qualquer arquivo e aplique-as em conjunto com a tarefa local.

## 1. Visão geral do Projeto Delta

O Projeto Delta é uma iniciativa acadêmica de monitoramento inteligente do consumo residencial de água. Sensores conectados a hidrômetros enviam dados para uma arquitetura que consolida o consumo, apoia a detecção de vazamentos, permite estimativas de gastos e disponibiliza informações por aplicações web, mobile e chatbot.

A plataforma possui responsabilidades distribuídas entre diferentes repositórios e tecnologias. A camada de dados adota PostgreSQL para informações cadastrais, relacionais e transacionais e MongoDB para telemetria IoT e dados de aplicação de alto volume. Redis e Neo4j aparecem na documentação de arquitetura como componentes planejados, mas não devem ser tratados como implementados neste repositório.

Cada repositório da organização `delta-app-ofc` possui histórico Git próprio. A pasta local que agrupa os repositórios não é um repositório Git e não deve ser inicializada como um.

## 2. Contexto deste repositório

O `delta-app-mobile` é o repositório destinado ao desenvolvimento do aplicativo móvel do Projeto Delta.

No estado atual da `main`, ainda não existe código-fonte do aplicativo nem uma tecnologia mobile definida por arquivos versionados. Não há manifesto de dependências, configuração de build, estrutura de telas, testes automatizados ou integração de API implementados neste repositório. Portanto, não presuma o uso de React Native, Expo, Flutter, Android nativo, iOS nativo ou qualquer outro framework ou linguagem sem que uma tarefa e arquivos oficiais os introduzam.

### Estrutura confirmada

- `README.md`: identifica o repositório como o espaço central de desenvolvimento do aplicativo móvel;
- `LICENSE`: licença do repositório;
- `.gitignore`: ignora configurações locais de IDE, arquivos `.env` e diretórios `__pycache__`;
- `.github/workflows/trigger_actions.yml`: chama o workflow reutilizável da organização para validar informações e checklists de Pull Requests.

O workflow existente não compila o aplicativo e não executa testes mobile. Ele deve ser descrito apenas como automação de governança de Pull Requests enquanto não houver outros jobs versionados.

### Tecnologias confirmadas

Neste momento, os únicos formatos técnicos confirmados no conteúdo versionado são Markdown para documentação e YAML para o workflow do GitHub Actions. O produto de destino é um aplicativo móvel, mas sua stack de implementação ainda não está registrada no repositório.

### Limites de atuação

- confira a árvore, o histórico e os arquivos reais antes de descrever a arquitetura;
- não invente telas, fluxos, APIs, modelos, bibliotecas, ferramentas ou integrações;
- não copie para este repositório estruturas internas presumidas de backend, bancos de dados, frontend web ou agentes de IA;
- trate protótipos, requisitos e tecnologias citados fora do repositório como referência, não como implementação confirmada;
- altere apenas os arquivos necessários ao escopo da tarefa.

## 3. Leitura obrigatória do TASK.md

Antes de executar qualquer tarefa, leia integralmente o arquivo `TASK.md` localizado na raiz deste repositório. Confirme nele o objetivo, as restrições, as dependências e os critérios de conclusão antes de modificar arquivos.

Se o `TASK.md` não existir, não o crie por iniciativa própria. Informe explicitamente a ausência e obtenha uma instrução de tarefa válida antes de alterar o repositório. Uma solicitação específica não autoriza mudanças fora do escopo descrito nela.

## 4. Padrão de branches e commits

Toda alteração deve ser feita em uma branch deste repositório, criada a partir da `main` atualizada.

O padrão de branches definido no `delta-handbook` é:

```text
<tipo>/<descricao-da-alteracao>
```

Tipos permitidos:

- `feat`: nova funcionalidade;
- `fix`: correção de bug;
- `refactor`: reorganização sem mudança de comportamento;
- `docs`: alteração de documentação;
- `test`: criação ou manutenção de testes;
- `style`: alteração exclusivamente de estilo.

Use uma descrição curta, objetiva, em minúsculas e separada por hífens, como `docs/agents-md` ou `feat/tela-consumo`. O exemplo não confirma que a funcionalidade já exista.

Fluxo de criação:

```text
git checkout main
git pull origin main
git checkout -b <tipo>/<descricao-da-alteracao>
```

Os commits seguem Conventional Commits no formato:

```text
<tipo>: descrição
```

Use os mesmos tipos permitidos para branches. Prefira commits pequenos e coerentes, não misture alterações sem relação e mantenha cada mudança no repositório responsável.

## 5. Padrão de documentação

Documentos novos ou atualizados devem seguir o padrão do `delta-handbook`:

- usar Markdown com extensão `.md`;
- apresentar título e objetivo claros;
- fornecer contexto, motivação e justificativas relevantes;
- organizar o conteúdo com hierarquia coerente de títulos e subtítulos;
- usar listas, tabelas, blocos de código e diagramas quando melhorarem a compreensão;
- nomear arquivos em minúsculas, com palavras separadas por hífen, como `fluxo-autenticacao.md`;
- verificar se já existe conteúdo semelhante antes de criar outro documento;
- documentar decisões e manter o histórico de atualizações relevantes quando aplicável;
- solicitar revisão das alterações relevantes antes do merge.

Ao registrar comportamento técnico, diferencie claramente o que foi confirmado em arquivos ou por execução, o que está apenas documentado como requisito e o que ainda é planejamento. Não apresente funcionalidades futuras como implementadas.

## 6. Manutenção da estrutura do repositório

A seção **Contexto deste repositório** representa a estrutura confirmada no momento da criação deste arquivo. Ela será revisada e atualizada periodicamente nesta conversa após commits oficiais modificarem a organização, as tecnologias ou as responsabilidades do repositório.

Antes de confiar integralmente nesta descrição, compare-a com a árvore e o histórico atuais da `main`. Não antecipe nesta seção arquivos ou tecnologias que ainda não tenham sido incorporados oficialmente.
