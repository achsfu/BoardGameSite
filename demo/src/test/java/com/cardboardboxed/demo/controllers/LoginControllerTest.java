package com.cardboardboxed.demo.controllers;

import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.boardgames.BoardGameRankRepository;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserRepository;
import com.cardboardboxed.demo.useracounts.SecurityConfig;
import com.cardboardboxed.demo.useracounts.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(LoginController.class)
@Import(SecurityConfig.class)
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
                        "/?error=Please+log+in+to+access+the+dashboard"
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

    @Test
    void loginRedirectsToDashboardOnValidCredentials() throws Exception {
        User user = new User();
        user.setUsername("sammy");
        user.setPassword("encoded");

        when(userRepository.findByUsername("sammy"))
                .thenReturn(user);

        when(passwordEncoder.matches("plain", "encoded"))
                .thenReturn(true);

        mockMvc.perform(
                        post("/login")
                                .param("username", "sammy")
                                .param("password", "plain")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(request().sessionAttribute("AUTH_USER", "sammy"));
    }

    @Test
    void loginRedirectsToLandingWithErrorOnInvalidCredentials() throws Exception {
        User user = new User();
        user.setUsername("sammy");
        user.setPassword("encoded");

        when(userRepository.findByUsername("sammy"))
                .thenReturn(user);

        when(passwordEncoder.matches("wrong", "encoded"))
                .thenReturn(false);

        mockMvc.perform(
                        post("/login")
                                .param("username", "sammy")
                                .param("password", "wrong")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?error=Invalid+username+or+password"));
    }

    @Test
    void loginSkipsAuthenticationWhenAlreadyLoggedIn() throws Exception {
        mockMvc.perform(
                        post("/login")
                                .sessionAttr("AUTH_USER", "sammy")
                                .param("username", "ignored")
                                .param("password", "ignored")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(userRepository, never()).findByUsername("ignored");
    }

    @Test
    void logoutRedirectsToLandingAndClearsSession() throws Exception {
        mockMvc.perform(
                        post("/logout")
                                .sessionAttr("AUTH_USER", "sammy")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(request().sessionAttributeDoesNotExist("AUTH_USER"));
    }
}