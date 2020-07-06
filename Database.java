import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.HashMap;
import java.util.ArrayList;


public class Database implements Storage {
  private Map<UUID, Message> messageDatabase = new HashMap<UUID, Message>();
  private Map<Integer, SortedMap<Long, UUID>> timestampIndex = new HashMap<Integer, SortedMap<Long, UUID>>();
  // maps groupId to username+AES key encrypted with their public key
  private Map<Integer, Group> userGroups = new HashMap<Integer, Group>();
  private final Lock userGroupLock = new ReentrantLock();
  private final Lock messageDataLock = new ReentrantLock();
  private final Condition hasMoreMessages = messageDataLock.newCondition();

  public void storeMessage(Message message) {
    messageDataLock.lock();
    try {
      messageDatabase.put(message.getUuid(), message);
      SortedMap<Long, UUID> timeIndex = timestampIndex.get(message.getDstGroupId());
      if (timeIndex == null) {
        // System.out.println("Creating new Index for "+message.getDstAddr().getHostAddress());
        timeIndex = new TreeMap<Long, UUID>();
        timestampIndex.put(Integer.valueOf(message.getDstGroupId()), timeIndex);
      }
      timeIndex.put(Long.valueOf(message.getTimestamp()), message.getUuid());
      hasMoreMessages.signalAll();
    } finally {
      messageDataLock.unlock();
    }
  }

  public Message loadMessage(UUID uuid) {
    messageDataLock.lock();
    try {
      return messageDatabase.get(uuid);
    } finally {
      messageDataLock.unlock();
    }
  }

  public Group loadGroup(int groupId) {
    userGroupLock.lock();
    Group group = userGroups.get(Integer.valueOf(groupId));
    userGroupLock.unlock();
    return group;
  }

  public boolean createGroupIfNotExists(int groupIdToCreate, Map<String, String> usernameToKey) {
    userGroupLock.lock();
    if (userGroups.containsKey(Integer.valueOf(groupIdToCreate))) {
      userGroupLock.unlock();
      return false;
    }
    // now, create this group 
    // TODO: think about whether we want to make the user enter group name instread. 
    // also, do we want to check if these users all exist already? 
    userGroups.put(Integer.valueOf(groupIdToCreate), new Group(groupIdToCreate, usernameToKey));
    userGroupLock.unlock();
    messageDataLock.lock();
    timestampIndex.put(Integer.valueOf(groupIdToCreate), new TreeMap<Long, UUID>());
    messageDataLock.unlock();
    return true;
  }
  
  // this method is only called by  loadMessageSinceOrNull, which holds the `messageDataLock`
  private List<Message> loadMessageSinceOrNull(Integer dstGroupId, long timestamp) {
    SortedMap<Long, UUID> timeIndex = timestampIndex.get(dstGroupId);
    if (timeIndex == null) {
      // System.out.println("Index does not exist for "+dstAddr.getHostAddress());
      return null;
    }
    List<Message> messages = new ArrayList<Message>();
    for (Map.Entry<Long, UUID> e: timeIndex.tailMap(Long.valueOf(timestamp)).entrySet()) {
      messages.add(loadMessage(e.getValue()));
    }
    return messages.isEmpty() ? null : messages;
  }

  public List<Message> loadMessageSince(Integer dstGroupId, long timestamp, boolean block) {
    messageDataLock.lock();
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
      messageDataLock.unlock();
    }
  }
}

