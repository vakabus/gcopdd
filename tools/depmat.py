#!/usr/bin/env python3

import sys
from getopt import gnu_getopt, GetoptError
from re import compile as Regex
from collections import namedtuple

from libgcopdd import Event, UserError, args_to_events, n_args_to_n_events, open_dump_entry, create_temp
import phasestack

INTEGER_PATTERN = Regex(r'\d+')
DepMat = namedtuple('DepMat', 'size, mapping, data')

####

def load(event):
	"""Loads `.depmat` dump entry of given `event`.
	
	The `event` contains the dump name and event hash,
	but not the `.depmat` suffix. Returns a DepMat object.
	"""
	with open_dump_entry(event + '.depmat') as file:
		mapping = []
		for line in file:
			if line.isspace():
				break
			mapping.append(int(line))
		size = len(mapping)
		data = list(map(int, INTEGER_PATTERN.findall(file.read())))
	assert len(data) == 3*size*size
	return DepMat(size, mapping, data)

def save(depmat, event):
	"""Saves `.depmat` dump entry to given `event`.
	
	The `event` contains the dump name and event hash,
	but not the `.depmat` suffix. `depmat` is a DepMat object.
	"""
	with open_dump_entry(event + '.depmat', 'wt') as file:
		rowsize = 3 * depmat.size
		for value in depmat.mapping or range(depmat.size):
			file.write(str(value) + '\n')
		for idx, value in enumerate(depmat.data):
			if idx % 3:
				file.write(':' + str(value))
			elif idx % rowsize:
				file.write(' ' + str(value))
			else:
				file.write('\n' + str(value))
		file.write('\n')

####

def add_to(matrix, addend):
	assert matrix.mapping is None
	for addend_y, matrix_y in enumerate(addend.mapping):
		matrix_row_offset = 3 * matrix.size * matrix_y
		addend_row_offset = 3 * addend.size * addend_y
		for addend_x, matrix_x in enumerate(addend.mapping):
			matrix_cell_offset = matrix_row_offset + 3*matrix_x
			addend_cell_offset = addend_row_offset + 3*addend_x
			matrix.data[matrix_cell_offset+0] += addend.data[addend_cell_offset+0]
			matrix.data[matrix_cell_offset+1] += addend.data[addend_cell_offset+1]
			matrix.data[matrix_cell_offset+2] += addend.data[addend_cell_offset+2]

def change_mapping(matrix, new_mapping):
	return DepMat(matrix.size, [new_mapping[old] for old in matrix.mapping], matrix.data)

def empty(size):
	return DepMat(size, None, [0] * (size*size*3))

####

def html(event):
	# TODO adapt from `viewers.depmat`
	print(f'<!doctype html><html><head><title>{event.event_id}.depmat</title></head><body>TODO</body></html>')

def csv(event):
	# XXX define format
	print('0,0,0')

def aggregate(events):
	aggregated_ps, mappings = phasestack.aggregate(events)
	aggregated_matrix = empty(aggregated_ps.size)
	for i, event in enumerate(events):
		matrix = event.get('depmat', load)
		add_to(aggregated_matrix, change_mapping(matrix, mappings[i]))
	return aggregated_matrix, aggregated_ps

def diff(from_, to):
	# TODO do something useful (currently just lame matrix subtraction)
	from_depmat = from_.get('depmat', load)
	from_ps = from_.get('phasestack', phasestack.load)
	neg_from_depmat = DepMat(from_depmat.size, from_depmat.mapping, [-value for value in from_depmat.data])
	neg_from = Event(depmat=neg_from_depmat, phasestack=from_ps)
	return aggregate([neg_from, to])

def expand(original_event, new_ps_event):
	original_matrix = original_event.get('depmat', load)
	original_ps = original_event.get('phasestack', phasestack.load)
	new_ps = new_ps_event.get('phasestack', phasestack.load)
	mapping = phasestack.get_mapping(original_ps, new_ps)
	new_matrix = change_mapping(original_matrix, mapping)
	return new_matrix, new_ps

####

def main():
	try:
		[_script_name, function, *args] = sys.argv
	except ValueError:
		raise UserError('Usage: depmat <FUNCTION> [<ARGS> ...]')
	function = function.lstrip('-')
	opts = None
	
	def scan_options(longopts_str):
		nonlocal opts, args
		opts_list, args_list = gnu_getopt(args, '', longopts_str.split())
		opts_dict = dict(opts_list)
		if len(opts_dict) != len(opts_list):
			raise UserError('Duplicate option!')
		opts = opts_dict
		args = args_list
	
	if function in ('help', 'usage'):
		print('Usage:')
		print('  depmat help')
		print('  depmat html [--tty] <EVENT>')
		print('  depmat csv [--tty] <EVENT>')
		print('  depmat aggregate <EVENT LIST ...> [--as <TARGET>]')
		print('  depmat diff <FROM_EVENT> <TO_EVENT> [--as <TARGET>]')
		print('  depmat expand <ORIGINAL> <NEW_PHASESTACK> [--as <TARGET>]')
	
	elif function == 'html':
		scan_options('tty')
		tty = '--tty' in opts
		if not tty and sys.stdout.isatty():
			raise UserError('Refusing to print to terminal. Redirect output or use --tty')
		(event,) = n_args_to_n_events(1, args, 'Usage: depmat html [--tty] <EVENT>')
		html(event)
	
	elif function == 'csv':
		scan_options('tty')
		tty = '--tty' in opts
		if not tty and sys.stdout.isatty():
			raise UserError('Refusing to print to terminal. Redirect output or use --tty')
		(event,) = n_args_to_n_events(1, args, 'Usage: depmat csv [--tty] <EVENT>')
		csv(event)
	
	elif function == 'aggregate':
		scan_options('as=')
		target = create_temp(opts.get('--as'))
		depmat, ps = aggregate(args_to_events(args))
		save(depmat, target)
		phasestack.save(ps, target)
	
	elif function == 'diff':
		scan_options('as=')
		target = create_temp(opts.get('--as'))
		(from_event, to_event) = n_args_to_n_events(2, args, 'Usage: depmat diff <FROM_EVENT> <TO_EVENT> [--as <TARGET>]')
		depmat, ps = diff(from_event, to_event)
		save(depmat, target)
		phasestack.save(ps, target)
	
	elif function == 'expand':
		scan_options('as=')
		target = create_temp(opts.get('--as'))
		original, new_phasestack = n_args_to_n_events(2, args, 'Usage: depmat expand <ORIGINAL> <NEW_PHASESTACK> [--as <TARGET>]')
		depmat, ps = expand(original, new_phasestack)
		save(depmat, target)
		phasestack.save(ps, target)
	
	else:
		raise UserError(f'No such function: {function}')

if __name__ == '__main__':
	try:
		main()
	except (UserError, GetoptError) as e:
		print(e.msg, file=sys.stderr)
		exit(1)
