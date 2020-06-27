import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.Date;

public class Sender {
	private Socket senderSkt;
	private PrintWriter msgStream;
	private String delim;

	public void run() throws Exception {
		System.out.println("Sender: connected and starting to send");
		Scanner usrSysInput = new Scanner(System.in);
		String usrMsg = "";
		while (true) {
			String line = usrSysInput.nextLine();
			if (line == "" || line == "\n") {
				System.out.println("Should have been true");
				 msgStream.write(usrMsg+delim);
				 msgStream.flush();
				 usrMsg = "";
			} else {
				usrMsg = usrMsg + line;
			}
		}
	}

	public Sender (InetAddress dstAddr) throws Exception {
		senderSkt = new Socket (dstAddr, Receiver.SERVER_PORT);
		msgStream = new PrintWriter (senderSkt.getOutputStream());
		delim = "\n---\n";
	}
}