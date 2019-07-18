from viewers.common import *
from re import compile as Regex
from collections import namedtuple
from math import log, exp, sqrt
from datetime import datetime
from html import escape

from viewers.default import view # also exported!


REQUEST_SEEN_PATTERN = Regex('\[(.*?)\]')

CompilationEvent = namedtuple('CompilationEvent', ['started', 'duration', 'request', 'recomp'])


def pretty_duration(d):
	digits = str(int(d))
	result = '&micro;s'
	while digits:
		result = digits[-3:] + ' ' + result
		digits = digits[:-3]
	return result


def html_aggregate(total_duration, duration_mean, duration_variance, duration_geometric_mean, events):
	yield 'Valid entries: <b>%s</b><br>' % len(events)
	yield 'Total duration: <b>%s</b><br>' % pretty_duration(total_duration)
	yield 'Duration mean: <b>%s</b> (&sigma; = <b>%s</b>)<br>' % (pretty_duration(duration_mean), pretty_duration(sqrt(duration_variance)))
	yield 'Duration variance (&sigma;&sup2;): <b>%s&sup2;</b><br>' % pretty_duration(duration_variance)
	yield '<del>Duration geometric mean</del>: <b>%s</b><br>' % pretty_duration(duration_geometric_mean)
	yield '<table class="mono">'
	yield '<tr><th>Request</th><th>Recompilation</th><th>Started</th><th>Duration</th></tr>'
	for event in events:
		yield '<tr><td>%s</td><td>%s/%s</td><td>%s</td><td style="text-align: right">%s</td></tr>' % (
			escape(event.request),
			event.recomp[0], event.recomp[1],
			event.started.time(),
			pretty_duration(event.duration),
		)
	yield '</table>'


def aggregate(files, open_sibling, params):
	# parse inputs
	events = []
	for timing_f, request_f in zip(files, open_sibling('request')):
		with request_f:
			try:
				started, duration = stripped_lines_close(timing_f)
			except ValueError:
				continue
			event = CompilationEvent(
				started = datetime.strptime(started, '%Y-%m-%dT%H:%M:%S.%fZ'),
				duration = int(duration),
				request = request_f.read(),
				recomp = [], # we need to sort the array first
			)
			events.append(event)

	# process data
	events.sort()
	seen = {}
	total_duration = 0
	total_square_duration = 0 # 0 ** 2
	total_log_duration = 0.0 # log(1)
	divisor = len(events) or 1 # for empty array, don't divide the total by zero
	for event in events:
		d = event.duration
		total_duration += d
		total_square_duration += d * d # d ** 2
		total_log_duration += log(d)
		seen_key = REQUEST_SEEN_PATTERN.search(event.request)
		if seen_key:
			# `seen[seen_key]` will be substituted for `seen_key`
			# as soon as we know its final value (see next loop)
			event.recomp[:] = increment_in_dict(seen, seen_key), seen_key
	for event in events:
		if event.recomp:
			event.recomp[1] = seen[event.recomp[1]]
		else:
			event.recomp[:] = '??'
	duration_mean = total_duration / divisor
	duration_variance = (total_duration*total_duration//divisor + total_square_duration) / divisor
	duration_geometric_mean = exp(total_log_duration / divisor)

	# output
	return html_aggregate(total_duration, duration_mean, duration_variance, duration_geometric_mean, events)
