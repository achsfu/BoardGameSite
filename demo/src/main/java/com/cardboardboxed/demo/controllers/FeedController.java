package com.cardboardboxed.demo.controllers;

import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.reviews.ReviewLikeRepository;
import com.cardboardboxed.demo.reviews.ReviewReply;
import com.cardboardboxed.demo.reviews.ReviewReplyRepository;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserFollowRepository;
import com.cardboardboxed.demo.useracounts.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class FeedController {

    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewReplyRepository reviewReplyRepository;

    public FeedController(
            UserRepository userRepository,
            UserFollowRepository userFollowRepository,
            ReviewRepository reviewRepository,
            ReviewLikeRepository reviewLikeRepository,
            ReviewReplyRepository reviewReplyRepository
    ) {
        this.userRepository = userRepository;
        this.userFollowRepository = userFollowRepository;
        this.reviewRepository = reviewRepository;
        this.reviewLikeRepository = reviewLikeRepository;
        this.reviewReplyRepository = reviewReplyRepository;
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

        Map<Integer, Long> reviewLikeCounts = buildReviewLikeCounts(feedReviews);
        Set<Integer> likedReviewIds = findLikedReviewIds(feedReviews, currentUser);
        Map<Integer, List<ReviewReply>> reviewReplies = buildReviewReplies(feedReviews);
        Map<Integer, Long> reviewReplyCounts = buildReviewReplyCounts(feedReviews);

        model.addAttribute("username", username);
        model.addAttribute("profilePictureUrl", currentUser.getProfilePictureUrl());
        model.addAttribute("followingCount", followingCount);
        model.addAttribute("feedReviews", feedReviews);
        model.addAttribute("reviewLikeCounts", reviewLikeCounts);
        model.addAttribute("likedReviewIds", likedReviewIds);
        model.addAttribute("reviewReplies", reviewReplies);
        model.addAttribute("reviewReplyCounts", reviewReplyCounts);

        return "feed";
    }

    private Map<Integer, Long> buildReviewLikeCounts(List<Review> reviews) {
        Map<Integer, Long> likeCounts = new HashMap<>();

        for (Review review : reviews) {
            if (review == null || review.getId() == null) {
                continue;
            }

            likeCounts.put(review.getId(), reviewLikeRepository.countByReview(review));
        }

        return likeCounts;
    }

    private Set<Integer> findLikedReviewIds(List<Review> reviews, User currentUser) {
        Set<Integer> likedReviewIds = new HashSet<>();

        if (currentUser == null) {
            return likedReviewIds;
        }

        for (Review review : reviews) {
            if (review == null || review.getId() == null) {
                continue;
            }

            if (reviewLikeRepository.existsByReviewAndUser(review, currentUser)) {
                likedReviewIds.add(review.getId());
            }
        }

        return likedReviewIds;
    }

    private Map<Integer, List<ReviewReply>> buildReviewReplies(List<Review> reviews) {
        Map<Integer, List<ReviewReply>> repliesByReview = new HashMap<>();

        for (Review review : reviews) {
            if (review == null || review.getId() == null) {
                continue;
            }

            repliesByReview.put(
                    review.getId(),
                    reviewReplyRepository.findByReviewOrderByCreatedAtAsc(review)
            );
        }

        return repliesByReview;
    }

    private Map<Integer, Long> buildReviewReplyCounts(List<Review> reviews) {
        Map<Integer, Long> replyCounts = new HashMap<>();

        for (Review review : reviews) {
            if (review == null || review.getId() == null) {
                continue;
            }

            replyCounts.put(review.getId(), reviewReplyRepository.countByReview(review));
        }

        return replyCounts;
    }
}