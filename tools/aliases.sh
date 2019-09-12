#!/bin/bash

# Adds useful aliases to a running shell.
# The aliases exist until the shell ends.
# This file must be sourced (using '. aliases.sh').
# Only works with bash. Requires 'readlink -f' (GNUism).

if test -z "$BASH_VERSION"; then
	echo "Sorry, this only works in bash." >&2
	return 1 2> /dev/null
	exit 1
fi

# If executed like $ bash < aliases.sh
if [[ -z "$BASH_SOURCE" ]]; then
	echo "This file must be sourced." >&2
	echo "Use '. aliases.sh'." >&2
	exit 1
fi

# If executed like $ aliases.sh
if [[ "$BASH_SOURCE" = "$0" ]]; then
	echo "This file must be sourced." >&2
	printf "Use '. %q' instead of '%q'.\n" "$BASH_SOURCE" "$BASH_SOURCE" >&2
	exit 1
fi

if [[ $# != 0 ]]; then
	echo "No arguments expected." >&2
	return 1
fi

if declare -F _gcopdd_alias > /dev/null; then
	echo "Aliases already defined." >&2
	return 1
fi

# Obtain absolute path of directory containing this file.
_gcopdd_tools_dir="$(readlink -f "$(dirname "$BASH_SOURCE")")"

_gcopdd_alias() {
	if type $1 2>/dev/null; then
		echo "Choose an alias for '$2'." >&2
		echo "Type '$1' to confirm hiding your '$1' with '$2'." >&2
		echo "Leave blank to not define an alias." >&2
		local _gcopdd_new_alias
		read -ep "New alias: " _gcopdd_new_alias
		[[ -n "$_gcopdd_new_alias" ]] && alias $_gcopdd_new_alias=$2
	else
		alias $1=$2
	fi
}

_gcopdd_alias match      "$_gcopdd_tools_dir/match.py"
_gcopdd_alias phasestack "$_gcopdd_tools_dir/phasestack.py"
_gcopdd_alias depmat     "$_gcopdd_tools_dir/depmat.py"

# vm is not in tools dir, but one level higher
_gcopdd_alias vm         "$(dirname "$_gcopdd_tools_dir")/vm"
