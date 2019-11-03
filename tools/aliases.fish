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

if set -q _gcopdd_tools_dir
	echo "Aliases already defined." >&2
	exit 1
end

# Obtain absolute path of directory containing this file.
set _gcopdd_tools_dir (readlink -f (dirname (status filename)))

alias evtgrep $_gcopdd_tools_dir/evtgrep
alias evtinfo $_gcopdd_tools_dir/evtinfo
alias ntar $_gcopdd_tools_dir/ntar.py
alias todir $_gcopdd_tools_dir/todir

alias phasestack $_gcopdd_tools_dir/phasestack.py
alias depmat $_gcopdd_tools_dir/depmat.py

# vm is not in tools dir, but one level higher
alias vm (dirname $_gcopdd_tools_dir)/vm

function _gcopdd_complete_cond
	# this function is an approximation of
	# "user is trying to complete the first argument"
	switch (count (commandline -o))
		case 1
			true
		case 2
			string length (commandline -t)
		case '*'
			false
	end
end

complete -c evtgrep -f
complete -n _gcopdd_complete_cond -c ntar -a 'help list dump hexdump xf' -f
complete -n _gcopdd_complete_cond -c phasestack -a 'TODO' -f
complete -n _gcopdd_complete_cond -c depmat -a 'help html csv aggregate diff expand' -f
