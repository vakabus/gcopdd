from collections import namedtuple
from itertools import count
from functools import reduce

from viewers.common import *


def html_node(node, params):
	if isinstance(node.desc, ClassDesc):
		yield '<li>'
		yield from html_class(node.desc)
		yield from html_subtree(node, params)
		yield '</li>'
	elif isinstance(node.desc, int):
		yield '<li class="here">'
		yield '<div id="%shere%i"></div>' % (params.cat, node.desc)
		yield '</li>'
	else:
		raise TypeError()


def html_subtree(node, params):
	yield '<ul>'
	for childnode in node.children:
		yield from html_node(childnode, params)
	yield '</ul>'


def html_class(desc):
	yield '%s<span style="color: lightgray">, %s</span>' % (desc.simplename, desc.package)


def html_view_tree(call_tree, params):
	yield from html_ctmode_switch(params)
	yield from html_subtree(call_tree, params)


def html_view_list(classes, params):
	yield from html_ctmode_switch(params)
	yield '<ul>'
	for cnt, desc in enumerate(classes):
		yield '<li>'
		yield '<span style="color: gray">%s.</span>%s' % (desc.package, desc.simplename)
		yield '<ul><div id="%shere%i" class="here"></div></ul>' % (params.cat, cnt)
		yield '</li>'
	yield '</ul>'


def view(file, open_sibling, params):
	mapping, lines = process_phasestack(stripped_lines_close(file), params)
	call_tree = read_call_tree(lines)
	return html_view_tree(call_tree, params)


def aggregate(files, open_sibling, params):
	mappings, lines = aggregate_phasestacks(map(stripped_lines_close, files), params)
	call_tree = read_call_tree(lines)
	return html_view_tree(call_tree, params)
