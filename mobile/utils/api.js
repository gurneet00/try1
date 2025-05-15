import axios from 'axios';
import NetInfo from '@react-native-community/netinfo';
import config from '../config';
import logger from './logger';
import { saveDataLocally } from './systemMonitor';

/**
 * Creates an API client with the given base URL and auth token
 * @param {string} baseUrl Server base URL
 * @param {string} authToken Authentication token
 * @returns {Object} Axios instance
 */
const createApiClient = (baseUrl, authToken) => {
  const client = axios.create({
    baseURL: baseUrl,
    timeout: 10000,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${authToken}`,
    },
  });

  // Add request interceptor for logging
  client.interceptors.request.use(
    (config) => {
      logger.debug(`API Request: ${config.method.toUpperCase()} ${config.url}`);
      return config;
    },
    (error) => {
      logger.error('API Request Error:', error);
      return Promise.reject(error);
    }
  );

  // Add response interceptor for logging
  client.interceptors.response.use(
    (response) => {
      logger.debug(`API Response: ${response.status} ${response.statusText}`);
      return response;
    },
    (error) => {
      if (error.response) {
        logger.error(`API Error: ${error.response.status} ${error.response.statusText}`);
      } else if (error.request) {
        logger.error('API Error: No response received');
      } else {
        logger.error('API Error:', error.message);
      }
      return Promise.reject(error);
    }
  );

  return client;
};

/**
 * Checks if the server is reachable and the authentication token is valid
 * @param {string} serverUrl Server URL
 * @param {string} authToken Authentication token
 * @returns {Promise<boolean>} True if connection is successful
 */
export const checkServerConnection = async (serverUrl, authToken) => {
  try {
    // First check if we have internet connection
    const netInfo = await NetInfo.fetch();
    if (!netInfo.isConnected) {
      logger.warn('No internet connection');
      return false;
    }

    // Create API client
    const client = createApiClient(serverUrl, authToken);
    
    // Try to ping the server
    const response = await client.get('/api/ping');
    
    return response.status === 200;
  } catch (error) {
    logger.error('Server connection check failed:', error);
    return false;
  }
};

/**
 * Sends system data to the server
 * @param {string} serverUrl Server URL
 * @param {string} authToken Authentication token
 * @param {Object} data System data to send
 * @returns {Promise<Object>} Server response
 */
export const sendDataToServer = async (serverUrl, authToken, data) => {
  try {
    // First check if we have internet connection
    const netInfo = await NetInfo.fetch();
    if (!netInfo.isConnected) {
      logger.warn('No internet connection, saving data locally');
      await saveDataLocally(data);
      return { status: 'offline', message: 'Data saved locally' };
    }

    // Create API client
    const client = createApiClient(serverUrl, authToken);
    
    // Send data to server
    const response = await client.post(config.apiEndpoint, data);
    
    logger.debug('Data sent to server successfully');
    return response.data;
  } catch (error) {
    logger.error('Failed to send data to server:', error);
    
    // Save data locally if sending fails
    await saveDataLocally(data);
    
    throw error;
  }
};

/**
 * Sends locally stored data to the server
 * @param {string} serverUrl Server URL
 * @param {string} authToken Authentication token
 * @returns {Promise<Object>} Result with count of sent and failed items
 */
export const sendLocalDataToServer = async (serverUrl, authToken) => {
  try {
    // Check if we have internet connection
    const netInfo = await NetInfo.fetch();
    if (!netInfo.isConnected) {
      return { status: 'offline', sent: 0, failed: 0 };
    }

    // Create API client
    const client = createApiClient(serverUrl, authToken);
    
    // Get list of locally stored data files
    const dirPath = `${RNFS.DocumentDirectoryPath}/system_data`;
    const exists = await RNFS.exists(dirPath);
    
    if (!exists) {
      return { status: 'success', sent: 0, failed: 0 };
    }
    
    const files = await RNFS.readDir(dirPath);
    if (files.length === 0) {
      return { status: 'success', sent: 0, failed: 0 };
    }
    
    // Send each file to the server
    let sent = 0;
    let failed = 0;
    
    for (const file of files) {
      try {
        // Read file content
        const content = await RNFS.readFile(file.path, 'utf8');
        const data = JSON.parse(content);
        
        // Send to server
        await client.post(config.apiEndpoint, data);
        
        // Delete file after successful send
        await RNFS.unlink(file.path);
        
        sent++;
      } catch (error) {
        logger.error(`Failed to send local data file ${file.name}:`, error);
        failed++;
      }
    }
    
    logger.debug(`Local data sync: ${sent} sent, ${failed} failed`);
    return { status: 'success', sent, failed };
  } catch (error) {
    logger.error('Failed to sync local data:', error);
    throw error;
  }
};
