from collections import namedtuple
from itertools import takewhile
from viewers_common import *


TreeNode = namedtuple('TreeNode', ['parent', 'desc', 'children'])


def read_call_tree(lines):
	stack = []
	root = TreeNode(None, None, [])
	currnode = root

	for line in lines:
		newstack = [ClassDesc.parse(fullname) for fullname in line.split()]
		if len(newstack) == len(stack)+1: # push?
			if all(map(ClassDesc.__eq__, newstack, stack)): # nothing else changed?
				# valid push, add subtree
				newclassdesc = newstack[-1]
				newnode = TreeNode(currnode, newclassdesc, [])
				currnode.children.append(newnode)
				# move to the subtree
				currnode = newnode
			else:
				raise Exception('Invalid phaseStack dump file: push modifies other items')
		elif len(newstack) == len(stack)-1: # pop?
			if all(map(ClassDesc.__eq__, newstack, stack)): # nothing else changed?
				# valid pop, leave subtree
				currnode = currnode.parent
			else:
				raise Exception('Invalid phaseStack dump file: pop modifies other items')
		else:
			raise Exception('Invalid phaseStack dump file')
		stack = newstack

	return root


def html_node(node):
	yield '<li>'
	yield from html_class(node.desc)
	yield from html_subtree(node)
	yield '</li>'


def html_subtree(node):
	yield '<ul>'
	for childnode in node.children:
		yield from html_node(childnode)
	yield '</ul>'


def html_class(desc):
	yield '%s<span style="color: lightgray">, %s</span>' % (desc.simplename, desc.package)


def view(lines_n):
	lines = map(str.strip, lines_n) # remove '\n' characters
	call_tree = read_call_tree(lines)

	return html_subtree(call_tree)
