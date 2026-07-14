package com.cardboardboxed.demo.controllers;

<<<<<<< Updated upstream
=======
import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.boardgames.BoardGameRankRepository;
import com.cardboardboxed.demo.boardgames.BoardGameAutocompleteRepository;
>>>>>>> Stashed changes
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
    //repository used to connect a review to the actual board game record
    private final BoardGameRankRepository boardGameRankRepository;
    //repository used to find the user currently logged in
    private final UserRepository userRepository;

    //ocnstructor injection gives this controller access to the needed repositories
<<<<<<< Updated upstream
    public ReviewController(ReviewRepository reviewRepository, UserRepository userRepository) {
=======
    public ReviewController(ReviewRepository reviewRepository, BoardGameRankRepository boardGameRankRepository, UserRepository userRepository,
            BoardGameAutocompleteRepository boardGameAutocompleteRepository) {
>>>>>>> Stashed changes
        this.reviewRepository = reviewRepository;
        this.boardGameRankRepository = boardGameRankRepository;
        this.userRepository = userRepository;
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
<<<<<<< Updated upstream
=======

        String resolvedGameTitle = boardGameAutocompleteRepository
                .resolveToExistingName(review.getGameTitle())
                .orElse(null);
        if (resolvedGameTitle == null || resolvedGameTitle.isBlank()) {
            return "redirect:/dashboard?error=Please+choose+a+valid+board+game";
        }

        BoardGameRank boardGame = boardGameRankRepository.findByTitleIgnoreCase(resolvedGameTitle);
        if (boardGame == null) {
            return "redirect:/dashboard?error=Please+choose+a+valid+board+game";
        }

        review.setGameTitle(resolvedGameTitle);
        review.setGame(boardGame);
>>>>>>> Stashed changes
        review.setUser(user);
        reviewRepository.save(review);
        //send user back to dashboard after review posted - user page will be implemented in the future, where reviews appear
        return "redirect:/dashboard";
    }
}