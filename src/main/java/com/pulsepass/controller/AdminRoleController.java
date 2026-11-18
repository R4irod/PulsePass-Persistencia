package com.pulsepass.controller;

import com.pulsepass.domain.Role;
import com.pulsepass.service.AdminRoleService;
import com.pulsepass.service.AdminRoleService.AccountRoles;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminRoleController {
    private final AdminRoleService service;

    public AdminRoleController(AdminRoleService service) {
        this.service = service;
    }

    public record AddRoleRequest(@NotNull Role role) {}

    @GetMapping("/lookup")
    public AccountRoles find(@RequestParam("identifier") String identifier) {
        return service.find(identifier);
    }

    @PostMapping("/{id}/roles")
    public AccountRoles add(@PathVariable("id") Long id,
                            @Valid @RequestBody AddRoleRequest request) {
        return service.add(id, request.role());
    }
}
