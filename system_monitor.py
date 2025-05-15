#!/usr/bin/env python3
"""
System Monitor Client
---------------------
This tool collects system information and sends it to a web server.
It is intended for legitimate system monitoring purposes only.

Usage:
    python system_monitor.py --server <server_url> --auth <auth_token>

Requirements:
    - Python 3.6+
    - psutil, requests libraries
"""

import argparse
import json
import os
import platform
import socket
import time
from datetime import datetime
import uuid
import logging
import sys

try:
    import psutil
    import requests
except ImportError:
    print("Required libraries not found. Installing...")
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "psutil", "requests"])
    import psutil
    import requests

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler("system_monitor.log"),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger("SystemMonitor")

class SystemMonitor:
    def __init__(self, server_url, auth_token, interval=60):
        """
        Initialize the system monitor.
        
        Args:
            server_url (str): URL of the server to send data to
            auth_token (str): Authentication token for the server
            interval (int): Data collection interval in seconds
        """
        self.server_url = server_url
        self.auth_token = auth_token
        self.interval = interval
        self.system_id = self._generate_system_id()
        logger.info(f"System Monitor initialized with ID: {self.system_id}")
        
    def _generate_system_id(self):
        """Generate a unique ID for this system."""
        try:
            # Try to use a hardware identifier
            return str(uuid.uuid5(uuid.NAMESPACE_DNS, socket.gethostname()))
        except:
            # Fallback to a random UUID
            return str(uuid.uuid4())
    
    def collect_system_info(self):
        """Collect basic system information."""
        info = {
            "timestamp": datetime.now().isoformat(),
            "system_id": self.system_id,
            "hostname": socket.gethostname(),
            "platform": platform.system(),
            "platform_release": platform.release(),
            "platform_version": platform.version(),
            "architecture": platform.machine(),
            "processor": platform.processor(),
            "boot_time": datetime.fromtimestamp(psutil.boot_time()).isoformat()
        }
        return info
    
    def collect_cpu_info(self):
        """Collect CPU usage information."""
        cpu_info = {
            "cpu_percent": psutil.cpu_percent(interval=1, percpu=True),
            "cpu_count": psutil.cpu_count(logical=True),
            "cpu_stats": dict(psutil.cpu_stats()._asdict()),
            "cpu_freq": dict(psutil.cpu_freq()._asdict()) if psutil.cpu_freq() else None,
        }
        return cpu_info
    
    def collect_memory_info(self):
        """Collect memory usage information."""
        virtual_mem = psutil.virtual_memory()
        swap_mem = psutil.swap_memory()
        
        memory_info = {
            "virtual_memory": {
                "total": virtual_mem.total,
                "available": virtual_mem.available,
                "used": virtual_mem.used,
                "percent": virtual_mem.percent
            },
            "swap_memory": {
                "total": swap_mem.total,
                "used": swap_mem.used,
                "free": swap_mem.free,
                "percent": swap_mem.percent
            }
        }
        return memory_info
    
    def collect_disk_info(self):
        """Collect disk usage information."""
        disk_info = {
            "partitions": [],
            "io_counters": dict(psutil.disk_io_counters(perdisk=False)._asdict()) if psutil.disk_io_counters() else None
        }
        
        for partition in psutil.disk_partitions():
            partition_info = dict(partition._asdict())
            try:
                usage = psutil.disk_usage(partition.mountpoint)
                partition_info["usage"] = {
                    "total": usage.total,
                    "used": usage.used,
                    "free": usage.free,
                    "percent": usage.percent
                }
            except PermissionError:
                partition_info["usage"] = None
            
            disk_info["partitions"].append(partition_info)
            
        return disk_info
    
    def collect_network_info(self):
        """Collect network information."""
        network_info = {
            "io_counters": dict(psutil.net_io_counters()._asdict()),
            "connections": [],
            "interfaces": {}
        }
        
        # Get network connections
        for conn in psutil.net_connections():
            connection_info = dict(conn._asdict())
            # Convert addresses to strings to make them JSON serializable
            if connection_info.get('laddr'):
                connection_info['laddr'] = str(connection_info['laddr'])
            if connection_info.get('raddr'):
                connection_info['raddr'] = str(connection_info['raddr'])
            network_info["connections"].append(connection_info)
        
        # Get network interfaces
        for interface_name, addresses in psutil.net_if_addrs().items():
            network_info["interfaces"][interface_name] = [dict(addr._asdict()) for addr in addresses]
            
        return network_info
    
    def collect_process_info(self, limit=10):
        """Collect information about running processes."""
        processes = []
        for proc in sorted(psutil.process_iter(['pid', 'name', 'username', 'cpu_percent', 'memory_percent']), 
                          key=lambda p: p.info['cpu_percent'] or 0, reverse=True)[:limit]:
            try:
                proc_info = proc.info
                proc_info['create_time'] = datetime.fromtimestamp(proc.create_time()).isoformat()
                processes.append(proc_info)
            except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
                pass
        
        return processes
    
    def collect_all_data(self):
        """Collect all system data."""
        try:
            data = {
                "system_info": self.collect_system_info(),
                "cpu_info": self.collect_cpu_info(),
                "memory_info": self.collect_memory_info(),
                "disk_info": self.collect_disk_info(),
                "network_info": self.collect_network_info(),
                "process_info": self.collect_process_info()
            }
            return data
        except Exception as e:
            logger.error(f"Error collecting system data: {e}")
            return None
    
    def send_data(self, data):
        """Send collected data to the server."""
        if not data:
            return False
        
        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {self.auth_token}"
        }
        
        try:
            response = requests.post(
                f"{self.server_url}/api/system-data",
                headers=headers,
                json=data,
                timeout=10
            )
            
            if response.status_code == 200:
                logger.info("Data sent successfully")
                return True
            else:
                logger.error(f"Failed to send data: {response.status_code} - {response.text}")
                return False
                
        except requests.exceptions.RequestException as e:
            logger.error(f"Error sending data: {e}")
            return False
    
    def run(self):
        """Run the monitoring loop."""
        logger.info(f"Starting system monitoring. Sending data every {self.interval} seconds.")
        
        try:
            while True:
                data = self.collect_all_data()
                if data:
                    success = self.send_data(data)
                    if not success:
                        # Save data locally if sending fails
                        self._save_data_locally(data)
                
                time.sleep(self.interval)
        except KeyboardInterrupt:
            logger.info("Monitoring stopped by user")
        except Exception as e:
            logger.error(f"Unexpected error: {e}")
    
    def _save_data_locally(self, data):
        """Save data locally if sending fails."""
        try:
            os.makedirs("data", exist_ok=True)
            filename = f"data/system_data_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
            with open(filename, 'w') as f:
                json.dump(data, f)
            logger.info(f"Data saved locally to {filename}")
        except Exception as e:
            logger.error(f"Error saving data locally: {e}")

def main():
    parser = argparse.ArgumentParser(description="System Monitor Client")
    parser.add_argument("--server", required=True, help="Server URL to send data to")
    parser.add_argument("--auth", required=True, help="Authentication token")
    parser.add_argument("--interval", type=int, default=60, help="Data collection interval in seconds")
    
    args = parser.parse_args()
    
    monitor = SystemMonitor(args.server, args.auth, args.interval)
    monitor.run()

if __name__ == "__main__":
    main()
