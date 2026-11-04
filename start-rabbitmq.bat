@echo off
title RabbitMQ Windows Service

sc.exe query RabbitMQ | findstr /C:"RUNNING" >nul
if not errorlevel 1 (
    echo RabbitMQ sudah berjalan sebagai Windows Service.
    goto :INFO
)

echo Menyalakan RabbitMQ Windows Service...
net start RabbitMQ
if errorlevel 1 (
    echo.
    echo RabbitMQ gagal dimulai. Coba Start dari Services atau jalankan file ini sebagai Administrator.
    pause
    exit /b 1
)

:INFO
echo.
echo AMQP       : localhost:5672
echo Dashboard  : http://localhost:15672
echo Login      : guest / guest
echo Tutup jendela ini tidak menghentikan service RabbitMQ.
pause
