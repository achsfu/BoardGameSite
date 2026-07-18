package com.cardboardboxed.demo.useracounts;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface UserFollowRepository extends JpaRepository<UserFollow, Integer> {
    Optional<UserFollow> findByFollowerAndFollowed(User follower, User followed);
    boolean existsByFollowerAndFollowed(User follower, User followed);
    //everybody this user follows
    List<UserFollow> findByFollower(User follower);
    //everybody who follows this user
    List<UserFollow> findByFollowed(User followed);
    long countByFollower(User follower);
    long countByFollowed(User followed);
    //this removes the follow relationship between two users.
    //@Transactional ensures that the database deletion completes entirely or rolls back if an error occurs
    @Transactional
    void deleteByFollowerAndFollowed(User follower, User followed);
}
