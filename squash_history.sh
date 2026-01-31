#!/bin/bash

# Script para apagar todo o histórico de commits e manter apenas um commit inicial
# ATENÇÃO: Esta operação é IRREVERSÍVEL!

set -e  # Para a execução em caso de erro

echo "=========================================="
echo "  SQUASH DE HISTÓRICO DE COMMITS"
echo "=========================================="
echo ""
echo "⚠️  ATENÇÃO: Esta operação irá:"
echo "  - Apagar TODO o histórico de commits"
echo "  - Criar um único commit com o estado atual"
echo "  - Fazer FORCE PUSH para o repositório remoto"
echo "  - Todos os colaboradores precisarão fazer novo clone"
echo ""
echo "Esta operação é IRREVERSÍVEL!"
echo ""

# Pergunta de confirmação
read -p "Você tem certeza que deseja continuar? (digite 'sim' para confirmar): " confirmacao

if [ "$confirmacao" != "sim" ]; then
    echo "Operação cancelada."
    exit 0
fi

echo ""
echo "Iniciando processo..."
echo ""

# Detectar a branch atual
CURRENT_BRANCH=$(git branch --show-current)
echo "Branch atual detectada: $CURRENT_BRANCH"
echo ""

# Perguntar qual branch usar
read -p "Deseja usar esta branch ou especificar outra? (pressione Enter para usar '$CURRENT_BRANCH' ou digite o nome): " BRANCH_INPUT

if [ -n "$BRANCH_INPUT" ]; then
    BRANCH_NAME="$BRANCH_INPUT"
else
    BRANCH_NAME="$CURRENT_BRANCH"
fi

echo "Branch selecionada: $BRANCH_NAME"
echo ""

# Verificar se há mudanças não commitadas
if ! git diff-index --quiet HEAD --; then
    echo "❌ Erro: Há mudanças não commitadas."
    echo "Por favor, faça commit ou stash das mudanças antes de continuar."
    exit 1
fi

echo "Passo 1/6: Checkout para a branch $BRANCH_NAME..."
git checkout "$BRANCH_NAME"

echo "Passo 2/6: Criando nova branch órfã..."
git checkout --orphan temp-squash-branch

echo "Passo 3/6: Adicionando todos os arquivos..."
git add -A

echo "Passo 4/6: Criando commit inicial..."
git commit -m "Initial commit - Estado atual do projeto

Este commit contém todo o estado atual do projeto.
O histórico anterior foi removido para simplificar o repositório."

echo "Passo 5/6: Substituindo a branch $BRANCH_NAME..."
git branch -D "$BRANCH_NAME"
git branch -m "$BRANCH_NAME"

echo ""
echo "Preparação concluída!"
echo ""
echo "=========================================="
echo "  PRONTO PARA FORCE PUSH"
echo "=========================================="
echo ""
echo "O repositório local foi atualizado."
echo "Agora você precisa fazer FORCE PUSH para o repositório remoto."
echo ""
echo "⚠️  ÚLTIMA CHANCE DE DESISTIR!"
echo ""
read -p "Deseja fazer force push agora? (digite 'sim' para confirmar): " push_confirmacao

if [ "$push_confirmacao" != "sim" ]; then
    echo ""
    echo "Force push cancelado."
    echo "Seu repositório local foi modificado, mas o remoto permanece inalterado."
    echo ""
    echo "Para fazer o push manualmente depois, execute:"
    echo "  git push -f origin $BRANCH_NAME"
    echo ""
    echo "Para reverter as mudanças locais:"
    echo "  git checkout $BRANCH_NAME"
    echo "  git reset --hard origin/$BRANCH_NAME"
    exit 0
fi

echo ""
echo "Passo 6/6: Fazendo force push para origin/$BRANCH_NAME..."
git push -f origin "$BRANCH_NAME"

echo ""
echo "=========================================="
echo "  ✅ CONCLUÍDO COM SUCESSO!"
echo "=========================================="
echo ""
echo "O histórico foi reduzido a um único commit."
echo ""
echo "Verificação:"
git log --oneline -5
echo ""
echo "IMPORTANTE: Avise todos os colaboradores!"
echo "Eles precisarão atualizar seus repositórios locais com:"
echo "  git fetch origin"
echo "  git reset --hard origin/$BRANCH_NAME"
echo ""
echo "Ou fazer um novo clone do repositório."
echo ""
