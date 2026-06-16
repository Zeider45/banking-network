package com.banking.core.repository;

import com.banking.core.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Busca un usuario por email (usado en autenticación) */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
