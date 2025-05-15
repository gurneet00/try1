/**
 * Formats bytes to a human-readable string
 * @param {number} bytes Number of bytes
 * @param {number} decimals Number of decimal places
 * @returns {string} Formatted string
 */
export const formatBytes = (bytes, decimals = 2) => {
  if (bytes === 0) return '0 Bytes';
  if (!bytes) return 'Unknown';

  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];

  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
};

/**
 * Formats a battery level as a percentage
 * @param {number} level Battery level (0-1)
 * @returns {string} Formatted percentage
 */
export const formatBatteryLevel = (level) => {
  if (level === undefined || level === null) return 'Unknown';
  
  // Convert from 0-1 to 0-100
  const percentage = Math.round(level * 100);
  return `${percentage}%`;
};

/**
 * Formats a date object or ISO string to a readable date/time
 * @param {Date|string} date Date object or ISO string
 * @returns {string} Formatted date/time
 */
export const formatDateTime = (date) => {
  if (!date) return 'Unknown';
  
  const dateObj = typeof date === 'string' ? new Date(date) : date;
  
  return dateObj.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
};

/**
 * Formats a duration in milliseconds to a readable string
 * @param {number} ms Duration in milliseconds
 * @returns {string} Formatted duration
 */
export const formatDuration = (ms) => {
  if (!ms) return 'Unknown';
  
  const seconds = Math.floor(ms / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);
  
  if (days > 0) {
    return `${days}d ${hours % 24}h`;
  } else if (hours > 0) {
    return `${hours}h ${minutes % 60}m`;
  } else if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s`;
  } else {
    return `${seconds}s`;
  }
};

/**
 * Formats a network type to a more readable string
 * @param {string} type Network type from NetInfo
 * @returns {string} Formatted network type
 */
export const formatNetworkType = (type) => {
  if (!type) return 'Unknown';
  
  const typeMap = {
    'wifi': 'WiFi',
    'cellular': 'Cellular',
    'ethernet': 'Ethernet',
    'bluetooth': 'Bluetooth',
    'wimax': 'WiMAX',
    'vpn': 'VPN',
    'other': 'Other',
    'none': 'None',
    'unknown': 'Unknown',
  };
  
  return typeMap[type] || type;
};

/**
 * Formats a cellular generation to a readable string
 * @param {string} generation Cellular generation from NetInfo
 * @returns {string} Formatted cellular generation
 */
export const formatCellularGeneration = (generation) => {
  if (!generation) return '';
  
  const generationMap = {
    '2g': '2G',
    '3g': '3G',
    '4g': '4G',
    '5g': '5G',
  };
  
  return generationMap[generation] || generation;
};
