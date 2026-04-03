package foodbridge.models;

public class User {
    private int    userId;
    private String name;
    private String role;   // "DONOR" or "NGO"
    private String phone;
    private String email;

    public User(int userId, String name, String role, String phone, String email) {
        this.userId = userId;
        this.name   = name;
        this.role   = role;
        this.phone  = phone;
        this.email  = email;
    }

    public int    getUserId() { return userId; }
    public String getName()   { return name;   }
    public String getRole()   { return role;   }
    public String getPhone()  { return phone;  }
    public String getEmail()  { return email;  }

    @Override
    public String toString() {
        return String.format("User[id=%d, name=%s, role=%s]", userId, name, role);
    }
}
