package com.banking.core.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un titular de cuentas dentro del banco.
 * Un usuario puede tener múltiples cuentas bancarias.
 */
@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre completo del titular */
    @Column(nullable = false)
    private String fullName;

    /** Email único utilizado como identificador de login */
    @Column(nullable = false, unique = true)
    private String email;

    /** Contraseña almacenada con BCrypt */
    @Column(nullable = false)
    private String password;

    /** Rol para control de acceso (ROLE_USER, ROLE_ADMIN) */
    @Column(nullable = false)
    private String role;

    /** Cuentas que pertenecen a este usuario */
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Account> accounts = new ArrayList<>();
}
