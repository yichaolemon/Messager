import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;
import java.util.HashMap;

/*
 * Protocol:
 * [Generally the command separator is the newline,
 * and within commands each component is separated by a pipe.]
 *
 * Client can send these commands:
 * login|[username]|[password]
 * create group|[group id]|[username]|[username]|...
 * fetch|[group id]|[start timestamp]
 * send|[group id]|[message]
 * close|[group id]
 * 
 * Corresponding Command line REPL:
 * login [username] [password]
 * create group [group id] [username],[username],...
 * enter group [group id]
 * [message]
 * exit
 * help -- to see the above options and usage 
 *
 * Server sends back:
 * message|[group id]|[message]
 * error|[error message]
 * public keys|[username]|[key]|...
 * 
 */

public class Sender extends Thread {
  private boolean inRepl = true;
  private int currentGroup;
  private final int SERVER_PORT = 5100;

  private final Lock msgLock = new ReentrantLock(); // protecting msg 
  private final Lock publicKeysLock = new ReentrantLock(); // protecting publicKeys
  private final Condition strNotEmpty = msgLock.newCondition();
  private StringBuilder msg; 
  private Socket senderSkt;
  private PrintWriter outputWriter;
  private Encryption encryptionEntity;
  private Map<String, String> publicKeys;

  private class SReceiver extends Thread {
    private int currentGroup;

    public synchronized void setCurrentGroup(int currentGroup) {
      this.currentGroup = currentGroup;
    }
    private synchronized int getCurrentGroup() {
      return this.currentGroup;
    }

    public void run() {
      Scanner inStream;
      try {
        inStream = new Scanner(senderSkt.getInputStream());
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
      // Main loop for fetching messages 
      try {
        while (inStream.hasNextLine()) {
          String nl = inStream.nextLine();
          // System.out.println(nl);
          String[] components = nl.split("\\|", 100);
          if (components[0].equals("error")) {
            System.out.println("ERROR: " + components[1]);
          } else if (components[0].equals("public keys")) {
            int i = 1;
            publicKeysLock.lock();
            while (i < components.length) {
              String user = components[i];
              String key = components[i+1];
              publicKeys.put(user, key);
              i = i+2;
            }
            publicKeysLock.unlock();
          } else {
            int groupReceived = Integer.parseInt(components[1]);
            String messageReceived = components[2];
            if (groupReceived == this.getCurrentGroup()) {
              System.out.println(messageReceived);
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
    }
  }

  public void writeMsg(String line) {
    msgLock.lock();
    try {
      msg.append(line);
      strNotEmpty.signal();
    }	finally {
      msgLock.unlock();
    }
  }

  private String getMsg() throws InterruptedException {
    msgLock.lock();
    try {
      while (msg.length() == 0) {
        strNotEmpty.await();
      }
      String msgString = msg.toString();
      msg.setLength(0); // no need for repetitive memory realloc each time
      return msgString;
    } finally {
      msgLock.unlock();
    }
  }

  public void run() {
    // tries to keep a persistent connection with server
    try {
      while (true) {
        this.handleInput(this.getMsg());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleInput(String input) {
    if (inRepl) {
      handleCommand(input);
    } else {
      handleMessage(input);
    }
  }

  // Regex parsing 
  private static final Pattern loginPattern = Pattern.compile("^login (\\w+) (\\S+)$");
  private static final Pattern createGroupPattern = Pattern.compile("^create group (\\d+) (\\w+(,\\w+)*)$");
  private static final Pattern enterGroupPattern = Pattern.compile("^enter group (\\d+)$");
  private static final Pattern helpPattern = Pattern.compile("^help$");

  private void handleCommand(String command) {
    Matcher loginMatch = loginPattern.matcher(command);
    Matcher createGroupMatch = createGroupPattern.matcher(command);
    Matcher enterGroupMatch = enterGroupPattern.matcher(command);
    Matcher helpMatch = helpPattern.matcher(command);

    if (loginMatch.matches()) {
      String username = loginMatch.group(1);
      String password = loginMatch.group(2);
      outputWriter.printf("login|%s|%s|%s\n", username, password, encryptionEntity.getPublicKey());
      // TODO: hide password using some console password parser
    }
    else if (createGroupMatch.matches()) {
      String groupId = createGroupMatch.group(1);
      String usernameStr = createGroupMatch.group(2);
      String[] usernames = usernameStr.split(",", usernameStr.length());
      StringBuilder msg = new StringBuilder();
      boolean foundAllKeys = true;

      for (String username: usernames) {
        if (!publicKeys.containsKey(username)) {
          System.out.printf("user %s not logged in yet\n", username);
          foundAllKeys = false;
          continue;
        }
        String userPublicKey = publicKeys.get(username);
        msg.append("|"+username+"|"+userPublicKey);
      }
      if (foundAllKeys) {
        outputWriter.printf("create group|%s%s\n", groupId, msg.toString());
      } 
    }
    else if (enterGroupMatch.matches()) {
      int groupId = Integer.parseInt(enterGroupMatch.group(1));
      enterGroup(groupId);
    }
    else if (helpMatch.matches()) {
      System.out.println("Available commands: login, create group, enter group");
    }
    else {
      System.out.println("unrecognized command");
    }
  }

  private void enterGroup(int groupId) {
    inRepl = false; // TODO: this is incorrect!! 
    currentGroup = groupId;
    receiver.setCurrentGroup(groupId);
    long timestamp = 0;  // TODO: remember the last timestamp for each group.  
    outputWriter.printf("fetch|%d|%d\n", currentGroup, timestamp);
  }

  private void handleMessage(String msg) {
    if (msg.equals("exit")) {
      inRepl = true;
      outputWriter.printf("close|%d\n", currentGroup);
      return;
    }
    outputWriter.printf("send|%d|%s\n", currentGroup, msg);
  }

  private SReceiver receiver;

  public Sender(InetAddress dstInetAddr) {
    msg = new StringBuilder();
    publicKeys = new HashMap<String, String>();
    try {
      senderSkt = new Socket(dstInetAddr, SERVER_PORT);
      System.out.println("Established connection with server at "+senderSkt.getRemoteSocketAddress().toString());
      outputWriter = new PrintWriter(senderSkt.getOutputStream(), true /*auto flushing*/);
      encryptionEntity = new Encryption(2048);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    receiver = new SReceiver();
    receiver.start();
    
  }
}
