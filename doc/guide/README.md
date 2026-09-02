# hexagonal-scaffold-java — Guia

Template Maven multi-módulo (Java 21 + Spring Boot 3) para arquitetura hexagonal pura. Nasce
validado — os 4 módulos compilam, os 25 testes passam, o jar sobe e responde.

---

## Como abrir este guia

> Rode a partir da **raiz do repo** (`hexagonal-scaffold-java/`).

```bash
npx serve doc/guide
```

Acesse **http://localhost:3000**. Os diagramas Mermaid não renderizam abrindo `index.html`
direto no navegador (`file://`) — precisa de um servidor HTTP, por isso o `npx serve`.

---

## O que está neste guia

| Página | Conteúdo |
|---|---|
| [Arquitetura](arquitetura) | Os 4 módulos Maven, o que cada um pode e não pode depender |
| [Módulo de exemplo — produto](modulo-exemplo) | Passeio pelo CRUD de exemplo, arquivo por arquivo |
| [Naming Conventions](naming-conventions) | Guia definitivo de nomeação de classes por papel arquitetural |
| [Pirâmide de testes](testes) | Os 3 níveis de teste usados aqui e por quê, + cobertura (JaCoCo) |
| [Prompting e CLAUDE.md](ia-workflow) | Como usar um assistente de IA para estender este projeto sem quebrar o padrão |
| [Skill de bootstrap](bootstrap) | Como gerar um projeto novo a partir deste scaffold |
| [Docker e Kubernetes (GCP)](deploy) | Build da imagem, manifests de deploy, por que não tem banco dentro do cluster |

## Links rápidos

- [`README.md`](../../README.md) da raiz — instruções de build/run
- [`CLAUDE.md`](../../CLAUDE.md) — instruções para assistentes de IA que trabalharem neste repo
- Skill de bootstrap: `.claude/skills/bootstrap-hexagonal-project/` (dentro deste repo — ver [Skill de bootstrap](bootstrap))
- Versão Kotlin deste mesmo scaffold: [`hexagonal-scaffold-kotlin`](https://github.com/bacelarnetto/hexagonal-scaffold-kotlin)
