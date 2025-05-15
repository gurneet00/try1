module.exports = {
  apps: [{
    name: "system-monitor",
    script: "server.py",
    interpreter: "python",
    args: "--port 5000 --auth default_token_change_me",
    watch: false,
    instances: 1,
    autorestart: true,
    max_memory_restart: "500M",
    env: {
      NODE_ENV: "production",
      PYTHONUNBUFFERED: "true"
    },
    log_date_format: "YYYY-MM-DD HH:mm:ss Z",
    error_file: "logs/system-monitor-error.log",
    out_file: "logs/system-monitor-out.log",
    merge_logs: true
  }]
}
