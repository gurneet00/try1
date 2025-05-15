# Manual Deployment Guide for System Monitor

This guide provides step-by-step instructions for manually deploying the System Monitor application to a Google Cloud Server (GCS) or any other Linux server.

## Prerequisites

- A Google Cloud Server (GCS) instance or any Linux server
- SSH access to the server
- Python 3.6+ installed on the server

## Step 1: Prepare the Files Locally

1. Create a deployment directory:
   ```
   mkdir system_monitor_deploy
   ```

2. Copy the necessary files to the deployment directory:
   ```
   copy server.py system_monitor_deploy\
   copy system_monitor.py system_monitor_deploy\
   copy requirements.txt system_monitor_deploy\
   copy -r templates system_monitor_deploy\
   copy -r static system_monitor_deploy\
   mkdir system_monitor_deploy\data
   ```

3. Create a systemd service file in the deployment directory:
   ```
   # system-monitor.service
   [Unit]
   Description=System Monitor Server
   After=network.target

   [Service]
   User=root
   WorkingDirectory=/opt/system-monitor
   ExecStart=/usr/bin/python3 /opt/system-monitor/server.py --port 5000 --auth default_token_change_me
   Restart=always
   RestartSec=10

   [Install]
   WantedBy=multi-user.target
   ```

4. Create an installation script in the deployment directory:
   ```bash
   #!/bin/bash

   # Install dependencies
   sudo apt-get update
   sudo apt-get install -y python3 python3-pip

   # Install Python packages
   sudo pip3 install -r requirements.txt

   # Create installation directory
   sudo mkdir -p /opt/system-monitor
   sudo cp -r * /opt/system-monitor/

   # Set up systemd service
   sudo cp system-monitor.service /etc/systemd/system/
   sudo systemctl daemon-reload
   sudo systemctl enable system-monitor
   sudo systemctl start system-monitor

   echo "System Monitor installed and started!"
   echo "Access the dashboard at http://$(hostname -I | awk '{print $1}'):5000"
   ```

5. Compress the deployment directory:
   ```
   zip -r system_monitor_deploy.zip system_monitor_deploy
   ```

## Step 2: Upload Files to the Server

### Option 1: Using gcloud (for GCS)

1. Upload the zip file to your GCS instance:
   ```
   gcloud compute scp system_monitor_deploy.zip mine2:~ --zone=asia-south2-c
   ```

### Option 2: Using SCP (for any server)

1. Upload the zip file to your server:
   ```
   scp system_monitor_deploy.zip username@server_ip:~
   ```

## Step 3: Install on the Server

1. SSH into your server:
   ```
   # For GCS
   gcloud compute ssh mine2 --zone=asia-south2-c

   # For any server
   ssh username@server_ip
   ```

2. Extract the zip file:
   ```
   unzip system_monitor_deploy.zip
   ```

3. Run the installation script:
   ```
   cd system_monitor_deploy
   chmod +x install.sh
   ./install.sh
   ```

## Step 4: Verify Installation

1. Check if the service is running:
   ```
   sudo systemctl status system-monitor
   ```

2. Open a web browser and navigate to:
   ```
   http://SERVER_IP:5000
   ```
   Replace `SERVER_IP` with your server's public IP address.

3. Use the default authentication token: `default_token_change_me`

## Step 5: Configure Clients

### Desktop Clients

Run the following command on each desktop system you want to monitor:

```
python system_monitor.py --server http://SERVER_IP:5000 --auth default_token_change_me
```

### Mobile Clients

Install the Mobile System Monitor app on your Android device and configure it with:
- Server URL: `http://SERVER_IP:5000`
- Auth Token: `default_token_change_me` (or your custom token)

## Troubleshooting

### Service Not Starting

Check the service logs:
```
sudo journalctl -u system-monitor
```

### Cannot Access Dashboard

1. Check if the service is running:
   ```
   sudo systemctl status system-monitor
   ```

2. Check if the port is open in the firewall:
   ```
   # For GCS
   gcloud compute firewall-rules create allow-system-monitor --allow tcp:5000

   # For other servers
   sudo ufw allow 5000/tcp
   ```

3. Verify the server's IP address:
   ```
   hostname -I
   ```

### Changing the Authentication Token

1. Edit the systemd service file:
   ```
   sudo nano /etc/systemd/system/system-monitor.service
   ```

2. Change the `--auth` parameter in the `ExecStart` line.

3. Restart the service:
   ```
   sudo systemctl daemon-reload
   sudo systemctl restart system-monitor
   ```
