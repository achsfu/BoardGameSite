package com.cardboardboxed.demo.controllers;

import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserFollowRepository;
import com.cardboardboxed.demo.useracounts.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserFollowRepository userFollowRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @Mock
    private Model model;

    private FeedController feedController;

    @BeforeEach
    void setUp() {
        feedController = new FeedController(
                userRepository,
                userFollowRepository,
                reviewRepository
        );
    }

    @Test
    void loggedOutUserIsRedirectedToLogin() {
        when(request.getSession(false)).thenReturn(null);

        String result = feedController.showFeed(model, request);

        assertEquals(
                "redirect:/login?error=Please+log+in+to+view+your+feed",
                result
        );

        verifyNoInteractions(
                userRepository,
                userFollowRepository,
                reviewRepository
        );
    }

    @Test
    void loggedInUserCanViewReviewsFromFeed() {
        User currentUser = mock(User.class);

        Review firstReview = mock(Review.class);
        Review secondReview = mock(Review.class);

        List<Review> reviews = List.of(
                firstReview,
                secondReview
        );

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("AUTH_USER")).thenReturn("sommy");
        when(userRepository.findByUsername("sommy"))
                .thenReturn(currentUser);

        when(userFollowRepository.countByFollower(currentUser))
                .thenReturn(2L);

        when(reviewRepository.findFeedReviewsForUser(currentUser))
                .thenReturn(reviews);

        String result = feedController.showFeed(model, request);

        assertEquals("feed", result);

        verify(model).addAttribute("username", "sommy");
        verify(model).addAttribute("followingCount", 2L);
        verify(model).addAttribute("feedReviews", reviews);

        verify(reviewRepository)
                .findFeedReviewsForUser(currentUser);
    }

    @Test
    void feedWorksWhenUserFollowsNobody() {
        User currentUser = mock(User.class);
        List<Review> emptyReviews = List.of();

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("AUTH_USER")).thenReturn("sommy");
        when(userRepository.findByUsername("sommy"))
                .thenReturn(currentUser);

        when(userFollowRepository.countByFollower(currentUser))
                .thenReturn(0L);

        when(reviewRepository.findFeedReviewsForUser(currentUser))
                .thenReturn(emptyReviews);

        String result = feedController.showFeed(model, request);

        assertEquals("feed", result);

        verify(model).addAttribute("username", "sommy");
        verify(model).addAttribute("followingCount", 0L);
        verify(model).addAttribute("feedReviews", emptyReviews);
    }

    @Test
    void invalidSessionUserIsRedirectedToLogin() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("AUTH_USER")).thenReturn("missingUser");
        when(userRepository.findByUsername("missingUser"))
                .thenReturn(null);

        String result = feedController.showFeed(model, request);

        assertEquals(
                "redirect:/login?error=User+account+not+found",
                result
        );

        verify(session).invalidate();
        verifyNoInteractions(
                userFollowRepository,
                reviewRepository
        );
    }
}