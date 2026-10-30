# CI técnico

## Objetivo

Em pull requests e por execução manual, o workflow verifica se `README.md`, `AGENTS.md` e `LICENSE` existem, podem ser lidos como UTF-8 e não estão vazios. A falha de qualquer verificação interrompe o job.

## Execução local

Na raiz do repositório, execute com Python 3:

```bash
python -c "from pathlib import Path; files = ('README.md', 'AGENTS.md', 'LICENSE'); [(print('OK:', f) if Path(f).read_text(encoding='utf-8').strip() else (_ for _ in ()).throw(ValueError(f'{f} vazio'))) for f in files]"
```

Esta verificação não compila nem testa um aplicativo: a versão atual do repositório ainda não contém código mobile ou ferramenta de build. Quando forem adicionados, a CI deve ser atualizada para executar seus comandos reais de build e testes.
