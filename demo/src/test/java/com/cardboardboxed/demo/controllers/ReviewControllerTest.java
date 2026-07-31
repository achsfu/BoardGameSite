package com.cardboardboxed.demo.controllers;

import com.cardboardboxed.demo.boardgames.BoardGameAutocompleteRepository;
import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.boardgames.BoardGameRankRepository;
import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewRepository reviewRepository;

    @MockitoBean
    private BoardGameRankRepository boardGameRankRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private BoardGameAutocompleteRepository boardGameAutocompleteRepository;

    // ---------- /reviews (posting) ----------

    @Test
    void postReview_noSession_redirectsToLogin() throws Exception {
        mockMvc.perform(
                        post("/reviews")
                                .param("gameTitle", "Catan")
                                .param("rating", "5")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=Please+log+in+to+post+a+review"));

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void postReview_sessionWithoutAuthUser_redirectsToLogin() throws Exception {
        mockMvc.perform(
                        post("/reviews")
                                .session(new MockHttpSession())
                                .param("gameTitle", "Catan")
                                .param("rating", "5")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=Please+log+in+to+post+a+review"));

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void postReview_userNotFound_throwsException() {
        assertThrows(Exception.class, () ->
                mockMvc.perform(
                        post("/reviews")
                                .sessionAttr("AUTH_USER", "ghost")
                                .param("gameTitle", "Catan")
                                .param("rating", "5")
                )
        );
    }

    @Test
    void postReview_gameNameDoesNotResolve_redirectsWithError() throws Exception {
        User michael = new User();
        michael.setUsername("michael");

        when(userRepository.findByUsername("michael")).thenReturn(michael);
        when(boardGameAutocompleteRepository.resolveToExistingName("Not A Game"))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        post("/reviews")
                                .sessionAttr("AUTH_USER", "michael")
                                .param("gameTitle", "Not A Game")
                                .param("rating", "5")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?error=Please+choose+a+valid+board+game"));

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void postReview_boardGameRecordMissing_redirectsWithError() throws Exception {
        User michael = new User();
        michael.setUsername("michael");

        when(userRepository.findByUsername("michael")).thenReturn(michael);
        when(boardGameAutocompleteRepository.resolveToExistingName("Catan"))
                .thenReturn(Optional.of("Catan"));
        when(boardGameRankRepository.findFirstByTitleIgnoreCaseOrderByRankPositionAsc("Catan"))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        post("/reviews")
                                .sessionAttr("AUTH_USER", "michael")
                                .param("gameTitle", "Catan")
                                .param("rating", "5")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?error=Please+choose+a+valid+board+game"));

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void postReview_validSubmissionWithNoRedirectTo_savesAndRedirectsToDashboard() throws Exception {
        User michael = new User();
        michael.setUsername("michael");

        BoardGameRank catan = new BoardGameRank();
        catan.setId(42);
        catan.setTitle("Catan");

        when(userRepository.findByUsername("michael")).thenReturn(michael);
        when(boardGameAutocompleteRepository.resolveToExistingName("Catan"))
                .thenReturn(Optional.of("Catan"));
        when(boardGameRankRepository.findFirstByTitleIgnoreCaseOrderByRankPositionAsc("Catan"))
                .thenReturn(Optional.of(catan));

        mockMvc.perform(
                        post("/reviews")
                                .sessionAttr("AUTH_USER", "michael")
                                .param("gameTitle", "Catan")
                                .param("rating", "5")
                                .param("reviewText", "Great game!")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    void postReview_validSubmissionWithGameRedirectTo_redirectsBackToGamePage() throws Exception {
        User michael = new User();
        michael.setUsername("michael");

        BoardGameRank catan = new BoardGameRank();
        catan.setId(42);
        catan.setTitle("Catan");

        when(userRepository.findByUsername("michael")).thenReturn(michael);
        when(boardGameAutocompleteRepository.resolveToExistingName("Catan"))
                .thenReturn(Optional.of("Catan"));
        when(boardGameRankRepository.findFirstByTitleIgnoreCaseOrderByRankPositionAsc("Catan"))
                .thenReturn(Optional.of(catan));

        mockMvc.perform(
                        post("/reviews")
                                .sessionAttr("AUTH_USER", "michael")
                                .param("gameTitle", "Catan")
                                .param("rating", "5")
                                .param("redirectTo", "/games/id/42")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/games/id/42"));

        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    void postReview_unsafeRedirectTo_fallsBackToDashboard() throws Exception {
        User michael = new User();
        michael.setUsername("michael");

        BoardGameRank catan = new BoardGameRank();
        catan.setId(42);
        catan.setTitle("Catan");

        when(userRepository.findByUsername("michael")).thenReturn(michael);
        when(boardGameAutocompleteRepository.resolveToExistingName("Catan"))
                .thenReturn(Optional.of("Catan"));
        when(boardGameRankRepository.findFirstByTitleIgnoreCaseOrderByRankPositionAsc("Catan"))
                .thenReturn(Optional.of(catan));

        mockMvc.perform(
                        post("/reviews")
                                .sessionAttr("AUTH_USER", "michael")
                                .param("gameTitle", "Catan")
                                .param("rating", "5")
                                .param("redirectTo", "http://evil.com")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    // ---------- /reviews/{id}/delete ----------

    @Test
    void deleteOwnReview_noSession_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/reviews/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=Please+log+in+to+manage+reviews"));

        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    void deleteOwnReview_reviewNotFound_redirectsToProfileWithError() throws Exception {
        when(reviewRepository.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(
                        post("/reviews/99/delete")
                                .sessionAttr("AUTH_USER", "michael")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?error=Review+not+found"));

        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    void deleteOwnReview_notOwner_redirectsToProfileWithError() throws Exception {
        User owner = new User();
        owner.setUsername("michael");

        Review review = new Review();
        review.setUser(owner);

        when(reviewRepository.findById(5)).thenReturn(Optional.of(review));

        mockMvc.perform(
                        post("/reviews/5/delete")
                                .sessionAttr("AUTH_USER", "intruder")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?error=You+can+only+delete+your+own+reviews"));

        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    void deleteOwnReview_owner_deletesAndRedirectsWithSuccess() throws Exception {
        User owner = new User();
        owner.setUsername("michael");

        Review review = new Review();
        review.setUser(owner);

        when(reviewRepository.findById(5)).thenReturn(Optional.of(review));

        mockMvc.perform(
                        post("/reviews/5/delete")
                                .sessionAttr("AUTH_USER", "michael")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?success=Review+deleted"));

        verify(reviewRepository, times(1)).delete(review);
    }
}
