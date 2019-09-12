import sys
import os
from os import path
import gzip


class Event:
	def __init__(self, event_id=None, **loaded_entries):
		self.event_id = event_id
		self.loaded_entries = loaded_entries
	
	def get(self, entry_type, load):
		result = self.loaded_entries.get(entry_type)
		if result is None:
			self.loaded_entries[entry_type] = result = load(self.event_id)
		return result


class UserError(Exception):
	def __init__(self, msg):
		self.msg = msg


def args_to_events(args):
	def inner():
		for arg in args:
			if path.isdir(arg):
				# this is a dump
				for basename in os.listdir(arg):
					event_id, entry_type, *_ = basename.split('.')
					if entry_type == 'request':
						yield arg + '/' + event_id
			elif path.exists(arg):
				# this is an event list
				with open(arg) as file:
					for line in file:
						line = line.split('#')[0].strip()
						if line != '':
							yield line
			else:
				# this is probably a single event
				yield arg
	return list(map(Event, inner()))


def n_args_to_n_events(n, args, msg):
	if len(args) != n:
		raise UserError(msg)
	events = [...] * n
	for idx, arg in enumerate(args):
		event1 = args_to_events([arg])
		if len(event1) == 0:
			raise UserError(f'{arg!r} is empty!')
		if len(event1) > 1:
			os.xxxx = event1
			raise UserError(f'{arg!r} contains multiple events (one expected)')
		(events[idx],) = event1
	return events


ENCODINGS = {'.gz': gzip.open, '': open}


def open_dump_entry(entry_name, mode='rt'):
	# XXX should the program look for dumps in CWD or in the `dumps` directory (regardless of CWD)?
	# Arguments for `dumps`:
	#  - typical use-case -- user does not have to `cd dumps`
	#  - consistent with other tools -- both `vm` and `dump-browser` always locate the `dumps` directory
	# Arguments for CWD:
	#  - consistent with unix conventions (note that unlike `vm` and `dump-browser`,
	#    `depmat` does take `dumps`-relative paths on commandline)
	#  - allows TAB-completion
	#
	# Until this is resolved, we only allow the case where both alternatives converge:
	assert os.getcwd().endswith('/dumps'), 'Please cd into dumps directory'
	
	# Look for the file using known filename extensions.
	for ext in ENCODINGS:
		filename = entry_name + ext
		if path.exists(filename):
			# It exists! Open it and return it.
			return ENCODINGS[ext](filename, mode)
	# No appropriate file exists. We use the default (uncompressed, without extension).
	# If we open for writing, this will be ok. If we open for reading, this will throw
	# FileNotFoundError, which is appropriate.
	return open(entry_name, mode)


def create_temp(event_id=None, info=sys.argv, generated_name_callback=print):
	if event_id is None:
		i = 0
		while True:
			try:
				event_id = f'temp{i}'
				with open(event_id + '.info', 'x') as file:
					print(info, file=file)
				generated_name_callback(event_id)
				break
			except FileExistsError:
				i += 1
	else:
		with open(event_id + '.info', 'a') as file:
			print(info, file=file)
	return event_id
