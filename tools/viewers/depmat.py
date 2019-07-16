from viewers.common import *


EXPECTED_ITERATIONS = 32


JS_LEGEND = """
	var from, to, ps = window.open('phasestack%s#noctrl', '', 'toolbar=no');
	function here(i) {
		return ps.document.getElementById('%shere' + i);
	}
	document.onmouseover = function(event) {
		if(event.target.className === 'fromto') {
			from = here(event.target.getAttribute('data-from'));
			to = here(event.target.getAttribute('data-to'));
			if(from) from.className = 'target targetfrom';
			if(to) to.className = 'target targetto';
			if(from === to && from) to.className = 'target';
		}
	};
	document.onmouseout = function(event) {
		if(event.target.className === 'fromto') {
			if(from) from.className = '';
			if(to) to.className = '';
		}
	};
	window.onunload = function() {
		ps.close();
	};
""".replace('\n', '')


def html_td(dv, row, col, params):
	color = css_color(dv.ratio(), dv.iterations / EXPECTED_ITERATIONS, 0.5)
	yield '<td style="background-color: %s" title="%s\n\nFrom:\n%s\n\nTo:\n%s" class="fromto" data-from="%i" data-to="%i"><div>' % (
		color, dv, col.desc(), row.desc(), col.id, row.id
	)
	yield '<a href="phasestack%s#%shere%i">From</a>' % (params, params.cat, col.id)
	yield '<a href="phasestack%s#%shere%i">To</a>' % (params, params.cat, row.id)
	yield '</div>%s<br>%s</td>' % (percent_str(dv.ratio()), pretty_number(dv.iterations))


def html_all(phases, matrix, params):
	yield from html_ctmode_switch(params)
	yield '<button onclick="%s">JS legend</button>' % (JS_LEGEND % (params, params.cat))
	yield '<table class="mono clickable-cells">'
	for row, matrix_row in zip(phases, matrix):
		yield '<tr>'
		for col, dv in zip(phases, matrix_row):
			yield from html_td(dv, row, col, params)
		yield '</tr>'
	yield '</table>'


def view(file, open_sibling, params):
	mapping, phasestack_lines = process_phasestack(stripped_lines_close(open_sibling('phasestack')), params)
	expl = CallTreePosition.list_from_phasestack_dump(phasestack_lines)
	phasestack_lines = len(phasestack_lines) # the contents are no longer needed (just the size)

	lines = stripped_lines_close(file)
	
	# depmat file has two segments separated by empty line
	phases = list(map(int, take_up_to_empty(lines)))
	matrix = read_depval_matrix(take_up_to_empty(lines))
	
	matrix = matrix_apply_mapping(phases, matrix, phasestack_lines, mapping, DependencyValue(0, 0, 0))

	return html_all(expl, matrix, params)


def aggregate(files, open_sibling, params):
	mappings, phasestack_lines = aggregate_phasestacks(map(stripped_lines_close, open_sibling('phasestack')), params)
	expl = CallTreePosition.list_from_phasestack_dump(phasestack_lines)
	phasestack_lines = len(phasestack_lines) # the contents are no longer needed (just the size)
	
	total_matrix = make_matrix(phasestack_lines, phasestack_lines, DependencyValue(0, 0, 0))
	
	for mapping, file in zip(mappings, files):
		lines = stripped_lines_close(file)
		
		# depmat file has two segments separated by empty line
		phases = list(map(int, take_up_to_empty(lines)))
		matrix = read_depval_matrix(take_up_to_empty(lines))
	
		matrix = matrix_apply_mapping(phases, matrix, phasestack_lines, mapping, DependencyValue(0, 0, 0))
		
		matrix_add(total_matrix, matrix)

	return html_all(expl, total_matrix, params)
