#!/usr/bin/env python3

import sys
from getopt import gnu_getopt, GetoptError
from collections import namedtuple

from libgcopdd import Event, UserError, open_dump_entry

PhaseStack = namedtuple('PhaseStack', 'size, data')

####

def load(event):
	with open_dump_entry(event + '.phasestack') as file:
		# Use `sys.intern` so that `viewers.common`
		# functions can use `is` instead of `==`.
		# TODO: remove this after removing
		#       `viewers.common` imports.
		data = list(map(sys.intern, map(str.strip, file)))
	return PhaseStack(len(data), data)

def save(ps, event):
	with open_dump_entry(event + '.phasestack', 'wt') as file:
		file.write('\n'.join(ps.data) + '\n')

####

def get_mapping(single, aggregated):
	# TODO adapt from viewers.common
	# please do not read this
	from viewers.common import _subseq_mapping as deprecated
	result = single.data[:]
	deprecated(aggregated.data, result)
	return result

####

def aggregate(events):
	# TODO adapt from viewers.common
	# please do not read this
	from viewers.common import aggregate_phasestacks as deprecated
	m, r = deprecated([event.get('phasestack', load).data for event in events], {'ctmode': 'full'})
	return PhaseStack(len(r), r), m

####

def main():
	raise NotImplementedError() # TODO

if __name__ == '__main__':
	try:
		main()
	except (UserError, GetoptError) as e:
		print(e.msg, file=sys.stderr)
		exit(1)
