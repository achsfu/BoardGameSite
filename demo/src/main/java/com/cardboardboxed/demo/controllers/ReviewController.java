package com.cardboardboxed.demo.controllers;

import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.boardgames.BoardGameRankRepository;
import com.cardboardboxed.demo.boardgames.BoardGameAutocompleteRepository;
import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReviewController {

    //repository used to save new reviews into the database
    private final ReviewRepository reviewRepository;
    //repository used to connect a review to the actual board game record
    private final BoardGameRankRepository boardGameRankRepository;
    //repository used to find the user currently logged in
    private final UserRepository userRepository;
    //repository used to normalize user input to valid board game names
    private final BoardGameAutocompleteRepository boardGameAutocompleteRepository;

    //ocnstructor injection gives this controller access to the needed repositories
    public ReviewController(ReviewRepository reviewRepository, BoardGameRankRepository boardGameRankRepository, UserRepository userRepository,
            BoardGameAutocompleteRepository boardGameAutocompleteRepository) {
        this.reviewRepository = reviewRepository;
        this.boardGameRankRepository = boardGameRankRepository;
        this.userRepository = userRepository;
        this.boardGameAutocompleteRepository = boardGameAutocompleteRepository;
    }

    //handle the review form submission from dashboard.html
    @PostMapping("/reviews")
    public String postReview(
            Review review,
            @RequestParam(name = "redirectTo", required = false) String redirectTo,
            HttpServletRequest request
    ) {
        String safeRedirectTarget = resolveSafeRedirectTarget(redirectTo);

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
            return "redirect:" + safeRedirectTarget + "?error=Please+choose+a+valid+board+game";
        }

        BoardGameRank boardGame = boardGameRankRepository.findByTitleIgnoreCase(resolvedGameTitle);
        if (boardGame == null) {
            return "redirect:" + safeRedirectTarget + "?error=Please+choose+a+valid+board+game";
        }

        review.setGameTitle(resolvedGameTitle);
        review.setGame(boardGame);
        review.setUser(user);
        reviewRepository.save(review);

        return "redirect:" + safeRedirectTarget;
    }

    private String resolveSafeRedirectTarget(String redirectTo) {
        if (redirectTo != null && !redirectTo.isBlank()) {
            String trimmed = redirectTo.trim();
            if (trimmed.startsWith("/games/")) {
                return trimmed;
            }
        }

        return "/dashboard";
    }
}