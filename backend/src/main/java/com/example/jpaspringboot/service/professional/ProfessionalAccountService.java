package com.example.jpaspringboot.service.professional;

import com.example.jpaspringboot.dto.professional.ProfessionalDtos;
import com.example.jpaspringboot.entity.AgentEntitlement;
import com.example.jpaspringboot.entity.ProfessionalAccount;
import com.example.jpaspringboot.repository.AdminRepository;
import com.example.jpaspringboot.repository.AgentEntitlementRepository;
import com.example.jpaspringboot.repository.ProfessionalAccountRepository;
import com.example.jpaspringboot.repository.UserRepository;
import com.example.jpaspringboot.security.HnbluePrincipal;
import com.example.jpaspringboot.service.impl.UserActivityLogServiceImpl;
import com.example.jpaspringboot.util.PasswordPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ProfessionalAccountService {
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ProfessionalAccountRepository accounts;
    private final AgentEntitlementRepository entitlements;
    private final UserRepository users;
    private final AdminRepository admins;
    private final PasswordEncoder passwordEncoder;
    private final UserActivityLogServiceImpl audit;

    public ProfessionalAccountService(ProfessionalAccountRepository accounts,
                                      AgentEntitlementRepository entitlements,
                                      UserRepository users,
                                      AdminRepository admins,
                                      PasswordEncoder passwordEncoder,
                                      UserActivityLogServiceImpl audit) {
        this.accounts = accounts;
        this.entitlements = entitlements;
        this.users = users;
        this.admins = admins;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    public Optional<ProfessionalAccount> authenticate(String username, String password) {
        if (username == null || password == null) return Optional.empty();
        return accounts.findByUsername(username.trim())
                .filter(ProfessionalAccount::isEnabled)
                .filter(account -> passwordEncoder.matches(password, account.getPasswordHash()));
    }

    @Transactional
    public void markLogin(ProfessionalAccount account) {
        account.setLastLoginAt(LocalDateTime.now());
        accounts.save(account);
    }

    @Transactional
    public ProfessionalDtos.TemporaryCredential create(String username, HnbluePrincipal actor) {
        String normalized = normalizeUsername(username);
        assertUsernameAvailable(normalized);
        return createInternal(normalized, actor);
    }

    @Transactional
    public ProfessionalDtos.BatchResult createBatch(String prefix, int count, HnbluePrincipal actor) {
        if (count < 1 || count > 300) throw new IllegalArgumentException("count must be between 1 and 300");
        String normalizedPrefix = normalizePrefix(prefix).toLowerCase(Locale.ROOT);
        List<ProfessionalDtos.TemporaryCredential> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String username = normalizedPrefix + "_" + String.format("%04d", i);
            if (!usernameAvailable(username)) {
                skipped.add(username);
                continue;
            }
            created.add(createInternal(username, actor));
        }
        return new ProfessionalDtos.BatchResult(List.copyOf(created), List.copyOf(skipped));
    }

    public Page<ProfessionalDtos.AccountView> list(String keyword, Pageable pageable) {
        Page<ProfessionalAccount> page = keyword == null || keyword.isBlank()
                ? accounts.findAll(pageable)
                : accounts.findByUsernameContainingIgnoreCase(keyword.trim(), pageable);
        return page.map(this::view);
    }

    @Transactional
    public ProfessionalDtos.AccountView setEnabled(long id, boolean enabled, HnbluePrincipal actor) {
        ProfessionalAccount account = requireAccount(id);
        if (account.isEnabled() != enabled) {
            account.setEnabled(enabled);
            account.setTokenVersion(account.getTokenVersion() + 1);
            accounts.save(account);
            record(actor, account, enabled ? "PROFESSIONAL_ENABLE" : "PROFESSIONAL_DISABLE");
        }
        return view(account);
    }

    @Transactional
    public ProfessionalDtos.TemporaryCredential resetPassword(long id, HnbluePrincipal actor) {
        ProfessionalAccount account = requireAccount(id);
        String temporaryPassword = generateTemporaryPassword();
        account.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        account.setPasswordChangeRequired(true);
        account.setTokenVersion(account.getTokenVersion() + 1);
        accounts.save(account);
        record(actor, account, "PROFESSIONAL_RESET_PASSWORD");
        return new ProfessionalDtos.TemporaryCredential(account.getId(), account.getUsername(), temporaryPassword);
    }

    @Transactional
    public void changeOwnPassword(HnbluePrincipal principal, String currentPassword, String newPassword) {
        PasswordPolicy.requireValid(newPassword);
        ProfessionalAccount account = requireAccount(principal.accountId());
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new IllegalArgumentException("current password is invalid");
        }
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        account.setPasswordChangeRequired(false);
        account.setTokenVersion(account.getTokenVersion() + 1);
        accounts.save(account);
        audit.record(Math.toIntExact(account.getId()), account.getUsername(), "PROFESSIONAL",
                Math.toIntExact(account.getId()), "PROFESSIONAL_CHANGE_PASSWORD",
                "Professional account changed its password", "/api/professional/change-password", "SUCCESS", null);
    }

    @Transactional
    public ProfessionalDtos.AccountView updateEntitlement(long id, ProfessionalDtos.EntitlementRequest request,
                                                           HnbluePrincipal actor) {
        ProfessionalAccount account = requireAccount(id);
        AgentEntitlement entitlement = entitlements.findByProfessionalAccountId(id).orElseGet(() -> newEntitlement(id));
        entitlement.setBasicQaEnabled(request.basicQaEnabled());
        entitlement.setProfessionalAnalysisEnabled(request.professionalAnalysisEnabled());
        entitlement.setMultiAgentEnabled(request.multiAgentEnabled());
        entitlement.setDailyRequestLimit(request.dailyRequestLimit());
        entitlement.setDailyTokenLimit(request.dailyTokenLimit());
        entitlement.setMaxTokensPerRequest(request.maxTokensPerRequest());
        entitlement.setMaxConcurrentTasks(request.maxConcurrentTasks());
        entitlement.setQueuePriority(request.queuePriority());
        entitlements.save(entitlement);
        record(actor, account, "PROFESSIONAL_UPDATE_ENTITLEMENT");
        return view(account);
    }

    public ProfessionalDtos.AccountView view(ProfessionalAccount account) {
        AgentEntitlement entitlement = entitlements.findByProfessionalAccountId(account.getId())
                .orElseGet(() -> newEntitlement(account.getId()));
        return new ProfessionalDtos.AccountView(account.getId(), account.getUsername(), account.isEnabled(),
                account.isPasswordChangeRequired(), account.getTokenVersion(), account.getCreatedAt(),
                account.getUpdatedAt(), account.getLastLoginAt(), entitlementView(entitlement));
    }

    public AgentEntitlement entitlementFor(long professionalId) {
        return entitlements.findByProfessionalAccountId(professionalId)
                .orElseGet(() -> newEntitlement(professionalId));
    }

    private ProfessionalDtos.TemporaryCredential createInternal(String username, HnbluePrincipal actor) {
        String password = generateTemporaryPassword();
        ProfessionalAccount account = new ProfessionalAccount();
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setEnabled(false);
        account.setPasswordChangeRequired(true);
        account.setTokenVersion(0);
        account.setCreatedBy(actor.username());
        account = accounts.save(account);
        entitlements.save(newEntitlement(account.getId()));
        record(actor, account, "PROFESSIONAL_CREATE_DISABLED");
        return new ProfessionalDtos.TemporaryCredential(account.getId(), account.getUsername(), password);
    }

    private AgentEntitlement newEntitlement(Long accountId) {
        AgentEntitlement entitlement = new AgentEntitlement();
        entitlement.setProfessionalAccountId(accountId);
        return entitlement;
    }

    private ProfessionalDtos.EntitlementView entitlementView(AgentEntitlement item) {
        return new ProfessionalDtos.EntitlementView(item.isBasicQaEnabled(), item.isProfessionalAnalysisEnabled(),
                item.isMultiAgentEnabled(), item.getDailyRequestLimit(), item.getDailyTokenLimit(),
                item.getMaxTokensPerRequest(), item.getMaxConcurrentTasks(), item.getQueuePriority());
    }

    private ProfessionalAccount requireAccount(long id) {
        return accounts.findById(id).orElseThrow(() -> new IllegalArgumentException("professional account not found"));
    }

    private void assertUsernameAvailable(String username) {
        if (!usernameAvailable(username)) throw new IllegalArgumentException("username already exists");
    }

    private boolean usernameAvailable(String username) {
        return !accounts.existsByUsernameIgnoreCase(username)
                && !users.existsByNameIgnoreCase(username)
                && !admins.existsByNameIgnoreCase(username);
    }

    private String normalizeUsername(String username) {
        if (username == null) throw new IllegalArgumentException("username is required");
        String value = username.trim();
        if (!value.matches("^[a-zA-Z0-9_-]{3,64}$")) {
            throw new IllegalArgumentException("username must contain 3-64 letters, digits, underscores or hyphens");
        }
        return value;
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null) throw new IllegalArgumentException("prefix is required");
        String value = prefix.trim();
        if (!value.matches("^[a-zA-Z0-9_-]{2,40}$")) {
            throw new IllegalArgumentException("prefix must contain 2-40 letters, digits, underscores or hyphens");
        }
        return value;
    }

    private String generateTemporaryPassword() {
        StringBuilder value = new StringBuilder("Aa9!");
        for (int i = 4; i < 20; i++) value.append(PASSWORD_ALPHABET.charAt(RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        char[] chars = value.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char t = chars[i]; chars[i] = chars[j]; chars[j] = t;
        }
        return new String(chars);
    }

    private void record(HnbluePrincipal actor, ProfessionalAccount target, String action) {
        audit.record(Math.toIntExact(actor.accountId()), actor.username(), actor.role().name(),
                Math.toIntExact(target.getId()), action, action, "/admin/professionals", "SUCCESS", null);
    }
}
