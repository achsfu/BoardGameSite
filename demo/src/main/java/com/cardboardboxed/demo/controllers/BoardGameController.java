package com.cardboardboxed.demo.controllers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.boardgames.BoardGameRankRepository;
import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.reviews.ReviewLikeRepository;
import com.cardboardboxed.demo.reviews.ReviewReply;
import com.cardboardboxed.demo.reviews.ReviewReplyRepository;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class BoardGameController {

    private static final int GAME_PAGE_REVIEW_LIMIT = 5;

    private final BoardGameRankRepository boardGameRankRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewReplyRepository reviewReplyRepository;
    private final UserRepository userRepository;

    public BoardGameController(
            BoardGameRankRepository boardGameRankRepository,
            ReviewRepository reviewRepository,
            ReviewLikeRepository reviewLikeRepository,
            ReviewReplyRepository reviewReplyRepository,
            UserRepository userRepository
    ) {
        this.boardGameRankRepository = boardGameRankRepository;
        this.reviewRepository = reviewRepository;
        this.reviewLikeRepository = reviewLikeRepository;
        this.reviewReplyRepository = reviewReplyRepository;
        this.userRepository = userRepository;
    }

    /*
     * Preferred route.
     *
     * The database ID uniquely identifies the exact game record that
     * the user clicked, even when multiple games have the same title.
     */
    @GetMapping("/games/id/{id}")
    public String showGamePageById(
            @PathVariable Integer id,
            @RequestParam(
                    name = "success",
                    required = false
            ) String success,
            @RequestParam(
                    name = "error",
                    required = false
            ) String error,
            Model model,
            HttpServletRequest request
    ) {
        BoardGameRank game = boardGameRankRepository
                .findById(id)
                .orElse(null);

        if (game == null) {
            return "redirect:/?error=Game+not+found";
        }

        addGameDetailsToModel(
                game,
                success,
                error,
                model,
                request
        );

        return "game-detail";
    }

    /*
     * Older title-based route.
     *
     * This remains so existing links do not immediately break.
     */
    @GetMapping("/games/{title}")
    public String showGamePageByTitle(
            @PathVariable String title,
            @RequestParam(
                    name = "success",
                    required = false
            ) String success,
            @RequestParam(
                    name = "error",
                    required = false
            ) String error,
            Model model,
            HttpServletRequest request
    ) {
        BoardGameRank game = findGameByTitle(title);

        if (game == null) {
            return "redirect:/?error=Game+not+found";
        }

        String redirectUrl =
                "/games/id/" + game.getId();

        if (success != null && !success.isBlank()) {
            redirectUrl += "?success="
                    + UriUtils.encodeQueryParam(
                            success,
                            StandardCharsets.UTF_8
                    );
        } else if (error != null && !error.isBlank()) {
            redirectUrl += "?error="
                    + UriUtils.encodeQueryParam(
                            error,
                            StandardCharsets.UTF_8
                    );
        }

        return "redirect:" + redirectUrl;
    }

    /*
     * Preferred description-update route using the unique game ID.
     */
    @PostMapping("/games/id/{id}/description")
    public String updateGameDescriptionById(
            @PathVariable Integer id,
            @RequestParam(
                    name = "description",
                    defaultValue = ""
            ) String description,
            HttpServletRequest request
    ) {
        BoardGameRank game = boardGameRankRepository
                .findById(id)
                .orElse(null);

        if (game == null) {
            return "redirect:/?error=Game+not+found";
        }

        if (!canEditDescription(request)) {
            return "redirect:/games/id/"
                    + game.getId()
                    + "?error=Only+admins+and+moderators+can+edit+descriptions";
        }

        saveDescription(game, description);

        return "redirect:/games/id/"
                + game.getId()
                + "?success=Description+updated";
    }

    /*
     * Older title-based description route retained for compatibility.
     */
    @PostMapping("/games/{title}/description")
    public String updateGameDescriptionByTitle(
            @PathVariable String title,
            @RequestParam(
                    name = "description",
                    defaultValue = ""
            ) String description,
            HttpServletRequest request
    ) {
        BoardGameRank game = findGameByTitle(title);

        if (game == null) {
            return "redirect:/?error=Game+not+found";
        }

        if (!canEditDescription(request)) {
            return "redirect:/games/id/"
                    + game.getId()
                    + "?error=Only+admins+and+moderators+can+edit+descriptions";
        }

        saveDescription(game, description);

        return "redirect:/games/id/"
                + game.getId()
                + "?success=Description+updated";
    }

    @GetMapping("/games/search")
    public String showGameSearchPage(
            @RequestParam(
                    name = "q",
                    defaultValue = ""
            ) String query,
            @RequestParam(
                    name = "page",
                    defaultValue = "1"
            ) int page,
            Model model
    ) {
        String trimmedQuery =
                query == null
                        ? ""
                        : query.trim();

        if (!trimmedQuery.isBlank()) {
            BoardGameRank exactMatch =
                    findGameByTitle(trimmedQuery);

            if (exactMatch != null
                    && exactMatch.getTitle() != null
                    && exactMatch.getTitle()
                            .equalsIgnoreCase(trimmedQuery)) {

                return "redirect:/games/id/"
                        + exactMatch.getId();
            }
        }

        int currentPage =
                Math.max(page, 1) - 1;

        int pageSize = 12;

        PageRequest pageRequest =
                PageRequest.of(
                        currentPage,
                        pageSize
                );

        Page<BoardGameRank> similarGamesPage =
                trimmedQuery.isBlank()
                        ? Page.empty(pageRequest)
                        : boardGameRankRepository
                                .searchSimilarGames(
                                        trimmedQuery,
                                        pageRequest
                                );

        model.addAttribute(
                "query",
                trimmedQuery
        );

        model.addAttribute(
                "similarGames",
                similarGamesPage.getContent()
        );

        model.addAttribute(
                "similarGamesPage",
                similarGamesPage
        );

        model.addAttribute(
                "currentPage",
                similarGamesPage.getNumber() + 1
        );

        model.addAttribute(
                "totalPages",
                similarGamesPage.getTotalPages()
        );

        return "game-search";
    }

    /*
     * Loads all information used by game-detail.html.
     *
     * Reviews are sorted by:
     * 1. Like count, highest first
     * 2. Creation date, newest first
     */
    private void addGameDetailsToModel(
            BoardGameRank game,
            String success,
            String error,
            Model model,
            HttpServletRequest request
    ) {
        List<Review> allReviews =
                new ArrayList<>(
                        reviewRepository
                                .findByGameOrderByCreatedAtDesc(game)
                );

        Map<Integer, Long> reviewLikeCounts =
                buildReviewLikeCounts(allReviews);

        allReviews.sort(
                Comparator
                        .comparingLong(
                                (Review review) ->
                                        reviewLikeCounts.getOrDefault(
                                                review.getId(),
                                                0L
                                        )
                        )
                        .reversed()
                        .thenComparing(
                                Review::getCreatedAt,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
        );

        List<Review> topReviews =
                allReviews.stream()
                        .limit(GAME_PAGE_REVIEW_LIMIT)
                        .toList();

        User currentUser =
                getCurrentUser(request);

        Set<Integer> likedReviewIds =
                findLikedReviewIds(
                        topReviews,
                        currentUser
                );

        Map<Integer, List<ReviewReply>> reviewReplies =
                buildReviewReplies(topReviews);

        Map<Integer, Long> reviewReplyCounts =
                buildReviewReplyCounts(topReviews);

        Double averageReviewScore =
                reviewRepository
                        .findAverageRatingByGame(game);

        long reviewCount =
                reviewRepository.countByGame(game);

        model.addAttribute(
                "game",
                game
        );

        model.addAttribute(
                "recentReviews",
                topReviews
        );

        model.addAttribute(
                "reviewLikeCounts",
                reviewLikeCounts
        );

        model.addAttribute(
                "likedReviewIds",
                likedReviewIds
        );

        model.addAttribute(
                "reviewReplies",
                reviewReplies
        );

        model.addAttribute(
                "reviewReplyCounts",
                reviewReplyCounts
        );

        model.addAttribute(
                "currentUser",
                currentUser
        );

        model.addAttribute(
                "averageReviewScore",
                averageReviewScore
        );

        model.addAttribute(
                "reviewCount",
                reviewCount
        );

        model.addAttribute(
                "canEditDescription",
                canEditDescription(request)
        );

        model.addAttribute(
                "successMessage",
                success
        );

        model.addAttribute(
                "errorMessage",
                error
        );
    }

    /*
     * Counts the likes attached to each review.
     *
     * The map key is the review ID.
     * The map value is the number of likes.
     */
    private Map<Integer, Long> buildReviewLikeCounts(
            List<Review> reviews
    ) {
        Map<Integer, Long> likeCounts =
                new HashMap<>();

        for (Review review : reviews) {
            if (review == null
                    || review.getId() == null) {

                continue;
            }

            long likeCount =
                    reviewLikeRepository
                            .countByReview(review);

            likeCounts.put(
                    review.getId(),
                    likeCount
            );
        }

        return likeCounts;
    }

    /*
     * Determines which visible reviews the current user has liked.
     */
    private Set<Integer> findLikedReviewIds(
            List<Review> reviews,
            User currentUser
    ) {
        Set<Integer> likedReviewIds =
                new HashSet<>();

        if (currentUser == null) {
            return likedReviewIds;
        }

        for (Review review : reviews) {
            if (review == null
                    || review.getId() == null) {

                continue;
            }

            boolean liked =
                    reviewLikeRepository
                            .existsByReviewAndUser(
                                    review,
                                    currentUser
                            );

            if (liked) {
                likedReviewIds.add(
                        review.getId()
                );
            }
        }

        return likedReviewIds;
    }

    /*
     * Loads the replies attached to each visible review.
     *
     * Replies are displayed from oldest to newest so that the
     * conversation reads naturally from top to bottom.
     */
    private Map<Integer, List<ReviewReply>> buildReviewReplies(
            List<Review> reviews
    ) {
        Map<Integer, List<ReviewReply>> repliesByReview =
                new HashMap<>();

        for (Review review : reviews) {
            if (review == null
                    || review.getId() == null) {

                continue;
            }

            List<ReviewReply> replies =
                    reviewReplyRepository
                            .findByReviewOrderByCreatedAtAsc(
                                    review
                            );

            repliesByReview.put(
                    review.getId(),
                    replies
            );
        }

        return repliesByReview;
    }

    /*
     * Counts the replies attached to each visible review.
     */
    private Map<Integer, Long> buildReviewReplyCounts(
            List<Review> reviews
    ) {
        Map<Integer, Long> replyCounts =
                new HashMap<>();

        for (Review review : reviews) {
            if (review == null
                    || review.getId() == null) {

                continue;
            }

            long replyCount =
                    reviewReplyRepository
                            .countByReview(review);

            replyCounts.put(
                    review.getId(),
                    replyCount
            );
        }

        return replyCounts;
    }

    private void saveDescription(
            BoardGameRank game,
            String description
    ) {
        String normalizedDescription =
                description == null
                        ? ""
                        : description.trim();

        game.setDescription(
                normalizedDescription.isBlank()
                        ? null
                        : normalizedDescription
        );

        boardGameRankRepository.save(game);
    }

    private BoardGameRank findGameByTitle(
            String title
    ) {
        if (title == null || title.isBlank()) {
            return null;
        }

        return boardGameRankRepository
                .findFirstByTitleIgnoreCaseOrderByRankPositionAsc(
                        title.trim()
                )
                .orElse(null);
    }

    /*
     * Returns the currently logged-in user.
     *
     * Returns null if there is no active authenticated session.
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

        return userRepository
                .findByUsername(username);
    }

    private boolean canEditDescription(
            HttpServletRequest request
    ) {
        User user =
                getCurrentUser(request);

        if (user == null) {
            return false;
        }

        String role =
                user.getRole();

        if (role == null) {
            return false;
        }

        String normalizedRole =
                role.trim()
                        .toUpperCase(Locale.ROOT);

        return "ADMIN".equals(normalizedRole)
                || "MODERATOR".equals(normalizedRole);
    }
}