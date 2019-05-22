from itertools import takewhile
from viewers_common import *


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
		yield '<li><span style="color: gray">%s.</span>%s</li>' % (desc.package, desc.simplename)
	yield '</ol>'


def view(lines_n):
	lines = map(str.strip, lines_n) # remove '\n' characters
	classes = read_classes(takewhile(nonempty, lines))
	matrix = read_depval_matrix(takewhile(nonempty, lines))

	yield '<table style="font-family: monospace"><tr><td>'
	yield from html_table(classes, matrix)
	yield '</td><td style="text-align: left; vertical-align: top">'
	yield from html_legend(classes)
	yield '</td></tr></table>'
