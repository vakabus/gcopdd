__all__ = [
	'stripped_lines_close', 'take_up_to_empty', 'increment_in_dict', 'increment_all_in_dict',
	# HTML & CSS
	'pretty_number', 'percent_str', 'css_color', 'html_ctmode_switch',
	# Data types
	'ClassDesc', 'DependencyValue', 'TreeNode', 'Canon',
	# Reading files
	'read_classes', 'read_matrix', 'read_depval_matrix',
	# Matrices
	'make_matrix', 'matrix_dimensions', 'matrix_add', 'matrix_madd',
	# Mapping matrices
	'matrix_apply_mapping', 'matrix_apply_mapping_to_rows', 'matrix_apply_mapping_to_columns',
	# Call tree
	'read_call_tree', 'CallTreePosition', 'process_phasestack', 'aggregate_phasestacks',
	# General array algorithms
	'uniq', 'roll', 'merge',
]

from itertools import count, takewhile
from collections import namedtuple
from html import escape


def stripped_lines_close(file):
	with file:
		yield from map(str.strip, file)


def take_up_to_empty(lines):
	return takewhile(bool, lines)


def increment_in_dict(d, key):
	count = d.get(key, 0) + 1
	d[key] = count
	return count


def increment_all_in_dict(d, keys):
	for key in keys:
		increment_in_dict(d, key)


################
## HTML & CSS ##
################


def pretty_number(num):
	if num > 1000000:
		return str(round(num / 1000000)) + 'M'
	elif num > 1000:
		return str(round(num / 1000)) + 'K'
	else:
		return str(num)


def percent_str(num):
	if num > 100.0:
		return '100%'
	if num < 0.0:
		return '0%'
	return str(round(num * 100.0)) + '%'


def css_color(red, saturation, lightness):
	"""
	Usage:
		css_color(red, saturation, lightness)

	red:
		0.0 ~ green (120deg)
		1.0 ~ red (0deg)
	saturation:
		0.0 ~ gray
		1.0 ~ color
	lightness:
		0.0 ~ dark (but not black)
		1.0 ~ light (but not white)
	"""
	return 'hsl(%s, %s, %s)' % (
		int((1.0-red) * 120.0), # 0.0 .. 1.0 ---> 120 .. 0 [degrees]
		percent_str(saturation),
		percent_str(0.25 + lightness*0.5), # 0.0 .. 1.0 ---> 25% .. 75%
	)


def html_ctmode_switch(params):
	curr_mode = _get_ctmode(params)
	yield '<fieldset class="ctmode-switch">'
	yield '<legend>Call tree mode</legend>'
	for mode in ['full', 'roll', 'stack', 'top']:
		yield '<input type="radio" name="%sctmode" value="%s" id="%sctmode_%s" onclick="%s"%s>' % (
			params.cat, mode, params.cat, mode,
			"location.href = '%s'; return false" % escape(params.str_with('ctmode', mode)),
			(' checked' if mode == curr_mode else ''),
		)
		yield '<label for="%sctmode_%s"> %s</label> ' % (params.cat, mode, mode)
	yield '</fieldset>'


################
## Data types ##
################


class ClassDesc(namedtuple('ClassDesc', ['fullname', 'package', 'simplename'])):
	@staticmethod
	def parse(fullname):
		if '.' in fullname:
			package, simplename = fullname.rsplit('.', 1)
		else:
			package, simplename = '(default package)', fullname
		return ClassDesc(fullname, package, simplename)


class DependencyValue(namedtuple('DependencyValue', ['count', 'totalCount', 'iterations'])):
	@staticmethod
	def parse(string):
		return DependencyValue(*map(int, string.split(':')))

	def ratio(self):
		return self.totalCount and self.count / self.totalCount
	
	def __add__(a, b):
		return DependencyValue(a.count + b.count, a.totalCount + b.totalCount, a.iterations + b.iterations)


TreeNode = namedtuple('TreeNode', ['parent', 'desc', 'children'])


class Canon:
	def __init__(self):
		self._dict = {}

	def canonicalize(self, obj):
		return self._dict.setdefault(obj, obj)
	
	def list_(self, iterator):
		return [self.canonicalize(item) for item in iterator]


###################
## Reading files ##
###################


def read_classes(iterator):
	return [ClassDesc.parse(fullname) for fullname in iterator]


def read_matrix(iterator, read_item):
	return [[read_item(item) for item in line.split(' ')] for line in iterator]


def read_depval_matrix(iterator):
	return read_matrix(iterator, DependencyValue.parse)


##############
## Matrices ##
##############


def make_matrix(height, width, value):
	return [[value for _x in range(width)] for _y in range(height)]


def matrix_dimensions(matrix):
	height = len(matrix)
	row_widths = map(len, matrix)
	width = next(row_widths, 0)
	assert all(row_width == width for row_width in row_widths)
	return height, width


def matrix_add(result, addend):
	for y, row in enumerate(addend):
		for x, value in enumerate(row):
			result[y][x] += value


def matrix_madd(result, addend, old_indices_y, old_indices_x, ymapping, xmapping):
	for y, row in zip(old_indices_y, addend):
		for x, value in zip(old_indices_x, row):
			result[ymapping[y]][xmapping[x]] += value
	return result


######################
## Mapping matrices ##
######################


def matrix_apply_mapping(old_indices, matrix, newsize, mapping, start):
	height, width = matrix_dimensions(matrix)
	assert len(old_indices) == width == height
	result = make_matrix(newsize, newsize, start)
	for y, row in zip(old_indices, matrix):
		for x, value in zip(old_indices, row):
			result[mapping[y]][mapping[x]] += value
	return result


def matrix_apply_mapping_to_rows(old_indices, matrix, newsize, mapping, start):
	height, width = matrix_dimensions(matrix)
	assert len(old_indices) == height
	result = make_matrix(newsize, width, start)
	for y, row in zip(old_indices, matrix):
		for x, value in enumerate(row):
			mapping[y]
			result[mapping[y]][x] += value
	return result


def matrix_apply_mapping_to_columns(old_indices, matrix, newsize, mapping, start):
	height, width = matrix_dimensions(matrix)
	assert len(old_indices) == width
	result = make_matrix(height, newsize, start)
	for y, row in enumerate(matrix):
		for x, value in zip(old_indices, row):
			result[y][mapping[x]] += value
	return result


###############
## Call tree ##
###############


def _get_ctmode(params):
	return params.get('ctmode', 'roll')


def _first_difference(a, b):
	for i, (a_i, b_i) in enumerate(zip(a, b)):
		if a_i != b_i:
			return i
	return min(len(a), len(b))


def read_call_tree(lines):
	stack = []
	root = TreeNode(None, None, [])
	currnode = root

	for num, line in enumerate(lines):
		newstack = [ClassDesc.parse(fullname) for fullname in line.split()]
		first_difference = _first_difference(stack, newstack)
		for _ in range(len(stack) - first_difference):
			# pop
			currnode = currnode.parent
		for item in newstack[first_difference:]:
			# push `item`
			newclassdesc = item
			newnode = TreeNode(currnode, newclassdesc, [])
			currnode.children.append(newnode)
			currnode = newnode
		# add a tag saying: "phase id X points here"
		currnode.children.append(TreeNode(currnode, num, []))
		stack = newstack

	return root


class CallTreePosition:
	def __init__(self, id):
		self.id = id
		self.inside = None
		self.before = None
		self.after = None
	
	def desc(self):
		if self.inside:
			desc = 'Inside ' + self.inside.simplename
		else:
			desc = 'Top-level'
		if self.before and self.after:
			desc += '\nBetween ' + self.after.simplename
			desc += '\nand ' + self.before.simplename
		elif self.before:
			desc += '\nBefore ' + self.before.simplename
		elif self.after:
			desc += '\nAfter ' + self.after.simplename
		return desc
	
	@staticmethod
	def list_from_phasestack_dump(lines):
		result = [CallTreePosition(i) for i in range(len(lines))]

		def walk_subtree(parent):
			last = None
			for this in parent.children:
				if isinstance(this.desc, ClassDesc):
					if isinstance(last, int):
						result[last].before = this.desc
						last = None
					walk_subtree(this)
				elif isinstance(this.desc, int):
					if isinstance(last, ClassDesc):
						result[this.desc].after = last
						last = None
					result[this.desc].inside = parent.desc
				else:
					raise TypeError()
				last = this.desc

		walk_subtree(read_call_tree(lines))
		return result


def process_phasestack(lines, params, canon=None):
	ctmode = _get_ctmode(params)
	if ctmode == 'roll':
		lines = (canon or Canon()).list_(lines)
		mapping = roll(lines)
		return mapping, lines
	list_ = canon.list_ if canon else list
	if ctmode == 'top':
		lines = list_((line.split() or ['Top-level'])[-1] for line in lines)
		mapping = uniq(lines)
		return mapping, lines
	lines = list_(lines)
	if ctmode == 'stack':
		mapping = uniq(lines)
		return mapping, lines
	return list(range(len(lines))), lines

def aggregate_phasestacks(orig_lines2, params):
	# 1) initialization
	ctmode = _get_ctmode(params)
	canon = Canon()
	# 2) orig_lines2 -> lines2, mappings'
	#    - apply the transformation (dictated by `ctmode`)
	#      to each input `phasestack` individually
	mappings = []
	lines2 = []
	for orig_lines in orig_lines2:
		mapping, lines = process_phasestack(orig_lines, params, canon)
		mappings.append(mapping)
		lines2.append(lines)
	del orig_lines2
	# 3) lines2, mappings' -> lines2, mappings', result
	#    - merge the transformed inputs into one output
	result = []
	for lines in _rm_subseq(lines2):
		result = merge(result, lines)
	# 4) lines2, mappings', result -> mappings, result
	#    - calculate the corresponding mappings of the inputs
	for mapping, lines in zip(mappings, lines2):
		_subseq_mapping(result, lines)
		for i, mapping_i in enumerate(mapping):
			mapping[i] = lines[mapping_i]
	return mappings, result


def _rm_subseq(sequences):
	return _rm_subseq_recursive(iter(sorted(sequences, key=len, reverse=True)))

def _rm_subseq_recursive(sequences):
	head = next(sequences, None)
	if head is not None:
		yield head
		yield from _rm_subseq_recursive(item for item in sequences if not _is_subseq(head, item))

def _is_subseq(longer, shorter):
	difference = len(longer) - len(shorter)
	skipped = 0
	longer_iter = iter(longer)
	for shorter_elem in shorter:
		while next(longer_iter) is not shorter_elem:
			skipped += 1
			if skipped > difference:
				# There are more elements remaining in `shorter`
				# than in `longer`, so it cannot be its subsequence.
				return False
	# We have successfully exhausted `shorter` by
	# matching its elements with those of `longer`.
	return True

def _subseq_mapping(longer, shorter):
	assert _is_subseq(longer, shorter)
	longer_index = 0
	longer_iter = iter(longer)
	for idx, shorter_elem in enumerate(shorter):
		while next(longer_iter) is not shorter_elem:
			longer_index += 1
		shorter[idx] = longer_index
		longer_index += 1


##############################
## General array algorithms ##
##############################


def uniq(array):
	mapping = [...] * len(array)
	positions = {}
	out_idx = 0
	for in_idx, elem in enumerate(array):
		mapping[in_idx] = pos = positions.setdefault(elem, out_idx)
		if pos == out_idx:
			array[out_idx] = elem
			out_idx += 1
	del array[out_idx:]
	return mapping


def roll(array):
	# Mark the original end of the array.
	size = len(array)
	# Create a mapping from the indices to the original array to the indices to the new one.
	# Initially, it is just an identity.
	mapping = list(range(size))
	mapping_change = [...] * size
	# Try all possible segment sizes separately.
	for seg in count(1):
		# Segments that are greater than half of the array definitely aren't repeated.
		if seg >= size//2:
			break
		# Number of consecutive pairs `(a, b)` such that `a + seg == b and array[a] == array[b]`.
		# If this number reaches `seg`, this means that we have found a repeated segment of size `seg`.
		same = 0
		# This loop copies data from `in_idx` to `out_idx`. Once a repeated segment is detected,
		# the `out_idx` is rolled back before the first occurence of the repeated segment,
		# so it can be overwritten by the second occurence (without any effect).
		out_idx = 0
		for in_idx in range(size - seg):
			array[out_idx] = array[in_idx]
			mapping_change[in_idx] = out_idx # note down how the mapping should be changed
			out_idx += 1
			if array[in_idx] is array[in_idx + seg]:
				same += 1
				# The following condition in this context is equivalent to
				# `array[in_idx-seg : in_idx] == array[in_idx : in_idx+seg]`
				# but it is asymptotically faster.
				if same == seg:
					# Roll back output (as stated in comment before the loop).
					out_idx -= seg
					# Reset the counter (because the items it applied to are overwritten).
					same = 0
			else:
				# Reset the counter (because this failed
				# condition interrupted the identical segments).
				same = 0
		# Copy the last segment. There is no following segment against which it should be checked.
		for in_idx in range(size - seg, size):
			array[out_idx] = array[in_idx]
			mapping_change[in_idx] = out_idx # note down how the mapping should be changed
			out_idx += 1
		# Update mapping accordingly.
		for index, old_mapping in enumerate(mapping):
			mapping[index] = mapping_change[old_mapping]
		# Mark the new end of the array.
		size = out_idx
	del array[size:]
	return mapping

def _prepend(old_list, item):
	size, tail = old_list
	return (size + 1, (item, tail))

def merge(a, b):
	# Notation: indexing with -1 in comments does NOT denote the last element,
	# but rather an imaginary before-zeroth element. To avoid confusion, the code will
	# use the following constant to denote the last element.
	LAST = -1 # Example: `array = [x, y, z]`, `array[LAST] == z`
	
	# Handle special cases:
	if not a or not b:
		return a + b
	
	# Plan:
	#   1) Create a table (2d array) `best[][]`, which at any pair of indices `(ai, bi)` holds the best way to
	#      merge `a[:ai+1]` and `b[:bi+1]` - in other words `best[ai-1][bi-1] == merge(a[:ai], b[:bi])`.
	#   2) Return `best[len(a)-1][len(b)-1]`, because
	#      `merge(a, b) == merge(a[:len(a)], b[:len(b)]) == best[len(a)-1][len(b)-1]`.
	#
	# Corner cases:
	#   - `best[-1][-1] == merge([], []) == []`
	#   - `best[-1][bi] == merge([], b[:bi]) == b[:bi]`
	#   - `best[ai][-1] == merge(a[:ai], []) == a[:ai]`
	#
	# We will build the table inductively, reusing previously calculated values for the calculation of new ones.
	# This is for some reason called dynamic programming (DP).
	#
	# For memory efficiency, we make following changes to the algorithm described above:
	# We will not remember the whole table `best[][]`, but just two columns/rows (with fixed `ai`) at time.
	# They will be denoted `best_ai == best[ai]` and `best_aim1 == best[ai-1]`.
	#
	# Moreover, we define following variables:
	#   - `best_ai_bi == best[ai][bi]`
	#   - `best_ai_bim1 == best[ai][bi-1]`
	#   - `best_ai_m1 == best[ai][-1]`
	#   - `best_aim1_bim1 == best[ai-1][bi-1]`
	#
	# The table items will not be arrays (because that would involve much memory copying),
	# but rather a sized linked list of items of the merged sequence in reverse order, like this:
	# `(3, (z, (y, (x, ()))))` ~ [x, y, z]
	# To manipulate these lists, we use `_prepend`:
	
	best_ai = [...] * len(b)
	# For `ai = -1`, `bi = -1`:
	best_ai_m1 = best_ai_bi = (0, ())
	# For `ai = -1`, `bi >= 0`:
	for bi, b_elem in enumerate(b):
		best_ai[bi] = best_ai_bi = _prepend(best_ai_bi, b_elem) # redefine `best_ai_bi` using previous value
	
	# Hack 1: store both columns/rows in one array (this works thanks to the way they are accessed)
	best_aim1 = best_ai
	
	# For `ai >= 0`:
	for a_elem in a:
		best_aim1_m1 = best_ai_m1
		best_ai_m1 = _prepend(best_aim1_m1, a_elem)
		# For `bi = 0` (note: `bi - 1 = -1`):
		best_aim1_bim1 = best_aim1_m1
		best_ai_bim1 = best_ai_m1
		# For `bi >= 0` (note: this includes `bi = 0`):
		for bi, b_elem in enumerate(b):
			if a_elem is b_elem:
				# merge `a[:ai]` and `b[:bi]`, then add `a[ai]`, equivalently `b[bi]`
				best_ai_bi = _prepend(best_aim1_bim1, a_elem)
			else:
				# use shorter merge, or - if equal in length - lexicographically greater one
				best_ai_bi = min(
					# merge `a[:ai]` and `b[:bi+1]`, then add `a[ai]`
					_prepend(best_aim1[bi], a_elem),
					# merge `a[:ai+1]` and `b[:bi]`, then add `b[bi]`
					_prepend(best_ai_bim1, b_elem),
				)
			# save the PREVIOUS value
			if bi:
				best_ai[bi-1] = best_ai_bim1
			# prepare for next iteration
			best_ai_bim1 = best_ai_bi
			best_aim1_bim1 = best_aim1[bi]
		# For `bi = len(b)` (note: out of bounds):
		# save the PREVIOUS value
		best_ai[LAST] = best_ai_bim1 # `best_ai[LAST]` ~ `best_ai[len(b)-1]`
		# These lines are not needed (see above, Hack 1):
		#   - `best_aim1 = best_ai`
		#   - `best_ai = <allocate new array>`
	
	# the resulting linked list is in `best[len(a)-1][len(b)-1]`
	# For `ai = len(a)` (thus `len(a)-1 == ai-1`):
	# the resulting linked list is in `best_aim1[len(b)-1]`
	# or equivalently `best_aim1[LAST]`
	# but that was assigned from `best_ai_bim1`, so we will copy-propagate
	length, tail = best_ai_bim1
	
	# "; DROP TABLE best; --
	del best_ai, best_aim1, best_ai_bim1, best_ai_bi, best_ai_m1, best_aim1_bim1
	# garbage from for-loops
	del a_elem, bi, b_elem
	
	# convert the linked list to a simple python list (array)
	result = [...] * length
	while tail:
		assert length >= 0
		length -= 1
		result[length], tail = tail
	
	assert _is_subseq(result, a)
	assert _is_subseq(result, b)
	
	return result
