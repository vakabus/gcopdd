from collections import namedtuple
from itertools import count
from functools import reduce

from viewers.common import *


def html_node(node, counts, total, params):
	if isinstance(node.desc, ClassDesc):
		yield '<li>'
		try:
			count = counts[node.children[0].desc]
			yield '<span style="color: %s">%s (%s)</span> ' % (
				css_color(count / total, 1.0, 0.5),
				percent_str(count / total),
				pretty_number(count),
			)
		except Exception:
			# Counts may be `None` (iff not viewing aggregated data).
			# Also, the tag may be missing (e.g. when the inverse mapping is ambiguous).
			# In the latter case the user can tell the counts from the children of this node.
			pass
		yield from html_class(node.desc)
		yield from html_subtree(node, counts, total, params)
		yield '</li>'
	elif isinstance(node.desc, int):
		yield '<li class="here">'
		yield '<div id="%shere%i"></div>' % (params.cat, node.desc)
		yield '</li>'
	else:
		raise TypeError()


def html_subtree(node, counts, total, params):
	yield '<ul>'
	for childnode in node.children:
		yield from html_node(childnode, counts, total, params)
	yield '</ul>'


def html_class(desc):
	yield '%s<span style="color: lightgray">, %s</span>' % (desc.simplename, desc.package)


def html_all(call_tree, counts, total, params):
	yield from html_ctmode_switch(params)
	yield from html_subtree(call_tree, counts, total, params)


def view(file, open_sibling, params):
	mapping, lines = process_phasestack(stripped_lines_close(file), params)
	call_tree = read_call_tree(lines)
	return html_all(call_tree, None, None, params)


def aggregate(files, open_sibling, params):
	mappings, lines = aggregate_phasestacks(map(stripped_lines_close, files), params)
	call_tree = read_call_tree(lines)
	counts = [0] * len(lines)
	total = 0
	for mapping in mappings:
		for i in set(mapping):
			counts[i] += 1
		total += 1
	return html_all(call_tree, counts, total, params)
