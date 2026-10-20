param([string]$ProjectRef = 'xoerzqgvcpjrbuwtbtdo')
$ErrorActionPreference = 'Stop'
if ($ProjectRef -notmatch '^[a-z]{20}$') { throw 'Invalid project reference' }
Write-Host 'Use the same values you saved in Supabase Edge Function Secrets.'
$botSecure = Read-Host 'TELEGRAM_BOT_TOKEN (hidden)' -AsSecureString
$webhookSecure = Read-Host 'TELEGRAM_WEBHOOK_SECRET (hidden)' -AsSecureString
$botPtr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($botSecure)
$webhookPtr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($webhookSecure)
try {
    $botToken = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($botPtr)
    $webhookSecret = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($webhookPtr)
    if ($webhookSecret -notmatch '^[A-Za-z0-9_-]{32,256}$') { throw 'Use a 32-256 character webhook secret: letters, digits, _ or -' }
    $botInfo = Invoke-RestMethod -Method Post -Uri "https://api.telegram.org/bot$botToken/getMe"
    if (!$botInfo.ok -or $botInfo.result.username -ine 'DrQuizman_bot') { throw 'Token does not belong to DrQuizman_bot' }
    $body = @{url="https://$ProjectRef.supabase.co/functions/v1/quizman/telegram";secret_token=$webhookSecret;allowed_updates=@('message','callback_query')} | ConvertTo-Json
    $result = Invoke-RestMethod -Method Post -Uri "https://api.telegram.org/bot$botToken/setWebhook" -ContentType 'application/json' -Body $body
    if (!$result.ok) { throw 'Webhook setup failed' }
    $status = Invoke-RestMethod -Method Post -Uri "https://api.telegram.org/bot$botToken/getWebhookInfo"
    Write-Host "Webhook registered: $($status.result.url)"
    Write-Host "Pending updates: $($status.result.pending_update_count)"
} catch {
    # Do not echo HTTP exceptions: they can include the bot token in the URL.
    Write-Host 'Setup failed. Check the bot token, webhook secret, network and deployed function.'
    exit 1
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($botPtr)
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($webhookPtr)
    $botToken = $null; $webhookSecret = $null; $body = $null
}
