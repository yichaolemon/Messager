import java.io.IOException;
import java.util.Scanner;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Receiver extends Thread {
	// NOTE: Port must be exposed by Docker
	public static final int SERVER_PORT = 5000;
	public void run() {
		ServerSocket serverSkt;
		try {
			serverSkt = new ServerSocket(SERVER_PORT);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		while (true) {
			String rcvdMsg = "";
			Socket connectSkt;
			try {
				System.out.println("Receiver: Trying to accept");
				connectSkt = serverSkt.accept();
				System.out.println("Receiver: Accepted new sender connection");
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			Scanner inStream;
			try {
				inStream = new Scanner(connectSkt.getInputStream());
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			while (inStream.hasNextLine()) {
				String nl = inStream.nextLine();
				if (nl == "---") {
					break;
				}
				rcvdMsg = rcvdMsg + nl + "\n";
			}
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
			Date date = new Date(System.currentTimeMillis());
			System.out.println(formatter.format(date)+": "+rcvdMsg);
		}
	}
}