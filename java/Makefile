
.PHONY: jar
jar: classes
	jar cfe bin/ds-client.jar dsclient.Main -C bin/ dsclient/

.PHONY: classes
classes:
	javac -d bin/ src/dsclient/*.java

.PHONY: clean
clean:
	rm -rf bin/*
