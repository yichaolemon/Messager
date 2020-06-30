import java.util.UUID;
import java.util.List;
import java.net.InetAddress;


public interface Storage {
	public void storeMessage(Message message);
	public Message loadMessage(UUID uuid);
	// if block is true, this method waits until there are messages to return.
	public List<Message> loadMessageSince(InetAddress dstAddr, long timestamp, boolean block);
}

