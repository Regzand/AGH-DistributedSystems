import socket
import struct
import datetime

LOGGING_SERVICE_PORT = 9999
LOGGING_SERVICE_ADDRESS = '224.3.2.1'

# setup socket
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
sock.bind((LOGGING_SERVICE_ADDRESS, LOGGING_SERVICE_PORT))
mreq = struct.pack("4sl", socket.inet_aton(LOGGING_SERVICE_ADDRESS), socket.INADDR_ANY)

sock.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, mreq)

while True:
    data = sock.recv(10240).decode("utf-8").split(" ")
    print(datetime.datetime.now(), "Client:", data[0], "Type:", data[1], "UUID:", data[2])