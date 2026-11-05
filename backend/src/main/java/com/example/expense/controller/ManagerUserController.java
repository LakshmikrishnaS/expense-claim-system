package com.example.expense.controller;

import com.example.expense.dto.PageResponse;
import com.example.expense.dto.StatusUpdateRequest;
import com.example.expense.dto.UserResponse;
import com.example.expense.security.UserDetailsImpl;
import com.example.expense.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/users")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class ManagerUserController {
    private final UserService userService;

    @GetMapping
    public PageResponse<UserResponse> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return userService.list(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.ASC, "name")));
    }

    @PatchMapping("/{id}/status")
    public UserResponse setStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest req,
                                  @AuthenticationPrincipal UserDetailsImpl principal) {
        return userService.setActive(id, req.active(), principal.getUser());
    }
}
