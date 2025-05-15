#!/usr/bin/env python3
"""
System Monitor Server
--------------------
A Flask-based web server that receives system data from clients and displays it.
Supports both desktop and mobile clients.

Usage:
    python server.py --port <port> --auth <auth_token>

Requirements:
    - Python 3.6+
    - Flask, Flask-HTTPAuth libraries
"""

import argparse
import json
import os
import logging
from datetime import datetime
import sys

try:
    from flask import Flask, request, jsonify, render_template, abort, send_from_directory
    from flask_httpauth import HTTPTokenAuth
except ImportError:
    print("Required libraries not found. Installing...")
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "flask", "flask-httpauth"])
    from flask import Flask, request, jsonify, render_template, abort, send_from_directory
    from flask_httpauth import HTTPTokenAuth

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler("server.log"),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger("SystemMonitorServer")

app = Flask(__name__)
auth = HTTPTokenAuth(scheme="Bearer")

# In-memory storage for system data
# In a production environment, you would use a database
systems_data = {}

# Create directories for data storage
os.makedirs("data", exist_ok=True)
os.makedirs("templates", exist_ok=True)
os.makedirs("static", exist_ok=True)

# Authentication token
AUTH_TOKEN = None

@auth.verify_token
def verify_token(token):
    """Verify the authentication token."""
    if token == AUTH_TOKEN:
        return "system_monitor_client"
    return None

def save_data(system_id, data):
    """Save system data to a file."""
    try:
        # Create a directory for this system if it doesn't exist
        system_dir = os.path.join("data", system_id)
        os.makedirs(system_dir, exist_ok=True)

        # Save the data to a timestamped file
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = os.path.join(system_dir, f"data_{timestamp}.json")

        with open(filename, 'w') as f:
            json.dump(data, f)

        logger.info(f"Data saved to {filename}")

        # Also save as latest.json for quick access
        with open(os.path.join(system_dir, "latest.json"), 'w') as f:
            json.dump(data, f)
    except Exception as e:
        logger.error(f"Error saving data: {e}")

@app.route('/api/ping', methods=['GET'])
@auth.login_required
def ping():
    """Simple endpoint to check if the server is up and authentication is working."""
    return jsonify({"status": "success", "message": "Server is running"})

@app.route('/api/system-data', methods=['POST'])
@auth.login_required
def receive_system_data():
    """Endpoint to receive system data from clients."""
    data = request.json

    # Handle desktop client data format
    if data and 'system_info' in data and 'system_id' in data['system_info']:
        system_id = data['system_info']['system_id']
        client_type = 'desktop'
    # Handle mobile client data format
    elif data and 'deviceId' in data:
        system_id = data['deviceId']
        client_type = 'mobile'

        # Get CPU info from the enhanced data if available
        cpu_info = {}
        if 'cpuInfo' in data:
            cpu_percent = data['cpuInfo'].get('usage', 0)
            cpu_count = data['cpuInfo'].get('processorCount', 1)
            cpu_model = data['cpuInfo'].get('model', 'Unknown')
            cpu_info = {
                'cpu_percent': [cpu_percent],
                'cpu_count': cpu_count,
                'cpu_model': cpu_model,
                'cpu_user_percent': data['cpuInfo'].get('userUsage', 0),
                'cpu_system_percent': data['cpuInfo'].get('systemUsage', 0),
                'cpu_idle_percent': data['cpuInfo'].get('idleUsage', 0)
            }
        else:
            # Fallback for older clients
            cpu_info = {
                'cpu_percent': [50],  # Default value
                'cpu_count': 1,
            }

        # Get process info if available
        process_info = []
        if 'processInfo' in data and 'topProcesses' in data['processInfo']:
            for proc in data['processInfo'].get('topProcesses', []):
                process_info.append({
                    'pid': proc.get('pid', 0),
                    'name': proc.get('processName', 'Unknown'),
                    'username': 'mobile',
                    'cpu_percent': 0,  # Not available in mobile data
                    'memory_percent': 0,  # Not available in mobile data
                    'create_time': data.get('timestamp', datetime.now().isoformat())
                })

        # Transform mobile data to match our storage format
        data = {
            'system_info': {
                'system_id': system_id,
                'hostname': data['deviceInfo'].get('deviceName', 'Unknown Mobile Device'),
                'platform': f"{data['deviceInfo'].get('systemName', 'Unknown')} Mobile",
                'platform_release': data['deviceInfo'].get('systemVersion', ''),
                'architecture': data['deviceInfo'].get('deviceType', ''),
                'processor': data['deviceInfo'].get('manufacturer', '') + ' ' + data['deviceInfo'].get('model', ''),
                'boot_time': data.get('timestamp', datetime.now().isoformat())
            },
            'cpu_info': cpu_info,
            'process_info': process_info,
            'memory_info': {
                'virtual_memory': {
                    'total': data['memoryInfo'].get('total', 0),
                    'available': data['memoryInfo'].get('free', 0),
                    'used': data['memoryInfo'].get('used', 0),
                    'percent': data['memoryInfo'].get('usedPercentage', 0)
                }
            },
            'disk_info': {
                'partitions': [
                    {
                        'device': 'Internal Storage',
                        'mountpoint': '/',
                        'fstype': 'unknown',
                        'usage': {
                            'total': data['storageInfo'].get('total', 0),
                            'used': data['storageInfo'].get('used', 0),
                            'free': data['storageInfo'].get('free', 0),
                            'percent': data['storageInfo'].get('usedPercentage', 0)
                        }
                    }
                ]
            },
            'network_info': {
                'io_counters': {
                    'bytes_sent': 0,  # Mobile devices don't typically provide this
                    'bytes_recv': 0
                }
            },
            'process_info': []  # Mobile devices don't typically provide process info
        }
    else:
        return jsonify({"error": "Invalid data format"}), 400

    systems_data[system_id] = {
        "last_update": datetime.now().isoformat(),
        "data": data,
        "client_type": client_type
    }

    # Save data to file
    save_data(system_id, data)

    return jsonify({"status": "success"})

@app.route('/')
def index():
    """Main page showing all monitored systems."""
    systems = []

    for system_id, system_data in systems_data.items():
        client_type = system_data.get("client_type", "desktop")
        systems.append({
            "id": system_id,
            "hostname": system_data["data"]["system_info"]["hostname"],
            "platform": system_data["data"]["system_info"]["platform"],
            "last_update": system_data["last_update"],
            "client_type": client_type,
            "is_mobile": client_type == "mobile"
        })

    return render_template('index.html', systems=systems)

@app.route('/system/<system_id>')
def system_details(system_id):
    """Page showing detailed information for a specific system."""
    if system_id not in systems_data:
        # Try to load from file
        try:
            system_dir = os.path.join("data", system_id)
            latest_file = os.path.join(system_dir, "latest.json")

            if os.path.exists(latest_file):
                with open(latest_file, 'r') as f:
                    data = json.load(f)

                systems_data[system_id] = {
                    "last_update": datetime.now().isoformat(),
                    "data": data
                }
            else:
                abort(404)
        except Exception as e:
            logger.error(f"Error loading data for system {system_id}: {e}")
            abort(404)

    return render_template('system.html', system=systems_data[system_id])

@app.route('/static/<path:path>')
def serve_static(path):
    """Serve static files."""
    return send_from_directory('static', path)

@app.route('/client')
def client():
    """Endpoint to collect system monitoring information directly from the browser."""
    try:
        # Create a SystemMonitor instance to collect data
        import platform
        import socket
        import uuid
        import psutil
        from datetime import datetime

        # Generate a system ID for this client
        try:
            system_id = str(uuid.uuid5(uuid.NAMESPACE_DNS, socket.gethostname()))
        except:
            system_id = str(uuid.uuid4())

        # Collect basic system information
        system_info = {
            "timestamp": datetime.now().isoformat(),
            "system_id": system_id,
            "hostname": socket.gethostname(),
            "platform": platform.system(),
            "platform_release": platform.release(),
            "platform_version": platform.version(),
            "architecture": platform.machine(),
            "processor": platform.processor(),
            "boot_time": datetime.fromtimestamp(psutil.boot_time()).isoformat()
        }

        # Collect CPU information
        cpu_info = {
            "cpu_percent": psutil.cpu_percent(interval=1, percpu=True),
            "cpu_count": psutil.cpu_count(logical=True),
            "cpu_stats": dict(psutil.cpu_stats()._asdict()),
            "cpu_freq": dict(psutil.cpu_freq()._asdict()) if psutil.cpu_freq() else None,
        }

        # Collect memory information
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

        # Collect disk information
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

        # Collect network information
        network_info = {
            "io_counters": dict(psutil.net_io_counters()._asdict()),
            "interfaces": {}
        }

        # Get network interfaces
        for interface_name, addresses in psutil.net_if_addrs().items():
            network_info["interfaces"][interface_name] = [dict(addr._asdict()) for addr in addresses]

        # Collect process information (top 10 by CPU usage)
        processes = []
        for proc in sorted(psutil.process_iter(['pid', 'name', 'username', 'cpu_percent', 'memory_percent']),
                          key=lambda p: p.info['cpu_percent'] or 0, reverse=True)[:10]:
            try:
                proc_info = proc.info
                proc_info['create_time'] = datetime.fromtimestamp(proc.create_time()).isoformat()
                processes.append(proc_info)
            except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
                pass

        # Combine all data
        data = {
            "system_info": system_info,
            "cpu_info": cpu_info,
            "memory_info": memory_info,
            "disk_info": disk_info,
            "network_info": network_info,
            "process_info": processes
        }

        # Save the collected data
        systems_data[system_id] = {
            "last_update": datetime.now().isoformat(),
            "data": data,
            "client_type": "web"
        }

        # Save data to file
        save_data(system_id, data)

        # Return a simple HTML page with the collected data
        return render_template('client.html', system=systems_data[system_id])

    except Exception as e:
        logger.error(f"Error collecting system data: {e}")
        return jsonify({"error": str(e)}), 500

@app.route('/download')
def download_apk():
    """Download the mobile APK file or show instructions."""
    try:
        # Path to the instructions HTML file
        instructions_path = os.path.join('downloads', 'mobile_instructions.html')

        # If we don't have an APK, serve the instructions page
        if not os.path.exists(instructions_path):
            # Create a basic instructions file
            with open(instructions_path, 'w') as f:
                f.write("""<!DOCTYPE html>
<html>
<head>
    <title>Mobile System Monitor - Installation Instructions</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; line-height: 1.6; }
        h1 { color: #2c3e50; }
        .container { max-width: 800px; margin: 0 auto; }
        .note { background-color: #f8f9fa; padding: 15px; border-left: 4px solid #007bff; margin-bottom: 20px; }
        code { background-color: #f1f1f1; padding: 2px 5px; border-radius: 3px; }
    </style>
</head>
<body>
    <div class="container">
        <h1>Mobile System Monitor - Installation Instructions</h1>

        <div class="note">
            <strong>Note:</strong> The APK file is not currently available for download. Please use the /client endpoint for system monitoring.
        </div>

        <h2>Using the Web Client</h2>
        <p>You can now use the web client to monitor your system:</p>
        <ol>
            <li>Navigate to <code>/client</code> in your web browser</li>
            <li>The system information will be collected and displayed</li>
            <li>Your system will be added to the monitoring dashboard</li>
        </ol>

        <h2>API Endpoints</h2>
        <ul>
            <li><code>/</code> - Main dashboard showing all monitored systems</li>
            <li><code>/client</code> - Web client for collecting system information</li>
            <li><code>/system/{system_id}</code> - Detailed view of a specific system</li>
            <li><code>/api/system-data</code> - API endpoint for sending system data (requires authentication)</li>
        </ul>
    </div>
</body>
</html>""")

        # Return the instructions HTML page
        return send_from_directory('downloads', 'mobile_instructions.html')

    except Exception as e:
        logger.error(f"Error serving mobile app download: {e}")
        abort(500)

def create_template_files():
    """Create the HTML template files."""
    # Create index.html
    index_html = """<!DOCTYPE html>
<html>
<head>
    <title>System Monitor Dashboard</title>
    <link rel="stylesheet" href="/static/style.css">
</head>
<body>
    <header>
        <h1>System Monitor Dashboard</h1>
        <a href="/client" class="button">Monitor This System</a>
    </header>
    <main>
        <div class="info-box" style="background-color: #e9ecef; padding: 15px; border-radius: 5px; margin-bottom: 20px;">
            <h2>System Monitoring</h2>
            <p>This dashboard shows all systems being monitored by the System Monitor application.</p>
            <p>To add your current system to the dashboard, click the "Monitor This System" button above.</p>
        </div>

        <h2>Monitored Systems</h2>
        {% if systems %}
            <div class="systems-grid">
                {% for system in systems %}
                    <div class="system-card">
                        <h3>{{ system.hostname }}</h3>
                        <p>ID: {{ system.id }}</p>
                        <p>Platform: {{ system.platform }}</p>
                        <p>Client Type: {{ system.client_type }}</p>
                        <p>Last Update: {{ system.last_update }}</p>
                        <a href="/system/{{ system.id }}" class="button">View Details</a>
                    </div>
                {% endfor %}
            </div>
        {% else %}
            <p>No systems are currently being monitored.</p>
            <p>Click the "Monitor This System" button to add your current system.</p>
        {% endif %}
    </main>
    <footer>
        <p>System Monitor Dashboard</p>
    </footer>
</body>
</html>"""

    # Create system.html
    system_html = """<!DOCTYPE html>
<html>
<head>
    <title>System Details - {{ system.data.system_info.hostname }}</title>
    <link rel="stylesheet" href="/static/style.css">
    <script src="/static/charts.js"></script>
</head>
<body>
    <header>
        <h1>System Details: {{ system.data.system_info.hostname }}</h1>
        <a href="/" class="button">Back to Dashboard</a>
    </header>
    <main>
        <div class="system-details">
            <section class="card">
                <h2>System Information</h2>
                <table>
                    <tr><th>Hostname</th><td>{{ system.data.system_info.hostname }}</td></tr>
                    <tr><th>Platform</th><td>{{ system.data.system_info.platform }} {{ system.data.system_info.platform_release }}</td></tr>
                    <tr><th>Architecture</th><td>{{ system.data.system_info.architecture }}</td></tr>
                    <tr><th>Processor</th><td>{{ system.data.system_info.processor }}</td></tr>
                    <tr><th>Boot Time</th><td>{{ system.data.system_info.boot_time }}</td></tr>
                    <tr><th>Last Update</th><td>{{ system.last_update }}</td></tr>
                </table>
            </section>

            <section class="card">
                <h2>CPU Information</h2>
                <div class="chart-container">
                    <canvas id="cpuChart"></canvas>
                </div>
                <table>
                    <tr><th>CPU Count</th><td>{{ system.data.cpu_info.cpu_count }}</td></tr>
                    <tr><th>CPU Usage</th><td>{{ system.data.cpu_info.cpu_percent }}</td></tr>
                </table>
            </section>

            <section class="card">
                <h2>Memory Information</h2>
                <div class="chart-container">
                    <canvas id="memoryChart"></canvas>
                </div>
                <table>
                    <tr><th>Total Memory</th><td>{{ system.data.memory_info.virtual_memory.total | filesizeformat }}</td></tr>
                    <tr><th>Available Memory</th><td>{{ system.data.memory_info.virtual_memory.available | filesizeformat }}</td></tr>
                    <tr><th>Used Memory</th><td>{{ system.data.memory_info.virtual_memory.used | filesizeformat }} ({{ system.data.memory_info.virtual_memory.percent }}%)</td></tr>
                </table>
            </section>

            <section class="card">
                <h2>Disk Information</h2>
                <table>
                    <tr>
                        <th>Device</th>
                        <th>Mountpoint</th>
                        <th>File System</th>
                        <th>Total</th>
                        <th>Used</th>
                        <th>Free</th>
                        <th>Usage</th>
                    </tr>
                    {% for partition in system.data.disk_info.partitions %}
                        {% if partition.usage %}
                        <tr>
                            <td>{{ partition.device }}</td>
                            <td>{{ partition.mountpoint }}</td>
                            <td>{{ partition.fstype }}</td>
                            <td>{{ partition.usage.total | filesizeformat }}</td>
                            <td>{{ partition.usage.used | filesizeformat }}</td>
                            <td>{{ partition.usage.free | filesizeformat }}</td>
                            <td>{{ partition.usage.percent }}%</td>
                        </tr>
                        {% endif %}
                    {% endfor %}
                </table>
            </section>

            <section class="card">
                <h2>Network Information</h2>
                <table>
                    <tr>
                        <th>Bytes Sent</th>
                        <td>{{ system.data.network_info.io_counters.bytes_sent | filesizeformat }}</td>
                    </tr>
                    <tr>
                        <th>Bytes Received</th>
                        <td>{{ system.data.network_info.io_counters.bytes_recv | filesizeformat }}</td>
                    </tr>
                </table>
            </section>

            <section class="card">
                <h2>Top Processes</h2>
                <table>
                    <tr>
                        <th>PID</th>
                        <th>Name</th>
                        <th>Username</th>
                        <th>CPU %</th>
                        <th>Memory %</th>
                        <th>Created</th>
                    </tr>
                    {% for process in system.data.process_info %}
                    <tr>
                        <td>{{ process.pid }}</td>
                        <td>{{ process.name }}</td>
                        <td>{{ process.username }}</td>
                        <td>{{ process.cpu_percent }}</td>
                        <td>{{ process.memory_percent }}</td>
                        <td>{{ process.create_time }}</td>
                    </tr>
                    {% endfor %}
                </table>
            </section>
        </div>
    </main>
    <footer>
        <p>System Monitor Dashboard</p>
    </footer>

    <script>
        // Initialize charts with system data
        document.addEventListener('DOMContentLoaded', function() {
            // CPU Chart
            const cpuData = {{ system.data.cpu_info.cpu_percent | tojson }};
            createCpuChart('cpuChart', cpuData);

            // Memory Chart
            const memoryData = {
                used: {{ system.data.memory_info.virtual_memory.used }},
                available: {{ system.data.memory_info.virtual_memory.available }}
            };
            createMemoryChart('memoryChart', memoryData);
        });
    </script>
</body>
</html>"""

    # Create client.html
    client_html = """<!DOCTYPE html>
<html>
<head>
    <title>System Monitor Client - {{ system.data.system_info.hostname }}</title>
    <link rel="stylesheet" href="/static/style.css">
    <script src="/static/charts.js"></script>
    <style>
        .refresh-button {
            background-color: #4CAF50;
            color: white;
            padding: 10px 15px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 16px;
            margin-top: 20px;
        }
        .refresh-button:hover {
            background-color: #45a049;
        }
        .status-message {
            margin-top: 10px;
            padding: 10px;
            border-radius: 4px;
        }
        .success {
            background-color: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }
        .error {
            background-color: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }
        .info-box {
            background-color: #e9ecef;
            padding: 15px;
            border-radius: 5px;
            margin-bottom: 20px;
        }
    </style>
</head>
<body>
    <header>
        <h1>System Monitor Client: {{ system.data.system_info.hostname }}</h1>
        <a href="/" class="button">Back to Dashboard</a>
    </header>
    <main>
        <div class="info-box">
            <h2>System Monitoring Information</h2>
            <p>This page collects system information from your device and sends it to the monitoring server.</p>
            <p>Your system has been added to the monitoring dashboard with ID: <strong>{{ system.data.system_info.system_id }}</strong></p>
            <p>Last update: <strong>{{ system.last_update }}</strong></p>
            <button class="refresh-button" onclick="window.location.reload()">Refresh Data</button>
            <div id="status-message" class="status-message success">
                Data collected and sent successfully!
            </div>
        </div>

        <div class="system-details">
            <section class="card">
                <h2>System Information</h2>
                <table>
                    <tr><th>Hostname</th><td>{{ system.data.system_info.hostname }}</td></tr>
                    <tr><th>Platform</th><td>{{ system.data.system_info.platform }} {{ system.data.system_info.platform_release }}</td></tr>
                    <tr><th>Architecture</th><td>{{ system.data.system_info.architecture }}</td></tr>
                    <tr><th>Processor</th><td>{{ system.data.system_info.processor }}</td></tr>
                    <tr><th>Boot Time</th><td>{{ system.data.system_info.boot_time }}</td></tr>
                    <tr><th>Last Update</th><td>{{ system.last_update }}</td></tr>
                </table>
            </section>

            <section class="card">
                <h2>CPU Information</h2>
                <div class="chart-container">
                    <canvas id="cpuChart"></canvas>
                </div>
                <table>
                    <tr><th>CPU Count</th><td>{{ system.data.cpu_info.cpu_count }}</td></tr>
                    <tr><th>CPU Usage</th><td>{{ system.data.cpu_info.cpu_percent }}</td></tr>
                </table>
            </section>

            <section class="card">
                <h2>Memory Information</h2>
                <div class="chart-container">
                    <canvas id="memoryChart"></canvas>
                </div>
                <table>
                    <tr><th>Total Memory</th><td>{{ system.data.memory_info.virtual_memory.total | filesizeformat }}</td></tr>
                    <tr><th>Available Memory</th><td>{{ system.data.memory_info.virtual_memory.available | filesizeformat }}</td></tr>
                    <tr><th>Used Memory</th><td>{{ system.data.memory_info.virtual_memory.used | filesizeformat }} ({{ system.data.memory_info.virtual_memory.percent }}%)</td></tr>
                </table>
            </section>

            <section class="card">
                <h2>Disk Information</h2>
                <table>
                    <tr>
                        <th>Device</th>
                        <th>Mountpoint</th>
                        <th>File System</th>
                        <th>Total</th>
                        <th>Used</th>
                        <th>Free</th>
                        <th>Usage</th>
                    </tr>
                    {% for partition in system.data.disk_info.partitions %}
                        {% if partition.usage %}
                        <tr>
                            <td>{{ partition.device }}</td>
                            <td>{{ partition.mountpoint }}</td>
                            <td>{{ partition.fstype }}</td>
                            <td>{{ partition.usage.total | filesizeformat }}</td>
                            <td>{{ partition.usage.used | filesizeformat }}</td>
                            <td>{{ partition.usage.free | filesizeformat }}</td>
                            <td>{{ partition.usage.percent }}%</td>
                        </tr>
                        {% endif %}
                    {% endfor %}
                </table>
            </section>

            <section class="card">
                <h2>Network Information</h2>
                <table>
                    <tr>
                        <th>Bytes Sent</th>
                        <td>{{ system.data.network_info.io_counters.bytes_sent | filesizeformat }}</td>
                    </tr>
                    <tr>
                        <th>Bytes Received</th>
                        <td>{{ system.data.network_info.io_counters.bytes_recv | filesizeformat }}</td>
                    </tr>
                </table>
            </section>

            <section class="card">
                <h2>Top Processes</h2>
                <table>
                    <tr>
                        <th>PID</th>
                        <th>Name</th>
                        <th>Username</th>
                        <th>CPU %</th>
                        <th>Memory %</th>
                        <th>Created</th>
                    </tr>
                    {% for process in system.data.process_info %}
                    <tr>
                        <td>{{ process.pid }}</td>
                        <td>{{ process.name }}</td>
                        <td>{{ process.username }}</td>
                        <td>{{ process.cpu_percent }}</td>
                        <td>{{ process.memory_percent }}</td>
                        <td>{{ process.create_time }}</td>
                    </tr>
                    {% endfor %}
                </table>
            </section>
        </div>
    </main>
    <footer>
        <p>System Monitor Dashboard</p>
    </footer>

    <script>
        // Initialize charts with system data
        document.addEventListener('DOMContentLoaded', function() {
            // CPU Chart
            const cpuData = {{ system.data.cpu_info.cpu_percent | tojson }};
            createCpuChart('cpuChart', cpuData);

            // Memory Chart
            const memoryData = {
                used: {{ system.data.memory_info.virtual_memory.used }},
                available: {{ system.data.memory_info.virtual_memory.available }}
            };
            createMemoryChart('memoryChart', memoryData);
        });
    </script>
</body>
</html>"""

    # Create style.css
    style_css = """body {
    font-family: Arial, sans-serif;
    margin: 0;
    padding: 0;
    background-color: #f5f5f5;
    color: #333;
}

header {
    background-color: #2c3e50;
    color: white;
    padding: 1rem;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

main {
    padding: 1rem;
}

footer {
    background-color: #2c3e50;
    color: white;
    text-align: center;
    padding: 1rem;
    margin-top: 2rem;
}

.systems-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 1rem;
}

.system-card {
    background-color: white;
    border-radius: 5px;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
    padding: 1rem;
}

.system-details {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(500px, 1fr));
    gap: 1rem;
}

.card {
    background-color: white;
    border-radius: 5px;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
    padding: 1rem;
    margin-bottom: 1rem;
}

table {
    width: 100%;
    border-collapse: collapse;
    margin-top: 1rem;
}

th, td {
    padding: 0.5rem;
    text-align: left;
    border-bottom: 1px solid #ddd;
}

th {
    background-color: #f2f2f2;
}

.button {
    display: inline-block;
    background-color: #3498db;
    color: white;
    padding: 0.5rem 1rem;
    text-decoration: none;
    border-radius: 3px;
    margin-top: 0.5rem;
}

.button:hover {
    background-color: #2980b9;
}

.chart-container {
    height: 200px;
    margin-bottom: 1rem;
}"""

    # Create charts.js
    charts_js = """function createCpuChart(canvasId, cpuData) {
    const ctx = document.getElementById(canvasId).getContext('2d');

    // Create labels for each CPU core
    const labels = [];
    for (let i = 0; i < cpuData.length; i++) {
        labels.push(`Core ${i}`);
    }

    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'CPU Usage (%)',
                data: cpuData,
                backgroundColor: 'rgba(54, 162, 235, 0.5)',
                borderColor: 'rgba(54, 162, 235, 1)',
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true,
                    max: 100
                }
            }
        }
    });
}

function createMemoryChart(canvasId, memoryData) {
    const ctx = document.getElementById(canvasId).getContext('2d');

    new Chart(ctx, {
        type: 'pie',
        data: {
            labels: ['Used', 'Available'],
            datasets: [{
                data: [memoryData.used, memoryData.available],
                backgroundColor: [
                    'rgba(255, 99, 132, 0.5)',
                    'rgba(75, 192, 192, 0.5)'
                ],
                borderColor: [
                    'rgba(255, 99, 132, 1)',
                    'rgba(75, 192, 192, 1)'
                ],
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false
        }
    });
}"""

    # Write the files
    with open(os.path.join("templates", "index.html"), 'w') as f:
        f.write(index_html)

    with open(os.path.join("templates", "system.html"), 'w') as f:
        f.write(system_html)

    with open(os.path.join("templates", "client.html"), 'w') as f:
        f.write(client_html)

    with open(os.path.join("static", "style.css"), 'w') as f:
        f.write(style_css)

    with open(os.path.join("static", "charts.js"), 'w') as f:
        f.write(charts_js)

def main():
    parser = argparse.ArgumentParser(description="System Monitor Server")
    parser.add_argument("--port", type=int, default=5000, help="Port to run the server on")
    parser.add_argument("--auth", required=True, help="Authentication token")

    args = parser.parse_args()

    global AUTH_TOKEN
    AUTH_TOKEN = args.auth

    # Create template files
    create_template_files()

    logger.info(f"Starting server on port {args.port}")
    app.run(host="0.0.0.0", port=args.port, debug=False)

if __name__ == "__main__":
    main()
