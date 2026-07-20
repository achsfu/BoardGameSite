package com.cardboardboxed.demo.controllers;

import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpSession;

import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminModerationController.class)
class AdminModerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ReviewRepository reviewRepository;

    @Test
    void playerCannotOpenModerationPage() throws Exception {

        User player = new User();
        player.setUsername("player");
        player.setRole("PLAYER");

        when(userRepository.findByUsername("player"))
                .thenReturn(player);

        mockMvc.perform(
                get("/moderation/reviews")
                        .sessionAttr("AUTH_USER", "player")
        )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void moderatorCanOpenModerationPage() throws Exception {

        User moderator = new User();
        moderator.setUsername("mod");
        moderator.setRole("MODERATOR");

        when(userRepository.findByUsername("mod"))
                .thenReturn(moderator);

        when(reviewRepository.findAll())
                .thenReturn(java.util.List.of());

        mockMvc.perform(
                get("/moderation/reviews")
                        .sessionAttr("AUTH_USER", "mod")
        )
                .andExpect(status().isOk())
                .andExpect(view().name("moderation-reviews"));
    }

    @Test
    void adminCanOpenModerationPage() throws Exception {

        User admin = new User();
        admin.setUsername("admin");
        admin.setRole("ADMIN");

        when(userRepository.findByUsername("admin"))
                .thenReturn(admin);

        when(reviewRepository.findAll())
                .thenReturn(java.util.List.of());

        mockMvc.perform(
                get("/moderation/reviews")
                        .sessionAttr("AUTH_USER", "admin")
        )
                .andExpect(status().isOk())
                .andExpect(view().name("moderation-reviews"));
    }

    @Test
    void moderatorCannotDeleteAllReviews() throws Exception {

        User moderator = new User();
        moderator.setUsername("mod");
        moderator.setRole("MODERATOR");

        when(userRepository.findByUsername("mod"))
                .thenReturn(moderator);

        mockMvc.perform(
                post("/moderation/reviews/delete-all")
                        .sessionAttr("AUTH_USER", "mod")
        )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(reviewRepository, never()).deleteAllInBatch();
    }

    @Test
    void adminCanDeleteAllReviews() throws Exception {

        User admin = new User();
        admin.setUsername("admin");
        admin.setRole("ADMIN");

        when(userRepository.findByUsername("admin"))
                .thenReturn(admin);

        mockMvc.perform(
                post("/moderation/reviews/delete-all")
                        .sessionAttr("AUTH_USER", "admin")
        )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/moderation/reviews"));

        verify(reviewRepository).deleteAllInBatch();
    }
}