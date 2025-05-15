import DeviceInfo from 'react-native-device-info';
import NetInfo from '@react-native-community/netinfo';
import RNFS from 'react-native-fs';
import config from '../config';
import logger from './logger';

/**
 * Collects all system data from the device
 * @returns {Promise<Object>} System data object
 */
export const collectSystemData = async () => {
  try {
    // Collect data in parallel for better performance
    const [
      deviceInfo,
      batteryInfo,
      storageInfo,
      memoryInfo,
      networkInfo,
    ] = await Promise.all([
      collectDeviceInfo(),
      collectBatteryInfo(),
      collectStorageInfo(),
      collectMemoryInfo(),
      collectNetworkInfo(),
    ]);

    // Generate timestamp and unique ID
    const timestamp = new Date().toISOString();
    const deviceId = await DeviceInfo.getUniqueId();

    // Combine all data
    const systemData = {
      timestamp,
      deviceId,
      deviceInfo,
      batteryInfo,
      storageInfo,
      memoryInfo,
      networkInfo,
    };

    logger.debug('System data collected successfully');
    return systemData;
  } catch (error) {
    logger.error('Error collecting system data:', error);
    throw error;
  }
};

/**
 * Collects basic device information
 * @returns {Promise<Object>} Device information
 */
const collectDeviceInfo = async () => {
  try {
    return {
      manufacturer: await DeviceInfo.getManufacturer(),
      model: await DeviceInfo.getModel(),
      systemName: DeviceInfo.getSystemName(),
      systemVersion: DeviceInfo.getSystemVersion(),
      appVersion: DeviceInfo.getVersion(),
      buildNumber: DeviceInfo.getBuildNumber(),
      bundleId: DeviceInfo.getBundleId(),
      deviceName: await DeviceInfo.getDeviceName(),
      userAgent: await DeviceInfo.getUserAgent(),
      deviceType: DeviceInfo.getDeviceType(),
      isTablet: DeviceInfo.isTablet(),
      isEmulator: await DeviceInfo.isEmulator(),
    };
  } catch (error) {
    logger.error('Error collecting device info:', error);
    return {
      error: error.message,
    };
  }
};

/**
 * Collects battery information
 * @returns {Promise<Object>} Battery information
 */
const collectBatteryInfo = async () => {
  if (!config.enableBatteryMonitoring) {
    return { disabled: true };
  }

  try {
    const batteryLevel = await DeviceInfo.getBatteryLevel();
    const isCharging = await DeviceInfo.isBatteryCharging();
    const powerState = await DeviceInfo.getPowerState();

    return {
      level: batteryLevel,
      charging: isCharging,
      powerState: powerState,
    };
  } catch (error) {
    logger.error('Error collecting battery info:', error);
    return {
      error: error.message,
    };
  }
};

/**
 * Collects storage information
 * @returns {Promise<Object>} Storage information
 */
const collectStorageInfo = async () => {
  if (!config.enableStorageMonitoring) {
    return { disabled: true };
  }

  try {
    const totalStorage = await DeviceInfo.getTotalDiskCapacity();
    const freeStorage = await DeviceInfo.getFreeDiskStorage();
    
    return {
      total: totalStorage,
      free: freeStorage,
      used: totalStorage - freeStorage,
      usedPercentage: ((totalStorage - freeStorage) / totalStorage) * 100,
    };
  } catch (error) {
    logger.error('Error collecting storage info:', error);
    return {
      error: error.message,
    };
  }
};

/**
 * Collects memory information
 * @returns {Promise<Object>} Memory information
 */
const collectMemoryInfo = async () => {
  try {
    const totalMemory = await DeviceInfo.getTotalMemory();
    const usedMemory = await DeviceInfo.getUsedMemory();
    
    return {
      total: totalMemory,
      used: usedMemory,
      free: totalMemory - usedMemory,
      usedPercentage: (usedMemory / totalMemory) * 100,
    };
  } catch (error) {
    logger.error('Error collecting memory info:', error);
    return {
      error: error.message,
    };
  }
};

/**
 * Collects network information
 * @returns {Promise<Object>} Network information
 */
const collectNetworkInfo = async () => {
  if (!config.enableNetworkMonitoring) {
    return { disabled: true };
  }

  try {
    const netInfo = await NetInfo.fetch();
    
    return {
      type: netInfo.type,
      isConnected: netInfo.isConnected,
      isWifi: netInfo.type === 'wifi',
      isCellular: netInfo.type === 'cellular',
      details: netInfo.details,
    };
  } catch (error) {
    logger.error('Error collecting network info:', error);
    return {
      error: error.message,
    };
  }
};

/**
 * Saves system data to local storage
 * @param {Object} data System data to save
 * @returns {Promise<void>}
 */
export const saveDataLocally = async (data) => {
  try {
    // Create directory if it doesn't exist
    const dirPath = `${RNFS.DocumentDirectoryPath}/system_data`;
    const exists = await RNFS.exists(dirPath);
    
    if (!exists) {
      await RNFS.mkdir(dirPath);
    }
    
    // Save data to file
    const timestamp = new Date().getTime();
    const filePath = `${dirPath}/data_${timestamp}.json`;
    await RNFS.writeFile(filePath, JSON.stringify(data), 'utf8');
    
    logger.debug(`Data saved locally to ${filePath}`);
    
    // Clean up old files if there are too many
    const files = await RNFS.readDir(dirPath);
    if (files.length > config.maxLocalStorageEntries) {
      // Sort by creation time (oldest first)
      files.sort((a, b) => a.mtime.getTime() - b.mtime.getTime());
      
      // Delete oldest files
      const filesToDelete = files.slice(0, files.length - config.maxLocalStorageEntries);
      for (const file of filesToDelete) {
        await RNFS.unlink(file.path);
      }
      
      logger.debug(`Cleaned up ${filesToDelete.length} old data files`);
    }
  } catch (error) {
    logger.error('Error saving data locally:', error);
    throw error;
  }
};
