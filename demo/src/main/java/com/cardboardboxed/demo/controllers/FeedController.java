package com.cardboardboxed.demo.controllers;

import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserFollowRepository;
import com.cardboardboxed.demo.useracounts.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class FeedController {

    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;
    private final ReviewRepository reviewRepository;

    public FeedController(
            UserRepository userRepository,
            UserFollowRepository userFollowRepository,
            ReviewRepository reviewRepository
    ) {
        this.userRepository = userRepository;
        this.userFollowRepository = userFollowRepository;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/feed")
    public String showFeed(
            Model model,
            HttpServletRequest request
    ) {
        HttpSession session = request.getSession(false);

        // The feed is only available to logged-in users.
        if (session == null || session.getAttribute("AUTH_USER") == null) {
            return "redirect:/login?error=Please+log+in+to+view+your+feed";
        }

        String username = (String) session.getAttribute("AUTH_USER");
        User currentUser = userRepository.findByUsername(username);

        // Protect against an invalid or deleted session user.
        if (currentUser == null) {
            session.invalidate();
            return "redirect:/login?error=User+account+not+found";
        }

        long followingCount =
                userFollowRepository.countByFollower(currentUser);

        List<Review> feedReviews =
                reviewRepository.findFeedReviewsForUser(currentUser);

        model.addAttribute("username", username);
        model.addAttribute("followingCount", followingCount);
        model.addAttribute("feedReviews", feedReviews);

        return "feed";
    }
}