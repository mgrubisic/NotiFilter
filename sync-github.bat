@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul

rem ==========================================================================
rem  Sinkronizacija lokalnog projekta NotiFilter s GitHub repozitorijem
rem
rem  Koristenje:
rem    sync-github.bat                     - projekt je u istom folderu kao skripta
rem    sync-github.bat "C:\put\do\projekta" - zadani folder projekta
rem    sync-github.bat "C:\put" dev         - zadani folder i grana
rem
rem  Sto radi:
rem    1. Ako folder jos nije git repozitorij, inicijalizira ga i spoji s
rem       GitHubom tako da lokalne datoteke postanu "izmjene" u odnosu na
rem       zadnje stanje na GitHubu (bez brisanja ikakvih lokalnih datoteka).
rem    2. Pokaze sto se promijenilo i pita za potvrdu.
rem    3. Napravi commit, povuce nove izmjene s GitHuba (pull --rebase)
rem       i posalje (push) lokalne izmjene.
rem ==========================================================================

set "REPO_URL=https://github.com/mgrubisic/NotiFilter.git"

set "PROJECT_DIR=%~dp0"
if not "%~1"=="" set "PROJECT_DIR=%~1"
set "BRANCH=%~2"

echo.
echo === NotiFilter ^<-^> GitHub sinkronizacija ===
echo Repozitorij: %REPO_URL%
echo Folder:      %PROJECT_DIR%
echo.

rem --- Provjera gita ---------------------------------------------------------
where git >nul 2>&1
if errorlevel 1 (
    echo [GRESKA] Git nije instaliran ili nije u PATH-u.
    echo Preuzmi ga s https://git-scm.com/download/win
    goto :fail
)

cd /d "%PROJECT_DIR%" 2>nul
if errorlevel 1 (
    echo [GRESKA] Folder ne postoji: %PROJECT_DIR%
    goto :fail
)

if not exist "settings.gradle.kts" (
    echo [UPOZORENJE] U ovom folderu nema settings.gradle.kts.
    echo Mozda skripta nije u korijenskom folderu projekta.
    choice /c DN /m "Svejedno nastaviti"
    if errorlevel 2 goto :cancel
)

rem --- Identitet za commit ---------------------------------------------------
for /f "delims=" %%a in ('git config user.name 2^>nul') do set "GIT_NAME=%%a"
if not defined GIT_NAME (
    set /p "GIT_NAME=Upisi svoje ime za git commit: "
    git config --global user.name "!GIT_NAME!"
)
for /f "delims=" %%a in ('git config user.email 2^>nul') do set "GIT_EMAIL=%%a"
if not defined GIT_EMAIL (
    set /p "GIT_EMAIL=Upisi e-mail za git commit: "
    git config --global user.email "!GIT_EMAIL!"
)

rem --- Inicijalizacija i remote ----------------------------------------------
if not exist ".git" (
    echo Inicijaliziram git repozitorij...
    git init -q
    if errorlevel 1 goto :fail
)

git remote get-url origin >nul 2>&1
if errorlevel 1 (
    git remote add origin "%REPO_URL%"
) else (
    git remote set-url origin "%REPO_URL%"
)

echo Dohvacam stanje s GitHuba...
git fetch origin
if errorlevel 1 (
    echo [GRESKA] Ne mogu dohvatiti repozitorij. Provjeri internet i adresu.
    goto :fail
)

rem --- Odredivanje grane -----------------------------------------------------
if not defined BRANCH (
    for /f "tokens=2" %%a in ('git ls-remote --symref origin HEAD 2^>nul ^| findstr /b /c:"ref:"') do set "REMOTE_HEAD=%%a"
    if defined REMOTE_HEAD set "BRANCH=!REMOTE_HEAD:refs/heads/=!"
)
if not defined BRANCH set "BRANCH=main"
echo Grana: %BRANCH%

set "REMOTE_HAS_BRANCH=0"
git rev-parse --verify --quiet "refs/remotes/origin/%BRANCH%" >nul 2>&1
if not errorlevel 1 set "REMOTE_HAS_BRANCH=1"

rem --- Prvo pokretanje: lokalni folder jos nema commitova --------------------
git rev-parse --verify --quiet HEAD >nul 2>&1
if errorlevel 1 (
    echo Prvo pokretanje: povezujem lokalne datoteke s granom origin/%BRANCH%...
    git symbolic-ref HEAD "refs/heads/%BRANCH%"
    if "!REMOTE_HAS_BRANCH!"=="1" (
        rem Postavi granu na stanje s GitHuba, a radni folder ostavi netaknut.
        git update-ref "refs/heads/%BRANCH%" "refs/remotes/origin/%BRANCH%"
        git reset -q
        git branch -q --set-upstream-to="origin/%BRANCH%" "%BRANCH%"
    )
) else (
    for /f "delims=" %%a in ('git rev-parse --abbrev-ref HEAD') do set "CUR_BRANCH=%%a"
    if /i not "!CUR_BRANCH!"=="%BRANCH%" (
        echo [UPOZORENJE] Trenutna lokalna grana je !CUR_BRANCH!, a ne %BRANCH%.
        choice /c DN /m "Sinkronizirati granu !CUR_BRANCH! umjesto toga"
        if errorlevel 2 goto :cancel
        set "BRANCH=!CUR_BRANCH!"
        set "REMOTE_HAS_BRANCH=0"
        git rev-parse --verify --quiet "refs/remotes/origin/!BRANCH!" >nul 2>&1
        if not errorlevel 1 set "REMOTE_HAS_BRANCH=1"
    )
)

rem --- Lokalne izmjene: prikaz, potvrda, commit ------------------------------
git add -A
git diff --cached --quiet
if errorlevel 1 (
    echo.
    echo Lokalne izmjene koje ce biti poslane:
    echo ----------------------------------------------------------------
    git status --short
    echo ----------------------------------------------------------------
    echo  M = izmijenjeno, A = novo, D = obrisano
    echo.
    choice /c DN /m "Napraviti commit s ovim izmjenama"
    if errorlevel 2 (
        git reset -q
        goto :cancel
    )
    set "MSG="
    set /p "MSG=Commit poruka (Enter = automatska): "
    if not defined MSG set "MSG=Sinkronizacija %date% %time:~0,5%"
    git commit -q -m "!MSG!"
    if errorlevel 1 goto :fail
    echo Commit napravljen.
) else (
    echo Nema lokalnih izmjena za commit.
)

rem --- Povlacenje novih izmjena s GitHuba ------------------------------------
if "%REMOTE_HAS_BRANCH%"=="1" (
    echo Povlacim nove izmjene s GitHuba...
    git pull --rebase --autostash origin "%BRANCH%"
    if errorlevel 1 (
        echo.
        echo [KONFLIKT] Iste datoteke su mijenjane lokalno i na GitHubu.
        echo Rijesi konflikte u oznacenim datotekama, zatim pokreni:
        echo     git add -A
        echo     git rebase --continue
        echo i ponovno pokreni ovu skriptu. Za odustajanje: git rebase --abort
        goto :fail
    )
)

rem --- Slanje na GitHub ------------------------------------------------------
echo Saljem izmjene na GitHub...
git push -u origin "%BRANCH%"
if errorlevel 1 (
    echo [GRESKA] Push nije uspio. Provjeri imas li pravo pisanja u repozitorij
    echo i jesi li prijavljen na GitHub - prozor za prijavu otvara Git Credential Manager.
    goto :fail
)

echo.
echo [OK] Lokalni folder i GitHub ^(%BRANCH%^) su sinkronizirani.
echo.
pause
exit /b 0

:cancel
echo Prekinuto, nista nije poslano.
pause
exit /b 1

:fail
echo.
pause
exit /b 1
