import java.security.SecureRandom;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;
import java.util.Random;
import java.util.HashSet;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.ListIterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


/*
 * UserAuthentication: 
 *   keeps track of user account handle and password
 *   user registration
 *   user chat groups (broadcast to everyone in the group)
 */
public class UserAuthentication implements AuthStorage {
  
  private static final int ITERATIONS = 10000;
  private static final int KEYLENGTH = 128;
  private Map<String, UserWithPassword> authDatabase; // maps from username to UserStorage 
  private List<String> usernameList = new ArrayList<String>(); // append only 
  private final Lock authDataLock = new ReentrantLock();
  private final Condition hasNewUserKey = authDataLock.newCondition();

  // username should be unique
  // password should be hashed 
  public class User {
    private String username;
    private Set<Integer> groups;
    private String publicKey; // used for getting the AES encryption key 
  
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

    public String getPublicKey() {
      return this.publicKey;
    }

    public User(String username, String publicKey) {
      this.username = username;
      this.groups = new HashSet<Integer>();
      this.publicKey = publicKey;
    }
  }
  
  private static class UserWithPassword {
    private User user; 
    private byte[] passwordHash;
    private byte[] salt;

    private boolean isVerifiedUser(String username, String password) {
      if (!username.equals(user.getUsername())) {
        return false;
      }
      try {
        byte[] givenHash = generateHash(password);
        return Arrays.equals(givenHash, passwordHash);
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
    }

    private byte[] generateHash(String password) throws Exception {
      PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEYLENGTH);
      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      return factory.generateSecret(spec).getEncoded();
    }

    public User getUser() {
      return user;
    }

    public void updateUser(User userStruct) {
      user = userStruct;
    }

    public boolean isInGroup(int groupId) {
      return user.isInGroup(groupId);
    }

    public UserWithPassword(User userStruct, String password) {
      user = userStruct;
      Random random = new SecureRandom();
      salt = new byte[16];
      random.nextBytes(salt);
      try {
        passwordHash = generateHash(password);
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
    }
  }
  
  public boolean isVerifiedGroupMember(String username, int groupId) {
    authDataLock.lock();
    UserWithPassword user = authDatabase.get(username);
    authDataLock.unlock();
    if (user == null) {
      return false;
    } else {
      return user.isInGroup(groupId); 
    }
  }

  public void updateNewGroupInfo(Integer groupId, List<String> usernameList) {
    authDataLock.lock();
    for (String username: usernameList) {
      UserWithPassword userContainer = authDatabase.get(username);
      if (userContainer == null) {
        // TODO: keep meta data for users that don't exist yet 
        continue;
      } 
      User user = userContainer.getUser();
      user.addGroup(Integer.valueOf(groupId));
      userContainer.updateUser(user);
      authDatabase.put(username, userContainer);
    }
    authDataLock.unlock();
  }

  private List<String> loadKeysSinceOrNull(int index/*, String username*/) { 
    if (usernameList.size() <= index) {
      return null;
    }
    ListIterator<String> iter = usernameList.listIterator(index);
    List<String> userPublicKeys  =  new ArrayList<String>();
    while (iter.hasNext()) {
      String user = iter.next();
      // if (user.equals(username)) {
        // continue;
      // }
      String userKey = authDatabase.get(user).getUser().getPublicKey();
      userPublicKeys.add(user+"|"+userKey);
    }
    return userPublicKeys.isEmpty() ? null : userPublicKeys;
  }

  public List<String> loadKeysSince(int index, boolean block, String username) {
    // System.out.println("Fetching messages since "+timestamp);
    authDataLock.lock();
    try {
      List<String> userKeyPairs;
      while ((userKeyPairs = loadKeysSinceOrNull(index/*, username*/)) == null || !block) {
        try {
          hasNewUserKey.await();
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }
      }
      return userKeyPairs;
    } finally {
      authDataLock.unlock();
    }
  }

  // TODO: allow users to change their public key across different logins
  public User loginOrRegister(String username, String password, String publicKey) {
    authDataLock.lock();
    UserWithPassword userInStorage = authDatabase.get(username);

    // register new user 
    if (userInStorage == null) {
      System.out.println("Registering new user with id: "+username+"password: "+password);
      // TODO: look up the group info 
      UserWithPassword newUser = new UserWithPassword(new User(username, publicKey), password);
      authDatabase.put(username, newUser);
      usernameList.add(username); // position in the list is its index 
      hasNewUserKey.signalAll();
      authDataLock.unlock();
      return newUser.getUser();
    }

    // authenticating existing user 
    authDataLock.unlock();
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
