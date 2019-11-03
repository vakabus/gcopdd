#!/usr/bin/env python3

import sys
from getopt import gnu_getopt, GetoptError
from collections import namedtuple

from libgcopdd import UserError

PhaseStack = namedtuple('PhaseStack', 'size, data')

####

def deserialize(string):
	# Use `sys.intern` so that `viewers.common` functions can use `is` instead of `==`.
	# TODO: remove this after removing `viewers.common` imports.
	data = list(map(sys.intern, string.split('\n')))
	return PhaseStack(len(data), data)

def serialize(ps):
	return '\n'.join(ps.data)

####

def get_mapping(single, aggregated):
	# TODO adapt from viewers.common
	# please do not read this
	from viewers.common import _subseq_mapping as deprecated
	result = single.data[:]
	deprecated(aggregated.data, result)
	return result

####

def aggregate(events, output_event):
	# TODO adapt from viewers.common
	# please do not read this
	from viewers.common import aggregate_phasestacks as deprecated
	m, r = deprecated([event.get_entry('phasestack', deserialize).data for event in events], {'ctmode': 'full'})
	output_event.set_entry('phasestack', PhaseStack(len(r), r), serialize)
	return len(r), m

####

def main():
	raise NotImplementedError() # TODO

if __name__ == '__main__':
	try:
		main()
	except (UserError, GetoptError) as e:
		print(e.msg, file=sys.stderr)
		exit(1)
