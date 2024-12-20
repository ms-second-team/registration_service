package ru.ms.second.team.registration.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "registrations")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class Registration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String email;
    private String phone;
    @Column(name = "event_id")
    private Long eventId;
    private String password;
    @Enumerated(EnumType.STRING)
    private RegistrationStatus status;
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}