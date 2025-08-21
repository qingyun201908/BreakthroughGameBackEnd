package BreakthroughGame.backEnd.UserModule.UserInfo.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
})
/**
 * 用户
 */
@Data
public class User {
    @Id
    @GeneratedValue
    private UUID id;


    @Column(nullable = false, length = 50)
    private String username;


    @Column(nullable = false, length = 120)
    private String email;


    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;


    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();


}