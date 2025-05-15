@echo off
echo Starting System Monitor Client...
echo.
echo This will start the client and connect to a local server on port 5000.
echo For monitoring a remote server, please edit this file with the correct server address.
echo.
echo Press Ctrl+C to stop the client.
echo.

python system_monitor.py --server http://localhost:5000 --auth default_token_change_me --interval 30
