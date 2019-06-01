from itertools import takewhile
from viewers.common import *


def html_td(dv, row_desc, col_desc, col_max):
	hue = int((1 - dv.ratio()/(col_max or 1)) * 120)
	sat = min(dv.iterations, 100)
	lit = 50 # 0%..100% ~ black..white
	color = 'hsl(%i, %i%%, %i%%)' % (hue, sat, lit)
	td = '<td style="background-color: %s" title="%s\nNode %s\nPhase %s">%i%%<br>%s</td>' % (color, dv, col_desc.simplename, row_desc.simplename, dv.ratio()*100, pretty_number(dv.iterations))
	yield td


def find_column_maxima(matrix):
	# convert matrix from array of rows to array of columns
	# for each column: calculate all the ratio()s and find the highest of them
	# return array, where each element corresponds to a row
	return [max(map(DependencyValue.ratio, column)) for column in zip(*matrix)]


def html_table(col_classes, row_classes, matrix, col_maxs):
	yield '<table class="simple-border">'
	yield '<tr>'
	yield '<th>&nbsp;</th>'
	for desc in col_classes:
		yield '<th>%i</th>' % (desc.index)
	yield '</tr>'
	for row_desc, row in zip(row_classes, matrix):
		yield '<tr>'
		yield '<th>%i</th>' % (row_desc.index)
		for col_desc, dv, col_max in zip(col_classes, row, col_maxs):
			yield from html_td(dv, row_desc, col_desc, col_max)
		yield '</tr>'
	yield '</table>'


def html_legend(classes):
	yield '<ol start="0">'
	for desc in classes:
		yield '<li><span style="color: gray">%s.</span>%s</li>' % (desc.package, desc.simplename)
	yield '</ol>'


def view(lines_n, *_):
	lines = map(str.strip, lines_n) # remove '\n' characters
	nodeClasses = read_classes(takewhile(nonempty, lines))
	phaseClasses = read_classes(takewhile(nonempty, lines))
	pre_phase_matrix = read_depval_matrix(takewhile(nonempty, lines))
	post_phase_matrix = read_depval_matrix(takewhile(nonempty, lines))

	yield '<table class="mono"><tr><td>'
	yield from html_table(nodeClasses, phaseClasses, pre_phase_matrix, find_column_maxima(pre_phase_matrix))
	yield from html_table(nodeClasses, phaseClasses, post_phase_matrix, find_column_maxima(post_phase_matrix))
	yield '</td><td style="text-align: left; vertical-align: top">'
	yield from html_legend(nodeClasses)
	yield from html_legend(phaseClasses)
	yield '</td></tr></table>'
