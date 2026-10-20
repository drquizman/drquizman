param([string]$ProjectRef = 'xoerzqgvcpjrbuwtbtdo')
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
if ($ProjectRef -ne 'xoerzqgvcpjrbuwtbtdo') { throw 'Unexpected project' }
Set-Location (Split-Path $PSScriptRoot -Parent)
$statusPath = Join-Path (Get-Location) 'supabase/.temp/telegram-setup-status.json'
New-Item -ItemType Directory -Force (Split-Path $statusPath) | Out-Null
function Save-Status($state) { @{status=$state; time=[DateTime]::UtcNow.ToString('o')} | ConvertTo-Json | Set-Content $statusPath }
Save-Status 'waiting-for-token'
Add-Type -AssemblyName System.Windows.Forms
$form = New-Object System.Windows.Forms.Form
$form.Text = 'Connect @DrQuizman_bot'
$form.Size = New-Object System.Drawing.Size(570,210)
$form.StartPosition = 'CenterScreen'
$form.TopMost = $true
$label = New-Object System.Windows.Forms.Label
$label.Text = "Paste the bot token from BotFather. It will be stored in Supabase Secrets.`nThe token is hidden and will not be sent to the chat."
$label.SetBounds(20,20,520,45)
$inputBox = New-Object System.Windows.Forms.TextBox
$inputBox.UseSystemPasswordChar = $true
$inputBox.SetBounds(20,75,510,25)
$button = New-Object System.Windows.Forms.Button
$button.Text = 'Connect bot'
$button.SetBounds(365,115,165,30)
$button.DialogResult = [System.Windows.Forms.DialogResult]::OK
$form.Controls.AddRange(@($label,$inputBox,$button))
$form.AcceptButton = $button
if ($form.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK) { Save-Status 'cancelled'; $form.Dispose(); exit 1 }
$botToken = $inputBox.Text.Trim()
$inputBox.Clear(); $form.Dispose()
$secretDir = $null
$stage = 'validate-token'
try {
    if ($botToken -notmatch '^\d+:[A-Za-z0-9_-]+$') { throw 'Invalid token format' }
    Save-Status 'connecting'
    $stage = 'telegram-getMe'
    $botInfo = Invoke-RestMethod -Method Post -Uri "https://api.telegram.org/bot$botToken/getMe" -TimeoutSec 30
    if (!$botInfo.ok -or $botInfo.result.username -ine 'DrQuizman_bot') { throw 'Wrong bot' }
    $stage = 'prepare-secret-file'
    $bytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes); $rng.Dispose()
    $webhookSecret = [BitConverter]::ToString($bytes).Replace('-','').ToLowerInvariant()
    $secretDir = Join-Path ([IO.Path]::GetTempPath()) ('quizman-secrets-'+[Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $secretDir | Out-Null
    $acl = New-Object Security.AccessControl.DirectorySecurity
    $acl.SetAccessRuleProtection($true,$false)
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent().User
    $rule = New-Object Security.AccessControl.FileSystemAccessRule($identity,'FullControl','ContainerInherit,ObjectInherit','None','Allow')
    $acl.AddAccessRule($rule)
    Set-Acl -LiteralPath $secretDir -AclObject $acl
    $envPath = Join-Path $secretDir 'bot.env'
    [IO.File]::WriteAllText($envPath,"TELEGRAM_BOT_TOKEN=$botToken`nTELEGRAM_WEBHOOK_SECRET=$webhookSecret`n")
    $stage = 'save-supabase-secrets'
    $ErrorActionPreference = 'Continue'
    $cliOutput = & npx.cmd --yes supabase@2.117.0 secrets set --project-ref $ProjectRef --env-file $envPath 2>&1
    $ErrorActionPreference = 'Stop'
    if ($LASTEXITCODE -ne 0) { throw 'Secrets setup failed' }
    $stage = 'register-webhook'
    $body = @{url="https://$ProjectRef.supabase.co/functions/v1/quizman/telegram";secret_token=$webhookSecret;allowed_updates=@('message','callback_query')} | ConvertTo-Json
    $result = Invoke-RestMethod -Method Post -Uri "https://api.telegram.org/bot$botToken/setWebhook" -ContentType 'application/json' -Body $body
    if (!$result.ok) { throw 'Webhook setup failed' }
    $stage = 'verify-webhook'
    $check = Invoke-RestMethod -Method Post -Uri "https://api.telegram.org/bot$botToken/getWebhookInfo"
    if ($check.result.url -ne "https://$ProjectRef.supabase.co/functions/v1/quizman/telegram") { throw 'Webhook verification failed' }
    Save-Status 'connected'
} catch {
    # HTTP errors may contain credentials in their URL; never emit exception details.
    @{status='failed';stage=$stage;errorType=$_.Exception.GetType().Name;time=[DateTime]::UtcNow.ToString('o')} | ConvertTo-Json | Set-Content $statusPath
} finally {
    if ($secretDir -and (Test-Path -LiteralPath $secretDir)) {
        $resolved = [IO.Path]::GetFullPath($secretDir)
        $tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
        if ($resolved.StartsWith($tempRoot,[StringComparison]::OrdinalIgnoreCase) -and (Split-Path $resolved -Leaf) -like 'quizman-secrets-*') {
            Remove-Item -LiteralPath $resolved -Recurse -Force
        }
    }
    $botToken=$null; $webhookSecret=$null; $body=$null; $cliOutput=$null
}
