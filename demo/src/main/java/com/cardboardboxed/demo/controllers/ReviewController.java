package com.cardboardboxed.demo.controllers;

import com.cardboardboxed.demo.boardgames.BoardGameAutocompleteRepository;
import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.boardgames.BoardGameRankRepository;
import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.reviews.ReviewLike;
import com.cardboardboxed.demo.reviews.ReviewLikeRepository;
import com.cardboardboxed.demo.reviews.ReviewReply;
import com.cardboardboxed.demo.reviews.ReviewReplyRepository;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReviewController {

    private static final int MAX_REPLY_LENGTH = 1000;

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewReplyRepository reviewReplyRepository;
    private final BoardGameRankRepository boardGameRankRepository;
    private final UserRepository userRepository;
    private final BoardGameAutocompleteRepository boardGameAutocompleteRepository;

    public ReviewController(
            ReviewRepository reviewRepository,
            ReviewLikeRepository reviewLikeRepository,
            ReviewReplyRepository reviewReplyRepository,
            BoardGameRankRepository boardGameRankRepository,
            UserRepository userRepository,
            BoardGameAutocompleteRepository boardGameAutocompleteRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.reviewLikeRepository = reviewLikeRepository;
        this.reviewReplyRepository = reviewReplyRepository;
        this.boardGameRankRepository = boardGameRankRepository;
        this.userRepository = userRepository;
        this.boardGameAutocompleteRepository =
                boardGameAutocompleteRepository;
    }

    /*
     * Creates a new review.
     */
    @PostMapping("/reviews")
    public String postReview(
            Review review,
            @RequestParam(
                    name = "redirectTo",
                    required = false
            ) String redirectTo,
            HttpServletRequest request
    ) {
        String safeRedirectTarget =
                resolveSafeRedirectTarget(redirectTo);

        User currentUser = getCurrentUser(request);

        if (currentUser == null) {
            return "redirect:/login"
                    + "?error=Please+log+in+to+post+a+review";
        }

        String resolvedGameTitle =
                boardGameAutocompleteRepository
                        .resolveToExistingName(
                                review.getGameTitle()
                        )
                        .orElse(null);

        if (resolvedGameTitle == null
                || resolvedGameTitle.isBlank()) {

            return "redirect:"
                    + safeRedirectTarget
                    + "?error=Please+choose+a+valid+board+game";
        }

        BoardGameRank boardGame =
                boardGameRankRepository
                        .findFirstByTitleIgnoreCaseOrderByRankPositionAsc(
                                resolvedGameTitle
                        )
                        .orElse(null);

        if (boardGame == null) {
            return "redirect:"
                    + safeRedirectTarget
                    + "?error=Please+choose+a+valid+board+game";
        }

        review.setGame(boardGame);
        review.setGameTitle(boardGame.getTitle());
        review.setUser(currentUser);

        reviewRepository.save(review);

        return "redirect:" + safeRedirectTarget;
    }

    /*
     * Likes a review when the user has not liked it yet.
     * Removes the like when the user has already liked it.
     */
    @Transactional
    @PostMapping("/reviews/{id}/like")
    public String toggleReviewLike(
            @PathVariable Integer id,
            @RequestParam(
                    name = "redirectTo",
                    required = false
            ) String redirectTo,
            HttpServletRequest request
    ) {
        User currentUser = getCurrentUser(request);

        if (currentUser == null) {
            return "redirect:/login"
                    + "?error=Please+log+in+to+like+reviews";
        }

        Review review =
                reviewRepository
                        .findById(id)
                        .orElse(null);

        if (review == null) {
            return "redirect:"
                    + resolveSafeRedirectTarget(redirectTo)
                    + "?error=Review+not+found";
        }

        ReviewLike existingLike =
                reviewLikeRepository
                        .findByReviewAndUser(
                                review,
                                currentUser
                        )
                        .orElse(null);

        if (existingLike == null) {
            ReviewLike reviewLike =
                    new ReviewLike(
                            review,
                            currentUser
                    );

            reviewLikeRepository.save(reviewLike);
        } else {
            reviewLikeRepository.delete(existingLike);
        }

        return "redirect:"
                + resolveReviewRedirectTarget(
                        review,
                        redirectTo
                );
    }

    /*
     * Adds a one-level reply beneath a review.
     */
    @Transactional
    @PostMapping("/reviews/{id}/reply")
    public String postReply(
            @PathVariable Integer id,
            @RequestParam(
                    name = "replyText",
                    defaultValue = ""
            ) String replyText,
            @RequestParam(
                    name = "redirectTo",
                    required = false
            ) String redirectTo,
            HttpServletRequest request
    ) {
        User currentUser = getCurrentUser(request);

        if (currentUser == null) {
            return "redirect:/login"
                    + "?error=Please+log+in+to+reply+to+reviews";
        }

        Review review =
                reviewRepository
                        .findById(id)
                        .orElse(null);

        if (review == null) {
            return "redirect:"
                    + resolveSafeRedirectTarget(redirectTo)
                    + "?error=Review+not+found";
        }

        String normalizedReply =
                replyText == null
                        ? ""
                        : replyText.trim();

        String reviewRedirectTarget =
                resolveReviewRedirectTarget(
                        review,
                        redirectTo
                );

        if (normalizedReply.isBlank()) {
            return "redirect:"
                    + reviewRedirectTarget
                    + "?error=Reply+cannot+be+empty";
        }

        if (normalizedReply.length() > MAX_REPLY_LENGTH) {
            return "redirect:"
                    + reviewRedirectTarget
                    + "?error=Reply+cannot+exceed+1000+characters";
        }

        ReviewReply reply =
                new ReviewReply(
                        review,
                        currentUser,
                        normalizedReply
                );

        reviewReplyRepository.save(reply);

        return "redirect:"
                + buildReplyRedirectTarget(
                        reviewRedirectTarget,
                        id
                );
    }

    /*
     * Allows users to delete only their own reviews.
     *
     * Likes and replies are automatically deleted because Review
     * owns both relationships using cascade and orphanRemoval.
     */
    @Transactional
    @PostMapping("/reviews/{id}/delete")
    public String deleteOwnReview(
            @PathVariable Integer id,
            HttpServletRequest request
    ) {
        User currentUser = getCurrentUser(request);

        if (currentUser == null) {
            return "redirect:/login"
                    + "?error=Please+log+in+to+manage+reviews";
        }

        Review review =
                reviewRepository
                        .findById(id)
                        .orElse(null);

        if (review == null
                || review.getUser() == null) {

            return "redirect:/profile"
                    + "?error=Review+not+found";
        }

        String reviewOwner =
                review.getUser().getUsername();

        if (reviewOwner == null
                || currentUser.getUsername() == null
                || !currentUser
                        .getUsername()
                        .equalsIgnoreCase(reviewOwner)) {

            return "redirect:/profile"
                    + "?error=You+can+only+delete+your+own+reviews";
        }

        reviewRepository.delete(review);

        return "redirect:/profile"
                + "?success=Review+deleted";
    }

    /*
     * Returns the authenticated user from the current session.
     */
    private User getCurrentUser(
            HttpServletRequest request
    ) {
        HttpSession session =
                request.getSession(false);

        if (session == null) {
            return null;
        }

        String username =
                (String) session.getAttribute(
                        "AUTH_USER"
                );

        if (username == null
                || username.isBlank()) {

            return null;
        }

        return userRepository.findByUsername(username);
    }

    /*
     * Uses the game's unique ID as the preferred redirect target.
     */
    private String resolveReviewRedirectTarget(
            Review review,
            String redirectTo
    ) {
        if (redirectTo != null
                && !redirectTo.isBlank()) {

            return resolveSafeRedirectTarget(redirectTo);
        }

        if (review != null
                && review.getGame() != null
                && review.getGame().getId() != null) {

            return "/games/id/"
                    + review.getGame().getId();
        }

        return "/dashboard";
    }

    /*
     * Prevents arbitrary external redirects.
     */
    private String buildReplyRedirectTarget(
            String redirectTarget,
            Integer reviewId
    ) {
        String target =
                redirectTarget == null
                        || redirectTarget.isBlank()
                        ? "/dashboard"
                        : redirectTarget;

        StringBuilder builder =
                new StringBuilder(target);

        if (builder.indexOf("?") >= 0) {
            builder.append("&");
        } else {
            builder.append("?");
        }

        builder.append("success=Reply+posted");

        if (reviewId != null) {
            builder.append("&openReply=")
                    .append(reviewId);
        }

        return builder.toString();
    }

    private String resolveSafeRedirectTarget(
            String redirectTo
    ) {
        if (redirectTo != null
                && !redirectTo.isBlank()) {

            String trimmed =
                    redirectTo.trim();
            String lowerCase =
                    trimmed.toLowerCase(Locale.ROOT);

            if (trimmed.startsWith("/")
                    && !trimmed.startsWith("//")
                    && !lowerCase.contains("://")
                    && !lowerCase.startsWith("javascript:")) {

                return trimmed;
            }
        }

        return "/dashboard";
    }
}