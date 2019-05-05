const logger = require('loggy');
const grpc = require('grpc');

// constants
const CHANGES_INTERVAL = 5000;
const CHANGES_CHANCE = 0.3;
const CHANGES_MAX = 0.2;
const DEFAULT_HOST = '0.0.0.0';

// parse arguments
const argv = require('yargs')
    .option('p', {
        alias: 'port',
        type: 'number',
        describe: 'Port to listen on',
        demandOption: true
    })
    .help()
    .argv;

// create server
const ExchangeRatesServer = require('./exchange-rates-server')
const server = new ExchangeRatesServer();

// bind server
server.bind(DEFAULT_HOST + ':' + argv['port'], grpc.ServerCredentials.createInsecure());
logger.info(`Server has been bound to ${DEFAULT_HOST}:${argv['port']}`);

// start server
server.start();
logger.info('Server started listening');

// start simulating rate changes
setInterval(server.simulateRatesChanges.bind(server), CHANGES_INTERVAL, CHANGES_CHANCE, CHANGES_MAX);
