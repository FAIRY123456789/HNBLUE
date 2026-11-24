package com.example.jpaspringboot.security;

import com.example.jpaspringboot.entity.Admin;
import com.example.jpaspringboot.entity.ProfessionalAccount;
import com.example.jpaspringboot.entity.User;
import com.example.jpaspringboot.repository.AdminRepository;
import com.example.jpaspringboot.repository.ProfessionalAccountRepository;
import com.example.jpaspringboot.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class IdentityService {
    private final AdminRepository adminRepository;
    private final ProfessionalAccountRepository professionalRepository;
    private final UserRepository userRepository;

    public IdentityService(AdminRepository adminRepository,
                           ProfessionalAccountRepository professionalRepository,
                           UserRepository userRepository) {
        this.adminRepository = adminRepository;
        this.professionalRepository = professionalRepository;
        this.userRepository = userRepository;
    }

    /**
     * Resolve every request against current database state. JWT role claims are
     * descriptive only; they never grant authority by themselves.
     */
    public Optional<HnbluePrincipal> resolve(String username, Long claimedTokenVersion) {
        if (username == null || username.isBlank()) return Optional.empty();

        Admin admin = adminRepository.findByName(username);
        if (admin != null) {
            return Optional.of(new HnbluePrincipal(admin.getId().longValue(), admin.getName(), AccountRole.ADMIN, 0, false));
        }

        ProfessionalAccount professional = professionalRepository.findByUsername(username).orElse(null);
        if (professional != null) {
            if (!professional.isEnabled()) return Optional.empty();
            if (claimedTokenVersion != null && claimedTokenVersion.longValue() != professional.getTokenVersion()) {
                return Optional.empty();
            }
            return Optional.of(new HnbluePrincipal(professional.getId(), professional.getUsername(),
                    AccountRole.PROFESSIONAL, professional.getTokenVersion(), professional.isPasswordChangeRequired()));
        }

        User user = userRepository.findByName(username);
        if (user != null) {
            return Optional.of(new HnbluePrincipal(user.getId().longValue(), user.getName(), AccountRole.PUBLIC, 0, false));
        }
        return Optional.empty();
    }

    /**
     * Resolve a queued task against the exact role that owned it. This avoids
     * accepting a same-named account from another identity domain and makes
     * disable/token-version changes effective before durable execution starts.
     */
    public Optional<HnbluePrincipal> resolveExpected(AccountRole role, String username,
                                                      Long claimedTokenVersion) {
        if (role == null || username == null || username.isBlank()) return Optional.empty();
        return switch (role) {
            case ADMIN -> {
                Admin admin = adminRepository.findByName(username);
                yield admin == null ? Optional.empty() : Optional.of(new HnbluePrincipal(
                        admin.getId().longValue(), admin.getName(), AccountRole.ADMIN, 0, false));
            }
            case PROFESSIONAL -> professionalRepository.findByUsername(username)
                    .filter(ProfessionalAccount::isEnabled)
                    .filter(account -> claimedTokenVersion == null
                            || account.getTokenVersion() == claimedTokenVersion.longValue())
                    .map(account -> new HnbluePrincipal(account.getId(), account.getUsername(),
                            AccountRole.PROFESSIONAL, account.getTokenVersion(),
                            account.isPasswordChangeRequired()));
            case PUBLIC -> {
                User user = userRepository.findByName(username);
                yield user == null ? Optional.empty() : Optional.of(new HnbluePrincipal(
                        user.getId().longValue(), user.getName(), AccountRole.PUBLIC, 0, false));
            }
        };
    }
}
