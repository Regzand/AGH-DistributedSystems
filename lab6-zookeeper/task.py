import sys
import logging
import subprocess
from operator import itemgetter

from kazoo.client import KazooClient, KazooState
from kazoo.exceptions import NoNodeError
from tree_format import format_tree


# settings
HOSTS = ['localhost:2181', 'localhost:2182', 'localhost:2183']
NODE = '/z'


# =================================================
#  APP
# =================================================
proc = None

def start_app():
	global proc
	if proc is None or proc.poll() is not None:
		proc = subprocess.Popen(sys.argv[1:])

def stop_app():
	global proc
	if proc is not None:
		proc.kill()


# =================================================
#  ZOOKEEPER
# =================================================
# supress kazoo warning
logging.basicConfig()

# setup zookeeper client
zk = KazooClient(hosts=','.join(HOSTS))

# handle connection events
@zk.add_listener
def listener(state):
	if state == KazooState.SUSPENDED:
		print('Lost connection. Trying to reconect...')
	if state == KazooState.CONNECTED:
		print('Successfully connected')

# children changes watcher
def watch_children(children):
	print(f'Node {NODE} has {len(children)} children: {children}')

# watch for node changes
@zk.DataWatch(NODE)
def watch_node(data, stat):
	if stat is None:
		print(f'Node {NODE} deleted')
		stop_app()
	else:
		print(f'Node {NODE} created')
		zk.ChildrenWatch(NODE, watch_children)
		start_app()

# connect
zk.start()
		
# =================================================
#  COMMANDS
# =================================================
def collect_tree(path=NODE):
	return (
		path.split('/')[-1],
		[collect_tree(path + '/' + child) for child in zk.get_children(path)]
	)

def command_tree():
	try:	
		print(format_tree(collect_tree(), itemgetter(0), itemgetter(1)))
	except NoNodeError:
		print(f'Node {NODE} does not exists')

commands = {
	'tree': command_tree,
	'exit': sys.exit
}

print(f"Available commands: {','.join(commands.keys())}")
while True:
	cmd = input().lower()
	
	if cmd in commands:
		commands[cmd]()
	else:
		print(f'Unknown command: {cmd}')

	
