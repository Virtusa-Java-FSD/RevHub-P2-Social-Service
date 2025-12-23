package com.revhub.service;

import com.revhub.exception.ResourceNotFoundException;
import com.revhub.model.Connection;
import com.revhub.model.User;
import com.revhub.repository.ConnectionRepository;
import com.revhub.repository.UserRepository;
import com.revhub.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConnectionRepository connectionRepository;
    private final com.revhub.repository.AdminRepository adminRepository;

    public User getCurrentUser(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        if (userPrincipal.getRole() == com.revhub.model.UserRole.ADMIN) {
            return adminRepository.findById(userPrincipal.getId())
                    .map(admin -> {
                        User user = new User();
                        user.setId(admin.getId());
                        user.setEmail(admin.getEmail());
                        user.setUsername(admin.getName()); // Use name as username
                        user.setRole(admin.getRole());
                        // Split name for first/last name
                        String[] nameParts = admin.getName().split(" ", 2);
                        user.setFirstName(nameParts[0]);
                        user.setLastName(nameParts.length > 1 ? nameParts[1] : "");
                        // Set other required fields to avoid null pointers in frontend
                        user.setBio("Administrator");
                        user.setProfilePicture(null);
                        return user;
                    })
                    .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        }
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User getUserById(Long id) {
        // First try to find in users table
        return userRepository.findById(id)
                .or(() -> {
                    // If not found, check if it's an admin
                    return adminRepository.findById(id)
                            .map(admin -> {
                                User user = new User();
                                user.setId(admin.getId());
                                user.setEmail(admin.getEmail());
                                user.setUsername(admin.getName());
                                user.setRole(admin.getRole());
                                String[] nameParts = admin.getName().split(" ", 2);
                                user.setFirstName(nameParts[0]);
                                user.setLastName(nameParts.length > 1 ? nameParts[1] : "");
                                user.setBio("Administrator");
                                user.setProfilePicture(null);
                                return user;
                            });
                })
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional
    public User updateProfile(Authentication authentication, User updateRequest) {
        User user = getCurrentUser(authentication);
        if (updateRequest.getFirstName() != null) {
            user.setFirstName(updateRequest.getFirstName());
        }
        if (updateRequest.getLastName() != null) {
            user.setLastName(updateRequest.getLastName());
        }
        if (updateRequest.getBio() != null) {
            user.setBio(updateRequest.getBio());
        }
        if (updateRequest.getProfilePicture() != null) {
            user.setProfilePicture(updateRequest.getProfilePicture());
        }
        return userRepository.save(user);
    }

    public List<User> searchUsers(String query) {
        return userRepository.searchUsers(query);
    }

    public List<User> getSuggestedUsers(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        // Get all accepted connections for current user using repository directly
        List<Connection> connections = connectionRepository.findAcceptedConnectionsByUserId(currentUser.getId());
        // Extract user IDs of all connected users
        Set<Long> connectedUserIds = connections.stream()
                .map(conn -> {
                    if (conn.getRequesterId().equals(currentUser.getId())) {
                        return conn.getReceiverId();
                    } else {
                        return conn.getRequesterId();
                    }
                })
                .collect(Collectors.toSet());
        // Return users excluding current user and connected users
        return userRepository.findAll().stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .filter(user -> !connectedUserIds.contains(user.getId()))
                .limit(10)
                .toList();
    }

    // Admin methods
    @Transactional
    public void blockUser(Long userId, String reason) {
        User user = getUserById(userId);
        user.setIsBanned(true);
        user.setBanReason(reason);
        user.setBannedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void deleteUserByAdmin(Long userId) {
        try {
            User user = getUserById(userId);
            user.setIsDeleted(true);
            user.setDeletedAt(java.time.LocalDateTime.now());
            userRepository.save(user);
            System.out.println("Successfully deleted user with ID: " + userId);
        } catch (Exception e) {
            System.err.println("Error deleting user " + userId + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
