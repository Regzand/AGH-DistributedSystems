import inspect
import random
import string
import sys
from typing import List

import Ice
from loguru import logger

from exchange_rates import Currency, ExchangeRates

# fix broken ice imports
sys.path.append('./idl')
import Banking


def _ice_currency(currency: Currency):
    """ Parses grpc currency to ice currency """
    return Banking.Currency._enumerators[int(currency)]


def _grpc_currency(currency) -> Currency:
    """ Parses ice currency to grpc currency """
    return Currency(currency.value)


def _generate_password(length: int = 10):
    """ Generates random password of given length"""
    return ''.join(random.choices(string.ascii_lowercase, k=length))


def authenticate(func):
    """ Decorator that performs pesel/password check """

    # create decorator
    def authenticate_decorator(*args, **kwargs):

        # retrieve arguments using inspect to match their names
        arguments = inspect.signature(func).bind(*args, **kwargs).arguments
        account = arguments['self']
        context = arguments['current'].ctx

        # check if password was provided
        if 'password' not in context:
            raise Banking.AuthenticationException('No password provided')

        # check password
        if context['password'] != account.password:
            raise Banking.AuthenticationException('Incorrect pesel/password combination')

        # if everything is fine execute normally
        return func(*args, **kwargs)

    # return decorator
    return authenticate_decorator


class AccountI(Banking.Account):
    """ Implementation of standard account """

    def __init__(self, bank, firstName: str, lastName: str, pesel: str, declared_income: float, password: str):
        """ Creates account """
        self._bank = bank
        self.firstName = firstName
        self.lastName = lastName
        self.pesel = pesel
        self.declared_income = declared_income
        self.password = password
        self.type = Banking.AccountType.STANDARD
        self.balance = {c: 0. for c in (self._bank.currencies + [self._bank.base_currency])}

    @authenticate
    def getBalance(self, current=None):
        logger.info("Client {} requested balance", self.pesel)
        return {_ice_currency(c): value for c, value in self.balance.items()}

    @authenticate
    def deposit(self, currency, amount: float, current=None):
        currency = _grpc_currency(currency)

        # check if currency is supported
        if currency not in self.balance:
            raise Banking.UnsupportedCurrencyException(f'Currency {currency.name} is not supported by this bank')

        # deposit to account
        self.balance[currency] += amount

        logger.info("Deposited {} {} to account of client {}", amount, currency.name, self.pesel)

    @authenticate
    def withdraw(self, currency, amount: float, current=None):
        currency = _grpc_currency(currency)

        # check if currency is supported
        if currency not in self.balance:
            raise Banking.UnsupportedCurrencyException(f'Currency {currency.name} is not supported by this bank')

        # check if has enough money
        if self.balance[currency] < amount:
            raise Banking.NotEnoughMoneyException('This account has not enough money to withdraw')

        # withdraw from acccount
        self.balance[currency] -= amount

        logger.info("Withdraw {} {} from account of client {}", amount, currency.name, self.pesel)

    def __repr__(self):
        return f"<Account(firstName='{self.firstName}'', lastName='{self.lastName}'', pesel='{self.pesel}'')>"


class PremiumAccountI(AccountI, Banking.PremiumAccount):
    """ Extends base account with premium functionality """

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.type = Banking.AccountType.PREMIUM

    @authenticate
    def getCreditOffer(self, currency, amount: float, monthsDuration: int, current=None):
        currency = _grpc_currency(currency)

        # check if currency is supported
        if currency not in self.balance:
            raise Banking.UnsupportedCurrencyException(f'Currency {currency.name} is not supported by this bank')

        # calculate total cost
        cost = amount + (amount * monthsDuration * self._bank.interest)

        logger.info("Client {} requested credit offer", self.pesel)

        # return response
        return Banking.CreditOffer(
            baseCurrency=cost * self._bank.rates[currency],
            foreignCurrency=cost
        )

    def __repr__(self):
        return f"<PremiumAccount(firstName='{self.firstName}'', lastName='{self.lastName}'', pesel='{self.pesel}'')>"


class BankI(Banking.Bank):

    def __init__(self, adapter, rates: ExchangeRates, base_currency: Currency, currencies: List[Currency], premium_threshold: int):
        self.adapter = adapter
        self.rates = rates
        self.base_currency = base_currency
        self.currencies = currencies
        self.premium_threshold = premium_threshold
        self.interest = 0.05
        self.accounts = dict()

    def registerAccount(self, firstName: str, lastName: str, pesel: str, declaredMonthlyIncome: float, current=None):

        # check pesel
        if pesel in self.accounts.keys():
            raise Banking.AccountExistsException('Account with given pesel already exists')

        # generate password
        password = _generate_password()

        # get type
        if declaredMonthlyIncome < self.premium_threshold:
            account_type = Banking.AccountType.STANDARD
        else:
            account_type = Banking.AccountType.PREMIUM

        # create account
        if account_type == Banking.AccountType.STANDARD:
            account = AccountI(self, firstName, lastName, pesel, declaredMonthlyIncome, password)
        else:
            account = PremiumAccountI(self, firstName, lastName, pesel, declaredMonthlyIncome, password)

        # register account
        self.adapter.add(account, Ice.stringToIdentity(pesel))
        self.accounts[pesel] = account

        # logging
        logger.info('Registered new account: {}', repr(account))

        # get proxy
        account_proxy = current.adapter.createProxy(Ice.stringToIdentity(account.pesel))
        if account.type == Banking.AccountType.PREMIUM:
            account_proxy = Banking.PremiumAccountPrx.uncheckedCast(account_proxy)
        else:
            account_proxy = Banking.AccountPrx.uncheckedCast(account_proxy)

        # response
        return Banking.RegistrationResult(firstName, lastName, pesel, password, _ice_currency(self.base_currency), account_type, account_proxy)

    def recoverAccount(self, pesel: str, password: str, current=None):

        # get account
        if pesel not in self.accounts:
            raise Banking.AuthenticationException('Wrong pesel')

        print(password, self.accounts[pesel].password)

        # get account
        if self.accounts[pesel].password != password:
            raise Banking.AuthenticationException('Wrong password')

        account = self.accounts[pesel]

        # get proxy
        account_proxy = current.adapter.createProxy(Ice.stringToIdentity(account.pesel))
        if account.type == Banking.AccountType.PREMIUM:
            account_proxy = Banking.PremiumAccountPrx.uncheckedCast(account_proxy)
        else:
            account_proxy = Banking.PremiumAccountPrx.uncheckedCast(account_proxy)

        return Banking.RegistrationResult(account.firstName, account.lastName, pesel, password, _ice_currency(self.base_currency), account.type, account_proxy)
