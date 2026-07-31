package com.cardboardboxed.demo.controllers;

import com.cardboardboxed.demo.boardgames.BoardGameAutocompleteRepository;
import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.boardgames.BoardGameRankRepository;
import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.UserRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({BoardGameController.class, BoardGameApiController.class})
class BoardGameSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BoardGameRankRepository boardGameRankRepository;

    @MockitoBean
    private ReviewRepository reviewRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private BoardGameAutocompleteRepository boardGameAutocompleteRepository;

    @Test
    void gameSearchRedirectsToIdOnExactTitleMatch() throws Exception {
        BoardGameRank game = createGame(42, "Catan");

        when(boardGameRankRepository.findFirstByTitleIgnoreCaseOrderByRankPositionAsc("Catan"))
                .thenReturn(Optional.of(game));

        mockMvc.perform(get("/games/search").param("q", "Catan"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/games/id/42"));
    }

    @Test
    void gameSearchRendersSimilarGamesWhenNoExactMatch() throws Exception {
        BoardGameRank similarA = createGame(1, "Catan Junior");
        BoardGameRank similarB = createGame(2, "Star Catan");

        PageRequest pageRequest = PageRequest.of(0, 12);
        PageImpl<BoardGameRank> page = new PageImpl<>(
                List.of(similarA, similarB),
                pageRequest,
                2
        );

        when(boardGameRankRepository.findFirstByTitleIgnoreCaseOrderByRankPositionAsc("catanx"))
                .thenReturn(Optional.empty());

        when(boardGameRankRepository.searchSimilarGames(eq("catanx"), any(PageRequest.class)))
                .thenReturn(page);

        mockMvc.perform(get("/games/search").param("q", "catanx"))
                .andExpect(status().isOk())
                .andExpect(view().name("game-search"))
                .andExpect(model().attribute("query", "catanx"))
                .andExpect(model().attribute("similarGames", hasSize(2)))
                .andExpect(model().attribute("currentPage", 1))
                .andExpect(model().attribute("totalPages", 1));
    }

    @Test
    void gameDetailByIdLoadsPageAndReviewStats() throws Exception {
        BoardGameRank game = createGame(7, "Azul");

        when(boardGameRankRepository.findById(7))
                .thenReturn(Optional.of(game));

        when(reviewRepository.findTop5ByGameOrderByCreatedAtDesc(game))
                .thenReturn(List.of(new Review()));

        when(reviewRepository.findAverageRatingByGame(game))
                .thenReturn(4.25);

        when(reviewRepository.countByGame(game))
                .thenReturn(3L);

        mockMvc.perform(get("/games/id/7"))
                .andExpect(status().isOk())
                .andExpect(view().name("game-detail"))
                .andExpect(model().attribute("game", game))
                .andExpect(model().attribute("reviewCount", 3L))
                .andExpect(model().attribute("averageReviewScore", 4.25))
                .andExpect(model().attribute("canEditDescription", false));
    }

    @Test
    void gameDetailByTitleRedirectsToIdRoute() throws Exception {
        BoardGameRank game = createGame(7, "Azul");

        when(boardGameRankRepository.findFirstByTitleIgnoreCaseOrderByRankPositionAsc("Azul"))
                .thenReturn(Optional.of(game));

        mockMvc.perform(get("/games/Azul"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/games/id/7"));
    }

    @Test
    void autocompleteReturnsSuggestions() throws Exception {
        when(boardGameAutocompleteRepository.findByPrefix("ca", 8))
                .thenReturn(List.of("Catan", "Carcassonne"));

        mockMvc.perform(get("/api/boardgames/autocomplete").param("q", "ca"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]", is("Catan")))
                .andExpect(jsonPath("$[1]", is("Carcassonne")));
    }

    @Test
    void resolveReturnsExactMatchTrueWhenResolvedNameMatchesQuery() throws Exception {
        when(boardGameAutocompleteRepository.resolveToExistingName("Catan"))
                .thenReturn(Optional.of("Catan"));

        mockMvc.perform(get("/api/boardgames/resolve").param("q", "Catan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvedName", is("Catan")))
                .andExpect(jsonPath("$.exactMatch", is(true)));
    }

    @Test
    void resolveReturnsExactMatchFalseWhenNoResolvedName() throws Exception {
        when(boardGameAutocompleteRepository.resolveToExistingName("does-not-exist"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/boardgames/resolve").param("q", "does-not-exist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvedName", is("")))
                .andExpect(jsonPath("$.exactMatch", is(false)));
    }

    private BoardGameRank createGame(int id, String title) {
        BoardGameRank game = new BoardGameRank();
        game.setId(id);
        game.setTitle(title);
        return game;
    }
}
