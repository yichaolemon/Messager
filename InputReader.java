import java.util.Scanner;

public class InputReader {
  private Scanner usrSysInput;
  private Sender sender;

  public void run() {

    if (sender.isInRepl()) {
      System.out.printf("❯ ");
    }
    while (usrSysInput.hasNextLine()) {
      String line = usrSysInput.nextLine();
      if (sender.isInRepl()) {
        System.out.printf("❯ ");
      }
      sender.writeMsg(line);
    }
  }

  public InputReader (Sender sender) {
    usrSysInput = new Scanner(System.in);
    this.sender = sender; 
  }
}
