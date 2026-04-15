param(
    [string]$DbHost = "127.0.0.1",
    [int]$Port = 5432,
    [string]$Database = "foodfest",
    [string]$User = "postgres",
    [string]$Password = "postgres",
    [string]$PsqlPath = "psql",
    [switch]$SkipSeed
)

$ErrorActionPreference = "Stop"

$migrationsDir = Join-Path $PSScriptRoot "migrations"
if (-not (Test-Path $migrationsDir)) {
    throw "Migrations directory not found: $migrationsDir"
}

if ($Password) {
    $env:PGPASSWORD = $Password
}

function Invoke-PsqlQuery {
    param([string]$Query)

    $output = & $PsqlPath -h $DbHost -p $Port -U $User -d $Database -v ON_ERROR_STOP=1 -t -A -c $Query
    if ($LASTEXITCODE -ne 0) {
        throw "Failed running SQL query against database $Database"
    }

    return $output
}

function Invoke-PsqlFile {
    param([string]$FilePath)

    & $PsqlPath -h $DbHost -p $Port -U $User -d $Database -v ON_ERROR_STOP=1 -f $FilePath
    if ($LASTEXITCODE -ne 0) {
        throw "Failed applying migration file: $FilePath"
    }
}

Write-Host "Ensuring migration history table exists..." -ForegroundColor Cyan
Invoke-PsqlQuery @"
CREATE TABLE IF NOT EXISTS schema_migrations (
    version INT PRIMARY KEY,
    filename TEXT NOT NULL,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
"@ | Out-Null

$migrationFiles = Get-ChildItem -Path $migrationsDir -Filter "V*__*.sql" -File |
    Where-Object { $_.Name -match '^V(\d+)__' } |
    Sort-Object {
        [int]([regex]::Match($_.Name, '^V(\d+)__').Groups[1].Value)
    }
if ($migrationFiles.Count -eq 0) {
    Write-Host "No migration files found in $migrationsDir" -ForegroundColor Yellow
    exit 0
}

foreach ($migration in $migrationFiles) {
    if ($migration.Name -notmatch '^V(\d+)__') {
        throw "Invalid migration file name format: $($migration.Name)"
    }

    $version = [int]$Matches[1]
    $escapedName = $migration.Name.Replace("'", "''")

    if ($SkipSeed -and $migration.Name -match '__seed_') {
        Write-Host "Skipping seed migration $($migration.Name) because -SkipSeed was provided" -ForegroundColor Yellow
        continue
    }

    $alreadyAppliedRaw = Invoke-PsqlQuery "SELECT 1 FROM schema_migrations WHERE version = $version;"
    $alreadyApplied = if ($null -eq $alreadyAppliedRaw) {
        ""
    } else {
        "$alreadyAppliedRaw".Trim()
    }
    if ($alreadyApplied -eq "1") {
        Write-Host "Skipping V$version ($($migration.Name)) - already applied" -ForegroundColor DarkGray
        continue
    }

    Write-Host "Applying V$version ($($migration.Name))..." -ForegroundColor Green
    Invoke-PsqlFile -FilePath $migration.FullName

    Invoke-PsqlQuery "INSERT INTO schema_migrations (version, filename) VALUES ($version, '$escapedName');" | Out-Null
    Write-Host "Applied V$version successfully" -ForegroundColor Green
}

Write-Host "All pending migrations have been applied." -ForegroundColor Cyan
