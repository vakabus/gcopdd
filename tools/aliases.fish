#!/bin/fish

# Adds useful aliases to a running shell.
# The aliases exist until the shell ends.
# This file must be sourced (using 'source aliases.fish').
# Only works with fish. Requires 'readlink -f' (GNUism).

if not status is-block
	echo "This file must be sourced." >&2
	printf "Use 'source %s' instead of '%s'.\n" (status filename) (status filename) >&2
	exit 1
end

if count $argv > /dev/null
	echo "No arguments expected." >&2
	exit 1
end

if functions -q _gcopdd_alias
	echo "Aliases already defined." >&2
	exit 1
end

# Obtain absolute path of directory containing this file.
set _gcopdd_tools_dir (readlink -f (dirname (status filename)))

function _gcopdd_alias
	if type $argv[1] 2>/dev/null
		echo "Choose an alias for '$argv[2]'." >&2
		echo "Type '$argv[1]' to confirm hiding your '$argv[1]' with '$argv[2]'." >&2
		echo "Leave blank to not define an alias." >&2
		read -lP "New alias: " _gcopdd_new_alias
		string length -q $_gcopdd_new_alias; and alias $_gcopdd_new_alias $argv[2]
	else
		alias $argv
	end
end

_gcopdd_alias match      $_gcopdd_tools_dir/match.py
_gcopdd_alias phasestack $_gcopdd_tools_dir/phasestack.py
_gcopdd_alias depmat     $_gcopdd_tools_dir/depmat.py

# vm is not in tools dir, but one level higher
_gcopdd_alias vm         (dirname $_gcopdd_tools_dir)/vm
