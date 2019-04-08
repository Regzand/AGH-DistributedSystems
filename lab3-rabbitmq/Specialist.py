import atexit
from random import randint
from time import sleep

from termcolor import colored

from hospital import Producer, Consumer


class Specialist:

    def __init__(self, examination_types):
        # setup results producer
        self._examinations_producer = Producer('localhost', 'examinations', 'topic')

        # setup consumer for examination requests
        self._examinations_consumer = Consumer('localhost', 'examinations', 'topic')
        for t in examination_types:
            self._examinations_consumer.register_queue(name=t, key=f"exam.{t}", callback=self._handle_examinations)
        self._examinations_consumer.start_consuming()

        # setup consumer for announcements
        self._announcements_consumer = Consumer('localhost', 'announcements', 'fanout')
        self._announcements_consumer.register_queue(callback=self._handle_announcements)
        self._announcements_consumer.start_consuming()

    def _handle_announcements(self, channel, method, props, body):
        print(colored("[A] " + body.decode(), "cyan"))
        channel.basic_ack(delivery_tag=method.delivery_tag)

    def _handle_examinations(self, channel, method, props, body):
        request = body.decode().split(" ")
        print(colored(f'Received request for {request[1]} examination for {request[0]} from {request[2]}', 'blue'))

        sleep(randint(1, 3))

        self._examinations_producer.send(f'{request[0]} {request[1]}', key=f'result.{request[2]}')
        print(colored(f'Results of {request[1]} examination for {request[0]} has been sent to {request[2]}', 'green'))
        channel.basic_ack(delivery_tag=method.delivery_tag)

    def close(self):
        self._examinations_producer.close()
        self._examinations_consumer.close()
        self._announcements_consumer.close()


if __name__ == '__main__':
    specialist = Specialist(input("Enter examination types: ").split(" "))
    atexit.register(specialist.close)

