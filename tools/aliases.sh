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

if [[ -n ${_gcopdd_tools_dir+defined} ]]; then
	echo "Aliases already defined." >&2
	return 1
fi

# Obtain absolute path of directory containing this file.
_gcopdd_tools_dir="$(readlink -f "$(dirname "$BASH_SOURCE")")"

alias evtgrep="$_gcopdd_tools_dir/evtgrep"
alias evtinfo="$_gcopdd_tools_dir/evtinfo"
alias ntar="$_gcopdd_tools_dir/ntar.py"
alias todir="$_gcopdd_tools_dir/todir"

alias phasestack="$_gcopdd_tools_dir/phasestack.py"
alias depmat="$_gcopdd_tools_dir/depmat.py"

# vm is not in tools dir, but one level higher
alias vm="$(dirname "$_gcopdd_tools_dir")/vm"

_gcopdd_complete() {
	# $1 .. command
	# $2 .. current word
	# $COMP_CWORD .. current word index
	if [[ $COMP_CWORD != 1 ]]
	then
		COMPREPLY=($(compgen -f "$2"))
	else
		case $1 in
			(ntar) COMPREPLY=($(compgen -W "help list dump hexdump xf" "$2"));;
			(phasestack) COMPREPLY=($(compgen -W "TODO" "$2"));;
			(depmat) COMPREPLY=($(compgen -W "help html csv aggregate diff expand" "$2"));;
		esac
	fi
}

complete evtgrep # disable file completions
complete -o filenames -F _gcopdd_complete ntar phasestack depmat
