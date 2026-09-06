<#
.SYNOPSIS
    Renomeia o molde para um projeto novo.

.DESCRIPTION
    O molde nasce com o nome neutro `app`: módulos `app-model`..`app-deploy`, pacote
    `br.com.gems.app`, classe `AppApplication`, realm `app-realm`, banco `app-db`. Este script troca
    tudo isso pelo nome do seu projeto — diretórios, pacotes, classes, artifactIds, nomes de
    contêiner, rede, volume, banco, realm, imagens e as ocorrências textuais nos arquivos de
    governança.

    Rode-o UMA VEZ, logo depois de clonar, com a árvore limpa. Ele é destrutivo por natureza: se
    houver alteração não commitada, ele se recusa a rodar, para que o `git diff` continue sendo o
    caminho de volta.

.PARAMETER Nome
    O nome do projeto, em minúsculas, sem espaço. Vira o prefixo dos módulos (`<nome>-model`), o
    último segmento do pacote e a base dos nomes de infraestrutura.

    Precisa ser um identificador Java válido em minúsculas: letras e dígitos, começando por letra.
    Hífen NÃO serve — ele quebraria o nome do pacote.

.PARAMETER Grupo
    O groupId Maven e o prefixo do pacote. Padrão: `br.com.gems`.

.PARAMETER Descricao
    Uma linha para o README e para o `<description>` do POM agregador.

.PARAMETER DryRun
    Mostra o que seria alterado, sem alterar nada.

.EXAMPLE
    .\scripts\inicializar-projeto.ps1 -Nome vendas -Descricao "Gestão de vendas e pós-venda"

.EXAMPLE
    .\scripts\inicializar-projeto.ps1 -Nome vendas -Grupo com.acme -DryRun
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-z][a-z0-9]*$')]
    [string]$Nome,

    [string]$Grupo = 'br.com.gems',

    [string]$Descricao = '',

    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot

# ---------------------------------------------------------------------------------------------
# Guardas. Nenhuma delas é excesso de zelo: sem elas, um rename parcial fica indistinguível de um
# rename completo, e é preciso ler o repositório inteiro para descobrir a diferença.
# ---------------------------------------------------------------------------------------------

if ($Nome -eq 'app') {
    throw "O nome 'app' é o do próprio molde. Escolha o nome do seu projeto."
}

Push-Location $raiz
try {
    $sujo = git status --porcelain --untracked-files=all
    if ($sujo -and -not $DryRun) {
        Write-Host 'Árvore de trabalho suja. Commite ou descarte antes de renomear:' -ForegroundColor Red
        $sujo | ForEach-Object { Write-Host "  $_" }
        throw 'Abortado. O `git diff` precisa ser o caminho de volta.'
    }
} finally {
    Pop-Location
}

$Pascal = $Nome.Substring(0, 1).ToUpper() + $Nome.Substring(1)

Write-Host ''
Write-Host "Renomeando o molde para '$Nome'" -ForegroundColor Cyan
Write-Host "  módulos ....... app-*            -> $Nome-*"
Write-Host "  pacote ........ br.com.gems.app  -> $Grupo.$Nome"
Write-Host "  classe main ... AppApplication   -> ${Pascal}Application"
Write-Host "  infra ......... app-db, app-realm, app_containers -> $Nome-*"
if ($DryRun) { Write-Host '  MODO SECO: nada será alterado.' -ForegroundColor Yellow }
Write-Host ''

# ---------------------------------------------------------------------------------------------
# 1. Conteúdo dos arquivos.
#
# A ORDEM DAS SUBSTITUIÇÕES IMPORTA: as mais específicas primeiro. Trocar `app` isolado antes de
# `br.com.gems.app` produziria `br.com.gems.<nome>` corretamente por acidente, mas destruiria
# `application.yml`, `ApplicationContext` e toda palavra que contenha "app". Por isso não existe
# nenhuma regra que troque `app` solto.
# ---------------------------------------------------------------------------------------------

$grupoAntigoPacote = 'br.com.gems.app'
$grupoNovoPacote = "$Grupo.$Nome"

$substituicoes = [ordered]@{
    # pacote e groupId
    $grupoAntigoPacote                  = $grupoNovoPacote
    'br/com/gems/app'                   = ($grupoNovoPacote -replace '\.', '/')
    '<groupId>br.com.gems</groupId>'    = "<groupId>$Grupo</groupId>"
    # módulos Maven
    'app-model'                         = "$Nome-model"
    'app-security'                      = "$Nome-security"
    'app-events'                        = "$Nome-events"
    'app-core'                          = "$Nome-core"
    'app-deploy'                        = "$Nome-deploy"
    # classe principal
    'AppApplication'                    = "${Pascal}Application"
    # infraestrutura
    'app_containers'                    = "${Nome}_containers"
    'app_staging_network'               = "${Nome}_staging_network"
    'app-postgres'                      = "$Nome-postgres"
    'app-keycloak'                       = "$Nome-keycloak"
    'app-localstack'                    = "$Nome-localstack"
    'app-backend'                       = "$Nome-backend"
    'app-frontend'                      = "$Nome-frontend"
    'app-db'                            = "$Nome-db"
    'app-access'                        = "$Nome-access"
    'app-realm'                         = "$Nome-realm"
    'app-bucket'                        = "$Nome-bucket"
    'app-test'                          = "$Nome-test"
    'app-login.css'                     = "$Nome-login.css"
    # variáveis de ambiente
    'APP_DB_'                           = ($Nome.ToUpper() + '_DB_')
    'APP_ADMIN_'                        = ($Nome.ToUpper() + '_ADMIN_')
    'APP_CORS_'                         = ($Nome.ToUpper() + '_CORS_')
    'APP_CEP_'                          = ($Nome.ToUpper() + '_CEP_')
    # namespace de configuração tipada
    'app.security.'                     = "$Nome.security."
    'app.cep.'                          = "$Nome.cep."
    'AppKeycloakProperties'             = "${Pascal}KeycloakProperties"
}

$extensoes = @('.java', '.xml', '.yml', '.yaml', '.json', '.md', '.ts', '.html', '.css', '.sh',
    '.properties', '.ftl', '.mdc', '.toml', '.conf', '.example', '.smoke', '.gitignore', '.dockerignore')

$arquivos = Get-ChildItem -Recurse -File $raiz -Force |
    Where-Object {
        $_.FullName -notmatch '\\\.git\\|\\node_modules\\|\\target\\|\\dist\\|\\\.angular\\' -and
        ($extensoes -contains $_.Extension -or $_.Name -in @('Dockerfile', 'Caddyfile', 'mvnw', 'mvnw.cmd', '.env.example'))
    }

$alterados = 0
foreach ($arquivo in $arquivos) {
    $conteudo = [System.IO.File]::ReadAllText($arquivo.FullName)
    $original = $conteudo

    foreach ($de in $substituicoes.Keys) {
        $conteudo = $conteudo.Replace($de, $substituicoes[$de])
    }

    # O namespace de configuração tem DUAS formas, e a substituição por ponto só alcança uma.
    # No Java e no @Value ele é `app.security.keycloak`; no YAML é uma chave de topo aninhada:
    #
    #   app:
    #     security:
    #       keycloak:
    #
    # Sem esta regra, o @ConfigurationProperties passa a apontar para `<nome>.security.keycloak`
    # enquanto o YAML continua dizendo `app:` — e a aplicação só falha no boot, com um erro de
    # binding que não menciona rename nenhum.
    if ($arquivo.Extension -in '.yml', '.yaml') {
        $conteudo = $conteudo -creplace '(?m)^app:$', "${Nome}:"
    }

    # -cne, e não -ne: a comparação padrão do PowerShell é case-insensitive, e passaria batido
    # numa troca que só muda a caixa.
    if ($conteudo -cne $original) {
        if (-not $DryRun) {
            [System.IO.File]::WriteAllText($arquivo.FullName, $conteudo, (New-Object System.Text.UTF8Encoding($false)))
        }
        $alterados++
    }
}

Write-Host "Conteúdo: $alterados arquivos" -ForegroundColor Green

# ---------------------------------------------------------------------------------------------
# 2. Caminhos. Do mais profundo para o mais raso, ou renomear um diretório invalidaria os caminhos
#    dos filhos que ainda faltam.
# ---------------------------------------------------------------------------------------------

$renomeados = 0

# 2a. arquivos
#
# `App[A-Z]` cobre AppApplication.java, mas também AppKeycloakProperties.java e qualquer outra
# classe que o molde prefixe. Renomear só a primeira deixa o arquivo com um nome e a classe com
# outro — e o Java recusa a compilar, o que é sorte: o modo de falha é alto.
#
# ⚠️ NÃO renomeie qualquer arquivo `app-*`. No frontend, `app-footer.component.ts` e
# `app-field-error` usam o **prefixo de seletor do Angular** (`"prefix": "app"` no angular.json),
# que é convenção do framework e não tem relação com o nome do projeto. Renomeá-los quebra os
# imports sem nenhum ganho. Só os arquivos de tema, nomeados a partir do projeto, entram.
$arquivosARenomear = @("app-login.css", "app-realm.yaml")

Get-ChildItem -Recurse -File $raiz -Force |
    Where-Object { $_.FullName -notmatch '\\\.git\\|\\node_modules\\|\\target\\|\\dist\\|\\\.angular\\' } |
    Where-Object { $_.Name -cmatch '^App[A-Z]' -or $arquivosARenomear -ccontains $_.Name } |
    ForEach-Object {
        $novo = $_.Name -creplace '^App(?=[A-Z])', $Pascal
        if ($arquivosARenomear -ccontains $_.Name) { $novo = $_.Name.Replace('app-', "$Nome-") }
        if ($novo -cne $_.Name) {
            if (-not $DryRun) { Rename-Item -LiteralPath $_.FullName -NewName $novo }
            $renomeados++
        }
    }

# 2b. diretórios, do mais profundo para o mais raso
Get-ChildItem -Recurse -Directory $raiz -Force |
    Where-Object { $_.FullName -notmatch '\\\.git\\|\\node_modules\\|\\target\\|\\dist\\|\\\.angular\\' } |
    Sort-Object { $_.FullName.Length } -Descending |
    ForEach-Object {
        $novo = $null
        if ($_.Name -ceq 'app' -and $_.Parent.Name -ceq 'gems') { $novo = $Nome }       # pacote Java
        elseif ($_.Name -ceq 'app' -and $_.Parent.Name -ceq 'keycloak-theme') { $novo = $Nome }
        elseif ($_.Name -clike 'app-*') { $novo = $_.Name.Replace('app-', "$Nome-") }

        if ($novo) {
            if (-not $DryRun -and (Test-Path -LiteralPath $_.FullName)) {
                Rename-Item -LiteralPath $_.FullName -NewName $novo
            }
            $renomeados++
        }
    }

Write-Host "Caminhos: $renomeados itens" -ForegroundColor Green

# ---------------------------------------------------------------------------------------------
# 3. A hierarquia do pacote, quando o grupo muda.
# ---------------------------------------------------------------------------------------------

if ($Grupo -cne 'br.com.gems' -and -not $DryRun) {
    Write-Host ''
    Write-Host "AVISO: o groupId mudou para '$Grupo'." -ForegroundColor Yellow
    Write-Host '  Os diretórios de pacote em src/main/java e src/test/java ainda estão sob' -ForegroundColor Yellow
    Write-Host "  br/com/gems/. Mova-os para $($Grupo -replace '\.', '/')/ à mão e rode o gate." -ForegroundColor Yellow
}

# ---------------------------------------------------------------------------------------------
# 4. Relatório e próximos passos.
# ---------------------------------------------------------------------------------------------

Write-Host ''
if ($DryRun) {
    Write-Host 'MODO SECO — nada foi alterado.' -ForegroundColor Yellow
    exit 0
}

Write-Host 'Pronto. Agora, nesta ordem:' -ForegroundColor Cyan
Write-Host ''
Write-Host '  1. Confira que não sobrou nada do molde:'
Write-Host '       git grep -in "\bapp-\|br\.com\.gems\.app\|AppApplication"'
Write-Host ''
Write-Host '  2. Rode o gate completo:'
Write-Host '       cd backend  ; .\mvnw.cmd clean install'
Write-Host '       cd ..\frontend ; npm install --legacy-peer-deps ; npm test ; npm run build'
Write-Host ''
Write-Host '  3. Suba o ambiente:'
Write-Host '       cd environments\local ; docker compose up -d'
Write-Host ''
Write-Host '  4. Ajuste o que é decisão sua, não renomeação:'
Write-Host '       - a paleta em frontend/src/app/app.config.ts (provideGemsTheme)'
Write-Host '       - o logo do tema em environments/keycloak-theme/'
Write-Host '       - o preâmbulo de .specify/memory/constitution.md'
Write-Host '       - o README.md'
Write-Host ''
Write-Host '  5. Quando o primeiro domínio real existir, remova `parceiro` e `solicitacao`' -ForegroundColor Yellow
Write-Host '     POR INTEIRO — backend, frontend, testes, changeset e specs/001.' -ForegroundColor Yellow
Write-Host '     É a última cláusula do Princípio VIII, e ela não é opcional.' -ForegroundColor Yellow
Write-Host ''
