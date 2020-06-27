import java.io.IOException;
import java.util.Scanner;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;

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
			// Loop over all messages
			while (inStream.hasNextLine()) {
				String nl = inStream.nextLine();
        ZonedDateTime now = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("America/Los_Angeles"));
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
				System.out.println(formatter.format(now) + ":" +nl);
			}
		}
	}
}
