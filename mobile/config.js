export default {
  // Server configuration
  serverUrl: 'http://your-server-address:5000',
  apiEndpoint: '/api/system-data',
  authToken: 'default_token_change_me',
  
  // Monitoring configuration
  updateInterval: 60000, // milliseconds (1 minute)
  backgroundUpdateInterval: 300000, // milliseconds (5 minutes)
  
  // Storage configuration
  localStorageKey: 'mobile_system_monitor_data',
  maxLocalStorageEntries: 100,
  
  // App configuration
  appName: 'Mobile System Monitor',
  appVersion: '1.0.0',
  
  // Feature flags
  enableBackgroundMonitoring: true,
  enableBatteryMonitoring: true,
  enableNetworkMonitoring: true,
  enableStorageMonitoring: true,
  enableProcessMonitoring: true,
  
  // Debug settings
  debugMode: __DEV__,
  logLevel: __DEV__ ? 'debug' : 'error',
};
