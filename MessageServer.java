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

  static private class MessageScanner {
    Scanner sc;

    public boolean hasNextMessage() {
      return sc.hasNext();
    }

    public String nextMessage() {
      StringBuilder msg = new StringBuilder();
      String next = sc.next();
      // For now, we don't allow the delimiter to appear in the message.
      // TODO: escape them by checking if `next` ends with an odd number of backslashes.
      msg.append(next);
      return msg.toString();
    }

    public MessageScanner(InputStream source) {
      // pipe is the deliminator indicating end of message 
      sc = new Scanner(source).useDelimiter(Pattern.compile("\\|"));
    }
  }

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
    	private InetAddress dstAddr;
      private boolean exit;

    	public void run() {
    		while (!exit && !serverSkt.isClosed()) {
          reportMessages(storage.loadMessageSince(dstAddr, maxTimestampSent+1, true));
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
          msgStream.write(msgString+"\n");
          msgStream.flush();
          maxTimestampSent = Long.max(maxTimestampSent, msg.getTimestamp());
        }
      }

    	public MessageReporter(PrintWriter msgStream, Storage storage, InetAddress dstAddr, long maxTimestampSent) {
    		this.msgStream = msgStream;
    		this.storage = storage;
    		this.dstAddr = dstAddr;
    		this.maxTimestampSent = maxTimestampSent;
        this.exit = false;
    	}
    }

    public void run() {
      System.out.println("Established connection with client at "+skt.getRemoteSocketAddress().toString());
      // (pipes in content are escaped as \| and backslashes are escaped as \\)
      MessageScanner sc;
      try {
        sc = new MessageScanner(skt.getInputStream());
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
      // at the start, at client, send the timestamp 
      long timestamp = Long.parseLong(sc.nextMessage());
      OutputStream outputStream;
      try {
        outputStream = skt.getOutputStream();
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
      // reporter reports back messages to each connecting client 
      MessageReporter reporterThread = new MessageReporter(
      	new PrintWriter(outputStream, true /*auto flushing*/),
      	storage,
      	dstAddr,
      	timestamp
      );
      reporterThread.start();

      // main loop for serving the client 
      while (true) {
        try {
          saveMessage(sc);
        } catch (Exception e) {
          System.out.println("****   connection closed by client "+dstAddr.getHostAddress()+"   ****");
          reporterThread.interrupt();
          return;
        }
        counter += 1;
      }
    }

    private void saveMessage(MessageScanner sc) throws Exception {
      String content = sc.nextMessage();
      String addr = sc.nextMessage();

      Message message = new Message(
          UUID.randomUUID(),
          System.currentTimeMillis(),
          skt.getInetAddress(),
          InetAddress.getByName(addr),
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
