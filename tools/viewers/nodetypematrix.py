#!/usr/bin/python3

# TODO refactor (copy-pasted from depmat)

from collections import namedtuple
from itertools import count, takewhile
import re


DependencyValue = namedtuple('DependencyValue', ['count', 'totalCount', 'iterations'])
DependencyValue.ratio = lambda dv: dv.totalCount and dv.count / dv.totalCount
ClassDesc = namedtuple('ClassDesc', ['index', 'fullname', 'package', 'simplename'])


def pretty_number(num):
	if num > 1000000:
		return str(round(num / 1000000)) + "M"
	elif num > 1000:
		return str(round(num / 1000)) + "K"
	else:
		return str(num)


def html_td(dv, row_desc, col_desc):
	hue = int((1-dv.ratio()**.25) * 120) # TODO dynamic scaling
	sat = 100 # TODO
	lit = 50 # 0%..100% ~ black..white
	color = 'hsl(%i, %i%%, %i%%)' % (hue, sat, lit)
	td = '<td style="background-color: %s" title="%s\nNode %s\nPhase %s">%i%%<br>%s</td>' % (color, dv, col_desc.simplename, row_desc.simplename, dv.ratio()*100, pretty_number(dv.iterations))
	yield td


def html_table(col_classes, row_classes, matrix):
	yield '<table class="simple-border">'
	yield '<tr>'
	yield '<th>&nbsp;</th>'
	for desc in col_classes:
		yield '<th>%i</th>' % (desc.index)
	yield '</tr>'
	for row_desc, row in zip(row_classes, matrix):
		yield '<tr>'
		yield '<th>%i</th>' % (row_desc.index)
		for col_desc, dv in zip(col_classes, row):
			yield from html_td(dv, row_desc, col_desc)
		yield '</tr>'
	yield '</table>'


def html_legend(classes):
	yield '<ol start="0">'
	for desc in classes:
		yield '<li><span style="color: gray">%s.</span>%s</li>' % (desc.package, desc.simplename)
	yield '</ol>'


def view(lines_n, dump, params):
	nodeClasses, phaseClasses = [], []

	# remove '\n' characters
	lines = map(str.strip, lines_n)
	# read first part of the file
	for lineno, fullname in enumerate(takewhile(bool, lines)):
		pkg_delim = fullname.rfind('.')
		package = fullname[:pkg_delim]
		simplename = fullname[pkg_delim+1:]
		nodeClasses.append(ClassDesc(lineno, fullname, package, simplename))
	# read first part of the file
	for lineno, fullname in enumerate(takewhile(bool, lines)):
		pkg_delim = fullname.rfind('.')
		package = fullname[:pkg_delim]
		simplename = fullname[pkg_delim+1:]
		phaseClasses.append(ClassDesc(lineno, fullname, package, simplename))
	# read the prePhase matrix
	pre_phase_matrix = [[DependencyValue(*map(int, item.split(':'))) for item in line.split(' ')] for line in takewhile(bool, lines)]
	# read the postPhase matrix
	post_phase_matrix = [[DependencyValue(*map(int, item.split(':'))) for item in line.split(' ')] for line in takewhile(bool, lines)]

	yield '<table style="font-family: monospace"><tr><td>'
	yield from html_table(nodeClasses, phaseClasses, pre_phase_matrix)
	yield from html_table(nodeClasses, phaseClasses, post_phase_matrix)
	yield '</td><td style="text-align: left; vertical-align: top">'
	yield from html_legend(nodeClasses)
	yield from html_legend(phaseClasses)
	yield '</td></tr></table>'


