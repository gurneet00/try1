import config from '../config';

// Log levels
const LOG_LEVELS = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3,
};

// Current log level from config
const currentLevel = LOG_LEVELS[config.logLevel] || LOG_LEVELS.info;

// Timestamp formatter
const getTimestamp = () => {
  return new Date().toISOString();
};

// Logger implementation
const logger = {
  /**
   * Log a debug message
   * @param {string} message The message to log
   * @param {...any} args Additional arguments to log
   */
  debug: (message, ...args) => {
    if (currentLevel <= LOG_LEVELS.debug) {
      console.debug(`[${getTimestamp()}] [DEBUG] ${message}`, ...args);
    }
  },

  /**
   * Log an info message
   * @param {string} message The message to log
   * @param {...any} args Additional arguments to log
   */
  info: (message, ...args) => {
    if (currentLevel <= LOG_LEVELS.info) {
      console.info(`[${getTimestamp()}] [INFO] ${message}`, ...args);
    }
  },

  /**
   * Log a warning message
   * @param {string} message The message to log
   * @param {...any} args Additional arguments to log
   */
  warn: (message, ...args) => {
    if (currentLevel <= LOG_LEVELS.warn) {
      console.warn(`[${getTimestamp()}] [WARN] ${message}`, ...args);
    }
  },

  /**
   * Log an error message
   * @param {string} message The message to log
   * @param {...any} args Additional arguments to log
   */
  error: (message, ...args) => {
    if (currentLevel <= LOG_LEVELS.error) {
      console.error(`[${getTimestamp()}] [ERROR] ${message}`, ...args);
    }
  },
};

export default logger;
