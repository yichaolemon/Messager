import java.util.UUID;
import java.util.List;


public interface AuthStorage {
  public User loginOrRegister(String username, byte[] password);
  public List<String> getGroupUsers(int groupId);
  public int createGroup(List<String> userIdList);
}

