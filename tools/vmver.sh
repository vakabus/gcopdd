#!/bin/bash

##
## First argument is required.
##

if [[ $# == 0 ]]
then
	echo "vmver.sh: Runs a java program with specified"
	echo "version of instrumented Graal JIT compiler."
	echo
	echo "Usage:"
	echo "    $0 COMMIT[,...] [VM ARGS ...]"
	echo "  If args are missing, they are read from stdin"
	echo "    and parsed by $SHELL. Each line will be run"
	echo "    in separate VM. Newlines cannot be escaped."
	echo
	echo "Example 1:"
	echo "    $0 vm-19.0.2 tests/Fibonacci.java"
	echo
	echo "Example 2:"
	echo "    $0 master,012ac1e -jar ~/scalabench.jar"
	echo
	echo "Example 3:"
	echo "    $0 012ac1e"
	echo "  stdin:"
	echo "    tests/Fibonacci.java"
	echo "    -jar ~/scalabench.jar"
	echo "    -jar 'Minecraft Launcher (1).jar'"
	echo "    /tmp/TooVerbose.java > /dev/null"
	exit 1
fi

##
## Find all necessary paths.
##

tools="`dirname "$0"`"
gcopdd="$tools/.."
graal="$gcopdd/graal"
vm="$gcopdd/vm"

##
## Pop first argument.
## "$@" now contains VM arguments.
##

commits="$1"
shift 1

##
## For each commit, execute the VM command[s].
##

if [[ $# == 0 ]]
then
	# There were no VM args. Read all VM commands from stdin.
	commands="`cat`"
	# Commits are separated by comma.
	IFS=','
	for commit in $commits
	do
		git -C "$graal" checkout "$commit" --
		make -C "$gcopdd"
		# Remove unsafe characters.
		scommit="`echo -n "$commit" | tr -c '0-9A-Z_a-z' '-'`"
		# Commands are separated by newline.
		IFS=$'\n'
		for command in $commands
		do
			# We need to parse the command by shell.
			"$SHELL" -c "'$vm' '-Dblood.rename=$scommit.%s' $command"
		done
	done
else
	# Commits are separated by comma.
	IFS=','
	for commit in $commits
	do
		git -C "$graal" checkout "$commit" --
		make -C "$gcopdd"
		# Remove unsafe characters.
		scommit="`echo -n "$commit" | tr -c '0-9A-Z_a-z' '-'`"
		# The arguments in "$@" are already parsed.
		"$vm" "-Dblood.rename=$scommit.%s" "$@"
	done
fi
