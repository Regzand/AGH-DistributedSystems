.DEFAULT_GOAL := all

idl:
	mkdir idl

grpc: idl
	python -m grpc_tools.protoc --proto_path=../ --python_out=. --grpc_python_out=. ../idl/exchange-rates.proto

all: grpc

clean:
	rm -rf ./idl
