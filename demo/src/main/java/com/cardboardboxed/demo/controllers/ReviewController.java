package com.cardboardboxed.demo.controllers;

import com.cardboardboxed.demo.boardgames.BoardGameAutocompleteRepository;
import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ReviewController {

    //repository used to save new reviews into the database
    private final ReviewRepository reviewRepository;
    //repository used to find the user currently logged in
    private final UserRepository userRepository;
    //repository used to normalize user input to valid board game names
    private final BoardGameAutocompleteRepository boardGameAutocompleteRepository;

    //ocnstructor injection gives this controller access to the needed repositories
    public ReviewController(ReviewRepository reviewRepository, UserRepository userRepository,
            BoardGameAutocompleteRepository boardGameAutocompleteRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.boardGameAutocompleteRepository = boardGameAutocompleteRepository;
    }

    //handle the review form submission from dashboard.html
    @PostMapping("/reviews")
    public String postReview(Review review, HttpServletRequest request) {
        //get current session without creating new one!
        HttpSession session = request.getSession(false);

        if (session == null) {
            return "redirect:/login?error=Please+log+in+to+post+a+review";
        }
        String username = (String) session.getAttribute("AUTH_USER");
        if (username == null) {
            return "redirect:/login?error=Please+log+in+to+post+a+review";
        }
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        String resolvedGameTitle = boardGameAutocompleteRepository
                .resolveToExistingName(review.getGameTitle())
                .orElse(null);
        if (resolvedGameTitle == null || resolvedGameTitle.isBlank()) {
            return "redirect:/dashboard?error=Please+choose+a+valid+board+game";
        }

        review.setGameTitle(resolvedGameTitle);
        review.setUser(user);
        reviewRepository.save(review);
        //send user back to dashboard after review posted - user page will be implemented in the future, where reviews appear
        return "redirect:/dashboard";
    }
}