@echo off
setlocal enabledelayedexpansion

REM Deploy System Monitor to Google Cloud Server
REM Usage: deploy_to_gcs.bat <server_name> <zone>

REM Check if server name and zone are provided
if "%~1"=="" (
    echo Usage: deploy_to_gcs.bat ^<server_name^> ^<zone^>
    echo Example: deploy_to_gcs.bat mine2 asia-south2-c
    exit /b 1
)

if "%~2"=="" (
    echo Usage: deploy_to_gcs.bat ^<server_name^> ^<zone^>
    echo Example: deploy_to_gcs.bat mine2 asia-south2-c
    exit /b 1
)

set SERVER_NAME=%~1
set ZONE=%~2
set DEPLOY_DIR=system_monitor_deploy

echo Preparing deployment package...

REM Create deployment directory
mkdir %DEPLOY_DIR%
mkdir %DEPLOY_DIR%\templates
mkdir %DEPLOY_DIR%\static
mkdir %DEPLOY_DIR%\data

REM Copy server files
copy server.py %DEPLOY_DIR%\
copy system_monitor.py %DEPLOY_DIR%\
copy requirements.txt %DEPLOY_DIR%\
copy README.md %DEPLOY_DIR%\
copy start_server.bat %DEPLOY_DIR%\

REM Copy template files
xcopy /E /I templates\* %DEPLOY_DIR%\templates\

REM Copy static files
xcopy /E /I static\* %DEPLOY_DIR%\static\

REM Create a systemd service file for running the server
echo [Unit] > %DEPLOY_DIR%\system-monitor.service
echo Description=System Monitor Server >> %DEPLOY_DIR%\system-monitor.service
echo After=network.target >> %DEPLOY_DIR%\system-monitor.service
echo. >> %DEPLOY_DIR%\system-monitor.service
echo [Service] >> %DEPLOY_DIR%\system-monitor.service
echo User=root >> %DEPLOY_DIR%\system-monitor.service
echo WorkingDirectory=/opt/system-monitor >> %DEPLOY_DIR%\system-monitor.service
echo ExecStart=/usr/bin/python3 /opt/system-monitor/server.py --port 5000 --auth default_token_change_me >> %DEPLOY_DIR%\system-monitor.service
echo Restart=always >> %DEPLOY_DIR%\system-monitor.service
echo RestartSec=10 >> %DEPLOY_DIR%\system-monitor.service
echo. >> %DEPLOY_DIR%\system-monitor.service
echo [Install] >> %DEPLOY_DIR%\system-monitor.service
echo WantedBy=multi-user.target >> %DEPLOY_DIR%\system-monitor.service

REM Create installation script
echo #!/bin/bash > %DEPLOY_DIR%\install.sh
echo. >> %DEPLOY_DIR%\install.sh
echo # Install dependencies >> %DEPLOY_DIR%\install.sh
echo sudo apt-get update >> %DEPLOY_DIR%\install.sh
echo sudo apt-get install -y python3 python3-pip >> %DEPLOY_DIR%\install.sh
echo. >> %DEPLOY_DIR%\install.sh
echo # Install Python packages >> %DEPLOY_DIR%\install.sh
echo sudo pip3 install -r requirements.txt >> %DEPLOY_DIR%\install.sh
echo. >> %DEPLOY_DIR%\install.sh
echo # Create installation directory >> %DEPLOY_DIR%\install.sh
echo sudo mkdir -p /opt/system-monitor >> %DEPLOY_DIR%\install.sh
echo sudo cp -r * /opt/system-monitor/ >> %DEPLOY_DIR%\install.sh
echo. >> %DEPLOY_DIR%\install.sh
echo # Set up systemd service >> %DEPLOY_DIR%\install.sh
echo sudo cp system-monitor.service /etc/systemd/system/ >> %DEPLOY_DIR%\install.sh
echo sudo systemctl daemon-reload >> %DEPLOY_DIR%\install.sh
echo sudo systemctl enable system-monitor >> %DEPLOY_DIR%\install.sh
echo sudo systemctl start system-monitor >> %DEPLOY_DIR%\install.sh
echo. >> %DEPLOY_DIR%\install.sh
echo echo "System Monitor installed and started!" >> %DEPLOY_DIR%\install.sh
echo echo "Access the dashboard at http://$(hostname -I | awk '{print $1}'):5000" >> %DEPLOY_DIR%\install.sh

REM Create a README for the deployment
echo # System Monitor Deployment > %DEPLOY_DIR%\DEPLOY_README.md
echo. >> %DEPLOY_DIR%\DEPLOY_README.md
echo This package contains the System Monitor server and client for deployment on a Google Cloud Server. >> %DEPLOY_DIR%\DEPLOY_README.md
echo. >> %DEPLOY_DIR%\DEPLOY_README.md
echo ## Installation >> %DEPLOY_DIR%\DEPLOY_README.md
echo. >> %DEPLOY_DIR%\DEPLOY_README.md
echo 1. Run the installation script: >> %DEPLOY_DIR%\DEPLOY_README.md
echo    ```bash >> %DEPLOY_DIR%\DEPLOY_README.md
echo    ./install.sh >> %DEPLOY_DIR%\DEPLOY_README.md
echo    ``` >> %DEPLOY_DIR%\DEPLOY_README.md
echo. >> %DEPLOY_DIR%\DEPLOY_README.md
echo 2. Access the dashboard: >> %DEPLOY_DIR%\DEPLOY_README.md
echo    - Open a web browser and navigate to `http://SERVER_IP:5000` >> %DEPLOY_DIR%\DEPLOY_README.md
echo    - The default authentication token is `default_token_change_me` >> %DEPLOY_DIR%\DEPLOY_README.md
echo. >> %DEPLOY_DIR%\DEPLOY_README.md
echo ## Configuration >> %DEPLOY_DIR%\DEPLOY_README.md
echo. >> %DEPLOY_DIR%\DEPLOY_README.md
echo - To change the authentication token, edit the systemd service file: >> %DEPLOY_DIR%\DEPLOY_README.md
echo   ```bash >> %DEPLOY_DIR%\DEPLOY_README.md
echo   sudo nano /etc/systemd/system/system-monitor.service >> %DEPLOY_DIR%\DEPLOY_README.md
echo   ``` >> %DEPLOY_DIR%\DEPLOY_README.md
echo. >> %DEPLOY_DIR%\DEPLOY_README.md
echo   Change the `--auth` parameter in the `ExecStart` line. >> %DEPLOY_DIR%\DEPLOY_README.md
echo. >> %DEPLOY_DIR%\DEPLOY_README.md
echo - To restart the service after configuration changes: >> %DEPLOY_DIR%\DEPLOY_README.md
echo   ```bash >> %DEPLOY_DIR%\DEPLOY_README.md
echo   sudo systemctl daemon-reload >> %DEPLOY_DIR%\DEPLOY_README.md
echo   sudo systemctl restart system-monitor >> %DEPLOY_DIR%\DEPLOY_README.md
echo   ``` >> %DEPLOY_DIR%\DEPLOY_README.md
echo. >> %DEPLOY_DIR%\DEPLOY_README.md
echo ## Monitoring Desktop Clients >> %DEPLOY_DIR%\DEPLOY_README.md
echo. >> %DEPLOY_DIR%\DEPLOY_README.md
echo To monitor desktop systems, run the following command on each system: >> %DEPLOY_DIR%\DEPLOY_README.md
echo. >> %DEPLOY_DIR%\DEPLOY_README.md
echo ```bash >> %DEPLOY_DIR%\DEPLOY_README.md
echo python system_monitor.py --server http://SERVER_IP:5000 --auth default_token_change_me >> %DEPLOY_DIR%\DEPLOY_README.md
echo ``` >> %DEPLOY_DIR%\DEPLOY_README.md
echo. >> %DEPLOY_DIR%\DEPLOY_README.md
echo ## Monitoring Mobile Clients >> %DEPLOY_DIR%\DEPLOY_README.md
echo. >> %DEPLOY_DIR%\DEPLOY_README.md
echo Install the Mobile System Monitor app on your Android device and configure it with: >> %DEPLOY_DIR%\DEPLOY_README.md
echo - Server URL: `http://SERVER_IP:5000` >> %DEPLOY_DIR%\DEPLOY_README.md
echo - Auth Token: `default_token_change_me` (or your custom token) >> %DEPLOY_DIR%\DEPLOY_README.md

REM Check if gcloud is installed
where gcloud >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: Google Cloud SDK (gcloud) is not installed or not in PATH.
    echo Please install Google Cloud SDK and try again.
    exit /b 1
)

REM Create a zip archive
echo Creating deployment archive...
powershell -Command "Compress-Archive -Path %DEPLOY_DIR%\* -DestinationPath system_monitor_deploy.zip -Force"

REM Upload to GCS
echo Uploading to Google Cloud Server %SERVER_NAME% in zone %ZONE%...
gcloud compute scp system_monitor_deploy.zip %SERVER_NAME%:~ --zone=%ZONE%

REM SSH into the server and run the installation
echo Connecting to server and running installation...
gcloud compute ssh %SERVER_NAME% --zone=%ZONE% --command="mkdir -p %DEPLOY_DIR% && unzip -o system_monitor_deploy.zip -d %DEPLOY_DIR% && cd %DEPLOY_DIR% && chmod +x install.sh && ./install.sh"

echo Deployment complete!
echo Access the dashboard at http://SERVER_IP:5000
echo Note: Replace SERVER_IP with the external IP of your GCS instance
