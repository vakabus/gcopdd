from html import escape
from viewers.common import *


def view(file):
	yield '<pre style="background-color: lightgray">'
	yield escape(file.read())
	yield '</pre>'
