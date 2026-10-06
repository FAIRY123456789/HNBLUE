package com.example.jpaspringboot.service.ai;

import com.example.jpaspringboot.entity.SystemSecret;
import com.example.jpaspringboot.repository.SystemSecretRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ApiSecretService {
    public static final String DEEPSEEK_SECRET = "deepseek_api_key";
    private final SystemSecretRepository repository;
    private final SecretCryptoService cryptoService;
    private final String environmentDeepSeekKey;

    public ApiSecretService(SystemSecretRepository repository,
                            SecretCryptoService cryptoService,
                            @Value("${api.password:}") String environmentDeepSeekKey) {
        this.repository = repository;
        this.cryptoService = cryptoService;
        this.environmentDeepSeekKey = normalize(environmentDeepSeekKey);
    }

    public Optional<String> getDeepSeekApiKey() {
        Optional<SystemSecret> stored = repository.findBySecretName(DEEPSEEK_SECRET);
        if (stored.isPresent() && cryptoService.isReady()) {
            try {
                SystemSecret value = stored.get();
                return Optional.of(cryptoService.decrypt(value.getSecretName(), value.getCipherText(), value.getInitializationVector()));
            } catch (IllegalStateException ignored) {
                // A wrong or rotated master key must not take down the assistant. The status endpoint reports it as unusable.
            }
        }
        return Optional.ofNullable(environmentDeepSeekKey);
    }

    public ProviderSetupStatus setupStatus() {
        return new ProviderSetupStatus(repository.existsBySecretName(DEEPSEEK_SECRET), cryptoService.isReady());
    }

    @Transactional
    public ProviderSetupStatus saveDeepSeekApiKeyOnce(String apiKey) {
        if (repository.existsBySecretName(DEEPSEEK_SECRET)) throw new SecretAlreadyConfiguredException();
        String normalized = validate(apiKey);
        if (!cryptoService.isReady()) throw new IllegalStateException("服务器尚未配置密钥加密主密钥");
        SecretCryptoService.EncryptedValue encrypted = cryptoService.encrypt(DEEPSEEK_SECRET, normalized);
        SystemSecret entity = new SystemSecret();
        entity.setSecretName(DEEPSEEK_SECRET);
        entity.setCipherText(encrypted.cipherText());
        entity.setInitializationVector(encrypted.initializationVector());
        entity.setLastFour("hidden");
        entity.setKeyVersion(1);
        entity.setUpdatedBy("one-time-setup");
        try {
            repository.saveAndFlush(entity);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new SecretAlreadyConfiguredException();
        }
        return new ProviderSetupStatus(true, true);
    }

    private String validate(String apiKey) {
        String normalized = normalize(apiKey);
        if (normalized == null || normalized.length() < 16 || normalized.length() > 256) {
            throw new IllegalArgumentException("API Key 长度必须为 16–256 个字符");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("API Key 不能包含空白字符");
        }
        return normalized;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    public record ProviderSetupStatus(boolean locked, boolean encryptionReady) {}

    public static class SecretAlreadyConfiguredException extends IllegalStateException {
        public SecretAlreadyConfiguredException() { super("API Key 已完成一次性设置，不能查看、修改或清除"); }
    }
}
