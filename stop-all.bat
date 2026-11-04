@echo off
title STOP ALL - ABL
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-all.ps1"
