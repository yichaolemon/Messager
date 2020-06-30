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
      // pipe
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
    private MessageReporter extends Thread {
    	public void run() {
    		while (true) {
    			reportMessages(storage.loadMessageSince(dstAddr, maxTimestampSent+1, true));
    		}
    	}

    	private long maxTimestampSent = 0;

    	private void reportMessages(List<Message> msgList) {
        for (Message msg: msgList) {
          String msgString = msg.toContentString();
          // System.out.println(msgString);
          msgStream.write(msgString+"\n");
          msgStream.flush();
          maxTimestampSent = Long.max(maxTimestampSent, msg.getTimestamp());
        }
    	}

    	private PrintWriter msgStream;
    	private Storage storage;
    	private InetAddress dstAddr;

    	public MessageReporter(PrintWriter msgStream, Storage storage, InetAddress dstAddr, long maxTimestampSent) {
    		this.msgStream = msgStream;
    		this.storage = storage;
    		this.dstAddr = dstAddr;
    		this.maxTimestampSent = maxTimestampSent;
    	}
    }

    public void run() {
      System.out.println("New connection established");
      // save|content|dstAddr|
      // or fetch|timestamp|
      // (pipes in content are escaped as \| and backslashes are escaped as \\)
      MessageScanner sc;
      try {
        sc = new MessageScanner(skt.getInputStream());
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }

      long timestamp = Long.parseLong(sc.nextMessage());
      MessageReporter reporter = new MessageReporter(
      	new PrintWriter(skt.getOutputStream(), true /*auto flushing*/),
      	storage,
      	dstAddr,
      	timestamp,
      )
      reporter.start();

      while (true) {
        try {
          saveMessage(sc);
        } catch (Exception e) {
          System.out.println("Connection closed by client");
          return;
        }
        System.out.println("****   "+Integer.toString(counter)+"   ****");
        counter += 1;
      }
    }

    // private void fetchMessage(MessageScanner sc, MessageReporter reporter) throws Exception {
    //   PrintWriter msgStream = new PrintWriter(skt.getOutputStream(), true /*auto flushing*/);
    //   List<Message> msgList = storage.loadMessageSince(skt.getInetAddress(), timestamp, false);
    //   if (msgList == null) {
    //     msgStream.write("<server msg>: no messages since timestamp\n");
    //     return;
    //   }
    //   reporter.reportMessages(msgList);
    // }

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
      // System.out.print("saved message:\n"+message.toString());
    }

    public ConnHandler(Socket skt, Storage storage) {
      this.skt = skt;
      this.storage = storage;
      this.counter = 0;
    }
  }

	public static void main(String[] args) throws Exception {
		ServerSocket serverSkt = new ServerSocket(SERVER_PORT);
		Storage storage = new Database();
    System.out.println("Server set up, ready to ROCK!!");
    while (true) {
			Socket connectSkt = serverSkt.accept();
			ConnHandler connHandler = new ConnHandler(connectSkt, storage);
			connHandler.start(); 
		}
	}
}
