package com.example.devprojects.repository;

import com.example.devprojects.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.role " +
            "LEFT JOIN FETCH u.specializations us " +
            "LEFT JOIN FETCH us.specialization " +
            "LEFT JOIN FETCH u.skills sk " +
            "LEFT JOIN FETCH sk.skill " +
            "LEFT JOIN FETCH sk.proficiencyLevel " +
            "WHERE u.email = :email")
    Optional<User> findByEmailWithAllData(@Param("email") String email);
}