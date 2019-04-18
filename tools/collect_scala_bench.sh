#!/bin/bash -e

# This script runs instrumented Graal on all ScalaBench benchmarks
# saving all the dumps into dumps/ top-level directory. As a first
# argument to this script, path to ScalaBench jar file is required.


#FIXME the benchmark does not seem to stop by itself.
#FIXME the benchmarks don't like running in our instrumented Graal
#      and they keep crashing. Can we do something with that?

if [[ $# != 1 ]]; then
	echo "Usage: $0 SCALABENCH.JAR" >&2
	exit 1
fi

project_root=$(git rev-parse --show-toplevel)
scalabench_jar="$(pwd)/$1"
benchmarks=$(java -jar "$scalabench_jar" --list-benchmarks)

cd "$project_root"

for bench in $benchmarks; do
	./vm -jar "$scalabench_jar" "$bench"
done

