#!/usr/bin/python3

from collections import namedtuple
from itertools import count
import re


DependencyValue = namedtuple('DependencyValue', ['count', 'totalCount', 'iterations'])
DependencyValue.ratio = lambda dv: dv.totalCount and dv.count / dv.totalCount
ClassDesc = namedtuple('ClassDesc', ['index', 'superclasses', 'fullname', 'package', 'simplename'])


def pretty_number(num):
	if num > 1000000:
		return str(round(num / 1000000)) + "M"
	elif num > 1000:
		return str(round(num / 1000)) + "K"
	else:
		return str(num)


def html_td(dv, row_desc, col_desc):
	hue = int((1-dv.ratio()) * 120) # 0..120 ~ red..green
	sat = min(dv.iterations, 100) # 0%..100% ~ gray..color
	lit = 50 # 0%..100% ~ black..white
	color = 'hsl(%i, %i%%, %i%%)' % (hue, sat, lit)
	td = '<td style="background-color: %s" title="%s\nFrom %s\nTo %s">%i%%<br>%s</td>' % (color, dv, col_desc.simplename, row_desc.simplename, dv.ratio()*100, pretty_number(dv.iterations))
	yield td


def html_table(classes, matrix):
	yield '<table class="simple-border">'
	yield '<tr>'
	yield '<th>&nbsp;</th>'
	for desc in classes:
		yield '<th>%i</th>' % (desc.index)
	yield '</tr>'
	for row_desc, row in zip(classes, matrix):
		yield '<tr>'
		yield '<th>%i</th>' % (row_desc.index)
		for col_desc, dv in zip(classes, row):
			yield from html_td(dv, row_desc, col_desc)
		yield '</tr>'
	yield '</table>'


def html_legend(classes):
	yield '<ol start="0">'
	for desc in classes:
		yield '<li title="%s"><span style="color: gray">%s.</span>%s</li>' % ("\n".join(desc.superclasses), desc.package, desc.simplename)
	yield '</ol>'


def view(lines_n, dump, params):
	classes = []

	# remove '\n' characters
	lines = (line[:-1] for line in lines_n)
	# read first part of the file
	for lineno, line in enumerate(lines):
		if not line:
			break
		superclasses = line.split(' ') # ordered from this class to java.lang.Object
		fullname = superclasses[0]
		pkg_delim = fullname.rfind('.')
		package = fullname[:pkg_delim]
		simplename = fullname[pkg_delim+1:]
		classes.append(ClassDesc(lineno, superclasses, fullname, package, simplename))
	# read the matrix from the rest of the file
	matrix = [[DependencyValue(*map(int, item.split(':'))) for item in line.split(' ')] for line in lines]

	yield '<table style="font-family: monospace"><tr><td>'
	yield from html_table(classes, matrix)
	yield '</td><td style="text-align: left; vertical-align: top">'
	yield from html_legend(classes)
	yield '</td></tr></table>'


