# This makefile can build JAR files for command line use
# or for Eclipse plugin use.

VERSION=0.6
.PHONY: build build-plugin test default unit-test regression-test

default: test

TESTBIN= bin/edu/uwm/cs/eclipse/LockedTestActivator.class
build-plugin : ${TESTBIN} 
	jar cmf META-INF/MANIFEST.MF edu.uwm.cs.locked-tests_${VERSION}.jar plugin.xml -C bin .

${TESTBIN}:
	@echo Unable to compile Eclipse plugin code in Makefile.
	@echo Load project into Eclipse and build.
	@echo Then come back and make build-plugin
	false

clean:
	rm -rf bin edu*.jar
