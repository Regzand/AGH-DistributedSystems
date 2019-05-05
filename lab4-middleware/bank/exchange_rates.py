import enum
import time
from typing import List

import grpc
from loguru import logger

import exchange_rates_pb2 as proto
from exchange_rates_pb2_grpc import ExchangeRatesServiceStub


class Currency(enum.IntEnum):
    """ Enum class representing currency constants from proto. """
    EUR = proto.EUR
    USD = proto.USD
    GBP = proto.GBP
    PLN = proto.PLN


class ExchangeRates:
    """ Auto-updated exchange rates table. """

    def __init__(self, base_currency: Currency, requested_currencies: List[Currency]):
        """ Creates exchange rates table """
        self._base_currency = base_currency
        self._requested_currencies = requested_currencies
        self._rates = dict()

    def __contains__(self, key: Currency):
        """ Checks if rate for given currency is available. """
        return key in self._rates

    def __getitem__(self, key: Currency):
        """ Returns rate for given value, or throws KeyError if rate is not available. """
        if key not in self:
            raise KeyError(f'Exchange rate for currency {key.name} is not available')
        return self._rates[key]

    def _subscribe(self, host: str, port: int):
        with grpc.insecure_channel(f'{host}:{port}') as channel:
            stub = ExchangeRatesServiceStub(channel)

            # setup request
            req = proto.ExchangeRatesSubscription()
            req.baseCurrency = int(self._base_currency)
            req.requestedCurrencies[:] = [int(c) for c in self._requested_currencies]

            # get subscription
            subscription = stub.subscribe(req)

            logger.debug('Subscribed to exchange rates service at {}:{}', host, port)

            # start fetching updates
            for update in subscription:
                for rate in update.rates:
                    currency = Currency(rate.currency)
                    self._rates[currency] = rate.value
                    logger.debug('Exchange rate updated: {} {}', currency.name, rate.value)

    def subscribe(self, host: str, port: int, retry_time: float = 5.):
        """
        Establishes connection with exchange rates server and starts receiving updates.
        Should be started as separate thread.
        """
        while True:
            try:
                self._subscribe(host, port)
            except grpc._channel._Rendezvous:
                logger.debug('Lost connection to exchange rates server')
                logger.debug('Reconnecting in {} seconds...', retry_time)
                time.sleep(retry_time)
