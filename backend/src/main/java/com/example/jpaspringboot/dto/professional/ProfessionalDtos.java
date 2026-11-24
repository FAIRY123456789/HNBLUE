package com.example.jpaspringboot.dto.professional;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class ProfessionalDtos {
    private ProfessionalDtos() {}

    public record CreateRequest(
            @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_-]{3,64}$") String username) {}

    public record BatchCreateRequest(
            @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_-]{2,40}$") String prefix,
            @Min(1) @Max(300) int count) {}

    public record StatusRequest(boolean enabled) {}

    public record PasswordChangeRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 8, max = 72) String newPassword) {}

    public record EntitlementRequest(boolean basicQaEnabled,
                                     boolean professionalAnalysisEnabled,
                                     boolean multiAgentEnabled,
                                     @Min(1) @Max(100000) int dailyRequestLimit,
                                     @Min(1) @Max(100000000) long dailyTokenLimit,
                                     @Min(128) @Max(200000) int maxTokensPerRequest,
                                     @Min(1) @Max(32) int maxConcurrentTasks,
                                     @Min(0) @Max(100) int queuePriority) {}

    public record AccountView(Long id, String username, boolean enabled,
                              boolean passwordChangeRequired, long tokenVersion,
                              LocalDateTime createdAt, LocalDateTime updatedAt,
                              LocalDateTime lastLoginAt, EntitlementView entitlement) {}

    public record EntitlementView(boolean basicQaEnabled, boolean professionalAnalysisEnabled,
                                  boolean multiAgentEnabled, int dailyRequestLimit,
                                  long dailyTokenLimit, int maxTokensPerRequest,
                                  int maxConcurrentTasks, int queuePriority) {}

    public record TemporaryCredential(Long accountId, String username, String temporaryPassword) {}
    public record BatchResult(List<TemporaryCredential> created, List<String> skipped) {}
}
