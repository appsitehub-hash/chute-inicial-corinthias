<<<<<<< HEAD
# chute-inicial-corinthias
=======
# App Corinthians — Gerenciador para Escolinha de Futebol (Kotlin)

Aplicação desktop para gerenciar equipes, jogadores e competições de uma escolinha de futebol ligada ao Corinthians.

Descrição rápida
- Gerencia times, jogadores e competições com persistência local em XML (pasta `DB/`).
- Inclui interface gráfica para administração das operações básicas de cadastro, edição, visualização e sorteios.

Funcionalidades atuais
- Cadastrar, editar e remover times com os campos: nome, técnico e avaliação.
- Cadastrar, editar e remover jogadores com os campos: nome, idade, posição e categoria (faixas Sub-7 até Sub-18+).
- Gerenciar categorias: criar e remover categorias aplicáveis aos jogadores.
- Visualizar times e seus jogadores, com agrupamento por categoria quando aplicável.
- Criar competições: definir nome, regra de eliminação (máx perdas), selecionar categorias e escolher participantes.
- Filtrar a lista de participantes por categorias selecionadas ao criar uma competição.
- Gerar sorteios/pareamentos para competições (criação automática de confrontos e indicação de folgas quando necessário).
- Persistência dos dados em `DB/data.xml` (times, jogadores, competições e categorias).

Requisitos mínimos
- JDK 11+ (JDK 17 recomendado).
- Gradle (opcional se usar o wrapper `./gradlew`).

Como executar (desenvolvimento)
1. Build do projeto:

```bash
# a partir da raiz do projeto
./gradlew build
```

2. Rodar a aplicação (modo console):

```bash
./gradlew run --args=""
```

3. Rodar a aplicação com interface gráfica:

```bash
./gradlew run --args="--gui"
```

Executável (distribuição)
- Se houver um `app.jar` empacotado disponível, execute:

```bash
java -jar app.jar --gui
```

Estrutura principal do repositório
- `src/` — código-fonte em Kotlin
- `GUI/` — código da interface gráfica
- `DB/` — dados persistidos (ex.: `data.xml`)
- `IMG/` — imagens e logotipo
- `build.gradle.kts`, `settings.gradle.kts` — configuração do Gradle
- `.gitignore` — regras para não versionar artefatos e arquivos de IDE

Contribuição
- Abra issues para bugs ou sugestões.
- Para contribuições, envie PRs com descrições claras. Adicione testes ao modificar lógica central.

Contato
- Telefone/WhatsApp: 11984517916
- E-mail: hkelvin798@gmail.com
>>>>>>> 223c72d (Prepare repo: keep source/resources, update README and .gitignore)
