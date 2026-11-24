package com.example.jpaspringboot.security;

import java.security.Principal;

public record HnbluePrincipal(Long accountId, String username, AccountRole role,
                              long tokenVersion, boolean passwordChangeRequired) implements Principal {
    @Override
    public String getName() {
        return username;
    }

    public String subjectKey() {
        return role.name() + ":" + accountId;
    }
}
