
.PHONY: jar
jar: bin/ds-client.jar

bin/ds-client.jar: bin/dsclient/*.class
	jar cfe bin/ds-client.jar dsclient.Main -C bin/ dsclient/

.PHONY: classes
classes: bin/dsclient/*.class

bin/dsclient/%.class: src/dsclient/*.java
	javac -d bin/ $^

.PHONY: clean
clean:
	rm -rf bin/*
