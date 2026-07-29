package com.cardboardboxed.demo.controllers;

import com.cardboardboxed.demo.boardgames.BoardGameAutocompleteRepository;
import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.boardgames.BoardGameRankRepository;
import com.cardboardboxed.demo.collections.CollectionItem;
import com.cardboardboxed.demo.collections.CollectionItem.CollectionType;
import com.cardboardboxed.demo.collections.CollectionItemRepository;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserFollowRepository;
import com.cardboardboxed.demo.useracounts.UserRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ProfileController.class)
class ProfileControllerCollectionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ReviewRepository reviewRepository;

    @MockitoBean
    private BoardGameAutocompleteRepository boardGameAutocompleteRepository;

    @MockitoBean
    private BoardGameRankRepository boardGameRankRepository;

    @MockitoBean
    private UserFollowRepository userFollowRepository;

    @MockitoBean
    private CollectionItemRepository collectionItemRepository;

    @Test
    void collectionRedirectsToLoginWhenUserIsNotLoggedIn() throws Exception {
        mockMvc.perform(get("/collection"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=Please+log+in+to+view+your+collection"));
    }

    @Test
    void collectionUsesNameAscendingSortByDefault() throws Exception {
        User user = createUser("sammy", "PLAYER");

        List<CollectionItem> items = List.of(
                createItem(1, user, "Zulu", CollectionType.OWNED, LocalDateTime.of(2026, 7, 10, 12, 0)),
                createItem(2, user, "Alpha", CollectionType.OWNED, LocalDateTime.of(2026, 7, 11, 12, 0))
        );

        List<BoardGameRank> games = List.of(
                createGame("Zulu", 2, 4, 3.0, 60),
                createGame("Alpha", 2, 4, 2.5, 45)
        );

        when(userRepository.findByUsername("sammy")).thenReturn(user);
        when(collectionItemRepository.findByUserOrderByAddedAtDesc(user)).thenReturn(items);
        when(reviewRepository.findByUserUsername("sammy")).thenReturn(List.of());
        when(boardGameRankRepository.findByNormalizedTitles(anyList())).thenReturn(games);

        mockMvc.perform(get("/collection").sessionAttr("AUTH_USER", "sammy"))
                .andExpect(status().isOk())
                .andExpect(view().name("collection"))
                .andExpect(model().attribute("sort", "name"))
                .andExpect(model().attribute("dir", "asc"))
                .andExpect(model().attribute("rows", contains(
                        hasProperty("name", is("Alpha")),
                        hasProperty("name", is("Zulu"))
                )));
    }

    @Test
    void collectionSliderBoundsComeFromUsersCollectionOnly() throws Exception {
        User user = createUser("sammy", "PLAYER");

        List<CollectionItem> items = List.of(
                createItem(1, user, "Tiny Game", CollectionType.OWNED, LocalDateTime.of(2026, 7, 1, 12, 0)),
                createItem(2, user, "Epic Game", CollectionType.WISHLIST, LocalDateTime.of(2026, 7, 2, 12, 0))
        );

        List<BoardGameRank> games = List.of(
                createGame("Tiny Game", 1, 2, 1.2, 30),
                createGame("Epic Game", 3, 5, 4.7, 180)
        );

        when(userRepository.findByUsername("sammy")).thenReturn(user);
        when(collectionItemRepository.findByUserOrderByAddedAtDesc(user)).thenReturn(items);
        when(reviewRepository.findByUserUsername("sammy")).thenReturn(List.of());
        when(boardGameRankRepository.findByNormalizedTitles(anyList())).thenReturn(games);

        mockMvc.perform(get("/collection").sessionAttr("AUTH_USER", "sammy"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("playersGlobalMin", 1))
                .andExpect(model().attribute("playersGlobalMax", 5))
                .andExpect(model().attribute("timeGlobalMin", 30))
                .andExpect(model().attribute("timeGlobalMax", 180))
                .andExpect(model().attribute("complexityGlobalMin", 1.2))
                .andExpect(model().attribute("complexityGlobalMax", 4.7));
    }

    @Test
    void collectionAppliesPlayerRangeFilter() throws Exception {
        User user = createUser("sammy", "PLAYER");

        List<CollectionItem> items = List.of(
                createItem(1, user, "Tiny Game", CollectionType.OWNED, LocalDateTime.of(2026, 7, 1, 12, 0)),
                createItem(2, user, "Epic Game", CollectionType.OWNED, LocalDateTime.of(2026, 7, 2, 12, 0))
        );

        List<BoardGameRank> games = List.of(
                createGame("Tiny Game", 1, 2, 1.2, 30),
                createGame("Epic Game", 3, 5, 4.7, 180)
        );

        when(userRepository.findByUsername("sammy")).thenReturn(user);
        when(collectionItemRepository.findByUserOrderByAddedAtDesc(user)).thenReturn(items);
        when(reviewRepository.findByUserUsername("sammy")).thenReturn(List.of());
        when(boardGameRankRepository.findByNormalizedTitles(anyList())).thenReturn(games);

        mockMvc.perform(
                        get("/collection")
                                .sessionAttr("AUTH_USER", "sammy")
                                .param("playersMin", "4")
                                .param("playersMax", "6")
                )
                .andExpect(status().isOk())
                .andExpect(model().attribute("rows", hasSize(1)))
                .andExpect(model().attribute("rows", contains(
                        hasProperty("name", is("Epic Game"))
                )));
    }

    @Test
    void collectionPaginatesAtTwentyEntriesPerPage() throws Exception {
        User user = createUser("sammy", "PLAYER");

        List<CollectionItem> items = new ArrayList<>();
        List<BoardGameRank> games = new ArrayList<>();

        for (int i = 1; i <= 21; i++) {
            String gameName = "Game " + i;
            items.add(createItem(i, user, gameName, CollectionType.OWNED, LocalDateTime.of(2026, 7, 1, 12, 0).plusDays(i)));
            games.add(createGame(gameName, 2, 4, 2.0, 60));
        }

        when(userRepository.findByUsername("sammy")).thenReturn(user);
        when(collectionItemRepository.findByUserOrderByAddedAtDesc(user)).thenReturn(items);
        when(reviewRepository.findByUserUsername("sammy")).thenReturn(List.of());
        when(boardGameRankRepository.findByNormalizedTitles(anyList())).thenReturn(games);

        mockMvc.perform(
                        get("/collection")
                                .sessionAttr("AUTH_USER", "sammy")
                                .param("page", "2")
                )
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 2))
                .andExpect(model().attribute("totalPages", 2))
                .andExpect(model().attribute("totalItems", 21))
                .andExpect(model().attribute("rows", hasSize(1)));
    }

    private User createUser(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        return user;
    }

    private CollectionItem createItem(
            int id,
            User user,
            String gameName,
            CollectionType type,
            LocalDateTime addedAt
    ) {
        CollectionItem item = new CollectionItem(user, gameName, type);
        ReflectionTestUtils.setField(item, "id", id);
        ReflectionTestUtils.setField(item, "addedAt", addedAt);
        return item;
    }

    private BoardGameRank createGame(
            String title,
            int minPlayers,
            int maxPlayers,
            double complexity,
            int playtime
    ) {
        BoardGameRank game = new BoardGameRank();
        game.setTitle(title);
        game.setMinPlayers(minPlayers);
        game.setMaxPlayers(maxPlayers);
        game.setGameWeight(complexity);
        game.setManufacturerPlaytime(playtime);
        return game;
    }
}
