# Como Apagar o Histórico de Commits

Este guia explica como reduzir todo o histórico de commits do repositório para um único commit, mantendo apenas o estado atual dos arquivos.

## ⚠️ ATENÇÃO - Importante Ler Antes de Executar

- **Esta operação é IRREVERSÍVEL** - todo o histórico de commits será perdido permanentemente
- **Requer force push** - todos os colaboradores precisarão fazer um novo clone do repositório
- **Faça backup** - recomenda-se fazer um backup do repositório antes de executar
- **Branches** - todas as branches que não forem atualizadas ficarão dessincronizadas

## Método 1: Script Automatizado (Recomendado)

Execute o script `squash_history.sh` incluído neste repositório:

```bash
# Torne o script executável
chmod +x squash_history.sh

# Execute o script
./squash_history.sh
```

O script irá:
1. Criar uma nova branch órfã (sem histórico)
2. Adicionar todos os arquivos atuais
3. Criar um único commit inicial
4. Substituir a branch principal
5. Fazer force push (com confirmação)

## Método 2: Passos Manuais

Se preferir fazer manualmente, siga estes passos:

### Passo 1: Fazer backup (opcional mas recomendado)
```bash
# Clone o repositório em outro local como backup
cd ..
git clone https://github.com/appsitehub-hash/chute-inicial-corinthias.git chute-inicial-corinthias-backup
```

### Passo 2: Criar uma nova branch órfã
```bash
# Certifique-se de estar na branch principal
git checkout main  # ou master, dependendo do nome da sua branch principal

# Crie uma nova branch órfã (sem histórico)
git checkout --orphan nova-branch-limpa
```

### Passo 3: Adicionar todos os arquivos
```bash
# Adicione todos os arquivos ao staging
git add -A

# Crie o primeiro (e único) commit
git commit -m "Initial commit - Estado atual do projeto"
```

### Passo 4: Substituir a branch principal
```bash
# Delete a branch antiga
git branch -D main  # ou master

# Renomeie a nova branch para o nome principal
git branch -m main  # ou master
```

### Passo 5: Force push para o GitHub
```bash
# Faça force push para o repositório remoto
git push -f origin main  # ou master
```

## Método 3: Usando Git Reset (Alternativa)

Outra abordagem é usar `git reset` para voltar ao início e criar um novo commit:

```bash
# Obtenha o hash do primeiro commit
FIRST_COMMIT=$(git rev-list --max-parents=0 HEAD)

# Faça soft reset para o primeiro commit
git reset --soft $FIRST_COMMIT

# Crie um novo commit com todas as mudanças
git commit --amend -m "Initial commit - Estado atual do projeto"

# Force push
git push -f origin main  # ou master
```

## Verificação

Após executar qualquer um dos métodos, verifique o histórico:

```bash
# Deve mostrar apenas 1 commit
git log --oneline

# Verifique que todos os arquivos estão presentes
git status
```

## Avisar Colaboradores

Após fazer o force push, todos os colaboradores precisarão atualizar seus repositórios locais:

```bash
# Cada colaborador deve executar:
git fetch origin
git reset --hard origin/main  # ou master
```

Ou simplesmente fazer um novo clone:

```bash
git clone https://github.com/appsitehub-hash/chute-inicial-corinthias.git
```

## Perguntas Frequentes

**P: Posso recuperar o histórico depois?**
R: Não, a menos que você tenha feito um backup antes. O histórico será permanentemente perdido.

**P: Isso afeta os arquivos do projeto?**
R: Não, todos os arquivos e suas versões mais recentes serão mantidos. Apenas o histórico de commits é removido.

**P: Preciso avisar minha equipe?**
R: Sim! Todos que têm cópias locais do repositório precisarão atualizar ou fazer um novo clone.

**P: Isso funciona com branches protegidas?**
R: Você precisará desabilitar temporariamente a proteção de branch no GitHub antes de fazer o force push.

## Desabilitar Proteção de Branch no GitHub

Se sua branch principal estiver protegida:

1. Acesse: Settings → Branches → Branch protection rules
2. Edite a regra da branch principal
3. Desabilite temporariamente "Do not allow force pushes"
4. Execute o squash do histórico
5. Reative a proteção

## Alternativa Sem Force Push

Se você não puder fazer force push (por exemplo, em repositórios com políticas rígidas), considere:

1. Criar um novo repositório
2. Copiar apenas os arquivos atuais (sem o `.git`)
3. Inicializar um novo repositório Git
4. Fazer o commit inicial
5. Fazer push para o novo repositório
