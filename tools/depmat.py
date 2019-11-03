#!/usr/bin/env python3

import sys
from getopt import gnu_getopt, GetoptError
from re import compile as Regex
from collections import namedtuple

from libgcopdd import UserError, AnonymousEvent, args_or_stdin_to_events_list, expect_n_events_in_args, create_output_event, create_output_text_file
import phasestack

INTEGER_PATTERN = Regex(r'\d+')
DepMat = namedtuple('DepMat', 'size, mapping, data')

####

def deserialize(string):
	mapping_s, data_s = string.split('\n\n', 1)
	mapping = list(map(int, mapping_s.split()))
	size = len(mapping)
	data = list(map(int, INTEGER_PATTERN.findall(data_s)))
	assert len(data) == 3*size*size
	return DepMat(size, mapping, data)

def serialize(depmat):
	size = depmat.size # number of rows / columns
	r = 6 * size # row size in the context of `data_s_list` (see below)
	# Allocate array with (6*size*size == 2*len(depmat.data)) elements.
	# Odd indices contain values, even indices contain delimiters.
	# Initialize with ':', which is the most common delimiter.
	# Replace some of the delimiters (and most importantly the values).
	data_s_list = [':'] * (6*size*size)
	data_s_list[5::6] = [' '] * (size*size)
	data_s_list[r-1::r] = ['\n'] * (size)
	data_s_list[::2] = map(str, depmat.data)
	data_s = ''.join(data_s_list)
	mapping_s = '\n'.join(map(str, depmat.mapping or range(depmat.size)))
	return mapping_s + '\n\n' + data_s

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
	try:
		request = event.get_entry('request', str)
	except KeyError:
		request = '???'
	depmat = event.get_entry('depmat', deserialize)
	return f'<!doctype html><html><head><title>{request} :: depmat</title></head><body><h1>TODO</h1>{depmat}</body></html>\n'

def csv(event):
	# XXX define format
	return '0,0,0\n'

def aggregate(events, output_event):
	size, mappings = phasestack.aggregate(events, output_event)
	aggregated_matrix = empty(size)
	for event, mapping in zip(events, mappings):
		matrix = event.get_entry('depmat', deserialize)
		add_to(aggregated_matrix, change_mapping(matrix, mapping))
	output_event.set_entry('depmat', aggregated_matrix, serialize)

def diff(from_, to, output_event):
	# TODO do something useful (currently just lame matrix subtraction)
	from_depmat = from_.get_entry('depmat', deserialize)
	from_ps = from_.get_entry('phasestack', phasestack.deserialize)
	neg_from_depmat = DepMat(from_depmat.size, from_depmat.mapping, [-value for value in from_depmat.data])
	neg_from = AnonymousEvent(depmat=neg_from_depmat, phasestack=from_ps)
	aggregate([neg_from, to], output_event)

def expand(original_event, new_ps_event, output_event):
	original_matrix = original_event.get_entry('depmat', deserialize)
	original_ps = original_event.get_entry('phasestack', phasestack.deserialize)
	new_ps = new_ps_event.get_entry('phasestack', phasestack.deserialize)
	mapping = phasestack.get_mapping(original_ps, new_ps)
	new_matrix = change_mapping(original_matrix, mapping)
	output_event.set_entry('phasestack', new_ps, phasestack.serialize)
	output_event.set_entry('depmat', new_matrix, serialize)

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
		print('  depmat html [<EVENT>]')
		print('  depmat csv [<EVENT>]')
		print('  depmat aggregate [<EVENT LIST ...>]')
		print('  depmat diff <FROM_EVENT> <TO_EVENT>')
		print('  depmat expand <ORIGINAL> <NEW_PHASESTACK>')
	
	elif function == 'html':
		#scan_options('')
		(event,) = expect_n_events_in_args(1, args or '-', 'Usage: depmat html [<EVENT>]')
		create_output_text_file('.html', html(event))
	
	elif function == 'csv':
		#scan_options('')
		(event,) = expect_n_events_in_args(1, args or '-', 'Usage: depmat csv [<EVENT>]')
		create_output_text_file('.csv', csv(event))
	
	elif function == 'aggregate':
		#scan_options('')
		aggregate(args_or_stdin_to_events_list(args), create_output_event())
	
	elif function == 'diff':
		#scan_options('')
		(from_event, to_event) = expect_n_events_in_args(2, args, 'Usage: depmat diff <FROM_EVENT> <TO_EVENT>')
		diff(from_event, to_event, create_output_event())
	
	elif function == 'expand':
		#scan_options('')
		(original, new_phasestack) = expect_n_events_in_args(2, args, 'Usage: depmat expand <ORIGINAL> <NEW_PHASESTACK>')
		expand(original, new_phasestack, create_output_event())
	
	else:
		raise UserError(f'No such function: {function!r}')

if __name__ == '__main__':
	try:
		main()
	except (UserError, GetoptError) as e:
		print(e.msg, file=sys.stderr)
		exit(1)
