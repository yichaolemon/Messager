import java.io.OutputStream;
import java.lang.Integer;
import java.util.regex.Pattern;
import java.net.Socket;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Scanner;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.UUID;
import java.util.List;


public class MessageServer {
	public static final int SERVER_PORT = 5100;
  private static ServerSocket serverSkt;

  static private class ConnHandler extends Thread {
    
    private InetAddress dstAddr;
    private Socket skt;
    private Storage storage;
    private int counter; 

    // MessageReporter waits for new data to send, and then sends it.
    // Only the MessageReporter thread is allowed to write to the
    // socket's output stream.
    private class MessageReporter extends Thread {

    	private PrintWriter msgStream;
    	private Storage storage;
    	private int groupId;
      private boolean exit;

      private synchronized boolean getExit() {
        return this.exit;
      }
      public synchronized void close() {
        this.exit = true;
      }

    	public void run() {
    		while (!exit && !serverSkt.isClosed()) {
          reportMessages(storage.loadMessageSince(groupId, maxTimestampSent+1, true));
        }
        System.out.println("reporterThread exiting");
    	}

    	private long maxTimestampSent = 0;

      private void reportMessages(List<Message> msgList) {
        if (msgList == null) {
          exit = true;
          return;
        } 
        for (Message msg: msgList) {
          String msgString = msg.toContentString();
          System.out.println("writing message: "+msgString+" at timestamp "+msg.getTimestamp());
          msgStream.printf("%s\n", msgString);
          maxTimestampSent = Long.max(maxTimestampSent, msg.getTimestamp());
        }
      }

    	public MessageReporter(PrintWriter msgStream, Storage storage, int groupId, long maxTimestampSent) {
    		this.msgStream = msgStream;
    		this.storage = storage;
    		this.groupId = groupId;
    		this.maxTimestampSent = maxTimestampSent;
        this.exit = false;
    	}
    }

    private PrintWriter outputWriter;
    private MessageReporter reporterThread;

    public void run() {
      System.out.println("Established connection with client at "+skt.getRemoteSocketAddress().toString());
      // (pipes in content are escaped as \| and backslashes are escaped as \\)
      Scanner sc;
      try {
        sc = new Scanner(skt.getInputStream());
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
      OutputStream outputStream;
      try {
        outputStream = skt.getOutputStream();
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
      printWriter = new PrintWriter(outputStream, true /*auto flushing*/);

      // main loop for serving the client 
      while (sc.hasNextLine()) {
        try {
          handleInput(sc.nextLine());
        } catch (Exception e) {
          System.out.println("****   connection closed by client "+dstAddr.getHostAddress()+"   ****");
          reporterThread.interrupt();
          return;
        }
        counter += 1;
      }
    }

    private void handleInput(String input) {
      String[] components = input.split("|", 100);
      switch (components[0]) {
        case "login":
          // TODO
          break;
        case "create group":
          // TODO
          break;
        case "fetch":
          // reporter reports back messages to each connecting client 
          long timestamp = Long.parseLong(components[2]);
          int groupId = Integer.parseInt(components[1]);
          reporterThread = new MessageReporter(
            printWriter,
            storage,
            groupId,
            timestamp
          );
          reporterThread.start();
          break;
        case "send":
          saveMessage(components[2], Integer.parseInt(components[1]));
          break;
        case "close":
          reporterThread.close();
          reporterThread = null;
          break;
        default:
          outputWriter.println("error|Unrecognized Input");
      }
    }

    private void saveMessage(String msg, int groupId) throws Exception {
      Message message = new Message(
          UUID.randomUUID(),
          System.currentTimeMillis(),
          skt.getInetAddress(),
          groupId,
          content);
      storage.storeMessage(message);
      System.out.println("****   saved new message from "+dstAddr.getHostAddress()+"   ****");
    }

    public ConnHandler(Socket skt, Storage storage) {
      this.dstAddr = skt.getInetAddress();
      this.skt = skt;
      this.storage = storage;
      this.counter = 0;
    }
  }

	public static void main(String[] args) throws Exception {
		serverSkt = new ServerSocket(SERVER_PORT);
		Storage storage = new Database();
    System.out.println("Server set up, ready to ROCK!!");
    while (true) {
			Socket connectSkt = serverSkt.accept();
			ConnHandler connHandler = new ConnHandler(connectSkt, storage);
			connHandler.start(); 
		}
	}
}
