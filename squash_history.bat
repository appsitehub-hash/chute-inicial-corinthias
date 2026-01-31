@echo off
REM Script para apagar todo o histórico de commits e manter apenas um commit inicial
REM ATENÇÃO: Esta operação é IRREVERSÍVEL!

setlocal enabledelayedexpansion

echo ==========================================
echo   SQUASH DE HISTÓRICO DE COMMITS
echo ==========================================
echo.
echo ⚠️  ATENÇÃO: Esta operação irá:
echo   - Apagar TODO o histórico de commits
echo   - Criar um único commit com o estado atual
echo   - Fazer FORCE PUSH para o repositório remoto
echo   - Todos os colaboradores precisarão fazer novo clone
echo.
echo Esta operação é IRREVERSÍVEL!
echo.

REM Pergunta de confirmação
set /p confirmacao="Você tem certeza que deseja continuar? (digite 'sim' para confirmar): "

if not "!confirmacao!"=="sim" (
    echo Operação cancelada.
    exit /b 0
)

echo.
echo Iniciando processo...
echo.

REM Detectar a branch atual
for /f "tokens=*" %%i in ('git branch --show-current') do set CURRENT_BRANCH=%%i
echo Branch atual detectada: !CURRENT_BRANCH!
echo.

REM Perguntar qual branch usar
set /p BRANCH_INPUT="Deseja usar esta branch ou especificar outra? (pressione Enter para usar '!CURRENT_BRANCH!' ou digite o nome): "

if "!BRANCH_INPUT!"=="" (
    set BRANCH_NAME=!CURRENT_BRANCH!
) else (
    set BRANCH_NAME=!BRANCH_INPUT!
)

echo Branch selecionada: !BRANCH_NAME!
echo.

REM Verificar se há mudanças não commitadas
git diff-index --quiet HEAD -- 2>nul
if errorlevel 1 (
    echo ❌ Erro: Há mudanças não commitadas.
    echo Por favor, faça commit ou stash das mudanças antes de continuar.
    exit /b 1
)

echo Passo 1/6: Checkout para a branch !BRANCH_NAME!...
git checkout "!BRANCH_NAME!"

echo Passo 2/6: Criando nova branch órfã...
git checkout --orphan temp-squash-branch

echo Passo 3/6: Adicionando todos os arquivos...
git add -A

echo Passo 4/6: Criando commit inicial...
git commit -m "Initial commit - Estado atual do projeto" -m "Este commit contém todo o estado atual do projeto. O histórico anterior foi removido para simplificar o repositório."

echo Passo 5/6: Substituindo a branch !BRANCH_NAME!...
git branch -D "!BRANCH_NAME!"
git branch -m "!BRANCH_NAME!"

echo.
echo Preparação concluída!
echo.
echo ==========================================
echo   PRONTO PARA FORCE PUSH
echo ==========================================
echo.
echo O repositório local foi atualizado.
echo Agora você precisa fazer FORCE PUSH para o repositório remoto.
echo.
echo ⚠️  ÚLTIMA CHANCE DE DESISTIR!
echo.
set /p push_confirmacao="Deseja fazer force push agora? (digite 'sim' para confirmar): "

if not "!push_confirmacao!"=="sim" (
    echo.
    echo Force push cancelado.
    echo Seu repositório local foi modificado, mas o remoto permanece inalterado.
    echo.
    echo Para fazer o push manualmente depois, execute:
    echo   git push -f origin !BRANCH_NAME!
    echo.
    echo Para reverter as mudanças locais:
    echo   git checkout !BRANCH_NAME!
    echo   git reset --hard origin/!BRANCH_NAME!
    exit /b 0
)

echo.
echo Passo 6/6: Fazendo force push para origin/!BRANCH_NAME!...
git push -f origin "!BRANCH_NAME!"

echo.
echo ==========================================
echo   ✅ CONCLUÍDO COM SUCESSO!
echo ==========================================
echo.
echo O histórico foi reduzido a um único commit.
echo.
echo Verificação:
git log --oneline -5
echo.
echo IMPORTANTE: Avise todos os colaboradores!
echo Eles precisarão atualizar seus repositórios locais com:
echo   git fetch origin
echo   git reset --hard origin/!BRANCH_NAME!
echo.
echo Ou fazer um novo clone do repositório.
echo.

endlocal
