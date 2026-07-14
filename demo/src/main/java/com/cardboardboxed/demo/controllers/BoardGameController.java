package com.cardboardboxed.demo.controllers;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.util.UriUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.boardgames.BoardGameRankRepository;
import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserRepository;

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

    @GetMapping("/games/{title}")
    public String showGamePage(
            @PathVariable String title,
            @RequestParam(name = "success", required = false) String success,
            @RequestParam(name = "error", required = false) String error,
            Model model,
            HttpServletRequest request
    ) {
        BoardGameRank game = boardGameRankRepository.findByTitleIgnoreCase(title);

        if (game == null) {
            return "redirect:/dashboard?error=Game+not+found";
        }

        List<Review> recentReviews = reviewRepository.findTop5ByGameOrderByCreatedAtDesc(game);
        Double averageReviewScore = reviewRepository.findAverageRatingByGame(game);
        long reviewCount = reviewRepository.countByGame(game);

        model.addAttribute("game", game);
        model.addAttribute("recentReviews", recentReviews);
        model.addAttribute("averageReviewScore", averageReviewScore);
        model.addAttribute("reviewCount", reviewCount);
        model.addAttribute("canEditDescription", canEditDescription(request));
        model.addAttribute("successMessage", success);
        model.addAttribute("errorMessage", error);

        return "game-detail";
    }

    @PostMapping("/games/{title}/description")
    public String updateGameDescription(
            @PathVariable String title,
            @RequestParam(name = "description", defaultValue = "") String description,
            HttpServletRequest request
    ) {
        BoardGameRank game = boardGameRankRepository.findByTitleIgnoreCase(title);
        if (game == null) {
            return "redirect:/dashboard?error=Game+not+found";
        }

        String encodedTitle = UriUtils.encodePathSegment(game.getTitle(), StandardCharsets.UTF_8);
        if (!canEditDescription(request)) {
            return "redirect:/games/" + encodedTitle + "?error=Only+admins+and+moderators+can+edit+descriptions";
        }

        String normalizedDescription = description == null ? "" : description.trim();
        game.setDescription(normalizedDescription.isBlank() ? null : normalizedDescription);
        boardGameRankRepository.save(game);

        return "redirect:/games/" + encodedTitle + "?success=Description+updated";
    }

    @GetMapping("/games/search")
    public String showGameSearchPage(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model
    ) {
        String trimmedQuery = query == null ? "" : query.trim();

        if (!trimmedQuery.isBlank()) {
            BoardGameRank exactMatch = boardGameRankRepository.findByTitleIgnoreCase(trimmedQuery);
            if (exactMatch != null
                    && exactMatch.getTitle() != null
                    && exactMatch.getTitle().equalsIgnoreCase(trimmedQuery)
                    && !Boolean.TRUE.equals(exactMatch.getIsExpansion())) {
                String encodedTitle = UriUtils.encodePathSegment(exactMatch.getTitle(), StandardCharsets.UTF_8);
                return "redirect:/games/" + encodedTitle;
            }
        }

        int currentPage = Math.max(page, 1) - 1;
        int pageSize = 12;
        PageRequest pageRequest = PageRequest.of(currentPage, pageSize);

        Page<BoardGameRank> similarGamesPage = trimmedQuery.isBlank()
                ? Page.empty(pageRequest)
                : boardGameRankRepository.searchSimilarGames(trimmedQuery, pageRequest);

        model.addAttribute("query", trimmedQuery);
        model.addAttribute("similarGames", similarGamesPage.getContent());
        model.addAttribute("similarGamesPage", similarGamesPage);
        model.addAttribute("currentPage", similarGamesPage.getNumber() + 1);
        model.addAttribute("totalPages", similarGamesPage.getTotalPages());
        model.addAttribute("games", boardGameRankRepository.findAllByIsExpansionOrderByRankPositionAsc(false));

        return "game-search";
    }

    private boolean canEditDescription(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        String username = (String) session.getAttribute("AUTH_USER");
        if (username == null || username.isBlank()) {
            return false;
        }

        User user = userRepository.findByUsername(username);
        if (user == null) {
            return false;
        }

        String role = user.getRole();
        if (role == null) {
            return false;
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        return "ADMIN".equals(normalizedRole) || "MODERATOR".equals(normalizedRole);
    }
}