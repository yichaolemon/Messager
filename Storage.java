import java.util.UUID;
import java.util.List;
import java.util.Map;


public interface Storage {
  public void storeMessage(Message message);
  public Message loadMessage(UUID uuid);
  // if block is true, this method waits until there are messages to return.
  public List<Message> loadMessageSince(Integer dstGroupId, long timestamp, boolean block);
  // username is here in case we would like to overload this in the future. 
  public boolean createGroupIfNotExists(int groupIdToCreate, Map<String, String> usernameToKey); 
  public Group loadGroup(int groupId);

}

