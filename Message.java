import java.util.UUID;
import java.net.InetAddress;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

/* container class for messages */
public class Message {
  private final UUID uuid;
  private final long timestamp; // server timestamp 
  private final InetAddress srcAddr;
  private final String srcUsername;
  private final int dstGroupId;
  private final String content;

  public UUID getUuid() {
    return uuid;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public int getDstGroupId() {
    return dstGroupId;
  }

  public InetAddress getSrcAddr() {
    return srcAddr;
  }

  public String getSrcUsername() {
    return srcUsername;
  }

  public String getContent() {
    return content;
  }

  public String toContentString() {
    StringBuilder stringBuilder = new StringBuilder();
    DateFormat formatter = new SimpleDateFormat("@[MM/dd HH:mm:ss]: ");
    Date date = new Date(this.getTimestamp());
    stringBuilder.append(this.getSrcUsername());
    stringBuilder.append(formatter.format(date));
    stringBuilder.append(this.getContent());
    return stringBuilder.toString();
  }

  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("Message:\n");
    stringBuilder.append("\tUUID:\t"+uuid.toString()+"\n");
    stringBuilder.append("\tTimestamp:\t"+Long.toString(timestamp)+"\n");
    stringBuilder.append("\tsrcAddress:\t"+srcAddr.getHostAddress()+"\n");
    stringBuilder.append("\tsrcUsername:\t"+srcUsername+"\n");
    stringBuilder.append("\tdstGroupId:\t"+String.valueOf(dstGroupId)+"\n");
    stringBuilder.append("\tContent:\t"+content+"\n");
    return stringBuilder.toString();
  }

  public Message(UUID uuid, long timestamp, InetAddress srcAddr, String srcUsername, int dstGroupId, String content) {
    this.uuid = uuid;
    this.timestamp = timestamp;
    this.srcAddr = srcAddr;
    this.srcUsername = srcUsername;
    this.dstGroupId = dstGroupId;
    this.content = content; 
  }
}
