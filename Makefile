RUN = docker run --rm -v $(CURDIR):/usr/src/messager -w /usr/src/messager openjdk:16-slim

Messager: Messager.java Receiver.java Sender.java
	$(RUN) javac $^

run: Messager
	$(RUN) java Messager
