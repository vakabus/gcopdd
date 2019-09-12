#!/usr/bin/env python3

import sys
import os
from os import path
from getopt import gnu_getopt, GetoptError
from re import compile as Regex

from libgcopdd import UserError, open_dump_entry, create_temp

####

TOKEN_PATTERN = Regex('[!&()*+|~]|([-.0-9:A-Z_a-z]+)|(.)')
END_OF_PATTERN = '/'

def match(dump_predicate, event_predicate):
	# XXX see `libgcopdd.open_dump_entry()`
	assert os.getcwd().endswith('/dumps'), 'Please cd into dumps directory'
	result = []
	for dump in os.listdir():
		if not path.isdir(dump):
			continue
		if not dump_predicate(dump):
			continue
		for filename in os.listdir(dump):
			event_id, entry_type, *_ = filename.split('.')
			if entry_type != 'request':
				continue
			with open_dump_entry(dump + '/' + event_id + '.request') as file:
				event = file.read().strip()
			if not event_predicate(event):
				continue
			result.append((dump + '/' + event_id, event))
	return result

def scan_pattern(string):
	try:
		string.encode('ascii') # ... discard the result
	except UnicodeEncodeError:
		raise UserError('Only ASCII supported') # unicode case mapping is a mess
	for match_object in TOKEN_PATTERN.finditer(string.lower()):
		lexeme, literal_value, invalid = match_object.group(0, 1, 2)
		if invalid is not None:
			raise UserError('Invalid character: %r' % invalid)
		yield (lexeme, literal_value)

def parse_pattern(tokens):
	# initialization
	curr, curr_val = next(tokens)
	# after all these `def`s and `class`es, the body continues
	def advance():
		nonlocal curr, curr_val
		curr, curr_val = next(tokens, (END_OF_PATTERN, None))
	def accept(accepted):
		if curr == accepted:
			advance()
			return True
		return False
	def expect(expected):
		if not accept(expected):
			raise UserError('Unexpected %r' % curr)
	def expect_lit():
		if curr_val is None:
			raise UserError('Unexpected %r' % curr)
		result = curr_val
		advance()
		return result
	# S -> '*' | DISJ
	def parse_s():
		if accept('*'):
			return Conj() # empty conjunction always holds
		else:
			return parse_disj()
	# DISJ -> CONJ (('|' | '+') CONJ)*
	def parse_disj():
		result = Disj([parse_conj()])
		while accept('|') or accept('+'):
			result.append(parse_conj())
		return result
	# CONJ -> LL ('&' LL)*
	def parse_conj():
		result = Conj([parse_ll()])
		while accept('&'):
			result.append(parse_ll())
		return result
	# LL -> '(' DISJ ')' | '!' LL | str | str '~' str
	def parse_ll():
		if accept('('):
			result = parse_disj()
			expect(')')
			return result
		if accept('!'):
			return Not(parse_ll())
		# otherwise:
		s = expect_lit()
		if accept('~'):
			return StrRange(s, expect_lit())
		else:
			return SubString(s)
	# disjunction: True iff at least one of its items is True
	class Disj(list):
		def apply(self, string):
			return any(item.apply(string) for item in self) # recall that `self` is a list
	# disjunction: True iff all its items are True
	class Conj(list):
		def apply(self, string):
			return all(item.apply(string) for item in self) # just replaced `any` by `all`
	# negation: True iff its subformula is False
	class Not:
		def __init__(self, formula):
			self.formula = formula
		def apply(self, string):
			return not self.formula.apply(string)
	# range predicate: True iff at least one prefix of given string is lexicographically between the two bounds
	class StrRange:
		def __init__(self, a, b):
			if a > b:
				raise UserError('Invalid range: %r comes after %r' % (a, b))
			self.a = a
			self.bn = b + '\x7f' # 'DEL' - highest ASCII char - we want "zzz" in "a~z"
		def apply(self, string):
			return self.a <= string <= self.bn
	# substring predicate: (for some fixed s) True iff given string contains s as its substring
	class SubString:
		def __init__(self, substring):
			self.substring = substring
		def apply(self, string):
			return self.substring in string
	# here is the promised body
	return parse_s()

def load_pattern(string):
	return parse_pattern(scan_pattern(string))

####

def main():
	args = sys.argv[1:]
	opts = None
	
	def scan_options(longopts_str):
		nonlocal opts, args
		opts_list, args_list = gnu_getopt(args, '', longopts_str.split())
		opts_dict = dict(opts_list)
		if len(opts_dict) != len(opts_list):
			raise UserError('Duplicate option!')
		opts = opts_dict
		args = args_list
	
	scan_options('help as=')
	
	if '--help' in opts:
		print('Usage: match [<DUMP_PATTERN> [<EVENT_PATTERN>]] [--as <TARGET>]')
		print('If you omit the patterns, they will be asked for interactively.')
		print('You can use this feature to avoid the shell interpreting')
		print('special characters in the patterns.')
		return
	
	if len(args) == 0:
		import readline # enables line-editing in input()
		dump_pattern_str = input('Dump pattern: ')
		event_pattern_str = input('Event pattern: ')
	elif len(args) == 1:
		import readline
		dump_pattern_str = args[0]
		event_pattern_str = input('Event pattern: ')
	elif len(args) == 2:
		dump_pattern_str = args[0]
		event_pattern_str = args[1]
	else:
		raise UserError('Too many arguments, see `match --help`.')
	
	target = create_temp(opts.get('--as'))
	
	dump_predicate = load_pattern(dump_pattern_str).apply
	event_predicate = load_pattern(event_pattern_str).apply
	result = match(dump_predicate, event_predicate)
	with open(target, 'w') as file:
		for path, comment in result:
			file.write(f'{path} # {comment}\n')
	
	print(f'({len(result)} events matched)', file=sys.stderr)

if __name__ == '__main__':
	try:
		main()
	except (UserError, GetoptError) as e:
		print(e.msg, file=sys.stderr)
		exit(1)
