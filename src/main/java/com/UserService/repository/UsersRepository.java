package com.UserService.repository;

import com.UserService.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {
    Logger log = LoggerFactory.getLogger(UsersRepository.class);

    Optional<Users> findByUserId(UUID userId);

    Optional<Users> findByEmail(String email);
}