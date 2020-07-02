# RUN_X86 = docker run --rm -v $(CURDIR):/usr/src/messager -w /usr/src/messager --expose=7000-8000  openjdk:16-slim
RUN_X86 = docker run --rm -v $(CURDIR):/usr/src/messager -w /usr/src/messager openjdk:16-slim
RUN_ARM = docker run --rm -v $(CURDIR):/usr/src/messager -w /usr/src/messager -p 5000:5000 armv7/armhf-java8 
RUN_SERVER = docker run --rm -v $(CURDIR):/usr/src/messager -w /usr/src/messager -p 5100:5100 armv7/armhf-java8
Messager_X86: Messager.java Receiver.java Sender.java InputReader.java
	$(RUN_X86) javac $^

Messager_ARM: Messager.java Receiver.java Sender.java InputReader.java
	$(RUN_ARM) javac $^

Messager_Server: Message.java Database.java Storage.java MessageServer.java
	$(RUN_SERVER) javac $^
