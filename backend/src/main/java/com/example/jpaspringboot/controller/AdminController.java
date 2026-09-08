package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.dto.UserDTO;
import com.example.jpaspringboot.entity.Admin;
import com.example.jpaspringboot.entity.User;
import com.example.jpaspringboot.entity.UserActivityLog;
import com.example.jpaspringboot.repository.AdminRepository;
import com.example.jpaspringboot.service.impl.AdministratorUserRelationServiceImpl;
import com.example.jpaspringboot.service.impl.UserActivityLogServiceImpl;
import com.example.jpaspringboot.service.impl.UserServiceImpl;
import com.example.jpaspringboot.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UserServiceImpl userServiceImpl;
    @Autowired private AdminRepository adminRepository;
    @Autowired private AdministratorUserRelationServiceImpl relationService;
    @Autowired private UserActivityLogServiceImpl activityLogService;

    @GetMapping("/hello")
    public String hello(){ return "nihao"; }

    @PostMapping("/add")
    public ResponseEntity<?> addNewUser(@RequestBody UserDTO userDTO, @RequestHeader(value = "Authorization", required = false) String token, HttpServletRequest request) {
        Admin admin = requireAdmin(token);
        if (admin == null) return unauthorizedOrForbidden(token);
        try {
            int id = userServiceImpl.addUser(userDTO.getName(), userDTO.getPassword(), userDTO.getEmail(), userDTO.getBirthdate());
            relationService.createRelationForCurrentUser(id, cleanToken(token));
            activityLogService.record(admin.getId(), admin.getName(), "Admin", id, "ADMIN_ADD_USER", "Admin added user", "/admin/add", "SUCCESS", request.getRemoteAddr());
            return ResponseEntity.ok().body(id);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", "????????????????"));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateUser(@RequestBody UserDTO userDTO, @RequestHeader(value = "Authorization", required = false) String token, HttpServletRequest request) {
        Admin admin = requireAdmin(token);
        if (admin == null) return unauthorizedOrForbidden(token);
        try {
            boolean isUpdated = userServiceImpl.updateUser(userDTO.getId(), userDTO.getName(), userDTO.getPassword(), userDTO.getEmail(), userDTO.getBirthdate());
            if (!isUpdated) return ResponseEntity.badRequest().body(Map.of("message", "No changes were saved or update failed"));
            activityLogService.record(admin.getId(), admin.getName(), "Admin", userDTO.getId(), "ADMIN_UPDATE_USER", "Admin updated user", "/admin/update", "SUCCESS", request.getRemoteAddr());
            return ResponseEntity.ok().body(Map.of("message", "User updated successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", "????????????????"));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String token, HttpServletRequest request) {
        Admin admin = requireAdmin(token);
        if (admin == null) return unauthorizedOrForbidden(token);
        relationService.deleteRelationForCurrentUser(Math.toIntExact(id), cleanToken(token));
        boolean deleted = userServiceImpl.deleteUserById(id);
        if (!deleted) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        activityLogService.record(admin.getId(), admin.getName(), "Admin", id.intValue(), "ADMIN_DELETE_USER", "Admin deleted user", "/admin/delete/" + id, "SUCCESS", request.getRemoteAddr());
        return ResponseEntity.ok().body(Map.of("message", "User deleted successfully"));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUsers(@RequestBody List<Long> ids, @RequestHeader(value = "Authorization", required = false) String token, HttpServletRequest request) {
        Admin admin = requireAdmin(token);
        if (admin == null) return unauthorizedOrForbidden(token);
        relationService.deleteRelationForCurrentUsers(ids, cleanToken(token));
        userServiceImpl.deleteUserByIds(ids);
        for (Long id : ids) activityLogService.record(admin.getId(), admin.getName(), "Admin", id.intValue(), "ADMIN_BATCH_DELETE_USER", "Admin batch deleted user", "/admin/delete", "SUCCESS", request.getRemoteAddr());
        return ResponseEntity.ok(Map.of("message", "Users deleted successfully"));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsersByAdmin(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestHeader(value = "Authorization", required = false) String token) {
        Admin admin = requireAdmin(token);
        if (admin == null) return unauthorizedOrForbidden(token);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return ResponseEntity.ok(safePage(userServiceImpl.findUsersManagedByAdmin(cleanToken(token), pageable)));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "") String keyword, @RequestHeader(value = "Authorization", required = false) String token) {
        Admin admin = requireAdmin(token);
        if (admin == null) return unauthorizedOrForbidden(token);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return ResponseEntity.ok(safePage(userServiceImpl.searchUsersByAdmin(cleanToken(token), keyword, pageable)));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> userDetail(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String token) {
        Admin admin = requireAdmin(token);
        if (admin == null) return unauthorizedOrForbidden(token);
        User user = userServiceImpl.findUserForAdmin(cleanToken(token), id);
        if (user == null) {
            if (userServiceImpl.userExists(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Forbidden"));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }
        List<Map<String, Object>> logs = activityLogService.recentForUser(user.getId(), 20).stream().map(this::safeLog).toList();
        return ResponseEntity.ok(Map.of("user", safeUser(user), "logs", logs));
    }

    private Admin requireAdmin(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            String username = JwtUtils.getUsernameFromToken(cleanToken(token));
            return adminRepository.findByName(username);
        } catch (Exception ex) { return null; }
    }

    private ResponseEntity<?> unauthorizedOrForbidden(String token) {
        if (token == null || token.isBlank()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Forbidden"));
    }

    private String cleanToken(String token) { return token == null ? "" : token.replace("Bearer ", ""); }

    private Map<String, Object> safePage(Page<User> page) {
        Map<String, Object> result = new HashMap<>();
        result.put("content", page.getContent().stream().map(this::safeUser).toList());
        result.put("totalElements", page.getTotalElements());
        result.put("totalPages", page.getTotalPages());
        result.put("number", page.getNumber());
        result.put("size", page.getSize());
        return result;
    }

    private Map<String, Object> safeUser(User user) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", user.getId());
        item.put("name", user.getName());
        item.put("email", user.getEmail());
        item.put("birthdate", user.getBirthdate());
        item.put("lastLoginAt", user.getLastLoginAt() == null ? null : user.getLastLoginAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return item;
    }

    private Map<String, Object> safeLog(UserActivityLog log) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", log.getId());
        item.put("actorName", log.getActorName());
        item.put("actorRole", log.getActorRole());
        item.put("actionType", log.getActionType());
        item.put("actionDescription", log.getActionDescription());
        item.put("resultStatus", log.getResultStatus());
        item.put("occurredAt", log.getOccurredAt() == null ? null : log.getOccurredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return item;
    }
}
