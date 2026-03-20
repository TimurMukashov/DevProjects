package com.example.devprojects.repository;

import com.example.devprojects.model.Favorite;
import com.example.devprojects.model.Project;
import com.example.devprojects.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Favorite.FavoriteId> {

    @Query("SELECT COUNT(f) > 0 FROM Favorite f WHERE f.user.id = :userId AND f.project.id = :projectId")
    boolean existsByUserIdAndProjectId(@Param("userId") Integer userId, @Param("projectId") Integer projectId);

    void deleteByUserAndProject(User user, Project project);

    List<Favorite> findAllByUser(User user);
}