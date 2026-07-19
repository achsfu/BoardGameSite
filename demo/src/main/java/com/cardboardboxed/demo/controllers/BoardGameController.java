package com.cardboardboxed.demo.controllers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

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
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class BoardGameController {

    private final BoardGameRankRepository boardGameRankRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public BoardGameController(
            BoardGameRankRepository boardGameRankRepository,
            ReviewRepository reviewRepository,
            UserRepository userRepository
    ) {
        this.boardGameRankRepository = boardGameRankRepository;
        this.reviewRepository = reviewRepository;
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
            @RequestParam(name = "success", required = false) String success,
            @RequestParam(name = "error", required = false) String error,
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
            @RequestParam(name = "success", required = false) String success,
            @RequestParam(name = "error", required = false) String error,
            Model model,
            HttpServletRequest request
    ) {
        BoardGameRank game = findGameByTitle(title);

        if (game == null) {
            return "redirect:/?error=Game+not+found";
        }

        /*
         * Redirect to the unique ID-based URL.
         */
        String redirectUrl = "/games/id/" + game.getId();

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
                query == null ? "" : query.trim();

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

        int currentPage = Math.max(page, 1) - 1;
        int pageSize = 12;

        PageRequest pageRequest =
                PageRequest.of(currentPage, pageSize);

        Page<BoardGameRank> similarGamesPage =
                trimmedQuery.isBlank()
                        ? Page.empty(pageRequest)
                        : boardGameRankRepository.searchSimilarGames(
                                trimmedQuery,
                                pageRequest
                        );

        model.addAttribute("query", trimmedQuery);
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
        model.addAttribute(
                "games",
                boardGameRankRepository
                        .findAllByIsExpansionOrderByRankPositionAsc(false)
        );

        return "game-search";
    }

    private void addGameDetailsToModel(
            BoardGameRank game,
            String success,
            String error,
            Model model,
            HttpServletRequest request
    ) {
        List<Review> recentReviews =
                reviewRepository.findTop5ByGameOrderByCreatedAtDesc(game);

        Double averageReviewScore =
                reviewRepository.findAverageRatingByGame(game);

        long reviewCount =
                reviewRepository.countByGame(game);

        model.addAttribute("game", game);
        model.addAttribute(
                "recentReviews",
                recentReviews
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

    private BoardGameRank findGameByTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }

        return boardGameRankRepository
                .findFirstByTitleIgnoreCaseAndIsExpansionFalse(
                        title.trim()
                )
                .orElse(null);
    }

    private boolean canEditDescription(
            HttpServletRequest request
    ) {
        HttpSession session =
                request.getSession(false);

        if (session == null) {
            return false;
        }

        String username =
                (String) session.getAttribute("AUTH_USER");

        if (username == null || username.isBlank()) {
            return false;
        }

        User user =
                userRepository.findByUsername(username);

        if (user == null) {
            return false;
        }

        String role = user.getRole();

        if (role == null) {
            return false;
        }

        String normalizedRole =
                role.trim().toUpperCase(Locale.ROOT);

        return "ADMIN".equals(normalizedRole)
                || "MODERATOR".equals(normalizedRole);
    }
}