import java.security.SecureRandom;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;
import java.util.Random;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;


/*
 * UserAuthentication: 
 *   keeps track of user account handle and password
 *   user registration
 *   user chat groups (broadcast to everyone in the group)
 */
public class UserAuthentication implements AuthStorage {
  
  private static final int ITERATIONS = 10000;
  private static final int KEYLENGTH = 128;
  private static Map<String, UserWithPassword> authDatabase; // maps from username to UserStorage 

  // username should be unique
  // password should be hashed 
  public class User {
    private String username;
    private Set<Integer> groups;
  
    public void addGroup(int groupId) {
      groups.add(Integer.valueOf(groupId));
    }

    public Set<Integer> getGroups() {
      return this.groups;
    }

    public boolean isInGroup(int groupId) {
      return groups.contains(Integer.valueOf(groupId));
    }

    public String getUsername() {
      return this.username;
    }

    public User(String username) {
      this.username = username;
      this.groups = new HashSet<Integer>();
    }
  }
  
  private static class UserWithPassword {
    private static User user; 
    private static byte[] passwordHash;
    private static byte[] salt;
    private static Random random;

    private boolean isVerifiedUser(String username, String password) {
      if (username != user.getUsername()) {
        return false;
      }
      return Arrays.equals(generateHash(password), passwordHash);
    }

    private byte[] generateHash(String password) {
      PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEYLENGTH);
      try {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        passwordHash = factory.generateSecret(spec).getEncoded();
        return passwordHash;
      } catch (Exception e) {
        throw new AssertionError("Error while hashing a password: " + e.getMessage(), e);
      } finally {
        spec.clearPassword();
      }
    }

    public User getUser() {
      return user;
    }

    public void updateUser(User user) {
      this.user = user;
    }

    public static boolean isInGroup(int groupId) {
      return user.isInGroup(groupId);
    }

    public UserWithPassword(User user, String password) {
      this.user = user;
      random = new SecureRandom();
      salt = new byte[16];
      random.nextBytes(salt);
      passwordHash = generateHash(password);
    }
  }
  
  public boolean isVerifiedGroupMember(String username, int groupId) {
    UserWithPassword user = authDatabase.get(username);
    if (user == null) {
      return false;
    } else {
      return user.isInGroup(groupId); 
    }
  }

  public void updateNewGroupInfo(Integer groupId, List<String> usernameList) {
    for (String username: usernameList) {
      UserWithPassword userContainer = authDatabase.get(username);
      if (userContainer == null) {
        // TODO: keep meta data for users that don't exist yet 
        continue;
      } 
      if (authDatabase.containsKey(username)) {
        User user = authDatabase.get(username).getUser();
        user.addGroup(Integer.valueOf(groupId));
        userContainer.updateUser(user);
        authDatabase.put(username, userContainer);
      }
    }
  }

  public User loginOrRegister(String username, String password) {
    UserWithPassword userInStorage = authDatabase.get(username);

    // register new user 
    if (userInStorage == null) {
      System.out.println("Registering new user with id: "+username);
      // TODO: look up the group info 
      UserWithPassword newUser = new UserWithPassword(new User(username), password);
      authDatabase.put(username, newUser);
      return newUser.getUser();
    }

    // authenticating existing user 
    if (userInStorage.isVerifiedUser(username, password)) {
      return userInStorage.getUser();
    } else {
      return null;
    }
  }

  public UserAuthentication() {
    authDatabase = new HashMap<String, UserWithPassword>();
  }
}
