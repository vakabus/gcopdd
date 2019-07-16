from viewers.common import *


EXPECTED_ITERATIONS = 32


JS_LEGEND = """
	var phase, ps = window.open('phasestack%s#noctrl', '', 'toolbar=no');
	function here(i) {
		return ps.document.getElementById('%shere' + i);
	}
	document.onmouseover = function(event) {
		if(event.target.getAttribute('data-phase') !== null) {
			phase = here(event.target.getAttribute('data-phase'));
			if(phase) phase.className = 'target';
		}
	};
	document.onmouseout = function(event) {
		if(event.target.getAttribute('data-phase') !== null) {
			if(phase) phase.className = '';
		}
	};
	window.onunload = function() {
		ps.close();
	};
""".replace('\n', '')


def html_td(dv, phase, node, col_max, params):
	color = css_color(dv.ratio()/(col_max or 1), dv.iterations / EXPECTED_ITERATIONS, 0.5)
	hint = 'Click to see phase stack'
	yield '<td style="background-color: %s" title="%s\n\nNode:\n%s\n\nPhase:\n%s\n\n%s">' % (
		color, dv, node.simplename, phase.desc(), hint
	)
	yield '<a href="phasestack%s#%shere%i" class="nolink" data-phase="%i">' % (params, params.cat, phase.id, phase.id)
	yield '%s<br>%s' % (percent_str(dv.ratio()), pretty_number(dv.iterations))
	yield '</a>'
	yield '</td>'


def css_table_header(nodes):
	# css probably doesn't offer a reasonable way to
	# rotate an element and THEN compute its dimensions,
	# so this code has to do exactly that
	header_height = max(len(node.simplename) for node in nodes) * 2 / 3
	return '''
		th div {
			transform: rotate(270deg);
			width: 1.25em;
			height: %.2fem;
			transform-origin: %.2fem;
			margin: 0.25em auto;
		}
	''' % (header_height, header_height / 2)


def html_table_header(nodes):
	yield '<tr>'
	for node in nodes:
		yield '<th><div>%s</div></th>' % (node.simplename)
	yield '</tr>'


def html_table(nodes, phases, matrix, col_maxs, params):
	for phase, row in zip(phases, matrix):
		yield '<tr>'
		for node, dv, col_max in zip(nodes, row, col_maxs):
			yield from html_td(dv, phase, node, col_max, params)
		yield '</tr>'


def html_all(nodes, phases, matrix1, matrix1m, matrix2, matrix2m, params):
	yield from html_ctmode_switch(params)
	yield '<button onclick="%s">JS legend</button>' % (JS_LEGEND % (params, params.cat))
	yield '<style>%s</style>' % css_table_header(nodes)
	yield '<table class="mono">'
	yield from html_table_header(nodes)
	yield from html_table(nodes, phases, matrix1, matrix1m, params)
	yield from html_table_header(nodes)
	yield from html_table(nodes, phases, matrix2, matrix2m, params)
	yield from html_table_header(nodes)
	yield '</table>'


def find_column_maxima(matrix):
	# convert matrix from array of rows to array of columns
	# for each column: calculate all the ratio()s and find the highest of them
	# return array, where each element corresponds to a row
	return [max(map(DependencyValue.ratio, column)) for column in zip(*matrix)]


def view(file, open_sibling, params):
	mapping, phasestack_lines = process_phasestack(stripped_lines_close(open_sibling('phasestack')), params)
	expl = CallTreePosition.list_from_phasestack_dump(phasestack_lines)
	phasestack_lines = len(phasestack_lines) # the contents are no longer needed (just the size)
	
	lines = stripped_lines_close(file)

	# nodemat file has four segments separated by empty line
	nodes = read_classes(take_up_to_empty(lines))
	phases = list(map(int, take_up_to_empty(lines)))
	matrix1 = read_depval_matrix(take_up_to_empty(lines)) # status before given phase
	matrix2 = read_depval_matrix(take_up_to_empty(lines)) # status after given phase

	matrix1 = matrix_apply_mapping_to_rows(phases, matrix1, phasestack_lines, mapping, DependencyValue(0, 0, 0))
	matrix2 = matrix_apply_mapping_to_rows(phases, matrix2, phasestack_lines, mapping, DependencyValue(0, 0, 0))
	
	matrix1m = find_column_maxima(matrix1)
	matrix2m = find_column_maxima(matrix2)

	return html_all(nodes, expl, matrix1, matrix1m, matrix2, matrix2m, params)


def aggregate(files, open_sibling, params):
	mappings, phasestack_lines = aggregate_phasestacks(map(stripped_lines_close, open_sibling('phasestack')), params)
	expl = CallTreePosition.list_from_phasestack_dump(phasestack_lines)
	phasestack_lines = len(phasestack_lines) # the contents are no longer needed (just the size)
	
	# NOTE: this is not what the function should look like.
	# In the comment bellow is a better, more readable, and more efficient version.
	# Please, read that one before going back and continuing here.
	# The reason behind using this version instead of the better one is
	# that we must read the headers of ALL files before we can aggregate thir bodies.
	# There are three possible solutions:
	#   - Read all headers, leaving the file descriptors open.
	#     This is dangerous, because it could involve having thousands of open FDs at once.
	#   - Close the files and reopen them later.
	#     This does not fit to the design of dump-browser, which gives us an already-opened FD and not an open() function.
	#   - Read all the files to memory (one after another) and then working in memory.
	#     This is the approach taken by this implementation (variable `pending`).
	# The commented-out version elegantly avoids this problem by using ***magic***.
	
	canon = Canon()
	all_nodes = []
	pending = []
	
	for file in files:
		lines = stripped_lines_close(file)
		
		# nodemat file has four segments separated by empty line
		nodes = read_classes(take_up_to_empty(lines))
		phases = list(map(int, take_up_to_empty(lines)))
		matrix1 = read_depval_matrix(take_up_to_empty(lines)) # status before given phase
		matrix2 = read_depval_matrix(take_up_to_empty(lines)) # status after given phase
		
		pending.append((nodes, phases, matrix1, matrix2))
		
		for idx, node in enumerate(nodes):
			nodes[idx] = canon.canonicalize(node)
		all_nodes = merge(all_nodes, nodes)
		
	uniq(all_nodes)
	all_nodes_index_of = {node: idx for idx, node in enumerate(all_nodes)}
	
	total_matrix1 = make_matrix(phasestack_lines, len(all_nodes), DependencyValue(0, 0, 0))
	total_matrix2 = make_matrix(phasestack_lines, len(all_nodes), DependencyValue(0, 0, 0))
	
	for mapping, (nodes, phases, matrix1, matrix2) in zip(mappings, pending):
		nodeids = range(len(nodes))
		for idx, node in enumerate(nodes):
			nodes[idx] = all_nodes_index_of[node]
	
		matrix1 = matrix_apply_mapping_to_rows(phases, matrix1, phasestack_lines, mapping, DependencyValue(0, 0, 0))
		matrix1 = matrix_apply_mapping_to_columns(nodeids, matrix1, len(all_nodes), nodes, DependencyValue(0, 0, 0))
		matrix2 = matrix_apply_mapping_to_rows(phases, matrix2, phasestack_lines, mapping, DependencyValue(0, 0, 0))
		matrix2 = matrix_apply_mapping_to_columns(nodeids, matrix2, len(all_nodes), nodes, DependencyValue(0, 0, 0))
		
		matrix_add(total_matrix1, matrix1)
		matrix_add(total_matrix2, matrix2)
	
	del pending # leave it to garbage-collector as soon as possible

	total_matrix1m = find_column_maxima(total_matrix1)
	total_matrix2m = find_column_maxima(total_matrix2)
	
	return html_all(all_nodes, expl, total_matrix1, total_matrix1m, total_matrix2, total_matrix2m, params)

'''
def aggregate(files, open_sibling, params):
	mappings, phasestack_lines = aggregate_phasestacks(map(stripped_lines_close, open_sibling('phasestack')), params)
	expl = CallTreePosition.list_from_phasestack_dump(phasestack_lines)
	phasestack_lines = len(phasestack_lines) # the contents are no longer needed (just the size)
	
	all_nodes = *** pull rabbit out of hat ***
	all_nodes_index_of = {node: idx for idx, node in enumerate(all_nodes)}
	
	total_matrix1 = make_matrix(phasestack_lines, len(all_nodes), DependencyValue(0, 0, 0))
	total_matrix2 = make_matrix(phasestack_lines, len(all_nodes), DependencyValue(0, 0, 0))
	
	for mapping, file in zip(mappings, files):
		lines = stripped_lines_close(file)
		
		# nodemat file has four segments separated by empty line
		nodes = read_classes(take_up_to_empty(lines))
		phases = list(map(int, take_up_to_empty(lines)))
		matrix1 = read_depval_matrix(take_up_to_empty(lines)) # status before given phase
		matrix2 = read_depval_matrix(take_up_to_empty(lines)) # status after given phase
		
		nodeids = range(len(nodes))
		for idx, node in enumerate(nodes):
			nodes[idx] = all_nodes_index_of[node]
	
		matrix1 = matrix_apply_mapping_to_rows(phases, matrix1, phasestack_lines, mapping, DependencyValue(0, 0, 0))
		matrix1 = matrix_apply_mapping_to_columns(nodeids, matrix1, len(all_nodes), nodes, DependencyValue(0, 0, 0))
		matrix2 = matrix_apply_mapping_to_rows(phases, matrix2, phasestack_lines, mapping, DependencyValue(0, 0, 0))
		matrix2 = matrix_apply_mapping_to_columns(nodeids, matrix2, len(all_nodes), nodes, DependencyValue(0, 0, 0))
		
		matrix_add(total_matrix1, matrix1)
		matrix_add(total_matrix2, matrix2)

	total_matrix1m = find_column_maxima(total_matrix1)
	total_matrix2m = find_column_maxima(total_matrix2)
	
	return html_all(all_nodes, expl, total_matrix1, total_matrix1m, total_matrix2, total_matrix2m, params)
'''
