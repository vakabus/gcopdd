from collections import namedtuple, OrderedDict
from os import listdir, path, mkdir
from html import escape
from http.server import HTTPServer, BaseHTTPRequestHandler
from importlib import import_module
from threading import Thread
from shutil import copyfileobj
from sys import argv
import traceback
import re


PATH_PATTERN = re.compile('/([^./?]+)\.([^./?]+)/([^/?]+?)(?:\.([0-9]+))?\.html')


STYLESHEET = b"""
	table.simple-border, table.simple-border td, table.simple-border th {
		border: solid 1px black;
		border-collapse: collapse;
	}
"""


DumpDict = namedtuple('DumpDict', ['all', 'by_test', 'by_type'])


Dump = namedtuple('Dump', ['test', 'date', 'type', 'ce']) # ce .. compilation event

def Dump_name(self):
	if self.ce != 0:
		return self.test + "." + self.date + "/" + self.type + "." + str(self.ce)
	else:
		return self.test + "." + self.date + "/" + self.type

Dump.name = Dump_name


def listdumps():
	for name in listdir():
		if not path.isdir(name) or name == "html":
			continue
		try:
			test, date = name.split('.')
			for filename in listdir(name):
				if "." in filename:
					type, ce_s = filename.rsplit(".", 1)
					ce = int(ce_s)
				else:
					type = filename
					ce = 0
				yield Dump(test, date, type, ce)
		except ValueError:
			# name.split didn't return 2-element array
			print('Warning: %r is not a valid report directory name. Expected "test.date"' % name)


def newestdump():
	return max(listdumps(), default=None, key=lambda dump: (dump.date, -dump.ce))


dump_dict_cache = None


def get_dump_dict():
	global dump_dict_cache
	if dump_dict_override_cache or dump_dict_cache == None:
		dump_dict_cache = get_dump_dict_inner()
	return dump_dict_cache


def get_dump_dict_inner():
	dumps = sorted(listdumps(), key=lambda dump: (dump.date, -dump.ce), reverse=True)

	by_test, by_type = OrderedDict(), OrderedDict()
	for dump in dumps:
		# For each triple (test,date,type) there
		# should be no more than one entry.
		if dump.ce != 0:
			# When dumping, we always start at 0,
			# so we can assume that for each
			# triple (test,date,type) if there
			# is any dump (test,date,type,*) then
			# there is also (test,date,type,0).
			continue

		if dump.test not in by_test:
			by_test[dump.test] = OrderedDict()
		by_date = by_test[dump.test]

		if dump.date not in by_date:
			by_date[dump.date] = []
		by_date[dump.date].append(dump)

		if dump.type not in by_type:
			by_type[dump.type] = []
		by_type[dump.type].append(dump)

	return DumpDict(dumps, by_test, by_type)


def html_link(dump, text, existing, current_dump):
	if dump[:3] == current_dump[:3]: # equal except ce
		yield '<li><a href="../%s.html" style="color: darkgreen">%s</a></li>' % (dump.name(), text)
	elif dump in existing:
		yield '<li><a href="../%s.html">%s</a></li>' % (dump.name(), text)
	else:
		yield '<li><a href="../%s.html" style="color: darkred">%s</a></li>' % (dump.name(), text)


def html_by_test(by_test, current_dump):
	for test, by_date in by_test.items():
		yield '<b>%s</b><ul>' % test
		for date, dumps in by_date.items():
			yield from html_link(Dump(test, date, current_dump.type, 0), date, dumps, current_dump)
		yield '</ul>'


def html_by_type(by_type, current_dump):
	yield '<ul>'
	for type, dumps in by_type.items():
		yield from html_link(Dump(current_dump.test, current_dump.date, type, 0), type, dumps, current_dump)
	yield '</ul>'


def html_ce_list(dumps, current_dump):
	for dump in dumps:
		if dump == current_dump:
			yield '<a href="../%s.html" style="color: darkgreen">%s</a> ' % (dump.name(), dump.ce)
		elif dump[:3] == current_dump[:3]: # equal except ce
			yield '<a href="../%s.html">%s</a> ' % (dump.name(), dump.ce)


def default_view(file):
	yield '<div style="border: solid 2px black">No viewer for %r found.</div>' % dump.type
	yield '<pre style="background-color: lightgray">'
	yield escape(file.read())
	yield '</pre>'


def html_all(dump):
	try:
		yield '<!doctype html><html><head><meta charset="utf8"><title>%s</title><link rel="stylesheet" href="../s.css"></head><body>' % dump.name()

		dumps = get_dump_dict()
		yield '<div style="position: absolute; top: 0; bottom: 0; right: 0; width: 16em; overflow: auto"><div style="margin: 1em">'
		yield from html_by_test(dumps.by_test, dump)
		yield '<hr>'
		yield from html_by_type(dumps.by_type, dump)
		yield '<hr>'
		yield from html_ce_list(dumps.all, dump)
		yield '</div></div>'

		yield '<div style="position: absolute; left: 0; top: 0; bottom: 0; right: 16em; overflow: auto"><div style="margin: 1em">'
		try:
			with open(dump.name()) as file:
				class ImportError(Exception): pass
				try:
					view = import_module('viewers.' + dump.type).view
				except ImportError:
					view = default_view
				yield from view(file)
		except FileNotFoundError:
			yield '<div style="border: solid 2px black">Dump %r not found.</div>' % dump.name()
		yield '</div></div>'

		yield '</body></html>'
	except Exception as e:
		yield '<br>... crashed with %r. See console.' % e
		traceback.print_exc()


class DumpBrowserHTTPRequestHandler(BaseHTTPRequestHandler):
	def do_GET(self):
		if '?' in self.path:
			file, params_str = self.path.split('?', 1)
		else:
			file, params_str = self.path, ''
		params = dict(param.split('=', 1) for param in params_str.split('&') if '=' in param)

		if file == '/':
			dump = newestdump()
			if dump:
				self.send_response(302)
				self.send_header('Location', self.absolute(dump.name() + '.html'))
				self.end_headers()
			else:
				self.send_response(200)
				self.send_header('Content-Type', 'text/plain')
				self.end_headers()
				self.wfile.write(b'No available dumps')
			return

		if file == '/STOP':
			self.send_response(200)
			self.send_header('Content-Type', 'text/plain')
			self.end_headers()
			self.wfile.write(b'Stopping')
			Thread(target=self.server.shutdown).start()
			return

		if file == '/s.css':
			self.send_response(200)
			self.send_header('Content-Type', 'text/css')
			self.end_headers()
			self.wfile.write(STYLESHEET)
			return

		mo = PATH_PATTERN.fullmatch(file)
		if mo:
			self.send_response(200)
			self.send_header('Content-Type', 'text/html')
			self.end_headers()
			test, date, type, ce_s = mo.group(1, 2, 3, 4)
			ce = int(ce_s) if ce_s else 0
			self.wfile.writelines(bytes(line, 'utf-8') for line in html_all(Dump(test, date, type, ce)))
			return

		self.send_response(400)
		self.send_header('Content-Type', 'text/plain')
		self.end_headers()
		self.wfile.write(b'Invalid URL')

	def absolute(self, s):
		return 'http://' + self.headers['Host'] + '/' + s


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


if len(argv) == 1:
	dump_dict_override_cache = False
	generate_all()
elif len(argv) == 2:
	dump_dict_override_cache = True
	HTTPServer(('localhost', int(argv[1])), DumpBrowserHTTPRequestHandler).serve_forever()
else:
	print('Bad invocation')
