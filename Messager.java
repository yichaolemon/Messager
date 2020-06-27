import java.net.InetAddress;

public class Messager {
  public static void main(String[] args) throws Exception {
  	if (args.length < 1) {
  		System.out.println("Usage: java Messager ip-address");
  		return;
  	}
    // System.out.println("Hello Totosheen");
    Receiver receiver = new Receiver();
    receiver.start(); // start the receiver thread
    Thread.sleep(5000);
    Sender sender = new Sender(InetAddress.getByName(args[0]));
    InputReader inputReader = new InputReader(sender);
    sender.start();
    inputReader.run();
  }
}
