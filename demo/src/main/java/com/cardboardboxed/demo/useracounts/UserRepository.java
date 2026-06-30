package com.cardboardboxed.demo.useracounts;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
    
public interface UserRepository extends JpaRepository<User, Integer> {

    User findByUsername(String username);
}
    
