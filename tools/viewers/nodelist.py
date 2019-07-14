from viewers.common import *


def html_view(classes):
	yield '<ul>'
	for desc in classes:
		yield '<li>'
		yield '<span style="color: gray">%s.</span>%s' % (desc.package, desc.simplename)
		yield '</li>'
	yield '</ul>'


def html_aggregate(counts_and_classes):
	yield '<ul>'
	for count, desc, ratio in counts_and_classes:
		yield '<li>'
		yield '%s (%s) <span style="color: gray">%s.</span>%s' \
			% (percent_str(ratio), pretty_number(count), desc.package, desc.simplename)
		yield '</li>'
	yield '</ul>'


def view(file, open_sibling, params):
	lines = stripped_lines_close(file)
	classes = read_classes(lines)
	return html_view(classes)


def aggregate(files, open_sibling, params):
	d = {}
	total = 0
	for file in files:
		lines = stripped_lines_close(file)
		classes = read_classes(lines)
		increment_all_in_dict(d, classes)
		total += 1
	return html_aggregate(sorted(((count, cls, count/total) for cls, count in d.items()), reverse=True))
