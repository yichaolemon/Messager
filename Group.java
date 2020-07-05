import java.util.Map;

public class Group {
  private final int groupId;
  private final Map<String, String> usernameToKey;

  public int getGroupId() {
    return groupId;
  }

  public String getUserKeyInGroup(String username) {
    String encryptedKey = usernameToKey.get(username);
    return encryptedKey;
  }

  public boolean isUserInGroup(String username) {
    return usernameToKey.containsKey(username);
  }

  /* public List<String> getAllUsers() {

  } */

  public Group(int groupId, Map<String, String> usernameToKey) {
    this.groupId = groupId;
    this.usernameToKey = usernameToKey;
  }
}
