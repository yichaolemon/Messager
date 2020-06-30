import java.time.ZonedDateTime;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
	private final Lock lock = new ReentrantLock();
	private final Condition hasMoreMessages = lock.newCondition();

	public void storeMessage(Message message) {
		lock.lock();
		try {
			database.put(message.getUuid(), message);
			SortedMap<Long, UUID> timeIndex = timestampIndex.get(message.getDstAddr());
			if (timeIndex == null) {
        // System.out.println("Creating new Index for "+message.getDstAddr().getHostAddress());
				timeIndex = new TreeMap<Long, UUID>();
				timestampIndex.put(message.getDstAddr(), timeIndex);
			}
			timeIndex.put(new Long(message.getTimestamp()), message.getUuid());
			hasMoreMessages.signalAll();
		} finally {
			lock.unlock();
		}
	}

	public Message loadMessage(UUID uuid) {
		lock.lock();
		try {
			return database.get(uuid);
		} finally {
			lock.unlock();
		}
	}

	private List<Message> loadMessageSinceOrNull(InetAddress dstAddr, long timestamp) {
		SortedMap<Long, UUID> timeIndex = timestampIndex.get(dstAddr);
		if (timeIndex == null) {
      // System.out.println("Index does not exist for "+dstAddr.getHostAddress());
			return null;
		}
		List<Message> messages = new ArrayList<Message>();
		for (Map.Entry<Long, UUID> e: timeIndex.tailMap(new Long(timestamp)).entrySet()) {
			messages.add(loadMessage(e.getValue()));
		}
		return messages.isEmpty() ? null : messages;
	}

	public List<Message> loadMessageSince(InetAddress dstAddr, long timestamp, boolean block) {
		lock.lock();
    // System.out.println("Fetching messages since "+timestamp);
		try {
			List<Message> messages;
			while ((messages = loadMessageSinceOrNull(dstAddr, timestamp)) == null || !block) {
        try {
          hasMoreMessages.await();
        } catch (Exception e) {
          return null;
        }
			}
			return messages;
		} finally {
			lock.unlock();
		}
	}
}

