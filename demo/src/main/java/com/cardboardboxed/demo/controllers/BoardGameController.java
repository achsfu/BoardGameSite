package com.cardboardboxed.demo.controllers;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.boardgames.BoardGameRankRepository;
import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.reviews.ReviewRepository;

@Controller
public class BoardGameController {

    private final BoardGameRankRepository boardGameRankRepository;
    private final ReviewRepository reviewRepository;

    public BoardGameController(BoardGameRankRepository boardGameRankRepository, ReviewRepository reviewRepository) {
        this.boardGameRankRepository = boardGameRankRepository;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/games/{title}")
    public String showGamePage(@PathVariable String title, Model model) {
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

        return "game-detail";
    }

    @GetMapping("/games/search")
    public String showGameSearchPage(
            @RequestParam(name = "q", defaultValue = "") String query,
            Model model
    ) {
        String trimmedQuery = query == null ? "" : query.trim();
        List<BoardGameRank> similarGames = trimmedQuery.isBlank()
                ? List.of()
                : boardGameRankRepository.findTop12ByTitleContainingIgnoreCaseOrderByRankPositionAsc(trimmedQuery);

        model.addAttribute("query", trimmedQuery);
        model.addAttribute("similarGames", similarGames);
        model.addAttribute("games", boardGameRankRepository.findAllByOrderByRankPositionAsc());

        return "game-search";
    }
}