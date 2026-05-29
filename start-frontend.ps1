$base = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Waimai - Start All Frontends" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$apps = @(
    @{Name="Admin"; Dir="web-admin"; Port=5173},
    @{Name="Customer"; Dir="web-customer"; Port=5174},
    @{Name="Rider"; Dir="web-rider"; Port=5175},
    @{Name="Merchant"; Dir="web-merchant"; Port=5176}
)

foreach ($app in $apps) {
    $dir = Join-Path $base $app.Dir
    if (-not (Test-Path (Join-Path $dir "node_modules"))) {
        Write-Host "[WARN] $($app.Name) missing node_modules, installing..." -ForegroundColor Yellow
        Push-Location $dir
        npm install
        Pop-Location
    }
}

foreach ($app in $apps) {
    $dir = Join-Path $base $app.Dir
    Write-Host "Starting $($app.Name) on :$($app.Port)..." -ForegroundColor Green
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$dir'; Write-Host '$($app.Name) port $($app.Port)'; npm run dev"
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Admin:    http://localhost:5173"
Write-Host "  Customer: http://localhost:5174"
Write-Host "  Rider:    http://localhost:5175"
Write-Host "  Merchant: http://localhost:5176"
Write-Host "========================================" -ForegroundColor Cyan
Read-Host "Press Enter to close"
