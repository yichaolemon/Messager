import java.util.UUID;
import java.util.List;


public interface Storage {
  public void storeMessage(Message message);
  public Message loadMessage(UUID uuid);
  // if block is true, this method waits until there are messages to return.
  public List<Message> loadMessageSince(Integer dstGroupId, long timestamp, boolean block);
  // username is here in case we would like to overload this in the future. 
  public boolean createGroupIfNotExists(Integer groupIdToCreate, List<String> usernameList, String username); 
}

