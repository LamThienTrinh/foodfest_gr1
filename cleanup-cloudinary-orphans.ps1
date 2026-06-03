param(
  [switch]$Apply,
  [string]$CloudName = $env:CLOUDINARY_CLOUD_NAME,
  [string]$ApiKey = $env:CLOUDINARY_API_KEY,
  [string]$ApiSecret = $env:CLOUDINARY_API_SECRET,
  [string]$DatabaseUrl = $env:DATABASE_URL,
  [string]$DatabaseUser = $env:DATABASE_USER,
  [string]$DatabasePassword = $env:DATABASE_PASSWORD,
  [int]$PreviewLimit = 20
)

$ErrorActionPreference = "Stop"

function Write-Step($msg) {
  Write-Host "`n=== $msg ===" -ForegroundColor Cyan
}

function Assert-True($condition, [string]$message) {
  if (-not $condition) {
    throw "ASSERT FAILED: $message"
  }
}

function Get-DotEnvValue([string]$filePath, [string]$key) {
  if (-not (Test-Path $filePath)) {
    return $null
  }

  $pattern = "^\s*$([Regex]::Escape($key))\s*=\s*(.*)\s*$"
  foreach ($line in Get-Content $filePath) {
    if ($line -match $pattern) {
      $value = $matches[1].Trim()
      if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
        $value = $value.Substring(1, $value.Length - 2)
      }
      return $value
    }
  }

  return $null
}

function Get-DbConnectionInfo([string]$jdbcUrl) {
  $url = $jdbcUrl
  if (-not $url -or $url.Trim().Length -eq 0) {
    $url = "jdbc:postgresql://localhost:5432/foodfest"
  }

  if ($url.StartsWith("jdbc:")) {
    $url = $url.Substring(5)
  }

  $uri = [Uri]$url
  $dbName = $uri.AbsolutePath.TrimStart('/')
  if (-not $dbName) {
    $dbName = "foodfest"
  }

  $port = $uri.Port
  if ($port -le 0) {
    $port = 5432
  }

  [pscustomobject]@{
    Host = $uri.Host
    Port = $port
    Database = $dbName
  }
}

function Invoke-DbTextQuery([pscustomobject]$conn, [string]$user, [string]$password, [string]$sql) {
  $psqlCmd = Get-Command psql -ErrorAction SilentlyContinue
  if (-not $psqlCmd) {
    throw "psql command not found. Please install PostgreSQL client tools and add psql to PATH."
  }

  $hadOldPassword = Test-Path Env:PGPASSWORD
  $oldPassword = $env:PGPASSWORD
  $env:PGPASSWORD = $password

  try {
    $args = @(
      "-h", $conn.Host,
      "-p", [string]$conn.Port,
      "-U", $user,
      "-d", $conn.Database,
      "-v", "ON_ERROR_STOP=1",
      "-At",
      "-c", $sql
    )

    $raw = & $psqlCmd.Source @args
    if ($LASTEXITCODE -ne 0) {
      throw "psql query failed with exit code $LASTEXITCODE"
    }

    return @($raw | Where-Object { $_ -and $_.Trim().Length -gt 0 })
  }
  finally {
    if ($hadOldPassword) {
      $env:PGPASSWORD = $oldPassword
    }
    else {
      Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    }
  }
}

function Get-PublicIdFromCloudinaryUrl([string]$url, [string]$cloudName) {
  try {
    $uri = [Uri]$url
  }
  catch {
    return $null
  }

  $path = $uri.AbsolutePath.TrimStart('/')
  $prefix = "$cloudName/image/upload/"
  if (-not $path.StartsWith($prefix)) {
    return $null
  }

  $tail = $path.Substring($prefix.Length)
  if (-not $tail) {
    return $null
  }

  $segments = @($tail.Split('/', [System.StringSplitOptions]::RemoveEmptyEntries))
  if ($segments.Count -eq 0) {
    return $null
  }

  $versionIndex = -1
  for ($i = 0; $i -lt $segments.Count; $i++) {
    if ($segments[$i] -match '^v\d+$') {
      $versionIndex = $i
      break
    }
  }

  if ($versionIndex -ge 0 -and $versionIndex -lt ($segments.Count - 1)) {
    $segments = @($segments[($versionIndex + 1)..($segments.Count - 1)])
  }

  if ($segments.Count -eq 0) {
    return $null
  }

  $lastIndex = $segments.Count - 1
  $last = $segments[$lastIndex]
  $dotIndex = $last.LastIndexOf('.')
  if ($dotIndex -gt 0) {
    $segments[$lastIndex] = $last.Substring(0, $dotIndex)
  }

  return ($segments -join '/')
}

function Get-CloudinaryHeaders([string]$apiKey, [string]$apiSecret) {
  $raw = "${apiKey}:${apiSecret}"
  $base64 = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($raw))
  return @{ Authorization = "Basic $base64" }
}

function Get-AllCloudinaryResources([string]$cloudName, [hashtable]$headers) {
  $all = @()
  $nextCursor = $null

  do {
    $uri = "https://api.cloudinary.com/v1_1/$cloudName/resources/image/upload?max_results=500"
    if ($nextCursor) {
      $uri += "&next_cursor=$([uri]::EscapeDataString($nextCursor))"
    }

    $response = Invoke-RestMethod -Method Get -Uri $uri -Headers $headers
    if ($response.resources) {
      $all += @($response.resources)
    }

    $nextCursor = $response.next_cursor
  } while ($nextCursor)

  return ,$all
}

function Remove-CloudinaryResourcesBatch([string]$cloudName, [hashtable]$headers, [string[]]$publicIds) {
  $query = ($publicIds | ForEach-Object { "public_ids[]=$([uri]::EscapeDataString($_))" }) -join '&'
  $deleteUri = "https://api.cloudinary.com/v1_1/$cloudName/resources/image/upload?$query"
  return Invoke-RestMethod -Method Delete -Uri $deleteUri -Headers $headers
}

try {
  $repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
  $envCandidates = @(
    (Join-Path $repoRoot ".env"),
    (Join-Path $repoRoot "server/.env")
  )

  foreach ($envFile in $envCandidates) {
    if (-not $CloudName) { $CloudName = Get-DotEnvValue -filePath $envFile -key "CLOUDINARY_CLOUD_NAME" }
    if (-not $ApiKey) { $ApiKey = Get-DotEnvValue -filePath $envFile -key "CLOUDINARY_API_KEY" }
    if (-not $ApiSecret) { $ApiSecret = Get-DotEnvValue -filePath $envFile -key "CLOUDINARY_API_SECRET" }

    if (-not $DatabaseUrl) {
      $DatabaseUrl = Get-DotEnvValue -filePath $envFile -key "DATABASE_URL"
      if (-not $DatabaseUrl) {
        $DatabaseUrl = Get-DotEnvValue -filePath $envFile -key "DB_URL"
      }
    }
    if (-not $DatabaseUser) { $DatabaseUser = Get-DotEnvValue -filePath $envFile -key "DATABASE_USER" }
    if (-not $DatabasePassword) { $DatabasePassword = Get-DotEnvValue -filePath $envFile -key "DATABASE_PASSWORD" }
  }

  if (-not $CloudName) {
    throw "Missing Cloudinary cloud name. Set CLOUDINARY_CLOUD_NAME or pass -CloudName."
  }
  if (-not $ApiKey) {
    throw "Missing Cloudinary API key. Set CLOUDINARY_API_KEY or pass -ApiKey."
  }
  if (-not $ApiSecret) {
    throw "Missing Cloudinary API secret. Set CLOUDINARY_API_SECRET or pass -ApiSecret."
  }
  if (-not $DatabaseUser) {
    $DatabaseUser = "postgres"
  }
  if (-not $DatabasePassword) {
    $DatabasePassword = "postgres"
  }

  Assert-True ($CloudName.Trim().Length -gt 0) "Cloudinary cloud name is required"
  Assert-True ($ApiKey.Trim().Length -gt 0) "Cloudinary API key is required"
  Assert-True ($ApiSecret.Trim().Length -gt 0) "Cloudinary API secret is required"

  $dbConn = Get-DbConnectionInfo $DatabaseUrl

  Write-Step "Collect image URLs from database"
  $sql = @"
SELECT DISTINCT url
FROM (
  SELECT TRIM(avatar_url) AS url FROM users WHERE avatar_url IS NOT NULL AND TRIM(avatar_url) <> ''
  UNION
  SELECT TRIM(image_url) AS url FROM posts WHERE image_url IS NOT NULL AND TRIM(image_url) <> ''
  UNION
  SELECT TRIM(image_url) AS url FROM dishes WHERE image_url IS NOT NULL AND TRIM(image_url) <> ''
  UNION
  SELECT TRIM(image_url) AS url FROM personal_dishes WHERE image_url IS NOT NULL AND TRIM(image_url) <> ''
) refs
WHERE url LIKE 'https://res.cloudinary.com/$CloudName/%'
ORDER BY url;
"@

  $cloudinaryUrlsInDb = Invoke-DbTextQuery -conn $dbConn -user $DatabaseUser -password $DatabasePassword -sql $sql
  $keepPublicIds = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::Ordinal)
  foreach ($u in $cloudinaryUrlsInDb) {
    $publicId = Get-PublicIdFromCloudinaryUrl -url $u -cloudName $CloudName
    if ($publicId) {
      [void]$keepPublicIds.Add($publicId)
    }
  }

  Write-Host "Cloudinary URLs referenced by DB: $($cloudinaryUrlsInDb.Count)" -ForegroundColor Yellow
  Write-Host "Distinct public_ids to keep: $($keepPublicIds.Count)" -ForegroundColor Yellow

  Write-Step "Load current Cloudinary resources"
  $headers = Get-CloudinaryHeaders -apiKey $ApiKey -apiSecret $ApiSecret
  $resources = Get-AllCloudinaryResources -cloudName $CloudName -headers $headers
  $allPublicIds = @($resources | ForEach-Object { [string]$_.public_id } | Sort-Object -Unique)
  $orphans = @($allPublicIds | Where-Object { -not $keepPublicIds.Contains($_) })

  Write-Host "Total Cloudinary resources: $($allPublicIds.Count)" -ForegroundColor Yellow
  Write-Host "Orphan resources found: $($orphans.Count)" -ForegroundColor Yellow

  if ($orphans.Count -gt 0) {
    $preview = @($orphans | Select-Object -First $PreviewLimit)
    Write-Host "Orphan preview (first $($preview.Count)):" -ForegroundColor Magenta
    $preview | ForEach-Object { Write-Host "- $_" }
  }

  if (-not $Apply) {
    Write-Host "`nDry-run only. Re-run with -Apply to delete orphan resources." -ForegroundColor Cyan
    exit 0
  }

  if ($orphans.Count -eq 0) {
    Write-Host "No orphan resources to delete." -ForegroundColor Green
    exit 0
  }

  Write-Step "Delete orphan Cloudinary resources"
  $deleted = 0
  $notFound = 0
  $failed = New-Object 'System.Collections.Generic.List[string]'

  for ($i = 0; $i -lt $orphans.Count; $i += 100) {
    $end = [Math]::Min($i + 99, $orphans.Count - 1)
    $batch = @($orphans[$i..$end])
    $resp = Remove-CloudinaryResourcesBatch -cloudName $CloudName -headers $headers -publicIds $batch

    $statusMap = @{}
    if ($resp.deleted) {
      $resp.deleted.PSObject.Properties | ForEach-Object {
        $statusMap[$_.Name] = [string]$_.Value
      }
    }

    foreach ($id in $batch) {
      $status = $statusMap[$id]
      if ($status -eq "deleted") {
        $deleted++
      }
      elseif ($status -eq "not_found") {
        $notFound++
      }
      else {
        $failed.Add("$id => $status") | Out-Null
      }
    }
  }

  Write-Host "Deleted: $deleted" -ForegroundColor Green
  Write-Host "Already missing (not_found): $notFound" -ForegroundColor Yellow
  Write-Host "Failed: $($failed.Count)" -ForegroundColor Red

  if ($failed.Count -gt 0) {
    $failed | Select-Object -First $PreviewLimit | ForEach-Object { Write-Host "- $_" -ForegroundColor Red }
    throw "Some resources failed to delete"
  }

  Write-Host "Cleanup completed successfully." -ForegroundColor Green
}
catch {
  Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
  exit 1
}