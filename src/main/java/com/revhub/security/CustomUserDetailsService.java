package com.revhub.security;

import com.revhub.exception.AccountStatusException;
import com.revhub.model.Admin;
import com.revhub.model.User;
import com.revhub.repository.AdminRepository;
import com.revhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
        private final UserRepository userRepository;
        private final AdminRepository adminRepository;

        @Override
        @Transactional
        public UserDetails loadUserByUsername(String emailOrUsername) throws UsernameNotFoundException {
                // 1. Try to find in User Repository
                Optional<User> userOpt = userRepository.findByEmail(emailOrUsername)
                                .or(() -> userRepository.findByUsername(emailOrUsername));
                if (userOpt.isPresent()) {
                        User user = userOpt.get();

                        // Check if user is deleted
                        if (user.getIsDeleted() != null && user.getIsDeleted()) {
                                throw new AccountStatusException(
                                                "This account has been deleted and cannot be accessed.");
                        }

                        // Check if user is banned
                        if (user.getIsBanned() != null && user.getIsBanned()) {
                                throw new AccountStatusException("This account has been banned by Admin");
                        }

                        return new UserPrincipal(
                                        user.getId(),
                                        user.getUsername(),
                                        user.getEmail(),
                                        user.getPassword(),
                                        user.getRole(),
                                        user.getFirstName(),
                                        user.getLastName(),
                                        user.getProfilePicture());
                }
                // 2. Try to find in Admin Repository
                Admin admin = adminRepository.findByEmail(emailOrUsername)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "User not found with email or username: " + emailOrUsername));
                // Split name for first/last name compatibility
                String[] nameParts = admin.getName().split(" ", 2);
                String firstName = nameParts[0];
                String lastName = nameParts.length > 1 ? nameParts[1] : "";
                return new UserPrincipal(
                                admin.getId(),
                                admin.getName(), // Use name as username for principal
                                admin.getEmail(),
                                admin.getPassword(),
                                admin.getRole(),
                                firstName,
                                lastName,
                                null); // No profile picture for admin
        }

        @Transactional
        public UserDetails loadUserById(Long id) {
                User user = userRepository.findById(id)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

                // Check if user is deleted
                if (user.getIsDeleted() != null && user.getIsDeleted()) {
                        throw new AccountStatusException(
                                        "This account has been deleted and cannot be accessed.");
                }

                // Check if user is banned
                if (user.getIsBanned() != null && user.getIsBanned()) {
                        throw new AccountStatusException("This account has been banned by Admin");
                }

                return new UserPrincipal(
                                user.getId(),
                                user.getUsername(),
                                user.getEmail(),
                                user.getPassword(),
                                user.getRole(),
                                user.getFirstName(),
                                user.getLastName(),
                                user.getProfilePicture());
        }
}
