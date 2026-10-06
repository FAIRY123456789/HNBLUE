package com.example.jpaspringboot.service.ai;

import com.example.jpaspringboot.entity.AiChatMessage;
import com.example.jpaspringboot.entity.AiUserMemory;
import com.example.jpaspringboot.repository.AiChatMessageRepository;
import com.example.jpaspringboot.repository.AiUserMemoryRepository;
import com.example.jpaspringboot.util.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class AiConversationService {
    private static final Pattern MEMORY_INTENT = Pattern.compile("^(?:请记住|记住|我希望|我更喜欢|我的偏好是|我关注|本项目需要).{2,280}$");
    private static final Pattern SENSITIVE = Pattern.compile("(?i)(密码|口令|token|api[ _-]?key|身份证|出生日期|邮箱|手机号|银行卡|健康|病史)");
    private final AiChatMessageRepository messageRepository;
    private final AiUserMemoryRepository memoryRepository;
    private final ObjectMapper objectMapper;

    public AiConversationService(AiChatMessageRepository messageRepository,
                                 AiUserMemoryRepository memoryRepository,
                                 ObjectMapper objectMapper) {
        this.messageRepository = messageRepository;
        this.memoryRepository = memoryRepository;
        this.objectMapper = objectMapper;
    }

    public Identity resolveIdentity(String authorization, String sessionId) {
        if (authorization != null && !authorization.isBlank()) {
            try {
                String username = JwtUtils.getUsernameFromToken(authorization);
                if (username != null && !username.isBlank()) {
                    return new Identity("user:" + username.trim().toLowerCase(Locale.ROOT), true);
                }
            } catch (Exception ignored) {
                // Invalid/expired tokens are treated as anonymous; never trust client user identifiers.
            }
        }
        return new Identity("anon:" + sessionId, false);
    }

    public List<Map<String, String>> recentMessages(Identity identity, String sessionId) {
        List<AiChatMessage> rows = new ArrayList<>(
                messageRepository.findTop20ByOwnerKeyAndSessionIdOrderByCreatedAtDescIdDesc(identity.ownerKey(), sessionId));
        Collections.reverse(rows);
        return rows.stream().map(row -> Map.of("role", row.getRole(), "content", row.getContent())).toList();
    }

    public String contextualizeLocally(String question, List<Map<String, String>> recent) {
        if (question == null || recent == null || recent.isEmpty()) return question;
        String compact = question.trim();
        boolean refersBack = compact.matches(".*(这个|那个|它|上述|前面|刚才|该方法|该数据|这些|其中|前者|后者|上一个|首项|第一项|最后一项|末项|第[一二三四五六七八九十0-9]+项).*");
        if (!refersBack || compact.length() > 120) return compact;
        for (int i = recent.size() - 1; i >= 0; i--) {
            Map<String, String> item = recent.get(i);
            if ("user".equals(item.get("role")) && item.get("content") != null && !item.get("content").isBlank()) {
                String previous = item.get("content").replaceAll("\\s+", " ").trim();
                if (previous.length() > 220) previous = previous.substring(0, 220);
                return "为消解当前追问中的指代，请结合前一相关问题检索。\n前一相关问题：" + previous + "\n当前问题：" + compact;
            }
        }
        return compact;
    }

    public List<String> keyMemories(Identity identity) {
        if (!identity.authenticated()) return List.of();
        return memoryRepository.findTop20ByOwnerKeyOrderByUpdatedAtDesc(identity.ownerKey())
                .stream().map(AiUserMemory::getMemoryValue).toList();
    }

    @Transactional
    public AiChatMessage saveMessage(Identity identity, String sessionId, String role, String content,
                                     Object sources, String evidenceStatus, String modelName, String injectionRisk) {
        AiChatMessage row = new AiChatMessage();
        row.setOwnerKey(identity.ownerKey());
        row.setSessionId(sessionId);
        row.setRole(role);
        row.setContent(content);
        row.setEvidenceStatus(evidenceStatus);
        row.setModelName(modelName);
        row.setInjectionRisk(injectionRisk);
        try { row.setSourcesJson(sources == null ? null : objectMapper.writeValueAsString(sources)); }
        catch (Exception ignored) { row.setSourcesJson(null); }
        return messageRepository.save(row);
    }

    @Transactional
    public void maybeRemember(Identity identity, AiChatMessage source, String text, boolean injectionBlocked) {
        if (!identity.authenticated() || injectionBlocked || text == null) return;
        String value = text.trim().replaceAll("\\s+", " ");
        if (value.length() < 4 || value.length() > 300 || !MEMORY_INTENT.matcher(value).matches()
                || SENSITIVE.matcher(value).find()) return;
        String key = "explicit-" + Integer.toUnsignedString(value.toLowerCase(Locale.ROOT).hashCode(), 36);
        AiUserMemory memory = memoryRepository.findByOwnerKeyAndMemoryKey(identity.ownerKey(), key)
                .orElseGet(AiUserMemory::new);
        memory.setOwnerKey(identity.ownerKey());
        memory.setMemoryKey(key);
        memory.setMemoryValue(value);
        memory.setConfidence(0.95d);
        memory.setSourceMessageId(source.getId());
        memoryRepository.save(memory);
    }

    public List<Map<String, Object>> history(Identity identity, String sessionId) {
        return messageRepository.findTop200ByOwnerKeyAndSessionIdOrderByCreatedAtAscIdAsc(identity.ownerKey(), sessionId)
                .stream().map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("role", row.getRole());
                    item.put("content", row.getContent());
                    item.put("evidenceStatus", row.getEvidenceStatus());
                    item.put("createdAt", row.getCreatedAt());
                    return item;
                }).toList();
    }

    @Transactional
    public long clear(Identity identity, String sessionId) {
        return messageRepository.deleteByOwnerKeyAndSessionId(identity.ownerKey(), sessionId);
    }

    public record Identity(String ownerKey, boolean authenticated) {}
}
