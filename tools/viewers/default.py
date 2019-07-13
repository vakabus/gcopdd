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


def view(file, get_sibling, params):
	return html_view(file.read())


def aggregate(files, get_sibling, params):
	return html_aggregate(file.read() for file in files)
