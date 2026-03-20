package com.example.devprojects.repository;

import com.example.devprojects.model.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecializationRepository extends JpaRepository<Specialization, Integer> {
    List<Specialization> findAllByOrderByNameAsc();
}