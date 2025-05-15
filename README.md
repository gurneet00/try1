# System Monitoring Tool

This is a legitimate system monitoring tool that allows you to collect system information and view it through a web interface. It's designed for system administrators, IT professionals, or individuals who want to monitor their own systems.

## Features

- **Comprehensive System Monitoring**: Collects data on CPU, memory, disk, network, and running processes
- **Secure Data Transmission**: Uses authentication tokens and can be configured to use HTTPS
- **Web Dashboard**: View system information through an intuitive web interface
- **Multi-System Support**: Monitor multiple systems from a single dashboard
- **Data Persistence**: Stores historical data for trend analysis

## Components

1. **System Monitor Client** (`system_monitor.py`): Runs on the systems you want to monitor
2. **System Monitor Server** (`server.py`): Receives and displays the monitoring data

## Requirements

- Python 3.6 or higher
- Required Python packages (automatically installed if missing):
  - `psutil`
  - `requests`
  - `flask`
  - `flask-httpauth`

## Setup Instructions

### Server Setup

1. Install Python 3.6+ if not already installed
2. Run the server with an authentication token:

```bash
python server.py --port 5000 --auth YOUR_SECRET_TOKEN
```

This will start the web server on port 5000. You can access the dashboard by opening a web browser and navigating to `http://localhost:5000`.

### Client Setup

1. Install Python 3.6+ on the system you want to monitor
2. Run the client with the server URL and the same authentication token:

```bash
python system_monitor.py --server http://server-address:5000 --auth YOUR_SECRET_TOKEN
```

Replace `server-address` with the IP address or hostname of the server.

## Security Considerations

- Use a strong, unique authentication token
- For production use, configure the server with HTTPS
- Be aware of the sensitive nature of system information
- Only monitor systems you own or have permission to monitor
- Store data securely and limit access to the dashboard

## Legitimate Use Cases

- Monitoring your own systems for performance issues
- System administration in a corporate environment (with proper authorization)
- Tracking resource usage across a network of computers
- Identifying performance bottlenecks
- Educational purposes to learn about system monitoring

## Customization

You can customize the monitoring interval by adding the `--interval` parameter to the client:

```bash
python system_monitor.py --server http://server-address:5000 --auth YOUR_SECRET_TOKEN --interval 30
```

This will collect and send data every 30 seconds instead of the default 60 seconds.

## Data Storage

The server stores the monitoring data in the `data` directory, organized by system ID. Each system has its own subdirectory containing:

- `latest.json`: The most recent data received from the system
- Timestamped JSON files containing historical data

## License

This software is provided for educational and legitimate system administration purposes only. Use responsibly and only on systems you own or have permission to monitor.

## Disclaimer

This tool is intended for legitimate system monitoring purposes only. The authors are not responsible for any misuse of this software. Always obtain proper authorization before monitoring any system.
