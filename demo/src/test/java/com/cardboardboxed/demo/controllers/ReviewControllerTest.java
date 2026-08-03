package com.cardboardboxed.demo.controllers;

import com.cardboardboxed.demo.boardgames.BoardGameAutocompleteRepository;
import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.boardgames.BoardGameRankRepository;
import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.reviews.ReviewLike;
import com.cardboardboxed.demo.reviews.ReviewLikeRepository;
import com.cardboardboxed.demo.reviews.ReviewReply;
import com.cardboardboxed.demo.reviews.ReviewReplyRepository;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserRepository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private ReviewLikeRepository reviewLikeRepository;

    @MockitoBean
    private ReviewReplyRepository reviewReplyRepository;

    @MockitoBean
    private BoardGameRankRepository boardGameRankRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private BoardGameAutocompleteRepository boardGameAutocompleteRepository;

    // =========================================================
    // Helpers
    // =========================================================

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        return user;
    }

    private BoardGameRank createGame(
            Integer id,
            String title
    ) {
        BoardGameRank game = new BoardGameRank();
        game.setId(id);
        game.setTitle(title);
        return game;
    }

    private Review createReview(
            User user,
            BoardGameRank game
    ) {
        Review review = new Review();
        review.setUser(user);
        review.setGame(game);
        return review;
    }

    // =========================================================
    // POST /reviews
    // =========================================================

    @Test
    void postReview_noSession_redirectsToLogin() throws Exception {
        mockMvc.perform(
                        post("/reviews")
                                .param("gameTitle", "Catan")
                                .param("rating", "5")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/login?error=Please+log+in+to+post+a+review"
                        )
                );

        verify(
                reviewRepository,
                never()
        ).save(any(Review.class));
    }

    @Test
    void postReview_sessionWithoutAuthUser_redirectsToLogin()
            throws Exception {

        mockMvc.perform(
                        post("/reviews")
                                .session(new MockHttpSession())
                                .param("gameTitle", "Catan")
                                .param("rating", "5")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/login?error=Please+log+in+to+post+a+review"
                        )
                );

        verify(
                reviewRepository,
                never()
        ).save(any(Review.class));
    }

    @Test
    void postReview_userNotFound_redirectsToLogin()
            throws Exception {

        when(
                userRepository.findByUsername("ghost")
        ).thenReturn(null);

        mockMvc.perform(
                        post("/reviews")
                                .sessionAttr(
                                        "AUTH_USER",
                                        "ghost"
                                )
                                .param(
                                        "gameTitle",
                                        "Catan"
                                )
                                .param(
                                        "rating",
                                        "5"
                                )
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/login?error=Please+log+in+to+post+a+review"
                        )
                );

        verify(
                reviewRepository,
                never()
        ).save(any(Review.class));
    }

    @Test
    void postReview_gameNameDoesNotResolve_redirectsWithError()
            throws Exception {

        User michael = createUser("michael");

        when(
                userRepository.findByUsername("michael")
        ).thenReturn(michael);

        when(
                boardGameAutocompleteRepository
                        .resolveToExistingName(
                                "Not A Game"
                        )
        ).thenReturn(Optional.empty());

        mockMvc.perform(
                        post("/reviews")
                                .sessionAttr(
                                        "AUTH_USER",
                                        "michael"
                                )
                                .param(
                                        "gameTitle",
                                        "Not A Game"
                                )
                                .param(
                                        "rating",
                                        "5"
                                )
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/dashboard?error=Please+choose+a+valid+board+game"
                        )
                );

        verify(
                reviewRepository,
                never()
        ).save(any(Review.class));
    }

    @Test
    void postReview_boardGameRecordMissing_redirectsWithError()
            throws Exception {

        User michael = createUser("michael");

        when(
                userRepository.findByUsername("michael")
        ).thenReturn(michael);

        when(
                boardGameAutocompleteRepository
                        .resolveToExistingName("Catan")
        ).thenReturn(Optional.of("Catan"));

        when(
                boardGameRankRepository
                        .findFirstByTitleIgnoreCaseOrderByRankPositionAsc(
                                "Catan"
                        )
        ).thenReturn(Optional.empty());

        mockMvc.perform(
                        post("/reviews")
                                .sessionAttr(
                                        "AUTH_USER",
                                        "michael"
                                )
                                .param(
                                        "gameTitle",
                                        "Catan"
                                )
                                .param(
                                        "rating",
                                        "5"
                                )
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/dashboard?error=Please+choose+a+valid+board+game"
                        )
                );

        verify(
                reviewRepository,
                never()
        ).save(any(Review.class));
    }

    @Test
    void postReview_validSubmissionWithNoRedirectTo_savesAndRedirectsToDashboard()
            throws Exception {

        User michael = createUser("michael");
        BoardGameRank catan =
                createGame(42, "Catan");

        when(
                userRepository.findByUsername("michael")
        ).thenReturn(michael);

        when(
                boardGameAutocompleteRepository
                        .resolveToExistingName("Catan")
        ).thenReturn(Optional.of("Catan"));

        when(
                boardGameRankRepository
                        .findFirstByTitleIgnoreCaseOrderByRankPositionAsc(
                                "Catan"
                        )
        ).thenReturn(Optional.of(catan));

        mockMvc.perform(
                        post("/reviews")
                                .sessionAttr(
                                        "AUTH_USER",
                                        "michael"
                                )
                                .param(
                                        "gameTitle",
                                        "Catan"
                                )
                                .param(
                                        "rating",
                                        "5"
                                )
                                .param(
                                        "reviewText",
                                        "Great game!"
                                )
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl("/dashboard")
                );

        ArgumentCaptor<Review> reviewCaptor =
                ArgumentCaptor.forClass(
                        Review.class
                );

        verify(
                reviewRepository,
                times(1)
        ).save(reviewCaptor.capture());

        Review savedReview =
                reviewCaptor.getValue();

        assertSame(
                michael,
                savedReview.getUser()
        );

        assertSame(
                catan,
                savedReview.getGame()
        );

        assertEquals(
                "Catan",
                savedReview.getGameTitle()
        );

        assertEquals(
                5,
                savedReview.getRating()
        );

        assertEquals(
                "Great game!",
                savedReview.getReviewText()
        );
    }

    @Test
    void postReview_validSubmissionWithGameRedirectTo_redirectsBackToGamePage()
            throws Exception {

        User michael = createUser("michael");
        BoardGameRank catan =
                createGame(42, "Catan");

        when(
                userRepository.findByUsername("michael")
        ).thenReturn(michael);

        when(
                boardGameAutocompleteRepository
                        .resolveToExistingName("Catan")
        ).thenReturn(Optional.of("Catan"));

        when(
                boardGameRankRepository
                        .findFirstByTitleIgnoreCaseOrderByRankPositionAsc(
                                "Catan"
                        )
        ).thenReturn(Optional.of(catan));

        mockMvc.perform(
                        post("/reviews")
                                .sessionAttr(
                                        "AUTH_USER",
                                        "michael"
                                )
                                .param(
                                        "gameTitle",
                                        "Catan"
                                )
                                .param(
                                        "rating",
                                        "5"
                                )
                                .param(
                                        "redirectTo",
                                        "/games/id/42"
                                )
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/games/id/42"
                        )
                );

        verify(
                reviewRepository,
                times(1)
        ).save(any(Review.class));
    }

    @Test
    void postReview_unsafeRedirectTo_fallsBackToDashboard()
            throws Exception {

        User michael = createUser("michael");
        BoardGameRank catan =
                createGame(42, "Catan");

        when(
                userRepository.findByUsername("michael")
        ).thenReturn(michael);

        when(
                boardGameAutocompleteRepository
                        .resolveToExistingName("Catan")
        ).thenReturn(Optional.of("Catan"));

        when(
                boardGameRankRepository
                        .findFirstByTitleIgnoreCaseOrderByRankPositionAsc(
                                "Catan"
                        )
        ).thenReturn(Optional.of(catan));

        mockMvc.perform(
                        post("/reviews")
                                .sessionAttr(
                                        "AUTH_USER",
                                        "michael"
                                )
                                .param(
                                        "gameTitle",
                                        "Catan"
                                )
                                .param(
                                        "rating",
                                        "5"
                                )
                                .param(
                                        "redirectTo",
                                        "http://evil.com"
                                )
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl("/dashboard")
                );
    }

    // =========================================================
    // POST /reviews/{id}/like
    // =========================================================

    @Test
    void toggleReviewLike_noSession_redirectsToLogin()
            throws Exception {

        mockMvc.perform(
                        post("/reviews/5/like")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/login?error=Please+log+in+to+like+reviews"
                        )
                );

        verify(
                reviewLikeRepository,
                never()
        ).save(any(ReviewLike.class));

        verify(
                reviewLikeRepository,
                never()
        ).delete(any(ReviewLike.class));
    }

    @Test
    void toggleReviewLike_reviewNotFound_redirectsWithError()
            throws Exception {

        User michael = createUser("michael");

        when(
                userRepository.findByUsername("michael")
        ).thenReturn(michael);

        when(
                reviewRepository.findById(99)
        ).thenReturn(Optional.empty());

        mockMvc.perform(
                        post("/reviews/99/like")
                                .sessionAttr(
                                        "AUTH_USER",
                                        "michael"
                                )
                                .param(
                                        "redirectTo",
                                        "/games/id/42"
                                )
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/games/id/42?error=Review+not+found"
                        )
                );

        verify(
                reviewLikeRepository,
                never()
        ).save(any(ReviewLike.class));
    }

    @Test
    void toggleReviewLike_notPreviouslyLiked_addsLike()
            throws Exception {

        User michael = createUser("michael");
        BoardGameRank catan =
                createGame(42, "Catan");

        Review review =
                createReview(michael, catan);

        when(
                userRepository.findByUsername("michael")
        ).thenReturn(michael);

        when(
                reviewRepository.findById(5)
        ).thenReturn(Optional.of(review));

        when(
                reviewLikeRepository
                        .findByReviewAndUser(
                                review,
                                michael
                        )
        ).thenReturn(Optional.empty());

        mockMvc.perform(
                        post("/reviews/5/like")
                                .sessionAttr(
                                        "AUTH_USER",
                                        "michael"
                                )
                                .param(
                                        "redirectTo",
                                        "/games/id/42"
                                )
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/games/id/42"
                        )
                );

        ArgumentCaptor<ReviewLike> likeCaptor =
                ArgumentCaptor.forClass(
                        ReviewLike.class
                );

        verify(
                reviewLikeRepository,
                times(1)
        ).save(likeCaptor.capture());

        ReviewLike savedLike =
                likeCaptor.getValue();

        assertSame(
                review,
                savedLike.getReview()
        );

        assertSame(
                michael,
                savedLike.getUser()
        );

        verify(
                reviewLikeRepository,
                never()
        ).delete(any(ReviewLike.class));
    }

    @Test
    void toggleReviewLike_profileRedirect_redirectsBackToProfilePage()
            throws Exception {

        User michael = createUser("michael");
        BoardGameRank catan =
                createGame(42, "Catan");

        Review review = createReview(michael, catan);

        when(userRepository.findByUsername("michael"))
                .thenReturn(michael);

        when(reviewRepository.findById(5))
                .thenReturn(Optional.of(review));

        when(reviewLikeRepository.findByReviewAndUser(
                review,
                michael
        )).thenReturn(Optional.empty());

        mockMvc.perform(
                        post("/reviews/5/like")
                                .sessionAttr("AUTH_USER", "michael")
                                .param("redirectTo", "/profile/michael")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/michael"));
    }

    @Test
    void toggleReviewLike_alreadyLiked_removesExistingLike()
            throws Exception {

        User michael = createUser("michael");
        BoardGameRank catan =
                createGame(42, "Catan");

        Review review =
                createReview(michael, catan);

        ReviewLike existingLike =
                new ReviewLike(
                        review,
                        michael
                );

        when(
                userRepository.findByUsername("michael")
        ).thenReturn(michael);

        when(
                reviewRepository.findById(5)
        ).thenReturn(Optional.of(review));

        when(
                reviewLikeRepository
                        .findByReviewAndUser(
                                review,
                                michael
                        )
        ).thenReturn(
                Optional.of(existingLike)
        );

        mockMvc.perform(
                        post("/reviews/5/like")
                                .sessionAttr(
                                        "AUTH_USER",
                                        "michael"
                                )
                                .param(
                                        "redirectTo",
                                        "/games/id/42"
                                )
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/games/id/42"
                        )
                );

        verify(
                reviewLikeRepository,
                times(1)
        ).delete(existingLike);

        verify(
                reviewLikeRepository,
                never()
        ).save(any(ReviewLike.class));
    }

    // =========================================================
    // POST /reviews/{id}/reply
    // =========================================================

    @Test
    void postReply_noSession_redirectsToLogin()
            throws Exception {

        mockMvc.perform(
                        post("/reviews/5/reply")
                                .param(
                                        "replyText",
                                        "I agree!"
                                )
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/login?error=Please+log+in+to+reply+to+reviews"
                        )
                );

        verify(
                reviewReplyRepository,
                never()
        ).save(any(ReviewReply.class));
    }

    @Test
    void postReply_reviewNotFound_redirectsWithError()
            throws Exception {

        User michael = createUser("michael");

        when(
                userRepository.findByUsername("michael")
        ).thenReturn(michael);

        when(
                reviewRepository.findById(99)
        ).thenReturn(Optional.empty());

        mockMvc.perform(
                        post("/reviews/99/reply")
                                .sessionAttr(
                                        "AUTH_USER",
                                        "michael"
                                )
                                .param(
                                        "replyText",
                                        "I agree!"
                                )
                                .param(
                                        "redirectTo",
                                        "/games/id/42"
                                )
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/games/id/42?error=Review+not+found"
                        )
                );

        verify(
                reviewReplyRepository,
                never()
        ).save(any(ReviewReply.class));
    }

    @Test
    void postReply_blankReply_redirectsWithError()
            throws Exception {

        User michael = createUser("michael");
        BoardGameRank catan =
                createGame(42, "Catan");

        Review review =
                createReview(michael, catan);

        when(
                userRepository.findByUsername("michael")
        ).thenReturn(michael);

        when(
                reviewRepository.findById(5)
        ).thenReturn(Optional.of(review));

        mockMvc.perform(
                        post("/reviews/5/reply")
                                .sessionAttr(
                                        "AUTH_USER",
                                        "michael"
                                )
                                .param(
                                        "replyText",
                                        "   "
                                )
                                .param(
                                        "redirectTo",
                                        "/games/id/42"
                                )
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/games/id/42?error=Reply+cannot+be+empty"
                        )
                );

        verify(
                reviewReplyRepository,
                never()
        ).save(any(ReviewReply.class));
    }

    @Test
    void postReply_replyLongerThan1000Characters_redirectsWithError()
            throws Exception {

        User michael = createUser("michael");
        BoardGameRank catan =
                createGame(42, "Catan");

        Review review =
                createReview(michael, catan);

        String tooLongReply =
                "a".repeat(1001);

        when(
                userRepository.findByUsername("michael")
        ).thenReturn(michael);

        when(
                reviewRepository.findById(5)
        ).thenReturn(Optional.of(review));

        mockMvc.perform(
                        post("/reviews/5/reply")
                                .sessionAttr(
                                        "AUTH_USER",
                                        "michael"
                                )
                                .param(
                                        "replyText",
                                        tooLongReply
                                )
                                .param(
                                        "redirectTo",
                                        "/games/id/42"
                                )
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/games/id/42?error=Reply+cannot+exceed+1000+characters"
                        )
                );

        verify(
                reviewReplyRepository,
                never()
        ).save(any(ReviewReply.class));
    }

    @Test
    void postReply_profileRedirect_savesAndOpensReplyPanel()
            throws Exception {

        User michael = createUser("michael");
        BoardGameRank catan = createGame(42, "Catan");

        Review review = createReview(michael, catan);

        when(userRepository.findByUsername("michael"))
                .thenReturn(michael);

        when(reviewRepository.findById(5))
                .thenReturn(Optional.of(review));

        mockMvc.perform(
                        post("/reviews/5/reply")
                                .sessionAttr("AUTH_USER", "michael")
                                .param("replyText", "  I completely agree!  ")
                                .param("redirectTo", "/profile/michael")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/profile/michael?success=Reply+posted&openReply=5"
                        )
                );
    }

    @Test
    void postReply_validReply_savesAndRedirectsWithSuccess()
            throws Exception {

        User michael = createUser("michael");
        BoardGameRank catan =
                createGame(42, "Catan");

        Review review =
                createReview(michael, catan);

        when(
                userRepository.findByUsername("michael")
        ).thenReturn(michael);

        when(
                reviewRepository.findById(5)
        ).thenReturn(Optional.of(review));

        mockMvc.perform(
                        post("/reviews/5/reply")
                                .sessionAttr(
                                        "AUTH_USER",
                                        "michael"
                                )
                                .param(
                                        "replyText",
                                        "  I completely agree!  "
                                )
                                .param(
                                        "redirectTo",
                                        "/games/id/42"
                                )
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/games/id/42?success=Reply+posted&openReply=5"
                        )
                );

        ArgumentCaptor<ReviewReply> replyCaptor =
                ArgumentCaptor.forClass(
                        ReviewReply.class
                );

        verify(
                reviewReplyRepository,
                times(1)
        ).save(replyCaptor.capture());

        ReviewReply savedReply =
                replyCaptor.getValue();

        assertSame(
                review,
                savedReply.getReview()
        );

        assertSame(
                michael,
                savedReply.getUser()
        );

        assertEquals(
                "I completely agree!",
                savedReply.getReplyText()
        );
    }

    // =========================================================
    // POST /reviews/{id}/delete
    // =========================================================

    @Test
    void deleteOwnReview_noSession_redirectsToLogin()
            throws Exception {

        mockMvc.perform(
                        post("/reviews/1/delete")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/login?error=Please+log+in+to+manage+reviews"
                        )
                );

        verify(
                reviewRepository,
                never()
        ).delete(any(Review.class));
    }

    @Test
    void deleteOwnReview_reviewNotFound_redirectsToProfileWithError()
            throws Exception {

        User michael = createUser("michael");

        when(
                userRepository.findByUsername("michael")
        ).thenReturn(michael);

        when(
                reviewRepository.findById(99)
        ).thenReturn(Optional.empty());

        mockMvc.perform(
                        post("/reviews/99/delete")
                                .sessionAttr(
                                        "AUTH_USER",
                                        "michael"
                                )
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/profile?error=Review+not+found"
                        )
                );

        verify(
                reviewRepository,
                never()
        ).delete(any(Review.class));
    }

    @Test
    void deleteOwnReview_notOwner_redirectsToProfileWithError()
            throws Exception {

        User owner = createUser("michael");
        User intruder = createUser("intruder");

        Review review = new Review();
        review.setUser(owner);

        when(
                userRepository.findByUsername("intruder")
        ).thenReturn(intruder);

        when(
                reviewRepository.findById(5)
        ).thenReturn(Optional.of(review));

        mockMvc.perform(
                        post("/reviews/5/delete")
                                .sessionAttr(
                                        "AUTH_USER",
                                        "intruder"
                                )
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/profile?error=You+can+only+delete+your+own+reviews"
                        )
                );

        verify(
                reviewRepository,
                never()
        ).delete(any(Review.class));
    }

    @Test
    void deleteOwnReview_owner_deletesAndRedirectsWithSuccess()
            throws Exception {

        User owner = createUser("michael");

        Review review = new Review();
        review.setUser(owner);

        when(
                userRepository.findByUsername("michael")
        ).thenReturn(owner);

        when(
                reviewRepository.findById(5)
        ).thenReturn(Optional.of(review));

        mockMvc.perform(
                        post("/reviews/5/delete")
                                .sessionAttr(
                                        "AUTH_USER",
                                        "michael"
                                )
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/profile?success=Review+deleted"
                        )
                );

        verify(
                reviewRepository,
                times(1)
        ).delete(review);
    }
}