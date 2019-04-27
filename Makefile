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

graal/.git:
	git submodule init graal
	git submodule update graal

mx/.git:
	git submodule init mx
	git submodule update mx

PLuG/.git:
	git submodule init PLuG
	git submodule update PLuG

PLuG/dist/PLuG.jar: PLuG/.git
	cd PLuG; ant

graal/compiler/mxbuild/dists/jdk11/graal.jar: mx/.git graal/.git
	cd graal/compiler; ../../mx/mx build; ../../mx/mx ideinit
	# if you have multiple JVM versions, this will fail
	# and give you hints how to fix it
	
graal.instrumented.jar: graal/compiler/mxbuild/dists/jdk11/graal.jar PLuG/dist/PLuG.jar blood/build/libs/blood-all.jar
	PLuG/plug.sh blood/build/libs/blood-all.jar --in graal/compiler/mxbuild/dists/jdk11/graal.jar --out graal.instrumented.jar

.PHONY: clean
clean:
	cd blood; ${GRADLE} clean
	cd graal/compiler; ../../mx/mx clean
	cd PLuG; ant clean
	rm graal.instrumented.jar

.PHONY: clean-graal
clean-graal:
	rm -rf graal/

.PHONY: clean-mx
clean-mx:
	rm -rf mx/

.PHONY: clean-plug
clean-plug:
	rm -rf PLuG/

.PHONY: clean-full
clean-full: clean-graal clean-mx clean-plug
	rm graal.instrumented.jar
	cd blood; ${GRADLE} clean

.PHONY: update-deps
update-deps: clean
	git submodule foreach git pull origin master

