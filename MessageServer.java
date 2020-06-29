import java.net.Socket;
import java.net.ServerSocket;
import java.util.Scanner;
import java.io.PrintWriter;

private class MessageScanner extends Scanner {
	public boolean hasNextMessage() {
		return this.hasNext();
	}

	public String nextMessage() {
		StringBuilder msg = new StringBuilder();
		String next = this.next();
		// For now, we don't allow the delimiter to appear in the message.
		// TODO: escape them by checking if `next` ends with an odd number of backslashes.
		msg.append(next);
		return msg.toString();
	}

	public MessageScanner(InputStream source) {
		// pipe
		super(source).useDelimiter("\\|");
	}
}

private class ConnHandler extends Thread {
	Socket skt;
	Storage storage;
	InetAddress dstAddr;

	public void run() {
		// server protocol 
		// save/fetch|content|dstAddr
		// (pipes in content are escaped as \| and backslashes are escaped as \\)
		MessageScanner sc = new MessageScanner(skt.getInputStream());
		String operation = sc.nextMessage();
		if (operation.equals("save")) {
			saveMessage(sc);
		} else if (operation.equals("fetch")) {
			fetchMessage(sc);
		}
	}

	private void fetchMessage(MessageScanner sc) {
		long timestamp = Long.toString(sc.nextMessage());
		PrintWriter msgStream = new PrintWriter(skt.getOutputStream(), true /*auto flushing*/);
		List<Message> msgList = storage.loadMessageSince(dstAddr, timestamp);
		if (msgList == null) {
			msgStream.write("<server msg>: no messages since timestamp\n");
			return;
		} else {
			for (Message msg: msgList) {
				msgString = msg.toString();
				msgStream.write(msgString+"\n");
				msgStream.flush();
			}
		}
	}

	private void saveMessage(MessageScanner sc) {
		String content = sc.nextMessage();
		String addr = sc.nextMessage();
		Message message = new Message(
			UUID.randomUUID(),
			System.currentTimeMillis(),
			skt.getInetAddress(),
			dstAddr,
			content);
		storage.storeMessage(message);
	}

	public ConnHandler(Socket skt, Storage storage) {
		this.skt = skt;
		this.storage = storage;
		this.dstAddr = InetAddress.getByName(addr);
	}
}

public class MessageServer {

	public static final int SERVER_PORT = 5100;

	public static void main() throws Exception {
		ServerSocket serverSkt = new ServerSocket(SERVER_PORT);
		Storage storage = new Database();
		while (true) {
			Socket connectSkt = serverSkt.accept();
			ConnHandler connHandler = new ConnHandler(connectSkt, storage);
			connHandler.start(); 
		}
	}
}