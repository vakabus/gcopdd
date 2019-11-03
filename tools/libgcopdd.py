import sys
import os
import itertools
from functools import lru_cache

from ntar import NtarFile


####


class UserError(Exception):
	"""
	Signalizes an error in user input.
	"""
	def __init__(self, msg):
		self.msg = msg


####


class Event:
	"""
	An object that represents a compilation event.
	"""
	def get_entry(self, entry_type, deserialize_fn):
		raise NotImplementedError('Abstract method')
	
	def set_entry(self, entry_type, entry_content, serialize_fn):
		raise NotImplementedError('Abstract method')


class AnonymousEvent(Event):
	"""
	An object that represents a compilation event.
	The object is ONLY stored in the memory of the process.
	"""
	def __init__(self, **entries):
		self.entries = entries
	
	def get_entry(self, entry_type, deserialize_fn):
		return self.entries[entry_type]
	
	def set_entry(self, entry_type, entry_content, serialize_fn):
		self.entries[entry_type] = entry_content
	
	def __repr__(self):
		kwargs_str = ', '.join(f'{key}={value!r}' for key, value in self.entries.items())
		return f'AnonymousEvent({kwargs_str})'


class NtarEvent(Event):
	"""
	An object that represents a compilation event.
	The object is backed by a Ntar file on filesystem.
	"""
	def __init__(self, ntar_file):
		self.ntar_file = ntar_file
	
	def get_entry(self, entry_type, deserialize_fn):
		return deserialize_fn(str(self.ntar_file[entry_type], 'utf-8'))
	
	def set_entry(self, entry_type, entry_content, serialize_fn):
		self.ntar_file[entry_type] = bytes(serialize_fn(entry_content), 'utf-8')
	
	def __repr__(self):
		return f'NtarEvent({self.ntar_file!r})'


####


@lru_cache(None)
def load_event(filename, mode='r'):
	return NtarEvent(NtarFile(open(filename, mode + 'b')))


def create_output_event():
	"""
	Creates `Event` object which on adding entries
	writes to stdout (if not a tty), or a newly
	created file (if stdout is a tty).
	"""
	if sys.stdout.isatty():
		for i in itertools.count():
			try:
				filename = f'temp{i}'
				file = open(filename, 'xb')
				print(f'Refusing to print to terminal. Output file is {filename!r}.')
				return NtarEvent(NtarFile(file))
			except FileExistsError:
				pass
	else:
		return NtarEvent(NtarFile(sys.stdout.buffer))


def create_output_text_file(suffix, contents):
	"""
	Writes text to stdout (if not a tty), or a newly
	created file (if stdout is a tty).
	"""
	if sys.stdout.isatty():
		for i in itertools.count():
			try:
				filename = f'temp{i}{suffix}'
				file = open(filename, 'xt')
				print(f'Refusing to print to terminal. Output file is {filename!r}.')
				with file:
					file.write(contents)
				return
			except FileExistsError:
				pass
	else:
		sys.stdout.write(contents)


####


def listdir2(dirname):
	"""Like os.listdir, but returns iterator, not list, and it contains paths, not names."""
	if not dirname.endswith('/'):
		dirname += '/'
	# [dirname + basename for basename in os.listdir(dirname)]
	return map(dirname.__add__, os.listdir(dirname))

def walk2(node):
	"""Returns an iterator of paths to regular files in a directory (recursively)."""
	if os.path.isdir(node):
		# [leaf for child in listdir2(node) for leaf in walk2(child)]
		return itertools.chain(*map(walk2, listdir2(node)))
	else:
		return [node]


def recursive_regular_files_list(roots):
	"""Returns a list of paths to regular files in each directory in `roots` list (recursively)."""
	# [leaf for root in roots for leaf in walk2(root)]
	return list(itertools.chain(*map(walk2, roots)))

def recursive_events_list(roots):
	"""Returns a list of `Event`s corresponding to regular files in each directory in `roots` list (recursively)."""
	# [load_event(leaf) for root in roots for leaf in walk2(root)]
	return list(map(load_event, itertools.chain(*map(walk2, roots))))


def args_or_stdin_to_events_list(args):
	"""Returns a list of `Event`s corresponding to regular files in each directory in `args` or stdin (recursively)."""
	if not args:
		if sys.stdin.isatty() and sys.stderr.isatty():
			print("No arguments, reading file list from stdin:", file=sys.stderr)
		str_is_nonempty = bool
		args = filter(str_is_nonempty, sys.stdin.read().split('\n'))
	return recursive_events_list(args)

def expect_n_events_in_args(n, args, msg):
	"""Checks number of `args`, returns a list of `Event`s corresponding to elements of `args`."""
	if len(args) != n:
		raise UserError(msg)
	events = [...] * n
	for idx, arg in enumerate(args):
		if arg == '-':
			if sys.stdin.isatty():
				raise UserError('Refusing to read event from tty (specify filename in arguments or redirect stdin)')
			events[idx] = NtarEvent(NtarFile(sys.stdin.buffer))
		else:
			try:
				events[idx] = load_event(arg)
			except IsADirectoryError as e:
				raise UserError(f'{e} (one event expected)')
			except IOError as e:
				raise UserError(f'{e}')
	return events
