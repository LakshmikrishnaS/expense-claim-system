package com.example.expense.service;

import com.example.expense.dto.PageResponse;
import com.example.expense.dto.UserResponse;
import com.example.expense.entity.User;
import com.example.expense.exception.BusinessRuleException;
import com.example.expense.exception.ResourceNotFoundException;
import com.example.expense.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable).map(UserResponse::from));
    }

    @Transactional
    public UserResponse setActive(Long id, boolean active, User actor) {
        User target = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (target.getId().equals(actor.getId())) {
            throw new BusinessRuleException("You cannot change your own active status");
        }
        target.setActive(active);
        return UserResponse.from(userRepository.save(target));
    }
}
