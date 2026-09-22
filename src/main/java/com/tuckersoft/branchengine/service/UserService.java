package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.Dtos.*;
import com.tuckersoft.branchengine.exception.BusinessException;
import com.tuckersoft.branchengine.exception.ResourceNotFoundException;
import com.tuckersoft.branchengine.model.User;
import com.tuckersoft.branchengine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserResponse getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.getCreatedAt());
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole(), u.getCreatedAt()))
                .toList();
    }

    public UserResponse updateRole(Long id, RoleUpdateRequest req, String currentAdminEmail) {
        if (!"ROLE_USER".equals(req.role()) && !"ROLE_ADMIN".equals(req.role())) {
            throw new BusinessException("Rol inválido");
        }

        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (target.getEmail().equals(currentAdminEmail)) {
            throw new BusinessException("Un administrador no puede modificar su propio rol");
        }

        target.setRole(req.role());
        userRepository.save(target);
        return new UserResponse(target.getId(), target.getEmail(), target.getDisplayName(), target.getRole(), target.getCreatedAt());
    }
}
