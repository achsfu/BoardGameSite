package com.cardboardboxed.demo.useracounts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
    
public interface UserRepository extends JpaRepository<User, Integer> {

    User findByUsername(String username);
    Page<User> findByUsernameContainingIgnoreCase(String query, Pageable pageable);
    List<User> findByRoleIgnoreCaseOrderByUsernameAsc(String role);
    List<User> findByRoleIgnoreCaseAndUsernameContainingIgnoreCaseOrderByUsernameAsc(String role, String username);
    boolean existsByRoleIgnoreCase(String role);
}
    
