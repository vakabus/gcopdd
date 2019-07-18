ifeq ($(CI), true)
 $(info Running in CI, using Gradle wrapper...)
 GRADLE=./gradlew
else
 GRADLE=gradle
endif

.PHONY: build
build: graal.instrumented.jar

blood/build/libs/blood-all.jar: $(shell find blood/src/)
	cd blood; ${GRADLE} shadowJar

PLuG/dist/PLuG.jar:
	rm -rf PLuG/
	git clone https://github.com/rqu/PLuG.git
	cd PLuG; ant

graal/.git:
	@echo "Please clone Graal in the root directory and checkout"
	@echo "the commit you want to collect data on."
	@echo ""
	@echo "Example:"
	@echo "> git clone https://github.com/graalvm/graal.git"
	@echo "> cd graal; git checkout befc159f4898e81648f4"
	@echo ""
	exit

mx/mx:
	rm -rf mx/
	git clone --depth 1 https://github.com/graalvm/mx.git

graal/compiler/mxbuild/dists/jdk11/graal.jar: graal/.git mx/mx
	cd graal/compiler; ../../mx/mx build
	# if you have multiple JVM versions, the above fails
	# and gives you hints how to fix it.
	-cd graal/compiler/mxbuild/dists; ln -s -r . jdk11
	# `mx` may have decided `graal.jar` is up to date.
	# touch it so that `make` knows next time.
	touch -c graal/compiler/mxbuild/dists/jdk11/graal.jar
	
graal.instrumented.jar: graal/compiler/mxbuild/dists/jdk11/graal.jar PLuG/dist/PLuG.jar blood/build/libs/blood-all.jar
	PLuG/plug.sh blood/build/libs/blood-all.jar --in graal/compiler/mxbuild/dists/jdk11/graal.jar --out graal.instrumented.jar

.PHONY: clean
clean:
	cd blood; ${GRADLE} clean
	-rm graal.instrumented.jar

.PHONY: clean-full
clean-full:
	rm -rf PLuG/
	rm -rf graal/
	rm -rf mx/
	mkdir PLuG
	mkdir graal
	mkdir mx
	-rm graal.instrumented.jar
	cd blood; ${GRADLE} clean

