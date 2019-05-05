// imports
const path = require('path');
const logger = require('loggy');
const async = require('async');
const grpc = require('grpc');
const protoLoader = require('@grpc/proto-loader');

// constants
const PROTO_PATH = path.resolve(__dirname, '../idl/exchange-rates.proto');
const BASE_CURRENCY = 'EUR';
const DEFAULT_RATES = {
    EUR: 1.0000,
    USD: 1.1155,
    GBP: 0.85785,
    PLN: 4.2853
};

// gRPC setup
const proto = grpc.loadPackageDefinition(protoLoader.loadSync(
    PROTO_PATH,
    {keepCase: true,
        longs: String,
        enums: String,
        defaults: true,
        oneofs: true
    }
));

class ExchangeRatesServer extends grpc.Server {

    constructor(){
        super();

        // register services
        this.addService(proto.ExchangeRatesService.service, {
            'subscribe': this._subscribe.bind(this)
        });

        // create subscription list
        this.subscriptions = [];

        // create rates map
        this.rates = Object.assign({}, DEFAULT_RATES)
    }

    /**
     * Subscribe endpoint, accepts ExchangeRatesSubscription and responds with stream of ExchangeRatesUpdate
     * @param {Writable} call Writable stream for responses with an additional
     *     request property for the request value.
     */
    _subscribe(call) {

        // logging
        logger.info('New subscription requested');

        // add to subscriptions list
        this.subscriptions.push(call);

        // send initial rates
        this._send_update(null, call, null);
    }

    /**
     * Sends an update about given currencies to given subscription
     * @param currencies
     * @param subscription
     * @param callback
     * @private
     */
    _send_update(currencies, subscription, callback) {
        const self = this;
        // create response
        let response = {
            rates: []
        };

        // for each requested currency
        subscription.request['requestedCurrencies'].forEach(function(key) {
            // check if this it is expected to be send
            if (currencies != null && !currencies.includes(key)) return;

            // add currency to response
            response.rates.push({
                currency: key,
                value: self.rates[BASE_CURRENCY] / self.rates[subscription.request['baseCurrency']] * self.rates[key]
            });
        });

        // if there is anything to send send it
        if (response.rates.length > 0)
            subscription.write(response);

        if(callback)
            callback();
    }

    /**
     * Simulates exchange rate changes and sends updates
     * @param probability
     * @param maxChange
     */
    simulateRatesChanges(probability, maxChange) {
        let changed = [];

        // simulate changes
        for (let key in this.rates) {
            if (!this.rates.hasOwnProperty(key)) continue;
            if (key === BASE_CURRENCY) continue;
            if (Math.random() > probability) continue;

            // calculate change
            let change = maxChange * (Math.random() * 2 - 1);
            this.rates[key] += change;

            // logging
            logger.log(`Rate changed: ${key} ${this.rates[key]} (${change < 0 ? "" : "+"}${change})`);

            // save change
            changed.push(key);
        }

        // send updates
        async.forEach(this.subscriptions, this._send_update.bind(this, changed),function (err) {});
    }

}

// exports
module.exports = ExchangeRatesServer;
