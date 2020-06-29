import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.HashMap;
import java.util.ArrayList;

public class Database implements Storage {
	private Map<UUID, Message> database = new HashMap<UUID, Message>();
	private Map<InetAddress, SortedMap<Long, UUID>> timestampIndex = new HashMap<InetAddress, SortedMap<Long, UUID>>();

	public synchronized void storeMessage(Message message) {
		database.put(message.getUuid(), message);
		SortedMap<Long, UUID> timeIndex = timestampIndex.get(message.getDstAddr());
		if (timeIndex == null) {
			timeIndex = new TreeMap<Long, UUID>();
			timestampIndex.put(message.getDstAddr(), timeIndex);
		}
		timeIndex.put(new Long(message.getTimestamp()), message.getUuid());
	}

	public synchronized Message loadMessage(UUID uuid) {
		return database.get(uuid);
	}

	public synchronized List<Message> loadMessageSince(InetAddress dstAddr, long timestamp) {
		SortedMap<Long, UUID> timeIndex = timestampIndex.get(dstAddr);
		if (timeIndex == null) {
			return null;
		}
		List<Message> messages = new ArrayList<Message>();
		for (Map.Entry<Long, UUID> e: timeIndex.tailMap(new Long(timestamp)).entrySet()) {
			messages.add(loadMessage(e.getValue()));
		}
		return messages;
	}
}

