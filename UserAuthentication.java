import java.security.SecureRandom;
import java.utils.Arrays;


/*
 * UserAuthentication: 
 *   keeps track of user account handle and password
 *   user registration
 *   user chat groups (broadcast to everyone in the group)
 */
public class UserAuthentication {
  
  private static final int ITERATIONS = 10000;
  private static final int KEYLENGTH = 128;
  // username should be unique
  // password should be hashed 
  public class User {
    private String username;
    private Set<Integer> groups;
  
    public void addGroup(int groupId) {
      groups.add(new Integer(groupId));
    }

    public Set<Integer> getGroups() {
      return this.groups;
    }

    public String getUsername() {
      return this.username;
    }

    public User(String username) {
      this.username = username;
      this.groups = new HashSet<Integer>();
    }
  }
  
  private class UserWithPassword {
    private static final User user; 
    private static final byte[] passwordHash;
    private static final byte[] salt;
    private static final Random random;

    private boolean isVerifiedUser(String username, String password) {
      if (username != user.getUsername()) {
        return false;
      }
      return Arrays.equals(generateHash(password), passwordHash);
    }

    private static byte[] generateHash(String password) {
      KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEYLENGTH);
      try {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        passwordHash = factory.generateSecret(spec).getEncoded();
        return passwordHash;
      } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
        throw new AssertionError("Error while hashing a password: " + e.getMessage(), e);
      } finally {
        spec.clearPassword();
      }
    }

    private User getUser() {
      return user;
    }

    private UserWithPassword(User user, String password) {
      this.user = user;
      random = new SecureRandom();
      salt = new byte[16];
      random.nextBytes(salt);
      passwordHash = generateHash(password);
    }
  }
  // maps from username to UserStorage 
  private Map<String, UserWithPassword> authDatabase;
  
  // TODO: implement these two functions 
  public boolean isVerifiedGroupMember(String username, int groupId) {

  }

  public void updateNewGroupInfo(Integer groupId, List<String> usernameList) {

  }

  public User loginOrRegister(String username, String password) {
    byte[] passwordHash = generateHash(password);
    UserWithPassword userInStorage = authDatabase.get(username);

    // register new user 
    if (userInStorage == null) {
      System.out.println("Registering new user with id: "+username);
      UserWithPassword newUser = new UserWithPassword(new User(username), password);
      authDatabase.put(newUser);
      return newUser.getUser();
    }

    // authenticating existing user 
    if (userInStorage.isVerifiedUser(username, password)) {
      return userInStorage.getUser();
    } else {
      return null;
    }
  }

  public List<String> getGroupUsers(int groupId);
  public int createGroup(List<String> userIdList);

  public UserAuthentication() {
    authDatabase = new HashMap<String, UserWithPassword>();
    SecureRandom random = new SecureRandom();
  }
}
