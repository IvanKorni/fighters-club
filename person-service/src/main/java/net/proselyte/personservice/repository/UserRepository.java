package net.proselyte.personservice.repository;

import net.proselyte.personservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE (:emails) IS NULL OR u.email IN :emails")
    List<User> findAllByEmails(@Param("emails") List<String> emails);
}
