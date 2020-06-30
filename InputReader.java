import java.util.Scanner;

public class InputReader {
	private Scanner usrSysInput;
	private Sender sender;

	public void run() {
		while (usrSysInput.hasNextLine()) {
			// TODO: Check whether the nextline is empty 
			String line = usrSysInput.nextLine();
			sender.writeMsg(line);
		}
	}

	public InputReader (Sender sender) {
		usrSysInput = new Scanner(System.in);
		this.sender = sender; 
	}
}
