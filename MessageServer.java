import java.io.OutputStream;
import java.util.Arrays;
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
  private static Storage messageStorage;

  static private class ConnHandler extends Thread {
    private final InetAddress dstAddr;
    private final Socket skt;
    private boolean isAuthenticated = false;
    private String username;

    // MessageReporter waits for new data to send, and then sends it.
    // Only the MessageReporter thread is allowed to write to the
    // socket's output stream.
    private class MessageReporter extends Thread {
    	private PrintWriter msgStream;
    	private int groupId;
      private boolean exit;
    	private long maxTimestampSent = 0;

      /* private synchronized boolean getExit() {
        return this.exit;
      } */
    
      public synchronized void close() {
        this.exit = true;
      }

    	public void run() {
    		while (!exit && !serverSkt.isClosed()) {
          reportMessages(messageStorage.loadMessageSince(groupId, maxTimestampSent+1, true));
        }
        System.out.println("reporterThread exiting");
    	}

      private void reportMessages(List<Message> msgList) {
        if (msgList == null) {
          exit = true;
          return;
        } 
        for (Message msg: msgList) {
          String msgString = msg.toContentString();
          // System.out.println("writing message: "+msgString+" at timestamp "+msg.getTimestamp());
          msgStream.printf("%s\n", msgString);
          maxTimestampSent = Long.max(maxTimestampSent, msg.getTimestamp());
        }
      }

    	public MessageReporter(PrintWriter msgStream, int groupId, long maxTimestampSent) {
    		this.msgStream = msgStream;
    		this.groupId = groupId;
    		this.maxTimestampSent = maxTimestampSent;
        this.exit = false;
    	}
    }

    private PrintWriter outputWriter;
    private MessageReporter reporterThread;

    public void run() {
      System.out.printf("Established connection with client at %s\n", skt.getRemoteSocketAddress().toString());
      Scanner sc;
      OutputStream outputStream;

      try {
        sc = new Scanner(skt.getInputStream());
        outputStream = skt.getOutputStream();
        outputWriter = new PrintWriter(outputStream, true /*auto flushing*/);
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }

      // main loop for serving the client 
      while (sc.hasNextLine()) {
        try {
          handleInput(sc.nextLine());
        } catch (Exception e) {
          // System.out.println("****   connection closed by client "+dstAddr.getHostAddress()+"   ****");
          e.printStackTrace();
          reporterThread.interrupt();
          sc.close();
          return;
        }
      }
    }

    private void handleInput(String input) {
      String[] components = input.split("|", 100);

      switch (components[0]) {

        case "login":
          String username = components[1];
          String password = components[2];
          UserAuthentication.User authResultUser = userAuthenticationStorage.loginOrRegister(username, password);
          if (authResultUser != null) {
            isAuthenticated = true;
            this.username = username;
            // username is set if and only if user is authenticated 
          } else {
            outputWriter.println("error|Incorrect password entered");
          } 
          break;

        case "create group":
          if (!isAuthenticated) {
            outputWriter.println("error|Please login first");
            return;
          }
          Integer groupIdToCreate = Integer.decode(components[1]);
          List<String> usernameList = Arrays.asList(Arrays.copyOfRange(components, 2, components.length));
          if (!messageStorage.createGroupIfNotExists(groupIdToCreate, usernameList, this.username)) {
            outputWriter.println("error|Group already exists");
          }
          userAuthenticationStorage.updateNewGroupInfo(groupIdToCreate, usernameList);
          break;

        case "fetch":
          // reporter reports back messages to each connecting client 
          if (!isAuthenticated) {
            outputWriter.println("error|Please login first");
            return;
          }
          long timestamp = Long.parseLong(components[2]);
          int groupId = Integer.parseInt(components[1]);
          if (userAuthenticationStorage.isVerifiedGroupMember(this.username, groupId)) {
            reporterThread = new MessageReporter(
                outputWriter,
                groupId,
                timestamp);
            reporterThread.start();
          } else {
            outputWriter.println("error|Unauthorized user for group");
          }
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

    private void saveMessage(String content, int groupId) {
      Message message = new Message(
          UUID.randomUUID(),
          System.currentTimeMillis(),
          skt.getInetAddress(),
          this.username,
          groupId,
          content);
      messageStorage.storeMessage(message);
      System.out.println("****   saved new message from "+dstAddr.getHostAddress()+"   ****");
    }

    public ConnHandler(Socket skt) {
      this.dstAddr = skt.getInetAddress();
      this.skt = skt;
    }
  }

  private static UserAuthentication userAuthenticationStorage;

	public static void main(String[] args) throws Exception {
		serverSkt = new ServerSocket(SERVER_PORT);
		messageStorage = new Database();
    System.out.println("Server set up, ready to ROCK!!");
    userAuthenticationStorage = new UserAuthentication();
    while (true) {
			Socket connectSkt = serverSkt.accept();
			ConnHandler connHandler = new ConnHandler(connectSkt);
			connHandler.start(); 
		}
	}
}
