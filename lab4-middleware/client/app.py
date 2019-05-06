import argparse
import sys

import Ice

# fix broken ice imports
sys.path.append('./idl')
import Banking

from cli import ClientShell


def _parse_arguments():
    parser = argparse.ArgumentParser(description='Bank application client')

    parser.add_argument('host', help='Host of bank server', type=str)

    parser.add_argument('port', help='Port of bank server', type=int)

    return parser.parse_args()


if __name__ == '__main__':

    # parse arguments
    args = _parse_arguments()

    # setup ice
    with Ice.initialize() as communicator:

        # get bank proxy
        bank_proxy = communicator.stringToProxy(f'Bank:default -h {args.host} -p {args.port}')
        bank_proxy = Banking.BankPrx.checkedCast(bank_proxy)
        if not bank_proxy:
            raise RuntimeError("Invalid Bank proxy")

        # start cli
        ClientShell(bank_proxy).cmdloop()
