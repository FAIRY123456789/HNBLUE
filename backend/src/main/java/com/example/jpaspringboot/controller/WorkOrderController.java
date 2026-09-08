package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.entity.Admin;
import com.example.jpaspringboot.entity.User;
import com.example.jpaspringboot.entity.WorkOrder;
import com.example.jpaspringboot.entity.WorkOrderActionLog;
import com.example.jpaspringboot.repository.AdminRepository;
import com.example.jpaspringboot.repository.UserRepository;
import com.example.jpaspringboot.repository.WorkOrderActionLogRepository;
import com.example.jpaspringboot.repository.WorkOrderRepository;
import com.example.jpaspringboot.util.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {
    private static final String SUBMITTED = "SUBMITTED";
    private static final String PROCESSING = "PROCESSING";
    private static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String RETURNED = "RETURNED";
    private static final String CLOSED = "CLOSED";
    private static final String DEFAULT_ASSIGNEE = "Admin_100001";

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderActionLogRepository logRepository;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;

    public WorkOrderController(WorkOrderRepository workOrderRepository,
                               WorkOrderActionLogRepository logRepository,
                               AdminRepository adminRepository,
                               UserRepository userRepository) {
        this.workOrderRepository = workOrderRepository;
        this.logRepository = logRepository;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Actor actor = actorFromToken(authorization);
        if (actor == null) return unauthorized();
        List<WorkOrder> rows;
        if (actor.firstLevelAdmin) {
            rows = workOrderRepository.findAllByOrderByUpdatedAtDesc();
        } else if (actor.admin) {
            rows = new ArrayList<>(workOrderRepository.findByAssignedAdminNameOrderByUpdatedAtDesc(actor.name));
            workOrderRepository.findBySubmitterNameOrderByUpdatedAtDesc(actor.name).forEach(item -> {
                if (rows.stream().noneMatch(existing -> Objects.equals(existing.getId(), item.getId()))) rows.add(item);
            });
            rows.sort(Comparator.comparing(WorkOrder::getUpdatedAt).reversed());
        } else {
            rows = workOrderRepository.findBySubmitterNameOrderByUpdatedAtDesc(actor.name);
        }
        return ResponseEntity.ok(Map.of("data", rows.stream().map(item -> toDto(item, false)).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id,
                                    @RequestHeader(value = "Authorization", required = false) String authorization) {
        Actor actor = actorFromToken(authorization);
        if (actor == null) return unauthorized();
        Optional<WorkOrder> optional = workOrderRepository.findById(id);
        if (optional.isEmpty()) return notFound();
        WorkOrder order = optional.get();
        if (!canView(actor, order)) return forbidden("No permission for this work order");
        return ResponseEntity.ok(Map.of("data", toDto(order, true)));
    }


    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody WorkOrderRequest request,
                                    @RequestHeader(value = "Authorization", required = false) String authorization) {
        Actor actor = actorFromToken(authorization);
        if (actor == null) return unauthorized();
        Optional<WorkOrder> optional = workOrderRepository.findById(id);
        if (optional.isEmpty()) return notFound();
        WorkOrder order = optional.get();
        if (!canView(actor, order)) return forbidden("No permission for this work order");
        if (CLOSED.equals(order.getStatus()) || APPROVED.equals(order.getStatus()) || REJECTED.equals(order.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Terminal work orders cannot be updated"));
        }
        if (!actor.admin && !Objects.equals(order.getSubmitterName(), actor.name)) {
            return forbidden("Normal users can only update their own work orders");
        }
        if (request != null) {
            if (!blank(request.title)) order.setTitle(request.title.trim());
            if (!blank(request.changeType)) order.setChangeType(request.changeType.trim());
            if (!blank(request.tableType)) order.setTableType(request.tableType.trim());
            order.setSourceCode(clean(request.sourceCode));
            order.setReason(clean(request.reason));
            order.setPayload(clean(request.payload));
            order.setNotes(clean(request.notes));
            if (actor.firstLevelAdmin && !blank(request.assignedAdminName)) assignAdmin(order, request.assignedAdminName, actor);
        }
        order.setUpdatedAt(LocalDateTime.now());
        order = workOrderRepository.save(order);
        appendLog(order, "UPDATE", order.getStatus(), order.getStatus(), actor, "Update work order");
        return ResponseEntity.ok(Map.of("data", toDto(order, true)));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody WorkOrderRequest request,
                                    @RequestHeader(value = "Authorization", required = false) String authorization) {
        Actor actor = actorFromToken(authorization);
        if (actor == null) return unauthorized();
        if (request == null || blank(request.title) || blank(request.changeType) || blank(request.tableType)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "title, changeType and tableType are required"));
        }
        LocalDateTime now = LocalDateTime.now();
        WorkOrder order = new WorkOrder();
        order.setCode(blank(request.code) ? "WO-" + System.currentTimeMillis() : request.code.trim());
        order.setTitle(request.title.trim());
        order.setChangeType(request.changeType.trim());
        order.setTableType(request.tableType.trim());
        order.setSourceCode(clean(request.sourceCode));
        order.setReason(clean(request.reason));
        order.setPayload(clean(request.payload));
        order.setNotes(clean(request.notes));
        order.setStatus(SUBMITTED);
        order.setSubmitterType(actor.admin ? "Admin" : "User");
        order.setSubmitterName(actor.name);
        order.setSubmitterId(actor.id);
        assignAdmin(order, request.assignedAdminName, actor);
        order.setTestCode(clean(request.testCode));
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order = workOrderRepository.save(order);
        appendLog(order, "CREATE", null, SUBMITTED, actor, "Create work order");
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", toDto(order, true)));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable Long id,
                                    @RequestBody(required = false) ActionRequest request,
                                    @RequestHeader(value = "Authorization", required = false) String authorization) {
        return changeStatus(id, authorization, request, PROCESSING, "ACCEPT", Set.of(SUBMITTED, RETURNED), true, false);
    }

    @PostMapping("/{id}/submit-approval")
    public ResponseEntity<?> submitApproval(@PathVariable Long id,
                                            @RequestBody(required = false) ActionRequest request,
                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return changeStatus(id, authorization, request, PENDING_APPROVAL, "SUBMIT_APPROVAL", Set.of(PROCESSING, RETURNED), true, false);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id,
                                     @RequestBody(required = false) ActionRequest request,
                                     @RequestHeader(value = "Authorization", required = false) String authorization) {
        return changeStatus(id, authorization, request, APPROVED, "APPROVE", Set.of(SUBMITTED, PROCESSING, PENDING_APPROVAL), false, true);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id,
                                    @RequestBody(required = false) ActionRequest request,
                                    @RequestHeader(value = "Authorization", required = false) String authorization) {
        return changeStatus(id, authorization, request, REJECTED, "REJECT", Set.of(SUBMITTED, PROCESSING, PENDING_APPROVAL), false, true);
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<?> returnBack(@PathVariable Long id,
                                        @RequestBody(required = false) ActionRequest request,
                                        @RequestHeader(value = "Authorization", required = false) String authorization) {
        return changeStatus(id, authorization, request, RETURNED, "RETURN", Set.of(SUBMITTED, PROCESSING, PENDING_APPROVAL), false, true);
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<?> close(@PathVariable Long id,
                                   @RequestBody(required = false) ActionRequest request,
                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        return changeStatus(id, authorization, request, CLOSED, "CLOSE", Set.of(SUBMITTED, PROCESSING, PENDING_APPROVAL, APPROVED, REJECTED, RETURNED), false, true);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<?> comment(@PathVariable Long id,
                                     @RequestBody(required = false) ActionRequest request,
                                     @RequestHeader(value = "Authorization", required = false) String authorization) {
        Actor actor = actorFromToken(authorization);
        if (actor == null) return unauthorized();
        Optional<WorkOrder> optional = workOrderRepository.findById(id);
        if (optional.isEmpty()) return notFound();
        WorkOrder order = optional.get();
        if (!canView(actor, order)) return forbidden("No permission for this work order");
        appendLog(order, "COMMENT", order.getStatus(), order.getStatus(), actor, request == null ? "" : clean(request.comment));
        return ResponseEntity.ok(Map.of("data", toDto(order, true)));
    }

    private ResponseEntity<?> changeStatus(Long id, String authorization, ActionRequest request, String targetStatus,
                                           String action, Set<String> allowedFrom, boolean adminRequired,
                                           boolean firstLevelRequired) {
        Actor actor = actorFromToken(authorization);
        if (actor == null) return unauthorized();
        Optional<WorkOrder> optional = workOrderRepository.findById(id);
        if (optional.isEmpty()) return notFound();
        WorkOrder order = optional.get();
        if (!canView(actor, order)) return forbidden("No permission for this work order");
        if (adminRequired && !actor.admin) return forbidden("Normal users cannot change approval status");
        if (firstLevelRequired && !actor.firstLevelAdmin) return forbidden("Only first-level admins can perform final actions");
        if (PROCESSING.equals(targetStatus) && !actor.admin) return forbidden("Only admins can accept work orders");
        if (PENDING_APPROVAL.equals(targetStatus) && (!actor.admin || actor.firstLevelAdmin)) return forbidden("Only second-level admins can submit final approval");
        if (!allowedFrom.contains(order.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Only admins can accept work orders"));
        }
        String fromStatus = order.getStatus();
        order.setStatus(targetStatus);
        order.setUpdatedAt(LocalDateTime.now());
        order = workOrderRepository.save(order);
        appendLog(order, action, fromStatus, targetStatus, actor, request == null ? "" : clean(request.comment));
        return ResponseEntity.ok(Map.of("data", toDto(order, true)));
    }

    private void assignAdmin(WorkOrder order, String requestedName, Actor actor) {
        String name = blank(requestedName) ? (actor.admin && !actor.firstLevelAdmin ? actor.name : DEFAULT_ASSIGNEE) : requestedName.trim();
        Admin admin = adminRepository.findByName(name);
        order.setAssignedAdminName(name);
        order.setAssignedAdminId(admin == null ? null : admin.getId());
    }

    private Actor actorFromToken(String authorization) {
        if (blank(authorization)) return null;
        try {
            String username = JwtUtils.getUsernameFromToken(authorization);
            Admin admin = adminRepository.findByName(username);
            if (admin != null) return new Actor(admin.getId(), admin.getName(), true, isFirstLevelAdmin(admin));
            User user = userRepository.findByName(username);
            if (user != null) return new Actor(user.getId(), user.getName(), false, false);
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean canView(Actor actor, WorkOrder order) {
        if (actor.firstLevelAdmin) return true;
        if (actor.admin) return Objects.equals(order.getAssignedAdminName(), actor.name) || Objects.equals(order.getSubmitterName(), actor.name);
        return "User".equals(order.getSubmitterType()) && Objects.equals(order.getSubmitterName(), actor.name);
    }

    private boolean isFirstLevelAdmin(Admin admin) {
        return admin != null && (Integer.valueOf(100000).equals(admin.getId()) || "Admin_100000".equals(admin.getName()));
    }

    private void appendLog(WorkOrder order, String action, String fromStatus, String toStatus, Actor actor, String comment) {
        WorkOrderActionLog log = new WorkOrderActionLog();
        log.setWorkOrderId(order.getId());
        log.setWorkOrderCode(order.getCode());
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setActorType(actor.admin ? "Admin" : "User");
        log.setActorName(actor.name);
        log.setActorId(actor.id);
        log.setCommentText(clean(comment));
        log.setCreatedAt(LocalDateTime.now());
        logRepository.save(log);
    }

    private Map<String, Object> toDto(WorkOrder order, boolean includeLogs) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", order.getId());
        dto.put("code", order.getCode());
        dto.put("title", order.getTitle());
        dto.put("changeType", order.getChangeType());
        dto.put("tableType", order.getTableType());
        dto.put("sourceCode", order.getSourceCode());
        dto.put("reason", order.getReason());
        dto.put("payload", order.getPayload());
        dto.put("notes", order.getNotes());
        dto.put("status", order.getStatus());
        dto.put("submitterType", order.getSubmitterType());
        dto.put("submitterName", order.getSubmitterName());
        dto.put("assignedAdminName", order.getAssignedAdminName());
        dto.put("testCode", order.getTestCode());
        dto.put("createdAt", order.getCreatedAt());
        dto.put("updatedAt", order.getUpdatedAt());
        if (includeLogs) dto.put("logs", logRepository.findByWorkOrderIdOrderByCreatedAtAsc(order.getId()));
        return dto;
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private ResponseEntity<?> unauthorized() { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Login required")); }
    private ResponseEntity<?> forbidden(String message) { return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", message)); }
    private ResponseEntity<?> notFound() { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Work order not found")); }

    public static class WorkOrderRequest {
        private String code;
        private String title;
        private String changeType;
        private String tableType;
        private String sourceCode;
        private String reason;
        private String payload;
        private String notes;
        private String assignedAdminName;
        private String testCode;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getChangeType() { return changeType; }
        public void setChangeType(String changeType) { this.changeType = changeType; }
        public String getTableType() { return tableType; }
        public void setTableType(String tableType) { this.tableType = tableType; }
        public String getSourceCode() { return sourceCode; }
        public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public String getAssignedAdminName() { return assignedAdminName; }
        public void setAssignedAdminName(String assignedAdminName) { this.assignedAdminName = assignedAdminName; }
        public String getTestCode() { return testCode; }
        public void setTestCode(String testCode) { this.testCode = testCode; }
    }

    public static class ActionRequest {
        private String comment;
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    private static class Actor {
        private final Integer id;
        private final String name;
        private final boolean admin;
        private final boolean firstLevelAdmin;

        private Actor(Integer id, String name, boolean admin, boolean firstLevelAdmin) {
            this.id = id;
            this.name = name;
            this.admin = admin;
            this.firstLevelAdmin = firstLevelAdmin;
        }
    }
}
