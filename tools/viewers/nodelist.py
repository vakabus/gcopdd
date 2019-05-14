from itertools import takewhile
from viewers_common import *


def html_class(desc):
	yield '<span style="color: gray">%s</span>.%s' % (desc.package, desc.simplename)


def view(lines_n):
	lines = map(str.strip, lines_n) # remove '\n' characters
	classes = read_classes(lines)

	yield '<ul>'
	for class_desc in classes:
		yield '<li>'
		yield from html_class(class_desc)
		yield '</li>'
	yield '</ul>'
