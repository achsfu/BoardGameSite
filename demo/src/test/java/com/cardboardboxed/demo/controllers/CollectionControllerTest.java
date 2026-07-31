package com.cardboardboxed.demo.controllers;

import com.cardboardboxed.demo.boardgames.BoardGameAutocompleteRepository;
import com.cardboardboxed.demo.collections.CollectionItem;
import com.cardboardboxed.demo.collections.CollectionItem.CollectionType;
import com.cardboardboxed.demo.collections.CollectionItemRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CollectionController.class)
class CollectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollectionItemRepository collectionItemRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private BoardGameAutocompleteRepository boardGameAutocompleteRepository;

    @Test
    void addRedirectsToLoginWhenUserIsNotLoggedIn() throws Exception {
        mockMvc.perform(
                        post("/collection/add")
                                .param("gameName", "Catan")
                                .param("collectionType", "OWNED")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(collectionItemRepository, never()).save(any());
    }

    @Test
    void addSavesNewGameToOwnedCollection() throws Exception {
        User user = createUser("jun");
        when(userRepository.findByUsername("jun")).thenReturn(user);
        when(boardGameAutocompleteRepository.resolveToExistingName("Catan"))
                .thenReturn(Optional.of("Catan"));
        when(collectionItemRepository.findByUserAndGameNameIgnoreCase(user, "Catan"))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        post("/collection/add")
                                .sessionAttr("AUTH_USER", "jun")
                                .param("gameName", "Catan")
                                .param("collectionType", "OWNED")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        verify(collectionItemRepository).save(any(CollectionItem.class));
    }

    @Test
    void addExistingGameMovesItToRequestedCollection() throws Exception {
        User user = createUser("jun");
        CollectionItem item =
                new CollectionItem(user, "Catan", CollectionType.WISHLIST);

        when(userRepository.findByUsername("jun")).thenReturn(user);
        when(boardGameAutocompleteRepository.resolveToExistingName("Catan"))
                .thenReturn(Optional.of("Catan"));
        when(collectionItemRepository.findByUserAndGameNameIgnoreCase(user, "Catan"))
                .thenReturn(Optional.of(item));

        mockMvc.perform(
                        post("/collection/add")
                                .sessionAttr("AUTH_USER", "jun")
                                .param("gameName", "Catan")
                                .param("collectionType", "OWNED")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        assertEquals(CollectionType.OWNED, item.getCollectionType());
        verify(collectionItemRepository).save(item);
    }

    @Test
    void addDoesNotSaveUnknownGame() throws Exception {
        User user = createUser("jun");
        when(userRepository.findByUsername("jun")).thenReturn(user);
        when(boardGameAutocompleteRepository.resolveToExistingName("Unknown Game"))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        post("/collection/add")
                                .sessionAttr("AUTH_USER", "jun")
                                .param("gameName", "Unknown Game")
                                .param("collectionType", "OWNED")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        verify(collectionItemRepository, never()).save(any());
    }

    @Test
    void removeDeletesItemOwnedByLoggedInUser() throws Exception {
        User user = createUser("jun");
        CollectionItem item =
                new CollectionItem(user, "Catan", CollectionType.OWNED);

        when(userRepository.findByUsername("jun")).thenReturn(user);
        when(collectionItemRepository.findByIdAndUser(1, user))
                .thenReturn(Optional.of(item));

        mockMvc.perform(
                        post("/collection/remove")
                                .sessionAttr("AUTH_USER", "jun")
                                .param("id", "1")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        verify(collectionItemRepository).delete(item);
    }

    @Test
    void removeDoesNotDeleteAnotherUsersItem() throws Exception {
        User user = createUser("jun");

        when(userRepository.findByUsername("jun")).thenReturn(user);
        when(collectionItemRepository.findByIdAndUser(1, user))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        post("/collection/remove")
                                .sessionAttr("AUTH_USER", "jun")
                                .param("id", "1")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        verify(collectionItemRepository, never()).delete(any());
    }

    @Test
    void moveChangesItemCollectionType() throws Exception {
        User user = createUser("jun");
        CollectionItem item =
                new CollectionItem(user, "Catan", CollectionType.WISHLIST);

        when(userRepository.findByUsername("jun")).thenReturn(user);
        when(collectionItemRepository.findByIdAndUser(1, user))
                .thenReturn(Optional.of(item));

        mockMvc.perform(
                        post("/collection/move")
                                .sessionAttr("AUTH_USER", "jun")
                                .param("id", "1")
                                .param("collectionType", "OWNED")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        assertEquals(CollectionType.OWNED, item.getCollectionType());
        verify(collectionItemRepository).save(item);
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setRole("PLAYER");
        return user;
    }
}