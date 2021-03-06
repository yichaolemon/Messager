import java.util.List;


public interface AuthStorage {
  public UserAuthentication.User loginOrRegister(String username, String password, String publicKey);
  public void updateNewGroupInfo(Integer groupId, List<String> usernameList);
  public boolean isVerifiedGroupMember(String username, int groupId);
}

