#!/usr/bin/env python3

__all__ = ['NtarFile', 'NtarException']

import sys
import struct
from zlib import compress, decompress
import os


class NtarException(Exception):
	pass


class NtarFile:
	def __init__(self, file):
		self._file= file
		if file.readable():
			self._entries = dict(entry_iter(file))
		else:
			self._entries = {}
		if not file.writable():
			file.close()
	
	def __getitem__(self, entry_name):
		return decompress(self._entries[entry_name])
	
	def __setitem__(self, entry_name, entry_content):
		entry_content = compress(entry_content)
		append_entry(self._file, entry_name, entry_content)
		self._file.flush()
		self._entries[entry_name] = entry_content
	
	def keys(self):
		return self._entries.keys()
	
	def items(self):
		for entry_name, entry_content in self._entries.items():
			yield entry_name, decompress(entry_content)
	
	def clear(self):
		self._file.truncate(0)
		self._file.flush()
		self._entries.clear()
	
	def get(self, entry_name, default=None):
		entry_content = self._entries.get(entry_name)
		return default if entry_content is None else decompress(entry_content)
	
	def __iter__(self):
		return iter(self._entries)
	
	def close(self):
		self._file.close()
	
	def __enter__(self):
		self._file.__enter__()
		return self
	
	def __exit__(self, exc_type, exc_value, exc_tb):
		self._file.__exit__(exc_type, exc_value, exc_tb)
	
	def __repr__(self):
		return f'NtarFile({self._file!r})'


def read_exactly(file, size):
	result = file.read(size)
	if len(result) != size:
		raise NtarException(f'Malformed ntar file {file.name!r}: expected {size} bytes, EOF after {len(result)} bytes.')
	return result

def read_sized_bytes(file):
	# '>' big endian, 'I' uint32
	size, = struct.unpack('>I', read_exactly(file, 4))
	return read_exactly(file, size)

def read_entry(file):
	entry_name = str(read_sized_bytes(file), 'ascii')
	entry_content = read_sized_bytes(file)
	return (entry_name, entry_content)

def entry_iter(file):
	while file.peek(1):
		yield read_entry(file)


def append_sized_bytes(file, buf):
	file.write(struct.pack('>I', len(buf)))
	file.write(buf)

def append_entry(file, entry_name, entry_content):
	append_sized_bytes(file, bytes(entry_name, 'ascii'))
	append_sized_bytes(file, entry_content)


def main():
	try:
		[_script_name, function, *args] = sys.argv
	except ValueError:
		print('Usage: depmat <FUNCTION> [<ARGS> ...]', file=sys.stderr)
		return 1
	function = function.lstrip('-')
	
	if function in ('help', 'usage'):
		print('Usage:')
		print('  ntar help')
		print('  ntar list <FILE>')
		print('  ntar dump <FILE>')
		print('  ntar hexdump <FILE>')
		print('  ntar xf <FILE> <DIR>')
	
	elif function == 'list':
		if len(args) != 1:
			print('Usage: ntar list <FILE>', file=sys.stderr)
			return 1
		for entry_name, entry_content in NtarFile(open(args[0], 'rb')).items():
			print(f'{entry_name:15} {len(entry_content)}')
	
	elif function == 'dump':
		if len(args) != 1:
			print('Usage: ntar dump <FILE>', file=sys.stderr)
			return 1
		for entry_name, entry_content in NtarFile(open(args[0], 'rb')).items():
			print(f'=== {entry_name} ===')
			sys.stdout.buffer.write(entry_content)
			print()
	
	elif function == 'hexdump':
		if len(args) != 1:
			print('Usage: ntar hexdump <FILE>', file=sys.stderr)
			return 1
		for entry_name, entry_content in NtarFile(open(args[0], 'rb')).items():
			print(f'=== {entry_name} ===')
			for offset in range(0, len(entry_content), 16):
				chunk = entry_content[offset : offset+16]
				hex_codes = ' '.join(f'{byte:02x}' for byte in chunk)
				ascii_chars = ''.join((chr(byte) if byte in range(32, 127) else '.') for byte in chunk)
				print(f'{offset:08x} {hex_codes:47}  >{ascii_chars}<')
			print(f'{len(entry_content):08x}')
	
	elif function == 'xf':
		if len(args) != 2:
			print('Usage: ntar xf <FILE> <DIR>', file=sys.stderr)
			return 1
		dir_name = args[1]
		os.mkdir(dir_name)
		for entry_name, entry_content in NtarFile(open(args[0], 'rb')).items():
			entry_name = entry_name.replace('/', '_')
			if entry_name.startswith('.'):
				entry_name = '_' + entry_name
			with open(os.path.join(dir_name, entry_name), 'wb') as file:
				file.write(entry_content)
	
	elif function == 'fm':
		program = os.path.join(sys.path[0], './ntar-fm')
		os.execv(program, [program] + args)
	
	else:
		print('ntar: No such function:', function, file=sys.stderr)
		return 1

if __name__ == '__main__':
	main()
