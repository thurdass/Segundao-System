package com.thurdass.system2a.repository;

import com.thurdass.system2a.entity.User;
import com.thurdass.system2a.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    long countByRole(Role role);
}
