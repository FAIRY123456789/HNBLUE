package com.example.jpaspringboot.controller.professional;

import com.example.jpaspringboot.dto.professional.ProfessionalDtos;
import com.example.jpaspringboot.security.AccountRole;
import com.example.jpaspringboot.security.HnbluePrincipal;
import com.example.jpaspringboot.service.professional.ProfessionalAccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/professional")
public class ProfessionalSelfController {
    private final ProfessionalAccountService service;

    public ProfessionalSelfController(ProfessionalAccountService service) {
        this.service = service;
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal HnbluePrincipal principal,
                                            @Valid @RequestBody ProfessionalDtos.PasswordChangeRequest request) {
        if (principal == null || principal.role() != AccountRole.PROFESSIONAL) {
            return ResponseEntity.status(403).body(Map.of("code", "professional_only"));
        }
        service.changeOwnPassword(principal, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "password changed; sign in again", "tokenRevoked", true));
    }
}
