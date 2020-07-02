

/*
 * UserAuthentication: 
 *   keeps track of user account handle and password
 *   user registration
 *   user chat groups (broadcast to everyone in the group)
 */
public class UserAuthentication {

  // user name should be unique
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

    public User(String username) {
      this.username = username;
      this.groups = new HashSet<Integer>();
    }
  }

  public UserAuthentication() {

  }
}
