.DEFAULT_GOAL := all

idl:
	mkdir idl

ice: idl
	slice2py ../idl/banking.ice --output-dir idl

all: ice

clean:
	rm -rf ./idl
