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
      sc = new Scanner(source).useDelimiter("\\|");
    }
  }

  static private class ConnHandler extends Thread {
    
    private InetAddress dstAddr;
    private Socket skt;
    private Storage storage;

    public void run() {
      System.out.println("New connection established");
      // server protocol 
      // save/fetch|content|dstAddr
      // (pipes in content are escaped as \| and backslashes are escaped as \\)
      MessageScanner sc;
      try {
        sc = new MessageScanner(skt.getInputStream());
      } catch (Exception e) {
        return;
      }
      String operation = sc.nextMessage();
      try {
        if (operation.equals("save")) {
          saveMessage(sc);
        } else if (operation.equals("fetch")) {
          fetchMessage(sc);
        }
      } catch (Exception e) {
        return;
      }
    }

    private void fetchMessage(MessageScanner sc) throws Exception {
      long timestamp = Long.parseLong(sc.nextMessage());
      PrintWriter msgStream = new PrintWriter(skt.getOutputStream(), true /*auto flushing*/);
      List<Message> msgList = storage.loadMessageSince(skt.getInetAddress(), timestamp);
      if (msgList == null) {
        msgStream.write("<server msg>: no messages since timestamp\n");
        return;
      } else {
        for (Message msg: msgList) {
          String msgString = msg.toString();
          msgStream.write(msgString+"\n");
          msgStream.flush();
        }
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
    }

    public ConnHandler(Socket skt, Storage storage) {
      this.skt = skt;
      this.storage = storage;
    }
  }

	public static void main(String[] args) throws Exception {
		ServerSocket serverSkt = new ServerSocket(SERVER_PORT);
		Storage storage = new Database();
		while (true) {
      System.out.println("Trying to establish a new connection");
			Socket connectSkt = serverSkt.accept();
			ConnHandler connHandler = new ConnHandler(connectSkt, storage);
			connHandler.start(); 
		}
	}
}
