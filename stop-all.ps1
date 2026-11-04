# Stop only ABL application JVMs. RabbitMQ is a separate Windows Service.

$root = $PSScriptRoot.TrimEnd("\")
$services = @(
    @{ Name = "Eureka"; Directory = "$root\eureka\eureka"; MainClass = "com.eureka.eureka.EurekaApplication"; Port = 8761 },
    @{ Name = "Auth"; Directory = "$root\auth\auth"; MainClass = "com.apda.auth.AuthApplication"; Port = 8086 },
    @{ Name = "Produk"; Directory = "$root\produk\produk"; MainClass = "com.apda.produk.ProdukApplication"; Port = 8083 },
    @{ Name = "Pelanggan"; Directory = "$root\pelanggan\pelanggan"; MainClass = "com.apda.pelanggan.PelangganApplication"; Port = 8082 },
    @{ Name = "Order"; Directory = "$root\order\order"; MainClass = "com.apda.order.OrderApplication"; Port = 8081 },
    @{ Name = "OrderQuery"; Directory = "$root\order-query\query"; MainClass = "com.apda.orderquery.OrderQueryApplication"; Port = 8087 },
    @{ Name = "Gateway"; Directory = "$root\gateway\gateway"; MainClass = "com.gateway.gateway.GatewayApplication"; Port = 9001 },
    @{ Name = "Producer"; Directory = "$root\producer\producer"; MainClass = "com.apda.producer.ProducerApplication"; Port = 8085 },
    @{ Name = "Consumer"; Directory = "$root\consumer\consumer"; MainClass = "com.apda.consumer.ConsumerApplication"; Port = $null }
)

$javaProcesses = @(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" -ErrorAction SilentlyContinue)
$stopped = @{}
$stoppedCount = 0

Write-Host ""
Write-Host "Mematikan proses aplikasi ABL..." -ForegroundColor Yellow

foreach ($service in $services) {
    $escapedDirectory = [regex]::Escape($service.Directory)
    $escapedMainClass = [regex]::Escape($service.MainClass)
    $serviceProcesses = @($javaProcesses | Where-Object {
        $_.CommandLine -and
        ($_.CommandLine -match $escapedDirectory -or $_.CommandLine -match $escapedMainClass)
    })

    foreach ($process in $serviceProcesses) {
        if ($stopped.ContainsKey($process.ProcessId)) {
            continue
        }

        try {
            Stop-Process -Id $process.ProcessId -Force -ErrorAction Stop
            $stopped[$process.ProcessId] = $true
            Write-Host ("  [OK] {0} dihentikan (PID {1})" -f $service.Name, $process.ProcessId) -ForegroundColor Green
            $stoppedCount++
        } catch {
            Write-Host ("  [WARN] {0} PID {1} gagal dihentikan: {2}" -f $service.Name, $process.ProcessId, $_.Exception.Message) -ForegroundColor Yellow
        }
    }

    if ($serviceProcesses.Count -eq 0 -and $service.Port) {
        $listeners = @(Get-NetTCPConnection -LocalPort $service.Port -State Listen -ErrorAction SilentlyContinue)
        if ($listeners.Count -gt 0) {
            Write-Host ("  [WARN] Port {0} dipakai proses yang tidak dikenali sebagai {1}; proses dibiarkan." -f $service.Port, $service.Name) -ForegroundColor Yellow
        } else {
            Write-Host ("  [--] {0} sudah berhenti" -f $service.Name) -ForegroundColor DarkGray
        }
    } elseif ($serviceProcesses.Count -eq 0) {
        Write-Host ("  [--] {0} tidak terdeteksi" -f $service.Name) -ForegroundColor DarkGray
    }
}

$rabbitService = Get-Service -Name "RabbitMQ" -ErrorAction SilentlyContinue
if ($rabbitService -and $rabbitService.Status -eq "Running") {
    Write-Host "  [KEEP] RabbitMQ Windows Service tetap berjalan." -ForegroundColor Cyan
}

Write-Host ""
Write-Host ("Selesai. {0} proses aplikasi dihentikan." -f $stoppedCount) -ForegroundColor Cyan
Read-Host "Tekan Enter untuk menutup"
