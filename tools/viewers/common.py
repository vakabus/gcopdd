from itertools import chain
from collections import namedtuple


ClassDesc = namedtuple('ClassDesc', ['index', 'fullname', 'package', 'simplename'])

def ClassDesc_parse(fullname, index=None):
	package, simplename = fullname.rsplit('.', 1)
	return ClassDesc(index, fullname, package, simplename)

ClassDesc.parse = ClassDesc_parse


def read_classes(iterator):
	return [ClassDesc.parse(fullname, lineno) for lineno, fullname in enumerate(iterator)]


def read_phases(iterator):
	return list(iterator)


def read_matrix(iterator, item_conv):
	return [[item_conv(item) for item in line.split(' ')] for line in iterator]


DependencyValue = namedtuple('DependencyValue', ['count', 'totalCount', 'iterations'])
DependencyValue.ratio = lambda dv: dv.totalCount and dv.count / dv.totalCount


def read_depval_matrix(iterator):
	return read_matrix(iterator, lambda item: DependencyValue(*map(int, item.split(':'))))


def pretty_number(num):
	if num > 1000000:
		return str(round(num / 1000000)) + 'M'
	elif num > 1000:
		return str(round(num / 1000)) + 'K'
	else:
		return str(num)


nonempty = bool


class StackExplanation:
	def __init__(self, inside=None, before=None, after=None):
		self.inside = inside
		self.before = before
		self.after = after
	
	def desc(self):
		if self.inside:
			desc = 'Inside ' + self.inside.simplename
		else:
			desc = 'Top level'
		if self.before and self.after:
			desc += '\nBetween ' + self.after.simplename
			desc += '\nand ' + self.before.simplename
		elif self.before:
			desc += '\nBefore ' + self.before.simplename
		elif self.after:
			desc += '\nAfter ' + self.after.simplename
		return desc


def last_or_None(list):
	if list:
		return list[-1]
	else:
		return None


def read_stack_explanation(lines):
	explanation = [StackExplanation()]
	stack = []

	for line in chain(lines, ['']):
		newstack = [ClassDesc.parse(fullname) for fullname in line.split()]
		if len(newstack) == len(stack)+1: # push?
			if all(map(ClassDesc.__eq__, newstack, stack)): # nothing else changed?
				# valid push
				explanation[-1].before = newstack[-1]
				explanation.append(StackExplanation(inside=newstack[-1]))
			else:
				raise Exception('Invalid phaseStack dump file: push modifies other items')
		elif len(newstack) == len(stack)-1: # pop?
			if all(map(ClassDesc.__eq__, newstack, stack)): # nothing else changed?
				# valid pop
				explanation.append(StackExplanation(inside=last_or_None(newstack), after=stack[-1]))
			else:
				raise Exception('Invalid phaseStack dump file: pop modifies other items')
		else:
			raise Exception('Invalid phaseStack dump file')
		stack = newstack

	return explanation
