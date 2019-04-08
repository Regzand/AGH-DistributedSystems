import atexit

from termcolor import colored

from hospital import Producer, Consumer


class Administrator:

    def __init__(self):
        # setup producer
        self._announcements_producer = Producer('localhost', 'announcements', 'fanout')

        # setup consumer
        self._examinations_consumer = Consumer('localhost', 'examinations', 'topic')
        self._examinations_consumer.register_queue(key="#", callback=self._handle_examinations)
        self._examinations_consumer.start_consuming()

    def _handle_examinations(self, channel, method, props, body):
        print(colored(f'[{method.routing_key}] {body.decode()}', "grey"))
        channel.basic_ack(delivery_tag=method.delivery_tag)

    def send_announcement(self, message):
        self._announcements_producer.send(message)

    def close(self):
        self._announcements_producer.close()
        self._examinations_consumer.close()


if __name__ == '__main__':
    admin = Administrator()
    atexit.register(admin.close)

    while True:
        admin.send_announcement(input("> "))
