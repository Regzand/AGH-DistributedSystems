# fix broken ice imports
import sys
sys.path.append('./idl')

import Banking


class BankI(Banking.Bank):
    pass
