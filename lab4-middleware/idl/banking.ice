module Banking
{

    // Currencies supported by api
    enum Currency {
        EUR = 0,
        USD = 1,
        GBP = 2,
        PLN = 3
    };

    // Account types provided by bank
    enum AccountType {
        STANDARD,
        PREMIUM
    };

    // Type for storing balance in many currencies
    dictionary<Currency, double> Balance;

    // Struct returned after request for credit offer
    struct CreditOffer {
        double baseCurrency;
        double foreignCurrency;
    }

    exception BaseException {
        string message;
    };

    exception AccountExistsException extends BaseException {};
    exception InvalidPeselException extends BaseException {};
    exception AuthenticationException extends BaseException {};
    exception PermissionException extends BaseException {};
    exception UnsupportedCurrencyException extends BaseException {};
    exception NotEnoughMoneyException extends BaseException {};

    // Interface for accessing account functionality
    interface Account {
        Balance getBalance() throws AuthenticationException;
        void deposit(Currency currency, double amount) throws AuthenticationException, UnsupportedCurrencyException;
        void withdraw(Currency currency, double amount) throws AuthenticationException, UnsupportedCurrencyException, NotEnoughMoneyException;
    };

    interface PremiumAccount extends Account {
        CreditOffer getCreditOffer(Currency currency, double amount, int monthsDuration) throws AuthenticationException, UnsupportedCurrencyException, PermissionException;
    };

    // Struct returned after registration, contains info needed for future connections
    struct RegistrationResult {
	string firstName;
	string lastName;
	string pesel;
        string password;
	Currency baseCurrency;
        AccountType type;
        Account* account;
    };

    // Main interface
    interface Bank {
        RegistrationResult registerAccount(string firstName, string lastName, string pesel, double declaredMonthlyIncome) throws AccountExistsException, InvalidPeselException;
        RegistrationResult recoverAccount(string pesel, string password) throws AuthenticationException;
    };


};
