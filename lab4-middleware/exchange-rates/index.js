const logger = require('loggy');
const grpc = require('grpc');

// constants
const CHANGES_INTERVAL = 5000;
const CHANGES_CHANCE = 0.3;
const CHANGES_MAX = 0.2;
const DEFAULT_HOST = '0.0.0.0';
const DEFAULT_PORT = '50001';

// create server
const ExchangeRatesServer = require('./exchange-rates-server')
const server = new ExchangeRatesServer();

// bind server
server.bind(DEFAULT_HOST + ':' + DEFAULT_PORT, grpc.ServerCredentials.createInsecure());
logger.info(`Server has been bound to ${DEFAULT_HOST}:${DEFAULT_PORT}`);

// start server
server.start();
logger.info('Server started listening');

// start simulating rate changes
setInterval(server.simulateRatesChanges.bind(server), CHANGES_INTERVAL, CHANGES_CHANCE, CHANGES_MAX);
