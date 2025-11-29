package net.proselyte.personservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;

@Setter
@Getter
@Entity
@Table(name = "users", schema = "person")
public class User extends BaseEntity {
    @Size(max = 1024)
    @Column(name = "email", nullable = false, unique = true, length = 1024)
    private String email;

    @Size(max = 64)
    @Column(name = "nickname", nullable = false, unique = true, length = 64)
    private String nickname;
}
