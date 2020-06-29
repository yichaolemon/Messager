import java.util.UUID;
import java.net.InetAddress;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;

/* container class for messages */
public class Message {
	private final UUID uuid;
	// server timestamp 
	private final long timestamp;
	private final InetAddress srcAddr;
	private final InetAddress dstAddr;
	private final String content;

	public UUID getUuid() {
		return uuid;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public InetAddress getSrcAddr() {
		return srcAddr;
	}

	public InetAddress getDstAddr() {
		return dstAddr;
	}

	public String getContent() {
		return content;
	}

  public String toContentString() {
    StringBuilder stringBuilder = new StringBuilder();
    DateFormat formatter = new SimpleDateFormat("@[MM/dd HH:mm:ss]: ");
    Date date = new Date(this.getTimestamp());
    stringBuilder.append(this.getSrcAddr().getHostAddress());
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
		stringBuilder.append("\tdstAddress:\t"+dstAddr.getHostAddress()+"\n");
		stringBuilder.append("\tContent:\t"+content+"\n");
		return stringBuilder.toString();
	}

	public Message(UUID uuid, long timestamp, InetAddress srcAddr, InetAddress dstAddr, String content) {
		this.uuid = uuid;
		this.timestamp = timestamp;
		this.srcAddr = srcAddr;
		this.dstAddr = dstAddr;
		this.content = content; 
	}
}
