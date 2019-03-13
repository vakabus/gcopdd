blood/build/libs/blood-all.jar:
	cd blood; gradle shadowJar

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
	
graal.instrumented.jar: graal/compiler/mxbuild/dists/jdk11/graal.jar blood/build/libs/blood-all.jar PLuG/dist/PLuG.jar
	PLuG/plug.sh blood/build/libs/blood-all.jar --in graal/compiler/mxbuild/dists/jdk11/graal.jar --out graal.instrumented.jar

clean:
	cd blood; gradle clean
	cd graal/compiler; mx clean
	cd PLuG; ant clean
	rm graal.instrumented.jar

clean-graal:
	rm -rf graal/

clean-mx:
	rm -rf mx/

clean-plug:
	rm -rf PLuG/

clean-full: clean-graal clean-mx clean-plug
	rm graal.instrumented.jar

update-deps: clean
	git submodule update
