$base = "http://127.0.0.1:8080"
$rand = Get-Random -Maximum 999999
$userA = "phase3_a_$rand"
$userB = "phase3_b_$rand"
$pass = "Passw0rd!"

function Invoke-JsonApi {
    param([string]$Method,[string]$Url,[object]$Body = $null,[hashtable]$Headers = @{})
    $params = @{ Method = $Method; Uri = $Url; Headers = $Headers; SkipHttpErrorCheck = $true }
    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body = ($Body | ConvertTo-Json -Depth 10)
    }
    $resp = Invoke-WebRequest @params
    $json = $null
    if ($resp.Content) { $json = $resp.Content | ConvertFrom-Json }
    [PSCustomObject]@{ Status = [int]$resp.StatusCode; Json = $json }
}

$regA = Invoke-JsonApi -Method POST -Url "$base/api/auth/register" -Body @{ username = $userA; password = $pass; fullName = "Phase3 User A" }
$regB = Invoke-JsonApi -Method POST -Url "$base/api/auth/register" -Body @{ username = $userB; password = $pass; fullName = "Phase3 User B" }
$tokenA = $regA.Json.data.token
$userIdB = $regB.Json.data.user.id

$profileNoToken = Invoke-JsonApi -Method GET -Url "$base/api/users/$userIdB/profile"
$profileWithTokenBefore = Invoke-JsonApi -Method GET -Url "$base/api/users/$userIdB/profile" -Headers @{ Authorization = "Bearer $tokenA" }
$followResp = Invoke-JsonApi -Method POST -Url "$base/api/users/$userIdB/follow" -Headers @{ Authorization = "Bearer $tokenA" }
$profileWithTokenAfter = Invoke-JsonApi -Method GET -Url "$base/api/users/$userIdB/profile" -Headers @{ Authorization = "Bearer $tokenA" }
$profileMissing = Invoke-JsonApi -Method GET -Url "$base/api/users/999999/profile"
$profileBad = Invoke-JsonApi -Method GET -Url "$base/api/users/0/profile"

[PSCustomObject]@{
    generatedUsers = @{ A = $userA; B = $userB }
    registerA = @{ status = $regA.Status; tokenPrefix = if($tokenA){$tokenA.Substring(0,[Math]::Min(18,$tokenA.Length))}else{$null}; userId = $regA.Json.data.user.id }
    registerB = @{ status = $regB.Status; userId = $userIdB }
    profileNoToken = @{ status = $profileNoToken.Status; isFollowing = $profileNoToken.Json.data.isFollowing; followerCount = $profileNoToken.Json.data.followerCount }
    profileWithTokenBeforeFollow = @{ status = $profileWithTokenBefore.Status; isFollowing = $profileWithTokenBefore.Json.data.isFollowing; followerCount = $profileWithTokenBefore.Json.data.followerCount }
    follow = @{ status = $followResp.Status; isFollowing = $followResp.Json.data.isFollowing }
    profileWithTokenAfterFollow = @{ status = $profileWithTokenAfter.Status; isFollowing = $profileWithTokenAfter.Json.data.isFollowing; followerCount = $profileWithTokenAfter.Json.data.followerCount }
    profile999999 = @{ status = $profileMissing.Status; message = $profileMissing.Json.message }
    profile0 = @{ status = $profileBad.Status; message = $profileBad.Json.message }
} | ConvertTo-Json -Depth 10
