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

@app.route('/download')
def download_apk():
    """Download the mobile APK file or show instructions."""
    try:
        # Create downloads directory if it doesn't exist
        os.makedirs('downloads', exist_ok=True)

        # Path to the APK file
        apk_path = os.path.join('downloads', 'MobileSystemMonitor.apk')

        # Path to the instructions HTML file
        instructions_path = os.path.join('downloads', 'mobile_instructions.html')

        # Check if we have a valid APK
        if os.path.exists(apk_path) and os.path.getsize(apk_path) > 0:
            # Log the download request
            logger.info(f"APK download requested. File size: {os.path.getsize(apk_path)} bytes")

            # Return the APK file as an attachment
            return send_from_directory(
                'downloads',
                'MobileSystemMonitor.apk',
                as_attachment=True,
                mimetype='application/vnd.android.package-archive'
            )
        else:
            # If no valid APK exists, try to build it
            logger.warning("APK file not found or empty. Attempting to build it...")

            try:
                # Check if we have the build script
                if os.path.exists('build_apk.bat'):
                    import subprocess
                    result = subprocess.run(['build_apk.bat'], capture_output=True, text=True)

                    if result.returncode == 0 and os.path.exists(apk_path) and os.path.getsize(apk_path) > 0:
                        logger.info(f"Successfully built APK. File size: {os.path.getsize(apk_path)} bytes")
                        return send_from_directory(
                            'downloads',
                            'MobileSystemMonitor.apk',
                            as_attachment=True,
                            mimetype='application/vnd.android.package-archive'
                        )
                    else:
                        logger.error(f"Failed to build APK: {result.stderr}")
            except Exception as e:
                logger.error(f"Error building APK: {e}")

            # If we still don't have an APK, serve the instructions page
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
            <strong>Note:</strong> The APK file is not currently available for download. Please contact the administrator or build it manually.
        </div>

        <h2>Building the APK Manually</h2>
        <ol>
            <li>Clone the repository: <code>git clone https://github.com/yourusername/system-monitor.git</code></li>
            <li>Navigate to the project directory: <code>cd system-monitor</code></li>
            <li>Run the build script: <code>build_apk.bat</code></li>
            <li>The APK will be available at: <code>downloads/MobileSystemMonitor.apk</code></li>
        </ol>

        <h2>Installing on Your Device</h2>
        <ol>
            <li>Enable "Install from Unknown Sources" in your device settings</li>
            <li>Transfer the APK to your device</li>
            <li>Open the APK on your device to install</li>
        </ol>

        <h2>Configuring the App</h2>
        <ol>
            <li>Open the app after installation</li>
            <li>Enter the server URL: <code>http://your-server-ip:5000</code></li>
            <li>Enter your authentication token</li>
            <li>Test the connection</li>
            <li>Enable monitoring as needed</li>
        </ol>
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
    </header>
    <main>
        <h2>Monitored Systems</h2>
        {% if systems %}
            <div class="systems-grid">
                {% for system in systems %}
                    <div class="system-card">
                        <h3>{{ system.hostname }}</h3>
                        <p>ID: {{ system.id }}</p>
                        <p>Platform: {{ system.platform }}</p>
                        <p>Last Update: {{ system.last_update }}</p>
                        <a href="/system/{{ system.id }}" class="button">View Details</a>
                    </div>
                {% endfor %}
            </div>
        {% else %}
            <p>No systems are currently being monitored.</p>
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
