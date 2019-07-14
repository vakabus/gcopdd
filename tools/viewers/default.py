from html import escape
from viewers.common import *


def html_view(data):
	yield '<pre style="background-color: lightgray">'
	yield escape(data)
	yield '</pre>'


def html_aggregate(data_it):
	yield '<pre style="background-color: lightgray">'
	for data in data_it:
		yield escape(data)
	yield '</pre>'


def read_and_close(file):
	with file:
		return file.read()


def view(file, open_sibling, params):
	return html_view(read_and_close(file))


def aggregate(files, open_sibling, params):
	return html_aggregate(map(read_and_close, files))
