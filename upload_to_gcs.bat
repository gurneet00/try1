@echo off
echo Uploading System Monitor to Google Cloud Server...

REM Check if server name and zone are provided
if "%~1"=="" (
    echo Usage: upload_to_gcs.bat ^<server_name^> ^<zone^>
    echo Example: upload_to_gcs.bat mine2 asia-south2-c
    exit /b 1
)

if "%~2"=="" (
    echo Usage: upload_to_gcs.bat ^<server_name^> ^<zone^>
    echo Example: upload_to_gcs.bat mine2 asia-south2-c
    exit /b 1
)

set SERVER_NAME=%~1
set ZONE=%~2

REM Check if gcloud is installed
where gcloud >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: Google Cloud SDK (gcloud) is not installed or not in PATH.
    echo Please install Google Cloud SDK and try again.
    exit /b 1
)

REM Create a zip archive
echo Creating deployment archive...
powershell -Command "Compress-Archive -Path system_monitor_deploy\* -DestinationPath system_monitor_deploy.zip -Force"

REM Upload to GCS
echo Uploading to Google Cloud Server %SERVER_NAME% in zone %ZONE%...
gcloud compute scp system_monitor_deploy.zip %SERVER_NAME%:~ --zone=%ZONE%

REM SSH into the server and run the installation
echo Connecting to server and running installation...
gcloud compute ssh %SERVER_NAME% --zone=%ZONE% --command="mkdir -p system_monitor_deploy && unzip -o system_monitor_deploy.zip -d system_monitor_deploy && cd system_monitor_deploy && chmod +x install.sh && ./install.sh"

echo Deployment complete!
echo Access the dashboard at http://SERVER_IP:5000
echo Note: Replace SERVER_IP with the external IP of your GCS instance
