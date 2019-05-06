import Ice

# load ice
ICE_DEFINITION_FILE = "../idl/banking.ice"
Ice.loadSlice(ICE_DEFINITION_FILE)
import Banking


class BankI(Banking.Bank):
    pass

