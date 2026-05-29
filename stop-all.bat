@echo off
echo Stopping all frontends...
taskkill /fi "WINDOWTITLE eq Admin*" /f >nul 2>&1
taskkill /fi "WINDOWTITLE eq Customer*" /f >nul 2>&1
taskkill /fi "WINDOWTITLE eq Rider*" /f >nul 2>&1
taskkill /fi "WINDOWTITLE eq Merchant*" /f >nul 2>&1
echo Done.
pause
