param(
  [string]$BaseUrl = "http://localhost:8080",
  [switch]$SkipRegister,
  [string]$Username,
  [string]$Password = "password123",
  [string]$FullName = "API Test User"
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

function Invoke-Json($method, $url, $token = $null, $body = $null) {
  $headers = @{
    "Accept" = "application/json"
  }
  if ($token) {
    $headers["Authorization"] = "Bearer $token"
  }

  if ($null -ne $body) {
    $json = ($body | ConvertTo-Json -Depth 20)
    return Invoke-RestMethod -Method $method -Uri $url -Headers $headers -ContentType "application/json" -Body $json
  }

  return Invoke-RestMethod -Method $method -Uri $url -Headers $headers
}

function Invoke-Raw($method, $url, $token = $null, $body = $null) {
  $headers = @{
    "Accept" = "application/json"
  }
  if ($token) {
    $headers["Authorization"] = "Bearer $token"
  }

  $params = @{
    Method = $method
    Uri = $url
    Headers = $headers
    SkipHttpErrorCheck = $true
  }

  if ($null -ne $body) {
    $params["ContentType"] = "application/json"
    $params["Body"] = ($body | ConvertTo-Json -Depth 20)
  }

  return Invoke-WebRequest @params
}

function Get-FirstDishId($baseUrl) {
  $resp = Invoke-Json GET "$baseUrl/api/dishes?page=1"
  Assert-True ($resp.success -eq $true) "GET /api/dishes should succeed"
  Assert-True ($resp.data -ne $null) "GET /api/dishes should have data"
  $items = $resp.data.data
  Assert-True ($items -ne $null -and $items.Count -gt 0) "Need at least 1 dish in database to run tests"
  return [int]$items[0].id
}

try {
  Write-Step "Health check"
  $health = Invoke-RestMethod -Method GET -Uri "$BaseUrl/health"
  Assert-True ($health -eq "OK") "GET /health should return OK"

  if (-not $Username -or $Username.Trim().Length -eq 0) {
    $suffix = (Get-Random -Minimum 10000 -Maximum 99999)
    $Username = "api_test_$suffix"
  }

  Write-Step "Auth (register/login)"
  if (-not $SkipRegister) {
    $regBody = @{ username = $Username; password = $Password; fullName = $FullName }
    $reg = Invoke-Json POST "$BaseUrl/api/auth/register" $null $regBody
    Assert-True ($reg.success -eq $true) "Register should succeed"
    Assert-True ($reg.data.token) "Register should return token"
  }

  $loginBody = @{ username = $Username; password = $Password }
  $login = Invoke-Json POST "$BaseUrl/api/auth/login" $null $loginBody
  Assert-True ($login.success -eq $true) "Login should succeed"
  $token = $login.data.token
  Assert-True ($token) "Login should return token"

  Write-Step "Pick a dish to test"
  $dishId = Get-FirstDishId $BaseUrl
  Write-Host "Using dishId=$dishId" -ForegroundColor Yellow

  Write-Step "Verify new routes exist on this running server"
  $probeFav = Invoke-Raw GET "$BaseUrl/api/favorites?page=1" $token
  if ($probeFav.StatusCode -eq 404) {
    throw "Server returned 404 for /api/favorites. Restart server with './gradlew :server:run' to load new routes."
  }
  $probeMy = Invoke-Raw GET "$BaseUrl/api/my-dishes?page=1" $token
  if ($probeMy.StatusCode -eq 404) {
    throw "Server returned 404 for /api/my-dishes. Restart server with './gradlew :server:run' to load new routes."
  }

  # -----------------------
  # FAVORITES
  # -----------------------
  Write-Step "Favorites: GET list (should be OK)"
  $favList1 = Invoke-Json GET "$BaseUrl/api/favorites?page=1" $token
  Assert-True ($favList1.success -eq $true) "GET /api/favorites should succeed"

  Write-Step "Favorites: check -> toggle on -> check -> toggle off"
  $chk1 = Invoke-Json GET "$BaseUrl/api/favorites/check/$dishId" $token
  Assert-True ($chk1.success -eq $true) "GET /api/favorites/check/{dishId} should succeed"

  $toggle1 = Invoke-Json POST "$BaseUrl/api/favorites/toggle/$dishId" $token
  Assert-True ($toggle1.success -eq $true) "POST /api/favorites/toggle/{dishId} should succeed"
  Assert-True ($toggle1.data.isFavorite -eq $true) "After first toggle, isFavorite should be true"

  $chk2 = Invoke-Json GET "$BaseUrl/api/favorites/check/$dishId" $token
  Assert-True ($chk2.success -eq $true) "Check after toggle should succeed"
  Assert-True ($chk2.data.isFavorite -eq $true) "Check should show isFavorite=true"

  $toggle2 = Invoke-Json POST "$BaseUrl/api/favorites/toggle/$dishId" $token
  Assert-True ($toggle2.success -eq $true) "Second toggle should succeed"
  Assert-True ($toggle2.data.isFavorite -eq $false) "After second toggle, isFavorite should be false"

  Write-Step "Favorites: add -> ids -> remove"
  $add = Invoke-Json POST "$BaseUrl/api/favorites/add/$dishId" $token
  Assert-True ($add.success -eq $true) "POST /api/favorites/add/{dishId} should succeed"

  $ids = Invoke-Json GET "$BaseUrl/api/favorites/ids" $token
  Assert-True ($ids.success -eq $true) "GET /api/favorites/ids should succeed"
  $idList = @($ids.data.ids)
  Assert-True ($idList -contains $dishId) "Favorite ids should contain dishId"

  $del = Invoke-Json DELETE "$BaseUrl/api/favorites/remove/$dishId" $token
  Assert-True ($del.success -eq $true) "DELETE /api/favorites/remove/{dishId} should succeed"

  # -----------------------
  # MY DISHES (personal)
  # -----------------------
  Write-Step "My dishes: list (should be OK)"
  $my1 = Invoke-Json GET "$BaseUrl/api/my-dishes?page=1" $token
  Assert-True ($my1.success -eq $true) "GET /api/my-dishes should succeed"

  Write-Step "My dishes: check saved (should be false)"
  $saved1 = Invoke-Json GET "$BaseUrl/api/my-dishes/check/$dishId" $token
  Assert-True ($saved1.success -eq $true) "GET /api/my-dishes/check/{originalDishId} should succeed"
  Assert-True ($saved1.data.hasSaved -eq $false) "Expected hasSaved=false before creating"

  Write-Step "My dishes: create"
  # Load original dish details to build request
  $dish = Invoke-Json GET "$BaseUrl/api/dishes/$dishId"
  Assert-True ($dish.success -eq $true) "GET /api/dishes/{id} should succeed"
  
  $createBody = @{
    originalDishId = $dishId
    dishName = $dish.data.name
    imageUrl = $dish.data.imageUrl
    description = $dish.data.description
    ingredients = $dish.data.ingredients
    instructions = $dish.data.instructions
    prepTime = $dish.data.prepTime
    cookTime = $dish.data.cookTime
    serving = $dish.data.serving
    note = "Saved by automated test"
    tagIds = @()
  }
  $created = Invoke-Json POST "$BaseUrl/api/my-dishes" $token $createBody
  Assert-True ($created.success -eq $true) "POST /api/my-dishes should succeed"
  $personalId = [int]$created.data.id
  Assert-True ($personalId -gt 0) "Created personal dish should have id"
  Write-Host "Created personalDishId=$personalId" -ForegroundColor Yellow

  Write-Step "My dishes: check saved (should be true)"
  $saved2 = Invoke-Json GET "$BaseUrl/api/my-dishes/check/$dishId" $token
  Assert-True ($saved2.success -eq $true) "Check after create should succeed"
  Assert-True ($saved2.data.hasSaved -eq $true) "Expected hasSaved=true after creating"

  Write-Step "My dishes: get by id"
  $get1 = Invoke-Json GET "$BaseUrl/api/my-dishes/$personalId" $token
  Assert-True ($get1.success -eq $true) "GET /api/my-dishes/{id} should succeed"
  Assert-True ($get1.data.id -eq $personalId) "Returned personal dish id should match"

  Write-Step "My dishes: update"
  $newName = "$($get1.data.dishName) (My Version)"
  $updateBody = @{ dishName = $newName }
  $upd = Invoke-Json PUT "$BaseUrl/api/my-dishes/$personalId" $token $updateBody
  Assert-True ($upd.success -eq $true) "PUT /api/my-dishes/{id} should succeed"
  Assert-True ($upd.data.dishName -eq $newName) "Dish name should be updated"

  Write-Step "My dishes: delete"
  $delMy = Invoke-Json DELETE "$BaseUrl/api/my-dishes/$personalId" $token
  Assert-True ($delMy.success -eq $true) "DELETE /api/my-dishes/{id} should succeed"

  Write-Host "`n========================================" -ForegroundColor Green
  Write-Host "ALL TESTS PASSED" -ForegroundColor Green
  Write-Host "========================================" -ForegroundColor Green
  Write-Host "User: $Username" -ForegroundColor DarkGray
  Write-Host "DishId: $dishId" -ForegroundColor DarkGray
}
catch {
  Write-Host "`n========================================" -ForegroundColor Red
  Write-Host "TESTS FAILED" -ForegroundColor Red
  Write-Host "========================================" -ForegroundColor Red
  Write-Host $_ -ForegroundColor Red
  exit 1
}
