blood/build/libs/blood-all.jar: $(shell find blood/src/)
	cd blood; ./gradlew shadowJar

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

.PHONY: build
build: graal.instrumented.jar

.PHONY: clean
clean:
	cd blood; ./gradlew clean
	cd graal/compiler; mx clean
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

.PHONY: update-deps
update-deps: clean
	git submodule update

# source: https://stackoverflow.com/questions/2214575/passing-arguments-to-make-run#14061796
# If the first argument is "vm"...
ifeq (vm,$(firstword $(MAKECMDGOALS)))
  # use the rest as arguments for "vm"
  VM_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  # ...and turn them into do-nothing targets
  $(eval $(VM_ARGS):;@:)
endif

.PHONY: vm
vm: graal.instrumented.jar
	@echo Testing, wheter Java binary is of correct version...
	java -version 2>&1 | head -n1 | grep version\ \"11 > /dev/null
	@echo Running Java with instrumented Graal
	java -server -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI\
		--module-path=graal/sdk/mxbuild/dists/jdk11/graal-sdk.jar:graal/truffle/mxbuild/dists/jdk11/truffle-api.jar\
		--upgrade-module-path=graal.instrumented.jar:graal/compiler/mxbuild/dists/jdk11/graal-management.jar\
		${VM_ARGS}
