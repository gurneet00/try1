# Instructions for Uploading System Monitor to GCS

Follow these steps to manually upload and deploy the System Monitor to your Google Cloud Server (GCS).

## Step 1: Prepare the Deployment Package

The deployment package has already been prepared for you in the `system_monitor_deploy` directory. It contains all the necessary files for the System Monitor.

## Step 2: Compress the Deployment Package

1. Open File Explorer and navigate to the project directory
2. Right-click on the `system_monitor_deploy` folder
3. Select "Send to" > "Compressed (zipped) folder"
4. This will create a file named `system_monitor_deploy.zip`

## Step 3: Upload to GCS Using gcloud

Open a command prompt and run the following commands:

```
gcloud compute scp system_monitor_deploy.zip mine2:~ --zone=asia-south2-c
```

This will upload the zip file to your GCS instance.

## Step 4: SSH into Your GCS Instance

```
gcloud compute ssh mine2 --zone=asia-south2-c
```

## Step 5: Extract and Install on the Server

Once you're connected to your GCS instance, run the following commands:

```bash
# Extract the zip file
unzip -o system_monitor_deploy.zip -d system_monitor_deploy

# Navigate to the extracted directory
cd system_monitor_deploy

# Make the installation script executable
chmod +x install.sh

# Run the installation script
./install.sh
```

## Step 6: Verify Installation

1. The installation script will output the URL to access the dashboard
2. Open a web browser and navigate to that URL (e.g., `http://SERVER_IP:5000`)
3. Use the default authentication token: `default_token_change_me`

## Step 7: Configure Clients

### Desktop Clients

Run the following command on each desktop system you want to monitor:

```
python system_monitor.py --server http://SERVER_IP:5000 --auth default_token_change_me
```

Replace `SERVER_IP` with the external IP of your GCS instance.

### Mobile Clients

Install the Mobile System Monitor app on your Android device and configure it with:
- Server URL: `http://SERVER_IP:5000`
- Auth Token: `default_token_change_me` (or your custom token)

## Troubleshooting

### Cannot Access Dashboard

1. Make sure the firewall allows traffic on port 5000:
   ```
   gcloud compute firewall-rules create allow-system-monitor --allow tcp:5000
   ```

2. Check if the service is running:
   ```
   sudo systemctl status system-monitor
   ```

3. View the service logs:
   ```
   sudo journalctl -u system-monitor
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
