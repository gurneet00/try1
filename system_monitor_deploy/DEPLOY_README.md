# System Monitor Deployment 
 
This package contains the System Monitor server and client for deployment on a Google Cloud Server. 
 
## Installation 
 
1. Run the installation script: 
   ```bash 
   ./install.sh 
   ``` 
 
2. Access the dashboard: 
   - Open a web browser and navigate to `http://SERVER_IP:5000` 
   - The default authentication token is `default_token_change_me` 
 
## Configuration 
 
- To change the authentication token, edit the systemd service file: 
  ```bash 
  sudo nano /etc/systemd/system/system-monitor.service 
  ``` 
 
  Change the `--auth` parameter in the `ExecStart` line. 
 
- To restart the service after configuration changes: 
  ```bash 
  sudo systemctl daemon-reload 
  sudo systemctl restart system-monitor 
  ``` 
 
## Monitoring Desktop Clients 
 
To monitor desktop systems, run the following command on each system: 
 
```bash 
python system_monitor.py --server http://SERVER_IP:5000 --auth default_token_change_me 
``` 
 
## Monitoring Mobile Clients 
 
Install the Mobile System Monitor app on your Android device and configure it with: 
- Server URL: `http://SERVER_IP:5000` 
- Auth Token: `default_token_change_me` (or your custom token) 
