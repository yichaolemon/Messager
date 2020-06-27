import java.io.IOException;
import java.util.Scanner;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Receiver extends Thread {
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
				connectSkt = serverSkt.accept();
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
				rcvdMsg = rcvdMsg + inStream.nextLine();
			}
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
			Date date = new Date(System.currentTimeMillis());
			System.out.println(formatter.format(date)+": "+rcvdMsg);
		}
	}
}