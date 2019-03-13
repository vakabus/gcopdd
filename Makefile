blood/build/libs/blood.jar:
	cd blood; gradle build

graal/.git:
	git submodule init graal
	git submodule update --depth=1 graal

mx/.git:
	git submodule init mx
	git submodule update --depth=1 mx

PLuG/.git:
	git submodule init PLuG
	git submodule update --depth=1 PLuG

PLuG/dist/PLuG.jar:
	cd PLuG; ant

graal/compiler/mxbuild: mx/.git graal/.git
	cd graal/compiler; ../../mx/mx build; ../../mx/mx ideinit
	# if you have multiple JVM versions, this will fail
	# and give you hints how to fix it
	
graal.instrumented.jar: graal/compiler/mxbuild blood/build/libs/blood.jar PLuG/dist/PLuG.jar
	PLuG/plug.sh blood/build/libs/blood.jar --in graal/compiler/mxbuild/dists/jdk11/graal.jar --out graal.instrumented.jar

clean:
	cd blood; gradle clean
	cd graal; mx clean
	cd PLuG; ant clean

update-deps: clean
	git submodule update --depth=1
