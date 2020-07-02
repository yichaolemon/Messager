import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/*

# Protocol
Generally the command separator is the newline,
and within commands each component is separated by a pipe.
Client can send these commands:
login|[username]|[password]
create group|[group id]|[username]|[username]|...
fetch|[group id]|[start timestamp]
send|[group id]|[message]
close|[group id]

Server sends back
message|[group id]|[message]
error|[error message]

 */

/*
Command line REPL
help
login [username] [password]
create group [group id] [username],[username],...
enter group [group id]
[message]
exit
*/

public class Sender extends Thread {
  private boolean inRepl = true;
  private int currentGroup;
  private StringBuilder msg;
  private final Lock lock = new ReentrantLock();
  private final Condition strNotEmpty = lock.newCondition();
  private Socket senderSkt;
  private PrintWriter outputWriter;
  private final int SERVER_PORT = 5100;

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
      // Loop over all messages
      try {
        while (inStream.hasNextLine()) {
          String nl = inStream.nextLine();
          String[] components = nl.split("|", 100);
          if (components[0].equals("error")) {
            System.out.println("ERROR: " + components[1]);
            continue;
          }
          int groupReceived = Integer.parseInt(components[1]);
          String messageReceived = components[2];
          if (groupReceived == this.getCurrentGroup()) {
            System.out.println(messageReceived);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
    }

  }

  public void writeMsg(String line) {
    lock.lock();
    try {
      msg.append(line);
      strNotEmpty.signal();
    }	finally {
      lock.unlock();
    }
  }

  private String getMsg() throws InterruptedException {
    lock.lock();
    try {
      while (msg.length() == 0) {
        strNotEmpty.await();
      }
      String msgString = msg.toString();
      msg.setLength(0);
      return msgString;
    } finally {
      lock.unlock();
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

  private static final Pattern loginPattern = Pattern.compile("^login (\\w+) (\\S+)$");
  private static final Pattern createGroupPattern = Pattern.compile("^create group (\\d+) (\\w+(,\\w+)*)$");
  private static final Pattern enterGroupPattern = Pattern.compile("^enter group (\\d+)$");
  private static final Pattern helpPattern = Pattern.compile("^help$");

  private void handleCommand(String command) {
    Matcher loginMatch = loginPattern.matcher(command);
    if (loginMatch.matches()) {
      String username = loginMatch.group(1);
      String password = loginMatch.group(2);
      outputWriter.printf("login|%s|%s\n", username, password);
      return;
    }
    Matcher createGroupMatch = createGroupPattern.matcher(command);
    if (createGroupMatch.matches()) {
      String groupId = createGroupMatch.group(1);
      String usernameStr = createGroupMatch.group(2);
      String[] usernames = usernameStr.split(",", usernameStr.length());
      outputWriter.printf("create group|%d", groupId);
      for (String username: usernames) {
        outputWriter.printf("|%s", username);
      }
      outputWriter.printf("\n");
      return;
    }
    Matcher enterGroupMatch = enterGroupPattern.matcher(command);
    if (enterGroupMatch.matches()) {
      int groupId = Integer.parseInt(enterGroupMatch.group(1));
      enterGroup(groupId);
      return;
    }
    Matcher helpMatch = helpPattern.matcher(command);
    if (helpMatch.matches()) {
      System.out.println("Available commands: login, create group, enter group");
      return;
    }
    System.out.println("unrecognized command");
  }

  private void enterGroup(int groupId) {
      inRepl = false;
      currentGroup = groupId;
      receiver.setCurrentGroup(groupId);
      long timestamp = 0;  // TODO
      outputWriter.printf("fetch|%d|%d\n", currentGroup, timestamp);
  }

  private void handleMessage(String msg) {
    if (msg.equals("exit")) {
      inRepl = true;
      outputWriter.printf("close|%d\n", currentGroup);
      return;
    }
    outputWriter.printf("%d|%s\n", currentGroup, msg);
  }

  private SReceiver receiver;

  public Sender(InetAddress dstInetAddr) {
    msg = new StringBuilder();
    try {
      senderSkt = new Socket(dstInetAddr, SERVER_PORT);
      System.out.println("Established connection with server at "+senderSkt.getRemoteSocketAddress().toString());
      outputWriter = new PrintWriter(senderSkt.getOutputStream(), true /*auto flushing*/);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    receiver = new SReceiver();
    receiver.start();
  }
}
