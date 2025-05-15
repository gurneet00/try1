# System Monitoring Application

This application allows you to monitor system information from various devices and view the collected data through a web interface.

## Features

- Collects comprehensive system data (CPU, memory, disk, network, processes)
- Web-based dashboard to view all monitored systems
- Web client for easy monitoring of any system with a browser
- API for desktop and mobile clients to send data
- Data storage and historical tracking
- PM2 support for running as a persistent service

## Setup Instructions

### Server Setup

1. Install Python 3.6+ if not already installed
2. Run the server with an authentication token:

```bash
python server.py --port 5000 --auth YOUR_SECRET_TOKEN
```

This will start the web server on port 5000. You can access the dashboard by opening a web browser and navigating to `http://localhost:5000`.

### Using the Web Client

The easiest way to monitor a system is to use the built-in web client:

1. Open a web browser on the system you want to monitor
2. Navigate to `http://server-address:5000/client`
3. The system information will be collected and displayed
4. Your system will be added to the monitoring dashboard

### Desktop Client Setup

For more comprehensive monitoring of desktop systems:

1. Install Python 3.6+ on the system you want to monitor
2. Run the client with the server URL and the same authentication token:

```bash
python system_monitor.py --server http://server-address:5000 --auth YOUR_SECRET_TOKEN
```

Replace `server-address` with the IP address or hostname of the server.

### Running as a Persistent Service with PM2

To run the system monitoring application permanently on servers:

1. Install Node.js and PM2 if not already installed:
```bash
npm install -g pm2
```

2. Start the server using PM2:
```bash
pm2 start ecosystem.config.js
```

3. Set PM2 to start on system boot:
```bash
pm2 startup
pm2 save
```

## API Endpoints

- `/` - Main dashboard showing all monitored systems
- `/client` - Web client for collecting system information
- `/system/{system_id}` - Detailed view of a specific system
- `/api/system-data` - API endpoint for sending system data (requires authentication)
- `/api/ping` - Simple endpoint to check if the server is up and authentication is working

## Data Storage

The server stores the monitoring data in the `data` directory, organized by system ID. Each system has its own subdirectory containing:

- `latest.json`: The most recent data received from the system
- Timestamped JSON files containing historical data

## Security Considerations

- Always change the default authentication token
- Use HTTPS in production environments
- Restrict access to the server using a firewall
- Consider using a reverse proxy like Nginx in production

## License

This software is provided for educational and legitimate system administration purposes only. Use responsibly and only on systems you own or have permission to monitor.

## Disclaimer

The authors of this software are not responsible for any misuse or damage caused by this software. Use at your own risk.
