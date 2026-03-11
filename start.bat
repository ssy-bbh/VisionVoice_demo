@echo off
chcp 65001 >nul
echo =========================================
echo       正在启动 VisionVoice 后端服务
echo =========================================

:: 1. 自动执行 ADB 端口映射
echo [1/3] 正在映射 Android 端口 (8000 -^> 8000)...
D:\SDK\platform-tools\adb.exe reverse tcp:8000 tcp:8000

:: 2. 激活 Conda 环境
echo [2/3] 正在激活 Conda 环境 (voice_demo)...
call conda activate voice_demo

:: 3. 启动 FastAPI 服务器
echo [3/3] 启动 Uvicorn 服务器...
uvicorn backend.server:app --host 0.0.0.0 --port 8000 --reload

pause