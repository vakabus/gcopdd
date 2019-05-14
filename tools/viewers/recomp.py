from collections import namedtuple
from datetime import datetime
from math import log
from viewers_common import *


CompilationEvent = namedtuple('CompilationEvent', ['method', 'recompNumber', 'phases', 'started', 'finished', 'duration', 'logdur'])

def CompilationEvent_parse(line):
	method, recompNumber_s, phases_s, started_s, finished_s = line.rsplit(' ', 4)
	recompNumber = int(recompNumber_s)
	phases = int(phases_s)
	started = datetime.strptime(started_s, '%Y-%m-%dT%H:%M:%S.%fZ')
	finished = datetime.strptime(finished_s, '%Y-%m-%dT%H:%M:%S.%fZ')
	duration = finished - started
	logdur = log(duration.total_seconds())
	return CompilationEvent(method, recompNumber, phases, started, finished, duration, logdur)

CompilationEvent.parse = CompilationEvent_parse


def view(lines_n):
	lines = map(str.strip, lines_n) # remove '\n' characters
	events = [CompilationEvent.parse(line) for line in lines]

	if not events:
		yield '(empty)'
		return

	min_recompNumber = min(e.recompNumber for e in events)
	min_phases = min(e.phases for e in events)
	min_logdur = min(e.logdur for e in events)
	max_recompNumber = max(e.recompNumber for e in events)
	max_phases = max(e.phases for e in events)
	max_logdur = max(e.logdur for e in events)
	range_recompNumber = max_recompNumber - min_recompNumber
	range_phases = max_phases - min_phases
	range_logdur = max_logdur - min_logdur

	yield '<table class="simple-border">'
	for event in events:
		rel_recompNumber = (event.recompNumber-min_recompNumber) / range_recompNumber
		rel_phases = (event.phases-min_phases) / range_phases
		rel_logdur = (event.logdur-min_logdur) / range_logdur
		nhue = 0
		hue_phases = int((1.0-rel_phases) * 120) # 0..120 ~ red..green
		hue_logdur = int((1.0-rel_logdur) * 120) # 0..120 ~ red..green
		nsat = 0
		sat = 100 # 0%..100% ~ gray..color
		lit_recompNumber = int(75 - rel_recompNumber*50) # 25%..75% ~ dark..light
		lit = 50 # 0%..100% ~ black..white
		color_recompNumber = 'hsl(%i, %i%%, %i%%)' % (nhue, nsat, lit_recompNumber)
		color_phases = 'hsl(%i, %i%%, %i%%)' % (hue_phases, sat, lit)
		color_logdur = 'hsl(%i, %i%%, %i%%)' % (hue_logdur, sat, lit)
		yield '<tr>'
		yield '<td>%s</td>' % event.method
		yield '<td style="background-color: %s">%i</td>' % (color_recompNumber, event.recompNumber)
		yield '<td style="background-color: %s">%i</td>' % (color_phases, event.phases)
		yield '<td>%s</td>' % event.started.time()
		yield '<td>%s</td>' % event.finished.time()
		yield '<td style="background-color: %s">%s</td>' % (color_logdur, event.duration)
		yield '</tr>'
	yield '</table>'
