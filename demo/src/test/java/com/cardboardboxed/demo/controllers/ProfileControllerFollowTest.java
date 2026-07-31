package com.cardboardboxed.demo.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cardboardboxed.demo.boardgames.BoardGameAutocompleteRepository;
import com.cardboardboxed.demo.boardgames.BoardGameRankRepository;
import com.cardboardboxed.demo.collections.CollectionItemRepository;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserFollow;
import com.cardboardboxed.demo.useracounts.UserFollowRepository;
import com.cardboardboxed.demo.useracounts.UserRepository;

@WebMvcTest(ProfileController.class)
class ProfileControllerFollowTest {

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

    private User edward;
    private User michael;

    @BeforeEach
    void setUp() {
        edward = new User("edward", "encoded-password", "PLAYER");
        edward.setId(1);

        michael = new User("michael", "encoded-password", "PLAYER");
        michael.setId(2);

        when(userRepository.findByUsername("edward")).thenReturn(edward);
        when(userRepository.findByUsername("michael")).thenReturn(michael);
        when(userRepository.findByUsername("nobody")).thenReturn(null);
    }

    // ---------- /profile/search ----------

    @Test
    void searchProfiles_notLoggedIn_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/profile/search"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=Please+log+in+to+search+profiles"));
    }

    @Test
    void searchProfiles_blankQuery_returnsAllUsers() throws Exception {
        Page<User> allUsers = new PageImpl<>(List.of(edward, michael));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(allUsers);

        mockMvc.perform(get("/profile/search").sessionAttr("AUTH_USER", "edward"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-search"))
                .andExpect(model().attribute("results", List.of(edward, michael)));

        verify(userRepository).findAll(any(Pageable.class));
        verify(userRepository, never()).findByUsernameContainingIgnoreCase(any(), any());
    }

    @Test
    void searchProfiles_withQuery_usesUsernameSearch() throws Exception {
        Page<User> matches = new PageImpl<>(List.of(michael));
        when(userRepository.findByUsernameContainingIgnoreCase(eq("mi"), any(Pageable.class)))
                .thenReturn(matches);

        mockMvc.perform(
                        get("/profile/search")
                                .param("q", "mi")
                                .sessionAttr("AUTH_USER", "edward")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("profile-search"))
                .andExpect(model().attribute("query", "mi"))
                .andExpect(model().attribute("results", List.of(michael)));

        verify(userRepository).findByUsernameContainingIgnoreCase(eq("mi"), any(Pageable.class));
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    // ---------- /profile/{username} (viewing another user) ----------

    @Test
    void viewProfile_notLoggedIn_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/profile/michael"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=Please+log+in+to+view+profiles"));
    }

    @Test
    void viewProfile_ownUsername_redirectsToOwnProfile() throws Exception {
        mockMvc.perform(get("/profile/edward").sessionAttr("AUTH_USER", "edward"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
    }

    @Test
    void viewProfile_userNotFound_redirectsToSearch() throws Exception {
        mockMvc.perform(get("/profile/nobody").sessionAttr("AUTH_USER", "edward"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/search?error=User+not+found"));
    }

    @Test
    void viewProfile_validTarget_populatesModelCorrectly() throws Exception {
        when(userFollowRepository.existsByFollowerAndFollowed(edward, michael)).thenReturn(true);
        when(userFollowRepository.countByFollowed(michael)).thenReturn(5L);
        when(userFollowRepository.countByFollower(michael)).thenReturn(2L);
        when(reviewRepository.findByUserUsername("michael")).thenReturn(List.of());

        mockMvc.perform(get("/profile/michael").sessionAttr("AUTH_USER", "edward"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-view"))
                .andExpect(model().attribute("username", "michael"))
                .andExpect(model().attribute("isFollowing", true))
                .andExpect(model().attribute("followerCount", 5L))
                .andExpect(model().attribute("followingCount", 2L));
    }

    // ---------- follow / unfollow ----------

    @Test
    void followUser_notLoggedIn_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/profile/michael/follow"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=Please+log+in+to+follow+users"));
    }

    @Test
    void followUser_self_isBlocked() throws Exception {
        mockMvc.perform(post("/profile/edward/follow").sessionAttr("AUTH_USER", "edward"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?error=Cannot+follow+your+own+account"));

        verify(userFollowRepository, never()).save(any(UserFollow.class));
    }

    @Test
    void followUser_targetNotFound_redirectsToSearch() throws Exception {
        mockMvc.perform(post("/profile/nobody/follow").sessionAttr("AUTH_USER", "edward"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/search?error=User+not+found"));
    }

    @Test
    void followUser_newFollow_savesRelationship() throws Exception {
        when(userFollowRepository.existsByFollowerAndFollowed(edward, michael)).thenReturn(false);

        mockMvc.perform(post("/profile/michael/follow").sessionAttr("AUTH_USER", "edward"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/michael"));

        verify(userFollowRepository, times(1)).save(any(UserFollow.class));
    }

    @Test
    void followUser_alreadyFollowing_doesNotDuplicateSave() throws Exception {
        when(userFollowRepository.existsByFollowerAndFollowed(edward, michael)).thenReturn(true);

        mockMvc.perform(post("/profile/michael/follow").sessionAttr("AUTH_USER", "edward"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/michael"));

        verify(userFollowRepository, never()).save(any(UserFollow.class));
    }

    @Test
    void unfollowUser_notLoggedIn_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/profile/michael/unfollow"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=Please+log+in+to+manage+your+followed+accounts"));
    }

    @Test
    void unfollowUser_success_deletesRelationship() throws Exception {
        mockMvc.perform(post("/profile/michael/unfollow").sessionAttr("AUTH_USER", "edward"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/michael"));

        verify(userFollowRepository, times(1)).deleteByFollowerAndFollowed(edward, michael);
    }

    // ---------- followers / following lists ----------

    @Test
    void showFollowers_notLoggedIn_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/profile/followers"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=Please+log+in+to+view+followers"));
    }

    @Test
    void showFollowers_returnsFollowerList() throws Exception {
        UserFollow relation = new UserFollow(michael, edward);
        when(userFollowRepository.findByFollowed(edward)).thenReturn(List.of(relation));

        mockMvc.perform(get("/profile/followers").sessionAttr("AUTH_USER", "edward"))
                .andExpect(status().isOk())
                .andExpect(view().name("connections"))
                .andExpect(model().attribute("listTitle", "Followers"))
                .andExpect(model().attribute("connections", List.of(michael)));
    }

    @Test
    void showFollowing_notLoggedIn_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/profile/following"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=Please+log+in+to+view+following"));
    }

    @Test
    void showFollowing_returnsFollowingList() throws Exception {
        UserFollow relation = new UserFollow(edward, michael);
        when(userFollowRepository.findByFollower(edward)).thenReturn(List.of(relation));

        mockMvc.perform(get("/profile/following").sessionAttr("AUTH_USER", "edward"))
                .andExpect(status().isOk())
                .andExpect(view().name("connections"))
                .andExpect(model().attribute("listTitle", "Following"))
                .andExpect(model().attribute("connections", List.of(michael)));
    }
}