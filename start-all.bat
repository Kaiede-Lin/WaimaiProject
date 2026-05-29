@echo off
echo ========================================
echo   外卖平台 - 一键启动所有前端
echo ========================================
echo.

set BASE_DIR=%~dp0

echo [1/4] 启动管理端 (web-admin :5173) ...
start "web-admin" cmd /c "cd /d "%BASE_DIR%web-admin" && npm run dev"

echo [2/4] 启动顾客端 (web-customer :5174) ...
start "web-customer" cmd /c "cd /d "%BASE_DIR%web-customer" && npm run dev"

echo [3/4] 启动骑手端 (web-rider :5175) ...
start "web-rider" cmd /c "cd /d "%BASE_DIR%web-rider" && npm run dev"

echo [4/4] 启动商家端 (web-merchant :5176) ...
start "web-merchant" cmd /c "cd /d "%BASE_DIR%web-merchant" && npm run dev"

echo.
echo ========================================
echo   全部启动完成！
echo   管理端:    http://localhost:5173
echo   顾客端:    http://localhost:5174
echo   骑手端:    http://localhost:5175
echo   商家端:    http://localhost:5176
echo ========================================
echo.
echo 关闭窗口不会停止服务，需要手动关闭各个终端。
pause
