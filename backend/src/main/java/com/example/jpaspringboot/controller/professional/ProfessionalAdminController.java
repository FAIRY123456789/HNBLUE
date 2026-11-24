package com.example.jpaspringboot.controller.professional;

import com.example.jpaspringboot.dto.professional.ProfessionalDtos;
import com.example.jpaspringboot.security.HnbluePrincipal;
import com.example.jpaspringboot.service.professional.ProfessionalAccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/professionals")
public class ProfessionalAdminController {
    private final ProfessionalAccountService service;

    public ProfessionalAdminController(ProfessionalAccountService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ProfessionalDtos.CreateRequest request,
                                    @AuthenticationPrincipal HnbluePrincipal actor) {
        return ResponseEntity.ok(service.create(request.username(), actor));
    }

    @PostMapping("/batch")
    public ResponseEntity<?> createBatch(@Valid @RequestBody ProfessionalDtos.BatchCreateRequest request,
                                         @AuthenticationPrincipal HnbluePrincipal actor) {
        return ResponseEntity.ok(service.createBatch(request.prefix(), request.count(), actor));
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(defaultValue = "") String keyword,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        return ResponseEntity.ok(service.list(keyword,
                PageRequest.of(Math.max(0, page), safeSize, Sort.by("id").ascending())));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> status(@PathVariable long id,
                                    @RequestBody ProfessionalDtos.StatusRequest request,
                                    @AuthenticationPrincipal HnbluePrincipal actor) {
        return ResponseEntity.ok(service.setEnabled(id, request.enabled(), actor));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable long id,
                                           @AuthenticationPrincipal HnbluePrincipal actor) {
        return ResponseEntity.ok(service.resetPassword(id, actor));
    }

    @PutMapping("/{id}/entitlement")
    public ResponseEntity<?> entitlement(@PathVariable long id,
                                         @Valid @RequestBody ProfessionalDtos.EntitlementRequest request,
                                         @AuthenticationPrincipal HnbluePrincipal actor) {
        return ResponseEntity.ok(service.updateEntitlement(id, request, actor));
    }
}
