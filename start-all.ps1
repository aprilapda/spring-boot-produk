# Start ABL microservices in dependency order.
# Services run without separate console windows; each module appends output to run.log.

$root = $PSScriptRoot

$jdkCandidates = @($env:JAVA_HOME, "C:\Program Files\Java\jdk-23")
$javaInstallRoot = "C:\Program Files\Java"
if (Test-Path $javaInstallRoot) {
    $jdkCandidates += Get-ChildItem -LiteralPath $javaInstallRoot -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        ForEach-Object { $_.FullName }
}

$javaHome = $null
foreach ($candidate in $jdkCandidates) {
    if ($candidate -and
        (Test-Path (Join-Path $candidate "bin\java.exe")) -and
        (Test-Path (Join-Path $candidate "bin\javac.exe"))) {
        $javaHome = (Resolve-Path $candidate).Path
        break
    }
}

if (-not $javaHome) {
    Write-Host "[ERROR] JDK tidak ditemukan. Set JAVA_HOME ke folder JDK dengan bin\java.exe dan bin\javac.exe." -ForegroundColor Red
    Read-Host "Tekan Enter untuk keluar"
    exit 1
}

$env:JAVA_HOME = $javaHome
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Write-Host "[JAVA] Menggunakan $env:JAVA_HOME" -ForegroundColor DarkGray

# ORDER-QUERY memakai database H2 file (persistent read model) di luar repo.
$ablDataDir = Join-Path $env:LOCALAPPDATA "ABL"
$env:ABL_DATA_DIR = $ablDataDir
foreach ($directory in @("auth", "order-write", "order-query")) {
    New-Item -ItemType Directory -Force -Path (Join-Path $ablDataDir $directory) | Out-Null
}
Write-Host "[DATA] ABL_DATA_DIR = $ablDataDir" -ForegroundColor DarkGray

function Test-Port($port) {
    $client = $null
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $connect = $client.ConnectAsync("127.0.0.1", [int]$port)
        return ($connect.Wait(1500) -and $client.Connected)
    } catch {
        return $false
    } finally {
        if ($client) {
            $client.Close()
        }
    }
}

function Get-ServiceProcesses($dir, $mainClass) {
    $escapedDir = [regex]::Escape($dir.TrimEnd("\"))
    $escapedMainClass = [regex]::Escape($mainClass)
    return @(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" -ErrorAction SilentlyContinue |
        Where-Object {
            $_.CommandLine -and
            ($_.CommandLine -match $escapedDir -or $_.CommandLine -match $escapedMainClass)
        })
}

function Start-Svc($label, $dir, $port, $mainClass) {
    if (-not (Test-Path $dir)) {
        Write-Host "[ERROR] Folder tidak ada: $dir" -ForegroundColor Red
        return
    }

    $existing = Get-ServiceProcesses $dir $mainClass
    if ($existing.Count -gt 0) {
        if (-not $port -or (Test-Port $port)) {
            Write-Host "[LEWATI] $label sudah berjalan" -ForegroundColor Yellow
            return
        }

        Write-Host "[ERROR] Proses $label ada, tetapi port $port belum siap. Periksa $dir\run.log" -ForegroundColor Red
        return
    }

    if ($port -and (Test-Port $port)) {
        Write-Host "[LEWATI] Port $port sudah aktif; $label tidak dijalankan ulang untuk menghindari proses duplikat." -ForegroundColor Yellow
        return
    }

    Write-Host "[START] $label (jendela service disembunyikan; log: $dir\run.log)" -ForegroundColor Cyan
    $command = "mvnw.cmd spring-boot:run >> `"$dir\run.log`" 2>&1"
    try {
        Start-Process -FilePath $env:ComSpec `
            -ArgumentList @("/d", "/c", $command) `
            -WorkingDirectory $dir `
            -WindowStyle Hidden | Out-Null
    } catch {
        Write-Host "[ERROR] Gagal memulai $label`: $($_.Exception.Message)" -ForegroundColor Red
        return
    }

    for ($i = 0; $i -lt 90; $i++) {
        Start-Sleep -Seconds 2
        $processes = Get-ServiceProcesses $dir $mainClass
        if ($port -and (Test-Port $port)) {
            Write-Host "[OK] $label siap di port $port" -ForegroundColor Green
            return
        }
        if (-not $port -and $processes.Count -gt 0) {
            Write-Host "[OK] $label proses berjalan (tanpa port HTTP)" -ForegroundColor Green
            return
        }
        if ($processes.Count -eq 0 -and $i -ge 5) {
            break
        }
    }

    Write-Host "[WARN] $label belum terdeteksi siap. Periksa log: $dir\run.log" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "==============================================" -ForegroundColor Magenta
Write-Host "   MENYALAKAN SERVICE ABL" -ForegroundColor Magenta
Write-Host "==============================================" -ForegroundColor Magenta

$rabbitService = Get-Service -Name "RabbitMQ" -ErrorAction SilentlyContinue
if (Test-Port 5672) {
    Write-Host "[OK] RabbitMQ aktif di port 5672" -ForegroundColor Green
} elseif ($rabbitService) {
    if ($rabbitService.Status -ne "Running") {
        try {
            Start-Service -Name "RabbitMQ" -ErrorAction Stop
            for ($i = 0; $i -lt 30; $i++) {
                Start-Sleep -Seconds 2
                if (Test-Port 5672) {
                    break
                }
            }
        } catch {
            Write-Host "[ERROR] RabbitMQ service gagal dimulai: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    if (Test-Port 5672) {
        Write-Host "[OK] RabbitMQ aktif di port 5672" -ForegroundColor Green
    } else {
        Write-Host "[WARN] RabbitMQ service terpasang, tetapi port 5672 belum siap." -ForegroundColor Yellow
    }
} else {
    Write-Host "[WARN] RabbitMQ Windows Service tidak ditemukan; fitur yang memakai broker tidak tersedia." -ForegroundColor Yellow
}

if (Test-Port 15672) {
    Write-Host "[OK] Dashboard RabbitMQ tersedia di http://localhost:15672" -ForegroundColor Green
} else {
    Write-Host "[INFO] Dashboard RabbitMQ tidak aktif pada port 15672." -ForegroundColor DarkYellow
}

Start-Svc "Eureka" "$root\eureka\eureka" 8761 "com.eureka.eureka.EurekaApplication"
Start-Svc "Auth" "$root\auth\auth" 8086 "com.apda.auth.AuthApplication"
Start-Svc "Produk" "$root\produk\produk" 8083 "com.apda.produk.ProdukApplication"
Start-Svc "Pelanggan" "$root\pelanggan\pelanggan" 8082 "com.apda.pelanggan.PelangganApplication"
Start-Svc "Order" "$root\order\order" 8081 "com.apda.order.OrderApplication"
Start-Svc "OrderQuery" "$root\order-query\query" 8087 "com.apda.orderquery.OrderQueryApplication"
Start-Svc "Gateway" "$root\gateway\gateway" 9001 "com.gateway.gateway.GatewayApplication"
Start-Svc "Producer" "$root\producer\producer" 8085 "com.apda.producer.ProducerApplication"

if ($env:MAIL_USERNAME -and $env:MAIL_PASSWORD -and $env:MAIL_TO -and
    $env:ABL_ENABLE_EMAIL_SENDING -eq "true") {
    Start-Svc "Consumer" "$root\consumer\consumer" $null "com.apda.consumer.ConsumerApplication"
} elseif ($env:MAIL_USERNAME -and $env:MAIL_PASSWORD) {
    Write-Host "[SKIP] Consumer: butuh MAIL_TO dan ABL_ENABLE_EMAIL_SENDING=true untuk mengizinkan email sungguhan." -ForegroundColor Yellow
} else {
    Write-Host "[SKIP] Consumer belum dijalankan: kredensial SMTP belum disetel atau email belum diizinkan." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "==============================================" -ForegroundColor Magenta
Write-Host "   STATUS PORT SERVICE" -ForegroundColor Magenta
Write-Host "==============================================" -ForegroundColor Magenta

$ports = [ordered]@{
    "8761" = "Eureka"
    "8086" = "Auth"
    "8083" = "Produk"
    "8082" = "Pelanggan"
    "8081" = "Order"
    "8087" = "OrderQuery"
    "9001" = "Gateway"
    "8085" = "Producer"
}
foreach ($port in $ports.Keys) {
    $state = if (Test-Port ([int]$port)) { "NYALA" } else { "MATI" }
    $color = if ($state -eq "NYALA") { "Green" } else { "Red" }
    Write-Host ("  [{0}] port {1}: {2}" -f $state, $port, $ports[$port]) -ForegroundColor $color
}

Write-Host ""
Write-Host "Dashboard Eureka : http://localhost:8761" -ForegroundColor Cyan
Write-Host "Gateway API      : http://localhost:9001" -ForegroundColor Cyan
Write-Host "Log setiap service: <folder service>\run.log" -ForegroundColor DarkGray
Write-Host "Service berjalan di latar belakang tanpa jendela CMD terpisah." -ForegroundColor DarkGray
Write-Host ""
Read-Host "Tekan Enter untuk menutup jendela ini (service tetap berjalan)"
