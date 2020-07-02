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
  private Map<UUID, Message> messageDatabase = new HashMap<UUID, Message>();
  private Map<Integer, SortedMap<Long, UUID>> timestampIndex = new HashMap<Integer, SortedMap<Long, UUID>>();
  private Map<Integer, Set<String>> userGroups = new HashMap<Integer, Set<String>>();

  private final Lock lock = new ReentrantLock();
  private final Condition hasMoreMessages = lock.newCondition();

  public void storeMessage(Message message) {
    lock.lock();
    try {
      messageDatabase.put(message.getUuid(), message);
      SortedMap<Long, UUID> timeIndex = timestampIndex.get(message.getDstGroupId());
      if (timeIndex == null) {
        // System.out.println("Creating new Index for "+message.getDstAddr().getHostAddress());
        timeIndex = new TreeMap<Long, UUID>();
        timestampIndex.put(message.getDstGroupId(), timeIndex);
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
      return messageDatabase.get(uuid);
    } finally {
      lock.unlock();
    }
  }

  public boolean createGroupIfNotExists(Integer groupIdToCreate, String<List> usernameList, String username) {
    if (userGroups.containsKey(groupIdToCreate)) {
      return false;
    }
    // now, create this group 
    // TODO: think about whether we want to make the user enter group name instread. 
    // also, do we want to check if these users all exist already? 
    userGroups.put(groupIdToCreate, usernameList);
    timestampIndex.put(groupIdToCreate, new TreeMap<Long, UUID>());
    return true;
  }
  
  private List<Message> loadMessageSinceOrNull(Integer dstGroupId, long timestamp) {
    SortedMap<Long, UUID> timeIndex = timestampIndex.get(dstGroupId);
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

  public List<Message> loadMessageSince(Integer dstGroupId, long timestamp, boolean block) {
    lock.lock();
    // System.out.println("Fetching messages since "+timestamp);
    try {
      List<Message> messages;
      while ((messages = loadMessageSinceOrNull(dstGroupId, timestamp)) == null || !block) {
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

