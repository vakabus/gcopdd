#!/bin/bash

# This script runs instrumented Graal on all ScalaBench benchmarks
# saving all the dumps into dumps/ top-level directory. As a first
# argument to this script, path to ScalaBench jar file is required.


#FIXME validate arguments
#FIXME the benchmark does not seem to stop by itself.
#FIXME the benchmarks don't like running in our instrumented Graal
#      and they keep crashing. Can we do something with that?

project_root=$(git rev-parse --show-toplevel)
scalabench_jar="$(pwd)/$1"
benchmarks=$(java -jar "$scalabench_jar" --list-benchmarks)

cd "$project_root"
mkdir -p "dumps"

for bench in $benchmarks; do
	name="${bench}_depmat_$(date "+%Y-%m-%d_%H-%M-%S")"
	./vm -jar "$scalabench_jar" "$bench"
	mv /tmp/gcopdd-depmat dumps/"$name"
done

