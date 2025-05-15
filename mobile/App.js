import React, {useState, useEffect} from 'react';
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  Alert,
  Switch,
} from 'react-native';
import {
  Provider as PaperProvider,
  Card,
  Title,
  Paragraph,
  Button,
  TextInput,
  ActivityIndicator,
} from 'react-native-paper';
import AsyncStorage from '@react-native-async-storage/async-storage';
import DeviceInfo from 'react-native-device-info';
import NetInfo from '@react-native-community/netinfo';
import BackgroundActions from 'react-native-background-actions';

import config from './config';
import {collectSystemData} from './utils/systemMonitor';
import {sendDataToServer, checkServerConnection} from './utils/api';
import {formatBytes, formatBatteryLevel} from './utils/formatters';
import logger from './utils/logger';

const App = () => {
  const [serverUrl, setServerUrl] = useState(config.serverUrl);
  const [authToken, setAuthToken] = useState(config.authToken);
  const [isMonitoring, setIsMonitoring] = useState(false);
  const [isBackgroundMonitoring, setIsBackgroundMonitoring] = useState(false);
  const [systemData, setSystemData] = useState(null);
  const [lastUpdateTime, setLastUpdateTime] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [serverStatus, setServerStatus] = useState('unknown');

  // Load saved settings
  useEffect(() => {
    const loadSettings = async () => {
      try {
        const savedServerUrl = await AsyncStorage.getItem('serverUrl');
        const savedAuthToken = await AsyncStorage.getItem('authToken');
        
        if (savedServerUrl) setServerUrl(savedServerUrl);
        if (savedAuthToken) setAuthToken(savedAuthToken);
        
        logger.debug('Settings loaded from storage');
      } catch (error) {
        logger.error('Failed to load settings:', error);
      }
    };
    
    loadSettings();
  }, []);

  // Save settings when changed
  useEffect(() => {
    const saveSettings = async () => {
      try {
        await AsyncStorage.setItem('serverUrl', serverUrl);
        await AsyncStorage.setItem('authToken', authToken);
        logger.debug('Settings saved to storage');
      } catch (error) {
        logger.error('Failed to save settings:', error);
      }
    };
    
    saveSettings();
  }, [serverUrl, authToken]);

  // Check server connection
  const checkConnection = async () => {
    setIsLoading(true);
    try {
      const isConnected = await checkServerConnection(serverUrl, authToken);
      setServerStatus(isConnected ? 'connected' : 'disconnected');
      if (isConnected) {
        Alert.alert('Success', 'Connected to server successfully!');
      } else {
        Alert.alert('Error', 'Could not connect to server. Please check URL and token.');
      }
    } catch (error) {
      setServerStatus('error');
      Alert.alert('Error', `Connection error: ${error.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  // Collect system data
  const updateSystemData = async () => {
    try {
      const data = await collectSystemData();
      setSystemData(data);
      setLastUpdateTime(new Date().toLocaleString());
      
      // Send data to server if monitoring is active
      if (isMonitoring) {
        await sendDataToServer(serverUrl, authToken, data);
      }
      
      return data;
    } catch (error) {
      logger.error('Error collecting system data:', error);
      Alert.alert('Error', `Failed to collect system data: ${error.message}`);
      return null;
    }
  };

  // Background task options
  const backgroundTaskOptions = {
    taskName: 'SystemMonitor',
    taskTitle: 'System Monitoring',
    taskDesc: 'Collecting system information',
    taskIcon: {
      name: 'ic_launcher',
      type: 'mipmap',
    },
    color: '#ff00ff',
    linkingURI: 'mobilesystemmonitor://app',
    parameters: {
      serverUrl,
      authToken,
    },
  };

  // Background task function
  const backgroundTask = async taskData => {
    const {serverUrl, authToken} = taskData;
    
    // Register interval to run every X minutes
    const intervalMs = config.backgroundUpdateInterval;
    
    await new Promise(async resolve => {
      const interval = setInterval(async () => {
        if (!BackgroundActions.isRunning()) {
          clearInterval(interval);
          resolve();
          return;
        }
        
        try {
          // Collect and send data
          const data = await collectSystemData();
          await sendDataToServer(serverUrl, authToken, data);
          logger.debug('Background data sent successfully');
        } catch (error) {
          logger.error('Background task error:', error);
        }
      }, intervalMs);
    });
  };

  // Start background monitoring
  const startBackgroundMonitoring = async () => {
    try {
      await BackgroundActions.start(backgroundTask, backgroundTaskOptions);
      setIsBackgroundMonitoring(true);
      Alert.alert('Success', 'Background monitoring started');
    } catch (error) {
      logger.error('Failed to start background monitoring:', error);
      Alert.alert('Error', `Failed to start background monitoring: ${error.message}`);
    }
  };

  // Stop background monitoring
  const stopBackgroundMonitoring = async () => {
    try {
      await BackgroundActions.stop();
      setIsBackgroundMonitoring(false);
      Alert.alert('Success', 'Background monitoring stopped');
    } catch (error) {
      logger.error('Failed to stop background monitoring:', error);
      Alert.alert('Error', `Failed to stop background monitoring: ${error.message}`);
    }
  };

  // Toggle monitoring
  const toggleMonitoring = async () => {
    if (!isMonitoring) {
      // Start monitoring
      setIsMonitoring(true);
      await updateSystemData();
      
      // Set interval for regular updates
      const intervalId = setInterval(updateSystemData, config.updateInterval);
      AsyncStorage.setItem('monitoringIntervalId', intervalId.toString());
    } else {
      // Stop monitoring
      setIsMonitoring(false);
      const intervalId = await AsyncStorage.getItem('monitoringIntervalId');
      if (intervalId) {
        clearInterval(parseInt(intervalId, 10));
        AsyncStorage.removeItem('monitoringIntervalId');
      }
    }
  };

  return (
    <PaperProvider>
      <SafeAreaView style={styles.container}>
        <StatusBar barStyle="dark-content" />
        <ScrollView contentContainerStyle={styles.scrollView}>
          <Card style={styles.card}>
            <Card.Content>
              <Title>Server Configuration</Title>
              <TextInput
                label="Server URL"
                value={serverUrl}
                onChangeText={setServerUrl}
                style={styles.input}
              />
              <TextInput
                label="Authentication Token"
                value={authToken}
                onChangeText={setAuthToken}
                secureTextEntry
                style={styles.input}
              />
              <Button
                mode="contained"
                onPress={checkConnection}
                loading={isLoading}
                style={styles.button}>
                Test Connection
              </Button>
              <Paragraph style={styles.statusText}>
                Server Status: {serverStatus}
              </Paragraph>
            </Card.Content>
          </Card>

          <Card style={styles.card}>
            <Card.Content>
              <Title>Monitoring Controls</Title>
              <View style={styles.switchContainer}>
                <Text>Enable Monitoring</Text>
                <Switch
                  value={isMonitoring}
                  onValueChange={toggleMonitoring}
                />
              </View>
              <View style={styles.switchContainer}>
                <Text>Background Monitoring</Text>
                <Switch
                  value={isBackgroundMonitoring}
                  onValueChange={val =>
                    val ? startBackgroundMonitoring() : stopBackgroundMonitoring()
                  }
                />
              </View>
              <Button
                mode="contained"
                onPress={updateSystemData}
                style={styles.button}>
                Update Now
              </Button>
              {lastUpdateTime && (
                <Paragraph style={styles.lastUpdate}>
                  Last Update: {lastUpdateTime}
                </Paragraph>
              )}
            </Card.Content>
          </Card>

          {systemData && (
            <Card style={styles.card}>
              <Card.Content>
                <Title>System Information</Title>
                <View style={styles.infoRow}>
                  <Text style={styles.infoLabel}>Device:</Text>
                  <Text style={styles.infoValue}>
                    {systemData.deviceInfo.manufacturer} {systemData.deviceInfo.model}
                  </Text>
                </View>
                <View style={styles.infoRow}>
                  <Text style={styles.infoLabel}>OS:</Text>
                  <Text style={styles.infoValue}>
                    {systemData.deviceInfo.systemName} {systemData.deviceInfo.systemVersion}
                  </Text>
                </View>
                <View style={styles.infoRow}>
                  <Text style={styles.infoLabel}>Battery:</Text>
                  <Text style={styles.infoValue}>
                    {formatBatteryLevel(systemData.batteryInfo.level)}
                    {systemData.batteryInfo.charging ? ' (Charging)' : ''}
                  </Text>
                </View>
                <View style={styles.infoRow}>
                  <Text style={styles.infoLabel}>Storage:</Text>
                  <Text style={styles.infoValue}>
                    {formatBytes(systemData.storageInfo.free)} free of{' '}
                    {formatBytes(systemData.storageInfo.total)}
                  </Text>
                </View>
                <View style={styles.infoRow}>
                  <Text style={styles.infoLabel}>Memory:</Text>
                  <Text style={styles.infoValue}>
                    {formatBytes(systemData.memoryInfo.used)} used of{' '}
                    {formatBytes(systemData.memoryInfo.total)}
                  </Text>
                </View>
                <View style={styles.infoRow}>
                  <Text style={styles.infoLabel}>Network:</Text>
                  <Text style={styles.infoValue}>
                    {systemData.networkInfo.type} 
                    {systemData.networkInfo.isConnected ? ' (Connected)' : ' (Disconnected)'}
                  </Text>
                </View>
              </Card.Content>
            </Card>
          )}
        </ScrollView>
      </SafeAreaView>
    </PaperProvider>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  scrollView: {
    padding: 16,
  },
  card: {
    marginBottom: 16,
    elevation: 2,
  },
  input: {
    marginBottom: 12,
  },
  button: {
    marginTop: 8,
    marginBottom: 8,
  },
  statusText: {
    marginTop: 8,
    fontStyle: 'italic',
  },
  switchContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginVertical: 8,
  },
  lastUpdate: {
    marginTop: 8,
    fontSize: 12,
    color: '#666',
  },
  infoRow: {
    flexDirection: 'row',
    marginBottom: 8,
  },
  infoLabel: {
    fontWeight: 'bold',
    width: 80,
  },
  infoValue: {
    flex: 1,
  },
});

export default App;
