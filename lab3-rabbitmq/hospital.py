from threading import Thread

from pika import BlockingConnection, ConnectionParameters


class Producer:

    def __init__(self, address, exchange_name, exchange_type):
        self._address = address
        self._exchange_name = exchange_name
        self._exchange_type = exchange_type

        # setup connection
        self._connection = BlockingConnection(ConnectionParameters(host=address))
        self._channel = self._connection.channel()

        # setup exchange
        self._channel.exchange_declare(exchange=exchange_name, exchange_type=exchange_type, auto_delete=True)

    def send(self, message, key=''):
        self._channel.basic_publish(body=message, exchange=self._exchange_name, routing_key=key)

    def close(self):
        self._channel.close()


class Consumer:

    def __init__(self, address, exchange_name, exchange_type):
        self._address = address
        self._exchange_name = exchange_name
        self._exchange_type = exchange_type

        # setup connection
        self._connection = BlockingConnection(ConnectionParameters(host=address))
        self._channel = self._connection.channel()

        # setup exchange
        self._channel.exchange_declare(exchange=exchange_name, exchange_type=exchange_type, auto_delete=True)
        self._channel.basic_qos(prefetch_count=1)

    def register_queue(self, name='', key='', callback=None):
        # setup queue
        name = self._channel.queue_declare(queue=name, auto_delete=True).method.queue

        # bind queue to channel, and register callback
        self._channel.queue_bind(queue=name, exchange=self._exchange_name, routing_key=key)
        self._channel.basic_consume(queue=name, on_message_callback=callback)

        return name

    def start_consuming(self):
        Thread(target=self._channel.start_consuming, args=()).start()

    def close(self):
        self._channel.close()
