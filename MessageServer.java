import java.io.OutputStream;
import java.util.Arrays;
import java.lang.Integer;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Scanner;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.UUID;
import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;;


public class MessageServer {
	public static final int SERVER_PORT = 5100;
  private static ServerSocket serverSkt;
  private static Storage messageStorage;

  static private class ConnHandler extends Thread {
    private final InetAddress dstAddr;
    private final Socket skt;
    private boolean isAuthenticated = false;
    private String username;
    private PrintWriter outputWriter;


    // Reporter waits for something to get added to a database
    // Its subclasses should implement `loadNextValue` and `serializeValue`.
    private class Reporter<V> extends Thread {
      private boolean exit;

      public synchronized void close() {
        this.exit = true;
      }

      private synchronized boolean getExit() {
        return this.exit;
      }

    	public void run() {
    		while (!getExit() && !serverSkt.isClosed()) {
          report(loadNextValue());
        }
        System.out.println("Reporter Thread exiting");
    	}

      protected V loadNextValue() {
        // subclass must implement
        return null;
      }

      protected String serializeValue(V value) {
        // subclass must implement
        return null;
      }

      private void report(V value) {
        // won't close for blocking reporters that wait on CV
        if (value == null) {
          close();
          return;
        } 
        outputWriter.printf("%s\n", serializeValue(value));
      }

    	protected Reporter() {
        this.exit = false;
    	}
    }

    private class GroupKeyReporter extends Reporter<Integer> {
      private int maxIndexSent = 0;

      @Override
      protected Integer loadNextValue() {
        try {
          int nextValue = userAuthenticationStorage.loadGroupUpdate(maxIndexSent, username);
          maxIndexSent += 1;
          return Integer.valueOf(nextValue);
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }
      }

      @Override
      protected String serializeValue(Integer value) {
        // look up the AES group key encrypted for username 
        Group group = messageStorage.loadGroup(value.intValue());
        String encryptedKey = group.getUserKeyInGroup(username);
        return String.format("group key|%d|%s", value.intValue(), encryptedKey);
      }

      public GroupKeyReporter() {
        super();
      }
    } 

    private class KeyReporter extends Reporter<List<String>> {
      private int maxIndexSent = 0;

      @Override
      protected List<String> loadNextValue() {
        List<String> nextValue = userAuthenticationStorage.loadKeysSince(maxIndexSent+1, true, username);
        maxIndexSent = Math.max(maxIndexSent, maxIndexSent+nextValue.size());
        return nextValue;
      }

      @Override
      protected String serializeValue(List<String> value) {
        StringBuilder msg = new StringBuilder();
        for (String userKeyPair: value) {
          msg.append("|"+userKeyPair);
        }
        return String.format("public keys%s", msg.toString());
      }

    	public KeyReporter() {
        super();
    	}
    }

    private class MessageReporter extends Reporter<Message> {
    	private int groupId;
    	private long maxTimestampSent;
      private List<Message> messageBuffer = null;

      @Override
      protected Message loadNextValue() {
        if (messageBuffer == null || messageBuffer.size() == 0) {
          messageBuffer = messageStorage.loadMessageSince(groupId, maxTimestampSent+1, true);
        }
        if (messageBuffer == null) {
          return null;
        }
        Message msg = messageBuffer.remove(0);
        maxTimestampSent = Long.max(maxTimestampSent, msg.getTimestamp());
        return msg;
      }

      @Override
      protected String serializeValue(Message msg) {
        return String.format("message|%d|%s", groupId, msg.toContentString());
      }

    	public MessageReporter(int groupId, long maxTimestampSent) {
        super();
    		this.groupId = groupId;
    		this.maxTimestampSent = maxTimestampSent;
    	}
    }

    private MessageReporter messageReporterThread;
    private KeyReporter keyReporterThread;
    private GroupKeyReporter groupKeyReporterThread;

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
          messageReporterThread.interrupt();
          sc.close();
          return;
        }
      }
    }

    private void handleInput(String input) {
      String[] components = input.split("\\|", 100);
      System.out.printf("Handling input string: %s\n", input);

      switch (components[0]) {

        case "login":
          String username = components[1];
          String password = components[2];
          String publicKey = components[3];
          if (username.equals(this.username) && isAuthenticated) {
            outputWriter.printf("error|Already logged in with username @%s\n", username);
            return;
          } else if (isAuthenticated) {
            outputWriter.printf("error|Currently logged in with username @%s, to log in to a different account, start a new session\n", this.username);
            return;
          }
          UserAuthentication.User authResultUser = userAuthenticationStorage.loginOrRegister(username, password, publicKey);
          if (authResultUser != null) {
            isAuthenticated = true;
            this.username = username;
            keyReporterThread = new KeyReporter();
            keyReporterThread.start();
            groupKeyReporterThread = new GroupKeyReporter();
            groupKeyReporterThread.start();
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
          List<String> userEncryptedKeyPairList = Arrays.asList(Arrays.copyOfRange(components, 2, components.length));
          ListIterator<String> iter = userEncryptedKeyPairList.listIterator();
          List<String> userList = new ArrayList<String>();
          Map<String, String> usernameToKey = new HashMap<String, String>();

          while (iter.hasNext()) {
            String user = iter.next();
            userList.add(user);
            usernameToKey.put(user, iter.next());
          }

          if (!messageStorage.createGroupIfNotExists(groupIdToCreate, usernameToKey)) {
            outputWriter.println("error|Group already exists");
            return;
          }
          userAuthenticationStorage.updateNewGroupInfo(groupIdToCreate, userList);
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
            messageReporterThread = new MessageReporter(groupId, timestamp);
            messageReporterThread.start();
          } else {
            outputWriter.println("error|Unauthorized user for group");
          }
          break;

        case "send":
          saveMessage(components[2], Integer.parseInt(components[1]));
          break;
        case "close":
          if (messageReporterThread == null) {
            outputWriter.println("error|Already exitted chat group");
            break;
          }
          messageReporterThread.close();
          messageReporterThread = null;
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
