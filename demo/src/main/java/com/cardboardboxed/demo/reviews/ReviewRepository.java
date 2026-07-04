package com.cardboardboxed.demo.reviews;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
//create repository for reviews
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByUserUsername(String username);
}
