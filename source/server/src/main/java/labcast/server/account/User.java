package labcast.server.account;

import labcast.common.protocol.Role;

public record User(long id, String username, String fullName, Role role, PasswordHasher.Hashed password) {}
