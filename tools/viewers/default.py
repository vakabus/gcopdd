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


STYLESHEET = b'''
	body {
		white-space: nowrap;
	}
	.here {
		list-style-type: none;
	}
	.here div {
		position: relative;
		top: -7em;
	}
	.here div:target::before {
		content: '< here';
		position: relative;
		left: -1em;
		top: 7em;
		color: red;
		font-weight: bold;
	}
	.mono {
		font-family: monospace;
	}
	.fromto div {
		display: none;
	}
	.fromto:active div, .fromto div:hover {
		display: block;
		width: 0;
		height: 0;
		position: relative;
		outline: solid 1px black;
	}
	.fromto div a {
		display: block;
		background-color: black;
		width: 3em;
		height: 1.5em;
	}
	.clickable-cells td {
		cursor: pointer;
	}
	.nolink {
		color: unset;
		text-decoration: unset;
		display: block;
	}
	fieldset.ctmode-switch {
		display: inline-block;
		margin: 0 1em 1em 0;
	}
	table fieldset.ctmode-switch {
		margin: 1em;
	}
'''
