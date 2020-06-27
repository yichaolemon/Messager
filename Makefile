RUN = docker run --rm -v $(CURDIR):/usr/src/messager -w /usr/src/messager -p 5000:5000 openjdk:16-slim

Messager: Messager.java Receiver.java Sender.java
	$(RUN) javac $^

run: Messager
	$(RUN) java Messager 127.0.0.1
