from collections import namedtuple


ClassDesc = namedtuple('ClassDesc', ['index', 'fullname', 'package', 'simplename'])

def ClassDesc_parse(fullname, index=None):
	package, simplename = fullname.rsplit('.', 1)
	return ClassDesc(index, fullname, package, simplename)

ClassDesc.parse = ClassDesc_parse


def read_classes(iterator):
	return [ClassDesc.parse(fullname, lineno) for lineno, fullname in enumerate(iterator)]


def read_matrix(iterator, item_conv):
	return [[item_conv(item) for item in line.split(' ')] for line in iterator]


DependencyValue = namedtuple('DependencyValue', ['count', 'totalCount', 'iterations'])
DependencyValue.ratio = lambda dv: dv.totalCount and dv.count / dv.totalCount


def read_depval_matrix(iterator):
	return read_matrix(iterator, lambda item: DependencyValue(*map(int, item.split(':'))))


def pretty_number(num):
	if num > 1000000:
		return str(round(num / 1000000)) + "M"
	elif num > 1000:
		return str(round(num / 1000)) + "K"
	else:
		return str(num)


nonempty = bool
