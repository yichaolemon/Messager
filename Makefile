RUN = docker run --rm -v $(CURDIR):/usr/src/messager -w /usr/src/messager openjdk:16-slim

Messager.class: Messager.java
	$(RUN) javac $^

run: Messager.class
	$(RUN) java Messager
