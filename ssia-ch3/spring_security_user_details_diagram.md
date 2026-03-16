
# Spring Security User Management Components

## Diagram

```
UserDetailsService ───────► UserDetails ───────► GrantedAuthority
        ▲                        │
        │                        │
        │                        ▼
   UserDetailsManager ───────────
```

## Explanation

### UserDetailsService

Interface responsible for loading user data.

Typical method:

```java
UserDetails loadUserByUsername(String username)
```

Spring Security calls this method during authentication to retrieve user information.

---

### UserDetails

Represents a user in Spring Security.

Typical fields:

- username
- password (hashed)
- authorities (roles / permissions)
- account status (enabled, locked, expired)

Example:

```java
UserDetails user =
    User.withUsername("user")
        .password("{bcrypt}$2a$10$...")
        .roles("USER")
        .build();
```

---

### GrantedAuthority

Represents a **permission or role** granted to a user.

Examples:

ROLE_USER  
ROLE_ADMIN  
READ_PRIVILEGES  
WRITE_PRIVILEGES  

Example implementation:

```java
new SimpleGrantedAuthority("ROLE_ADMIN")
```

A user may have **multiple authorities**.

---

### UserDetailsManager

Extension of `UserDetailsService` that also allows **user management operations**.

Extra capabilities:

- createUser
- updateUser
- deleteUser
- changePassword

Example:

```java
UserDetailsManager manager = new InMemoryUserDetailsManager();

manager.createUser(
    User.withUsername("admin")
        .password("{noop}password")
        .roles("ADMIN")
        .build()
);
```

---

## Relationship Summary

| Component | Responsibility |
|---|---|
| UserDetailsService | Loads user by username |
| UserDetails | Represents authenticated user |
| GrantedAuthority | Represents roles / permissions |
| UserDetailsManager | Manages user accounts |

---

## Authentication Flow (simplified)

```
login request
      ↓
UserDetailsService.loadUserByUsername()
      ↓
UserDetails
      ↓
GrantedAuthority (roles)
      ↓
Authentication created
```
