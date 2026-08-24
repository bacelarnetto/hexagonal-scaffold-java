# Skill de bootstrap

Gerar um projeto novo a partir deste scaffold tem dois caminhos.

## Caminho manual (formulário como checklist)

Preencha estes dados e siga o passo a passo do [`README.md`](../../README.md#gerando-um-projeto-novo-a-partir-daqui)
da raiz:

| Campo | Exemplo |
|---|---|
| Nome da aplicação | `padaria-api` |
| Pacote base | `br.com.padaria` |
| Diretório de destino | `~/work/padaria-api` |
| Primeiro domínio de negócio | `pedido` |
| Entidade de exemplo | `Pedido` |
| Banco de dados | MySQL (único suportado hoje) |

## Caminho via IA (o mesmo formulário, mas conversado)

Se você usa Claude Code, existe um skill em `.claude/skills/bootstrap-hexagonal-project/` — dentro
deste repo, então chega junto quando alguém clona o scaffold, sem precisar instalar nada à parte.
Ele faz exatamente os mesmos passos do caminho manual, mas perguntando os campos acima
interativamente e executando a cópia/renomeação/troca de banco por você. Para acionar: peça para
"criar um projeto novo a partir do hexagonal-scaffold-java" (ou `/bootstrap-hexagonal-project` se o
nome for reconhecido como skill na sua sessão).

O skill:
1. Pergunta os 5 campos da tabela acima (banco fixo em MySQL por enquanto)
2. Copia o scaffold para o destino e remove `target/`/`.git` do template
3. Renomeia pacote Java e coordenadas Maven (`groupId`, `artifactId` de cada um dos 4 módulos) e a
   classe principal `ScaffoldApplication`
4. Renomeia o módulo de exemplo `produto` para o domínio informado
5. Troca H2 por MySQL + Testcontainers (ver `mysql-setup.md` dentro da pasta do skill) e gera
   `docker-compose.yml`
6. Roda `mvn test` no projeto gerado antes de considerar a tarefa concluída

Esse skill é genérico — serve para qualquer projeto novo, não só derivados de um domínio específico.

## Por que dois caminhos

O caminho manual funciona sem depender de nenhuma ferramenta de IA. O caminho via IA existe porque
a renomeação de pacote é o passo mais fácil de fazer pela metade sem perceber (import esquecido,
diretório não movido) — um assistente seguindo o skill como checklist reduz esse risco, mas não é
obrigatório.
