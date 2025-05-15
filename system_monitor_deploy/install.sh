#/bin/bash 
 
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
 
echo "System Monitor installed and started" 
echo "Access the dashboard at http://$(hostname -I | awk '{print $1}'):5000" 
