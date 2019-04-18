#!/usr/bin/python3

from collections import namedtuple
import re


DependencyValue = namedtuple('DependencyValue', ['count', 'totalCount', 'iterations'])
DependencyValue.ratio = lambda dv: dv.totalCount and dv.count / dv.totalCount


def html_td(dv):
	hue = int((1-dv.ratio()) * 120) # 0..120 ~ red..green
	sat = min(dv.iterations, 100) # 0%..100% ~ gray..color
	lit = 50 # 0%..100% ~ black..white
	color = 'hsl(%i, %i%%, %i%%)' % (hue, sat, lit)
	td = '<td style="background-color: %s" title="%s">%i%%<br>%i</td>' % (color, dv, dv.ratio()*100, dv.iterations)
	yield td


def html_th(idx, desc):
	abbr = re.sub('[^A-Z]', '', desc[0][-1])
	th = '<th>%i<br>%s</th>' % (idx, abbr)
	yield th


def html_empty_th():
	th = '<th>&nbsp;</th>'
	yield th


def html_table(classes, matrix):
	yield '<table cellspacing="0">'
	yield '<tr>'
	yield from html_empty_th()
	for idx, desc in enumerate(classes):
		yield from html_th(idx, desc)
	yield '</tr>'
	for idx, (desc, row) in enumerate(zip(classes, matrix)):
		yield '<tr>'
		yield from html_th(idx, desc)
		for dv in row:
			yield from html_td(dv)
		yield '</tr>'
	yield '</table>'


def html_legend(classes):
	yield '<ol start="0">'
	for desc in classes:
		fullname = desc[0][-1].split('.')
		package = ".".join(fullname[:-1])
		name = fullname[-1]
		yield '<li title="%r"><span style="color: gray">%s.</span>%s</li>' % (desc[0], package, name)
	yield '</ol>'


def view(lines_n):
	classes = []

	# remove '\n' characters
	lines = (line[:-1] for line in lines_n)
	# read first part of the file
	for lineno, line in enumerate(lines):
		if not line:
			break
		# add a tuple similar to this to `classes`:
		# (['java.lang.Object', '...BasePhase', ..., '...SpecificPhase'], 42)
		classes.append((line.split(' ')[::-1], lineno))
	# read the matrix from the rest of the file
	matrix = [[DependencyValue(*map(int, item.split(':'))) for item in line.split(' ')] for line in lines]

	# sort the classes by
	# (1) DFS preorder in the class hierarchy tree
	# (2) qualified name lexicographically
	classes.sort()
	# sort the matrix rows and columns accordingly
	matrix = [[matrix[i][j] for _, j in classes] for _, i in classes]

	yield '<table><tr><td>'
	yield from html_table(classes, matrix)
	yield '</td><td style="text-align: left; vertical-align: top">'
	yield from html_legend(classes)
	yield '</td></tr></table>'


