#!/bin/bash

# Deploy System Monitor to Google Cloud Server
# Usage: ./deploy_to_gcs.sh <server_name> <zone>

# Check if server name and zone are provided
if [ "$#" -ne 2 ]; then
    echo "Usage: ./deploy_to_gcs.sh <server_name> <zone>"
    echo "Example: ./deploy_to_gcs.sh mine2 asia-south2-c"
    exit 1
fi

SERVER_NAME=$1
ZONE=$2
DEPLOY_DIR="system_monitor_deploy"

echo "Preparing deployment package..."

# Create deployment directory
mkdir -p $DEPLOY_DIR
mkdir -p $DEPLOY_DIR/templates
mkdir -p $DEPLOY_DIR/static
mkdir -p $DEPLOY_DIR/data

# Copy server files
cp server.py $DEPLOY_DIR/
cp system_monitor.py $DEPLOY_DIR/
cp requirements.txt $DEPLOY_DIR/
cp README.md $DEPLOY_DIR/
cp start_server.bat $DEPLOY_DIR/

# Copy template files
cp -r templates/* $DEPLOY_DIR/templates/

# Copy static files
cp -r static/* $DEPLOY_DIR/static/

# Create a systemd service file for running the server
cat > $DEPLOY_DIR/system-monitor.service << EOL
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
EOL

# Create installation script
cat > $DEPLOY_DIR/install.sh << EOL
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
EOL

# Make installation script executable
chmod +x $DEPLOY_DIR/install.sh

# Create a README for the deployment
cat > $DEPLOY_DIR/DEPLOY_README.md << EOL
# System Monitor Deployment

This package contains the System Monitor server and client for deployment on a Google Cloud Server.

## Installation

1. Run the installation script:
   \`\`\`
   ./install.sh
   \`\`\`

2. Access the dashboard:
   - Open a web browser and navigate to \`http://SERVER_IP:5000\`
   - The default authentication token is \`default_token_change_me\`

## Configuration

- To change the authentication token, edit the systemd service file:
  \`\`\`
  sudo nano /etc/systemd/system/system-monitor.service
  \`\`\`
  
  Change the \`--auth\` parameter in the \`ExecStart\` line.

- To restart the service after configuration changes:
  \`\`\`
  sudo systemctl daemon-reload
  sudo systemctl restart system-monitor
  \`\`\`

## Monitoring Desktop Clients

To monitor desktop systems, run the following command on each system:

\`\`\`
python system_monitor.py --server http://SERVER_IP:5000 --auth default_token_change_me
\`\`\`

## Monitoring Mobile Clients

Install the Mobile System Monitor app on your Android device and configure it with:
- Server URL: \`http://SERVER_IP:5000\`
- Auth Token: \`default_token_change_me\` (or your custom token)
EOL

# Create a tar.gz archive
echo "Creating deployment archive..."
tar -czf system_monitor_deploy.tar.gz $DEPLOY_DIR

# Upload to GCS
echo "Uploading to Google Cloud Server $SERVER_NAME in zone $ZONE..."
gcloud compute scp system_monitor_deploy.tar.gz $SERVER_NAME:~ --zone=$ZONE

# SSH into the server and run the installation
echo "Connecting to server and running installation..."
gcloud compute ssh $SERVER_NAME --zone=$ZONE --command="tar -xzf system_monitor_deploy.tar.gz && cd $DEPLOY_DIR && ./install.sh"

echo "Deployment complete!"
echo "Access the dashboard at http://SERVER_IP:5000"
echo "Note: Replace SERVER_IP with the external IP of your GCS instance"
