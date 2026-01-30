# App Corinthians — Funcionalidades

Esta aplicação desktop em Kotlin é um gerenciador simples para escolinhas de futebol.

Funcionalidades

- Cadastrar, editar e remover times (nome, técnico, avaliação).
- Cadastrar, editar e remover jogadores (nome, idade, posição, categoria).
- Gerenciar categorias de idade para jogadores.
- Visualizar times e seus jogadores, com agrupamento por categoria.
- Criar e gerenciar competições (definir nome, regra de eliminação e participantes).
- Gerar sorteios/pareamentos para competições.
- Persistência local em XML no diretório `DB/`.

Requisitos básicos

- JDK 11+ (recomendado JDK 17)
- Gradle (ou usar o wrapper `./gradlew`)

Como executar (ex.: desenvolvimento)

1. Build:

```bash
./gradlew build
```

2. Executar (modo GUI):

```bash
./gradlew run --args="--gui"
```

3. Executar (modo console):

```bash
./gradlew run --args=""
```


-- Esta README foi reduzida para mostrar apenas as funcionalidades principais do sistema.
