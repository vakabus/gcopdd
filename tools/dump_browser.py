from html import escape
from http.server import HTTPServer, BaseHTTPRequestHandler
from importlib import import_module
from os import listdir, path, mkdir
from sys import argv, stderr
from threading import Thread
import gzip
import traceback


#####################
## Utility methods ##
#####################


def fsplit(string, delim, count):
	count -= string.count(delim)
	if count < 0:
		raise Exception('String has too many parts')
	return string.split(delim) + count*['']


def warn(msg):
	print('WARNING: ' + msg, file=stderr)


################
## Filesystem ##
################


DECODERS = {None: open, 'gz': gzip.open}
EXCLUDE_DUMPS = {'html'}
MAIN_TYPE = 'request'


def is_dump(name):
	return name not in EXCLUDE_DUMPS and path.isdir(name)


def list_dumps():
	return sorted(filter(is_dump, listdir()), reverse=True)


def get_newest_dump():
	return max(filter(is_dump, listdir()), default=None)


def list_dump_entries(dump):
	events = set()
	types = set()
	total = 0
	for entry_name in listdir(dump):
		try:
			event, type, _encoding = fsplit(entry_name, '.', 2)
			if not event or not type: raise Exception()
		except Exception:
			warn('Invalid entry name: ' + entry_name)
			continue
		events.add(event)
		types.add(type)
		total += 1
	if total == 0:
		warn('No entries found')
	# this should not happen. the condition is just sort
	# of "checksum". it does not find all inconsistencies
	# (e.g. one entry is duplicate and another missing)
	if len(events) * len(types) != total:
		warn('%i events * %i types != %i entries' % (len(events), len(types), total))
	return events, types


def get_dump_entry_filename(dump, event, type, encoding):
	if encoding is None:
		basename = event + '.' + type
	else:
		basename = event + '.' + type + '.' + encoding
	return path.join(dump, basename)


def open_dump_entry(dump, event, type):
	for encoding in DECODERS:
		filename = get_dump_entry_filename(dump, event, type, encoding)
		if path.exists(filename):
			return DECODERS[encoding](filename, 'rt')


def get_event_name(dump, event):
	file = open_dump_entry(dump, event, MAIN_TYPE)
	if file is None:
		return '???'
	name = file.read()
	file.close()
	return name


def get_view_function(type):
	try:
		return import_module('viewers.' + type).view
	except ImportError:
		return import_module('viewers.default').view


################
## HTML & CSS ##
################


STYLESHEET = b"""
	div#header {
		position: fixed;
		left: 0;
		top: 0;
		width: 100%;
		height: 3em;
		line-height: 3em;
		background: lightgray;
		overflow: hidden;
	}
	div#header-placeholder {
		height: 3em;
	}
	div#header select {
		width: 20em;
		margin-left: 1em;
	}
	div#header a {
		margin-left: 1em;
	}
	.current {
		color: darkgreen;
	}
	table.simple-border, table.simple-border td, table.simple-border th {
		border: solid 1px black;
		border-collapse: collapse;
	}
	.here:target::before {
		content: '< here';
		position: relative;
		left: -1em;
		top: 3em;
		color: red;
		font-weight: bold;
	}
	.here {
		position: relative;
		top: -3em;
	}
	.mono {
		font-family: monospace;
	}
	.fromto {
		display: none;
	}
	td:active .fromto, .fromto:hover {
		display: block;
		width: 0;
		height: 0;
		position: relative;
		outline: solid 1px black;
	}
	.fromto a {
		display: block;
		background-color: black;
		width: 3em;
		height: 1.5em;
	}
	.clickable-cells td {
		cursor: pointer;
	}
"""


def html_select(name, options, selected, pref, suff):
	yield '<select name="%s" onchange="location.href = this.value">' % name
	for value, text in options:
		if value == selected:
			yield '<option value="%s%s%s" selected>%s</option>' % (pref, value, suff, escape(text))
		else:
			yield '<option value="%s%s%s">%s</option>' % (pref, value, suff, escape(text))
	yield '</select>'


def html_all(dumps, events, types, dump, event, type, inner_html_generator):
	yield '<!doctype html><html><head><meta charset="utf8"><title>Dump browser</title>'
	yield '<link rel="stylesheet" href="../s.css"></head><body>'

	yield '<div id="header">'
	yield from html_select('dump', dumps, dump, '../', '/_.%s.html' % type)
	yield from html_select('event', events, event, '', '.%s.html' % type)
	for value, text in types:
		if value == type:
			yield '<a href="%s.%s.html" class="current">%s</a>' % (event, value, text)
		else:
			yield '<a href="%s.%s.html">%s</a>' % (event, value, text)
	yield '</div>'
	
	yield '<div id="header-placeholder"></div>'

	yield from inner_html_generator

	yield '</body></html>'


def html_exception(e):
	yield '<br>... crashed with %r. See console.' % e


def html_nonexistent():
	yield 'No such dump entry found.'


def html_notype():
	yield 'Choose dump type from the navigation bar.'


#####################
## Viewer dispatch ##
#####################


def catch_errors(function):
	try:
		yield from function()
	except Exception as e:
		traceback.print_exc()
		yield from html_exception(e)


def get_inner_html_generator(dump, event, type):
	def get_sibling(type):
		return open_dump_entry(dump, event, type)
	if type == '_':
		return html_notype()
	if event == '_':
		return ("TODO aggregation") # TODO
	file = open_dump_entry(dump, event, type)
	if file is None:
		return html_nonexistent()
	return catch_errors(lambda: get_view_function(type)(file, get_sibling, event))


def neco(dump, event, type):
	dumps = list_dumps()
	events, types = list_dump_entries(dump)
	
	# replace plain lists with `(value, text)` pairs
	# where `value` is the internal representation
	# and `text` is the user-friendly representation
	dumps = ((dump, dump.replace('.', ' --- ')) for dump in dumps)
	events = [('_', 'Aggregated results, statistics')] \
		+ sorted((event, event.upper() + ' --- ' + get_event_name(dump, event)) for event in events)
	types = sorted((type, type) for type in types)
	
	return html_all(dumps, events, types, dump, event, type, get_inner_html_generator(dump, event, type))


#################
## HTTP Server ##
#################


class DumpBrowserHTTPRequestHandler(BaseHTTPRequestHandler):
	def do_GET(self):
		file, params = fsplit(self.path, '?', 1)
		
		special_paths = {
			'/': self.do_GET_root,
			'/STOP': self.do_GET_STOP,
			'/s.css': self.do_GET_css,
		}
		
		if file in special_paths:
			special_paths[file]()
			return
		
		empty, dump, entry = fsplit(file, '/', 2)
		if empty != '':
			self.send_error(400, explain='Path must start with "/"')
			return
		
		event, type, html = fsplit(entry, '.', 2)
		if html != 'html' or not dump or not event or not type:
			self.send_error(404, explain='Usage: "http://HOST/DUMP/EVENT_HASH.TYPE.html"')
			return
		
		self.send_response(200)
		self.send_header('Content-Type', 'text/html')
		self.end_headers()
		self.wfile.writelines(bytes(line, 'utf-8') for line in neco(dump, event, type))
	
	def do_GET_root(self):
		dump = get_newest_dump()
		if dump:
			self.send_response(302)
			self.send_header('Location', self.absolute(dump + '/_._.html'))
			self.end_headers()
		else:
			self.send_error(204, explain='No available dumps')
	
	def do_GET_STOP(self):
		self.send_error(202, explain='Stopping')
		Thread(target=self.server.shutdown).start()
	
	def do_GET_css(self):
		self.send_response(200)
		self.send_header('Content-Type', 'text/css')
		self.end_headers()
		self.wfile.write(STYLESHEET)
	
	def absolute(self, s):
		return 'http://' + self.headers['Host'] + '/' + s


#####################
## Producing files ##
#####################


def create_html_dir():
	i = 0
	name = 'html'
	while True:
		try:
			mkdir(name)
			return name
		except FileExistsError:
			i += 1
			name = 'html.' + str(i)


def generate_all():
	dumps = get_dump_dict().all
	dir = create_html_dir()
	for dump in dumps:
		name = dir + '/' + dump.name() + '.html'
		try:
			mkdir(path.dirname(name))
		except FileExistsError:
			pass
		with open(name, 'w') as file:
			file.writelines(html_all(dump))


##########
## Main ##
##########


if len(argv) == 1:
	TODO # TODO
	dump_dict_override_cache = False
	generate_all()
elif len(argv) == 2:
	dump_dict_override_cache = True
	HTTPServer(('localhost', int(argv[1])), DumpBrowserHTTPRequestHandler).serve_forever()
else:
	print('Bad invocation')
