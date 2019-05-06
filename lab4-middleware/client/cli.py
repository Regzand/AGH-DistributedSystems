import cmd
from argparse import ArgumentParser
from functools import wraps

import Banking
from tabulate import tabulate


_CURRENCY_MAP = {c.name: c for c in Banking.Currency._enumerators.values()}


def _currency(currency: str):
    if not currency:
        return None
    return _CURRENCY_MAP[currency.upper()]


def argument(*args, **kwargs):
    """ Decorator that parses arguments using argparse parser """
    def parse_decorator(func):

        # if parser is present, add argument and leave func as it is
        if hasattr(func, "__parser__"):
            func.__parser__.add_argument(*args, **kwargs)
            return func

        # else create parser and wrap function
        setattr(func, "__parser__", ArgumentParser(prog=func.__name__[3:]))
        func.__parser__.add_argument(*args, **kwargs)

        @wraps(func)
        def parse_wrapper(self, arguments):
            try:
                arguments = func.__parser__.parse_args(arguments.split(" "))
            except SystemExit:
                return

            return func(self, arguments)
        return parse_wrapper

    return parse_decorator


def except_errors(func):
    """ Decorator that wraps function nto try-except block """
    @wraps(func)
    def wrapper(*args, **kwargs):
        try:
            return func(*args, **kwargs)
        except Banking.BaseException as err:
            print('Error:', err.message)
        except Exception as err:
            print('Error:', str(err))
    return wrapper


class ClientShell(cmd.Cmd):
    intro = 'Welcome to bank client shell. Type help or ? to list commands.'

    def __init__(self, bank):
        super().__init__()
        self.bank = bank
        self.account = None
        self.accounts = {}

    @property
    def prompt(self):
        if self.account:
            return f'{self.account.firstName} {self.account.lastName} > '
        else:
            return '> '

    @except_errors
    @argument('income', type=int)
    @argument('pesel')
    @argument('last_name')
    @argument('first_name')
    def do_register(self, args):
        """ Creates account """
        result = self.bank.registerAccount(
            firstName=args.first_name,
            lastName=args.last_name,
            pesel=args.pesel,
            declaredMonthlyIncome=args.income
        )

        result.account = Banking.PremiumAccountPrx.checkedCast(result.account)
        if result is None:
            result.account = Banking.AccountPrx.checkedCast(result.account)

        self.accounts[args.pesel] = result
        self.account = result

        print(f'Created account of type {result.type} with password {result.password}')

    @except_errors
    @argument('password')
    @argument('pesel')
    def do_recover(self, args):
        """ Recover access account """
        result = self.bank.recoverAccount(
            pesel=args.pesel,
            password=args.password
        )

        self.accounts[args.pesel] = result
        self.account = result

        print(f'Recovered account of type {result.type} with password {result.password}')

    @except_errors
    def do_accounts(self, args):
        """ List available accounts """
        table = [(a.pesel, a.firstName, a.lastName, a.type) for a in self.accounts.values()]
        print(tabulate(table, headers=["pesel", "first name", "last name", "type"]))

    @except_errors
    @argument('pesel')
    def do_switch(self, args):
        """ Change active account """
        if args.pesel not in self.accounts:
            return print('There is no logged in client with that pesel')

        self.account = self.accounts[args.pesel]

    @except_errors
    @argument('amount', type=float)
    @argument('currency', type=_currency)
    def do_deposit(self, args):
        """ Deposit money in given currency """
        self.account.account.deposit(args.currency, args.amount, {'password': self.account.password})

    @except_errors
    @argument('amount', type=float)
    @argument('currency', type=_currency)
    def do_withdraw(self, args):
        """ Withdraw money in given currency """
        self.account.account.withdraw(args.currency, args.amount, {'password': self.account.password})

    @except_errors
    def do_balance(self, args):
        """ Display account balance """
        balance = self.account.account.getBalance({'password': self.account.password})
        print(tabulate(balance.items(), headers=["currency", "amount"]))

    @except_errors
    @argument('months_duration', type=int)
    @argument('amount', type=float)
    @argument('currency', type=_currency)
    def do_credit(self, args):
        """ Get credit offer """
        offer = self.account.account.getCreditOffer(args.currency, args.amount, args.months_duration, {'password': self.account.password})
        print(f'Credit will cost {offer.foreignCurrency} {args.currency.name}, thats {offer.baseCurrency} {self.account.baseCurrency.name}')

    def do_exit(self, args):
        """ Exit client shell """
        return True
