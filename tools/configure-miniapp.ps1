param([Parameter(Mandatory=$true)][string]$MiniappUrl)
$ErrorActionPreference='Stop'
$uri=[Uri]$MiniappUrl
if ($uri.Scheme -ne 'https' -or $uri.UserInfo -or $uri.Query -or $uri.Fragment) { throw 'Expected a public HTTPS miniapp URL' }
Set-Location (Split-Path $PSScriptRoot -Parent)
$bytes=New-Object byte[] 32
$rng=[Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes); $rng.Dispose()
$adminToken=[BitConverter]::ToString($bytes).Replace('-','').ToLowerInvariant()
$envFile=Join-Path (Get-Location) 'supabase/.temp/miniapp-setup.env'
try {
    [IO.File]::WriteAllText($envFile,"MINIAPP_URL=$MiniappUrl`nQUIZMAN_ADMIN_TOKEN=$adminToken`n")
    & npx.cmd --yes supabase@2.117.0 secrets set --project-ref xoerzqgvcpjrbuwtbtdo --env-file $envFile | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Cannot configure miniapp secrets' }
    $result=Invoke-RestMethod -Method Post -Uri 'https://xoerzqgvcpjrbuwtbtdo.supabase.co/functions/v1/quizman/admin/configure-bot' -Headers @{Authorization="Bearer $adminToken"}
    if (!$result.ok) { throw 'Bot menu setup failed' }
    Write-Output 'Bot catalog button configured.'
} finally {
    Remove-Item -LiteralPath $envFile -Force -ErrorAction SilentlyContinue
    & npx.cmd --yes supabase@2.117.0 secrets unset QUIZMAN_ADMIN_TOKEN --project-ref xoerzqgvcpjrbuwtbtdo --yes | Out-Null
    $adminToken=$null
}
