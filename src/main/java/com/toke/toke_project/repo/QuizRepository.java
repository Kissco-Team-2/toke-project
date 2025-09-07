package com.toke.toke_project.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.toke.toke_project.domain.Quiz;


@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

}
