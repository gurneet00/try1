@echo off
echo Starting System Monitor Server...
echo.
echo This will start the server on port 5000 with a default authentication token.
echo For production use, please change the authentication token.
echo.
echo Press Ctrl+C to stop the server.
echo.

python server.py --port 5000 --auth default_token_change_me
