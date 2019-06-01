from itertools import takewhile, count
from viewers.common import *


IdDesc = namedtuple('IdDesc', ['id', 'desc'])


def html_td(dv, row, col, ce):
	hue = int((1-dv.ratio()) * 120) # 0..120 ~ red..green
	sat = min(dv.iterations, 100) # 0%..100% ~ gray..color
	lit = 50 # 0%..100% ~ black..white
	color = 'hsl(%i, %i%%, %i%%)' % (hue, sat, lit)
	fromlink = '%s.phasestack.html#here%i' % (ce, col.id)
	tolink = '%s.phasestack.html#here%i' % (ce, row.id)
	td = '<td style="background-color: %s" title="%s\n\nFrom:\n%s\n\nTo:\n%s"><div class="fromto"><a href="%s">From</a><a href="%s">To</a></div>%i%%<br>%s</td>' % (color, dv, col.desc, row.desc, fromlink, tolink, dv.ratio()*100, pretty_number(dv.iterations))
	yield td


def html_table(phases, matrix, ce):
	yield '<table class="simple-border mono clickable-cells">'
	for row, matrix_row in zip(phases, matrix):
		yield '<tr>'
		for col, dv in zip(phases, matrix_row):
			yield from html_td(dv, row, col, ce)
		yield '</tr>'
	yield '</table>'


def view(lines_n, get_sibling, ce):
	lines = map(str.strip, lines_n) # remove '\n' characters
	phases_expl = read_stack_explanation(get_sibling('phasestack'))
	phases = [IdDesc(int(i)+1, phases_expl[int(i)+1].desc()) for i in takewhile(nonempty, lines)]
	matrix = read_depval_matrix(takewhile(nonempty, lines))

	yield from html_table(phases, matrix, ce)
