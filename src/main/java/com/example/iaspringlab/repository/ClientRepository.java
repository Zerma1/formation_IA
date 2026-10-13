package com.example.iaspringlab.repository;

import com.example.iaspringlab.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByEmail(String email);
}
