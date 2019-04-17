#!/usr/bin/python3

from collections import namedtuple
import re

classes = []

DependencyValue = namedtuple('DependencyValue', ['count', 'totalCount', 'iterations'])
DependencyValue.ratio = lambda dv: dv.totalCount and dv.count / dv.totalCount



with open('/tmp/gcopdd-depmat') as lines_n:
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

def print_td(dv):
	hue = int((1-dv.ratio()) * 120) # 0..120 ~ red..green
	sat = min(dv.iterations, 100) # 0%..100% ~ gray..color
	lit = 50 # 0%..100% ~ black..white
	color = 'hsl(%i, %i%%, %i%%)' % (hue, sat, lit)
	td = '<td style="background-color: %s" title="%s">%i%%<br>%i</td>' % (color, dv, dv.ratio()*100, dv.iterations)
	print(td)

def print_th(idx, desc):
	abbr = re.sub('[^A-Z]', '', desc[0][-1])
	th = '<th>%i<br>%s</th>' % (idx, abbr)
	print(th)

def print_empty_th():
	th = '<th>&nbsp;</th>'
	print(th)

def print_table():
	print('<table cellspacing="0">')
	print('<tr>')
	print_empty_th()
	for idx, desc in enumerate(classes):
		print_th(idx, desc)
	print('</tr>')
	for idx, (desc, row) in enumerate(zip(classes, matrix)):
		print('<tr>')
		print_th(idx, desc)
		for dv in row:
			print_td(dv)
		print('</tr>')
	print('</table>')

def print_legend():
	print('<ol start="0">')
	for desc in classes:
		fullname = desc[0][-1].split('.')
		package = ".".join(fullname[:-1])
		name = fullname[-1]
		print('<li title="%r"><span style="color: gray">%s.</span>%s</li>' % (desc[0], package, name))
	print('</ol>')

def print_html():
	print('<!doctype html><html><head><meta charset="utf8"><title>GCOPDD dependency matrix</title></head><body>')
	print('<table><tr><td>')
	print_table()
	print('</td><td style="text-align: left; vertical-align: top">')
	print_legend()
	print('</td></tr></table>')
	print('</body></html>')

print_html()

