# Contributing

Obrigado por contribuir com SafePix Async.

## Fluxo de trabalho

1. Crie uma branch a partir de `main`.
2. Use nomes claros, como `feat/nova-fila`, `fix/validacao-pix` ou `docs/readme`.
3. Mantenha mudancas pequenas e focadas.
4. Rode testes antes de abrir PR.
5. Abra PR descrevendo problema, solucao e evidencias de teste.

## Padrao de commits

Use Conventional Commits:

```text
feat: adiciona novo comportamento
fix: corrige falha
docs: altera documentacao
test: adiciona ou ajusta testes
refactor: reorganiza codigo sem mudar comportamento
chore: tarefa de manutencao
```

## Qualidade

Antes de enviar mudancas:

```powershell
.\mvnw.cmd test
```

Para validar build sem testes:

```powershell
.\mvnw.cmd -DskipTests package
```

## Padrao de codigo

- Use Java 21.
- Prefira injecao por construtor.
- Mantenha nomes de filas e exchanges centralizados em configuracao.
- Evite regra de negocio em controllers.
- Valide entradas HTTP com Bean Validation.
- Inclua testes para comportamento novo.

## Pull requests

PR deve conter:

- Objetivo da mudanca.
- Arquivos principais alterados.
- Como testar.
- Riscos ou limitacoes conhecidas.

Checklist sugerido:

- [ ] Codigo compila.
- [ ] Testes passam.
- [ ] Documentacao atualizada quando necessario.
- [ ] Nao inclui credenciais, tokens ou dados sensiveis.

## Seguranca

Nao commite:

- Senhas reais.
- Tokens.
- Chaves privadas.
- Arquivos `.env` com dados sensiveis.
- Dumps de banco ou mensagens reais de clientes.

