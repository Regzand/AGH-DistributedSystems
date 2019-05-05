import argparse
import threading

from argparse_utils import enum_action

from exchange_rates import Currency, ExchangeRates


def _parse_arguments():
    parser = argparse.ArgumentParser(description='Bank application server')

    parser.add_argument('-p', '--port',
                        type=int,
                        required=True,
                        help='Port to listen to'
                        )
    parser.add_argument('-eh', '--exchange-host',
                        required=True,
                        help='Host of exchange rates service'
                        )
    parser.add_argument('-ep', '--exchange-port',
                        type=int,
                        required=True,
                        help='Port of exchange rates service'
                        )
    parser.add_argument('-b', '--base-currency',
                        action=enum_action(Currency),
                        required=True,
                        help='Base currency for this bank'
                        )
    parser.add_argument('-c', '--currencies',
                        action=enum_action(Currency),
                        nargs='*',
                        default=list(Currency),
                        help='Currencies supported by this bank'
                        )

    return parser.parse_args()


if __name__ == '__main__':

    # parse arguments
    args = _parse_arguments()

    # create and start exchange rates
    exchange_rates = ExchangeRates(args.base_currency, args.currencies)
    threading.Thread(target=exchange_rates.subscribe, args=(args.exchange_host, args.exchange_port)).start()

