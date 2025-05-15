#!/bin/bash

# System Monitor Deployment Script with PM2
# This script deploys the System Monitor project and sets it up to run permanently with PM2

# Exit on any error
set -e

echo "=== System Monitor Deployment with PM2 ==="
echo

# Check if running as root or with sudo
if [ "$EUID" -ne 0 ]; then
  echo "Please run this script with sudo or as root"
  exit 1
fi

# Install dependencies
echo "Installing dependencies..."
apt update
apt install -y python3 python3-pip nodejs npm
npm install -g pm2

# Install Python packages
echo "Installing Python packages..."
pip3 install -r requirements.txt

# Create installation directory
echo "Setting up installation directory..."
mkdir -p /opt/system-monitor
cp -r * /opt/system-monitor/
cd /opt/system-monitor

# Set up PM2
echo "Setting up PM2..."
pm2 start ecosystem.config.js
pm2 save
pm2 startup

# Set up firewall
echo "Setting up firewall..."
if command -v ufw &> /dev/null; then
  ufw allow 5000/tcp
  echo "Firewall rule added for port 5000"
fi

# Get server IP
SERVER_IP=$(hostname -I | awk '{print $1}')

echo
echo "=== Deployment Complete ==="
echo "System Monitor is now running permanently with PM2"
echo
echo "Access the dashboard at: http://$SERVER_IP:5000"
echo
echo "PM2 Commands:"
echo "  - Check status: pm2 status"
echo "  - View logs: pm2 logs system-monitor"
echo "  - Restart: pm2 restart system-monitor"
echo "  - Stop: pm2 stop system-monitor"
echo
echo "The system will automatically start on server reboot"
