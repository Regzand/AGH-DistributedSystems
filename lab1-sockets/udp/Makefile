CC = gcc
CFLAGS = -Wall -std=gnu11 -DLOG_USE_COLOR
LDLIBS = -lpthread -lm
LIBS = ../include/log.c ../include/argtable3.c

client: client.c ${LIBS}

all: client

clean:
	rm -f *.o client