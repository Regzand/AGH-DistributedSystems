import atexit

from termcolor import colored

from hospital import Producer, Consumer


class Doctor:

    def __init__(self, name):
        self._name = name

        # setup producer
        self._examinations_producer = Producer('localhost', 'examinations', 'topic')

        # setup consumer for examination results
        self._examinations_consumer = Consumer('localhost', 'examinations', 'topic')
        self._examinations_consumer.register_queue(key=f"result.{name}", callback=self._handle_examinations)
        self._examinations_consumer.start_consuming()

        # setup consumer for announcements
        self._announcements_consumer = Consumer('localhost', 'announcements', 'fanout')
        self._announcements_consumer.register_queue(callback=self._handle_announcements)
        self._announcements_consumer.start_consuming()

    def _handle_announcements(self, channel, method, props, body):
        print(colored(body.decode(), "cyan"))
        channel.basic_ack(delivery_tag=method.delivery_tag)

    def _handle_examinations(self, channel, method, props, body):
        result = body.decode().split(" ")
        print(colored(f'Received results of {result[1]} examination for {result[0]}', 'green'))
        channel.basic_ack(delivery_tag=method.delivery_tag)

    def request_examination(self, patient, examination):
        self._examinations_producer.send(f'{patient} {examination} {self._name}', key=f'exam.{examination}')

    def close(self):
        self._examinations_producer.close()
        self._examinations_consumer.close()
        self._announcements_consumer.close()


if __name__ == '__main__':
    doctor = Doctor(input("Enter doctor name: "))
    atexit.register(doctor.close)

    print("Enter requests in format: <patient> <type>")

    while True:
        request = input("> ").split(" ")
        doctor.request_examination(request[0], request[1])
