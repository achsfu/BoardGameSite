package com.cardboardboxed.demo.controllers;

import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.boardgames.BoardGameRankRepository;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserRepository;
import com.cardboardboxed.demo.useracounts.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(LoginController.class)
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private BoardGameRankRepository boardGameRankRepository;

    @MockitoBean
    private ReviewRepository reviewRepository;

    @Test
    void dashboardRedirectsToLoginWhenUserIsNotLoggedIn() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/login?error=Please+log+in+to+access+the+dashboard"
                ));
    }

    @Test
    void dashboardLoadsForLoggedInUser() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        user.setRole("PLAYER");

        when(userRepository.findByUsername("testuser"))
                .thenReturn(user);

        mockMvc.perform(
                        get("/dashboard")
                                .sessionAttr("AUTH_USER", "testuser")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("username", "testuser"))
                .andExpect(model().attribute("role", "PLAYER"));
    }

    @Test
    void dashboardAddsUsernameAndRoleToModel() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        user.setRole("PLAYER");

        when(userRepository.findByUsername("testuser"))
                .thenReturn(user);

        mockMvc.perform(
                        get("/dashboard")
                                .sessionAttr("AUTH_USER", "testuser")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("username", "testuser"))
                .andExpect(model().attribute("role", "PLAYER"));
    }

    @Test
    void userDefaultsToPlayerRoleWhenRoleIsMissing() throws Exception {
        User user = new User();
        user.setUsername("testuser");

        when(userRepository.findByUsername("testuser"))
                .thenReturn(user);

        mockMvc.perform(
                        get("/dashboard")
                                .sessionAttr("AUTH_USER", "testuser")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("role", "PLAYER"));
    }
}