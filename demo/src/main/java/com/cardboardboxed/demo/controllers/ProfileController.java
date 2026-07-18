package com.cardboardboxed.demo.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.cardboardboxed.demo.boardgames.BoardGameAutocompleteRepository;
import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.UserRepository;
import com.cardboardboxed.demo.useracounts.User;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestParam;

//NEW IMPORTS FOR USER LOOKUP AND FOLLOW FUNCTION
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.cardboardboxed.demo.useracounts.UserFollow;
import com.cardboardboxed.demo.useracounts.UserFollowRepository;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final BoardGameAutocompleteRepository boardGameAutocompleteRepository;
    //NEW FIELD FOR USER LOOKUP AND FOLLOW FUNCTION
    private final UserFollowRepository userFollowRepository;

    //add parameter for userfollowrepository in the constructor
    public ProfileController(UserRepository userRepository, ReviewRepository reviewRepository,
            BoardGameAutocompleteRepository boardGameAutocompleteRepository, UserFollowRepository userFollowRepository) {
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.boardGameAutocompleteRepository = boardGameAutocompleteRepository;
        this.userFollowRepository = userFollowRepository;
    }

    @GetMapping("/profile")
    public String showProfile(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("AUTH_USER") == null) {
            return "redirect:/login?error=Please+log+in+to+view+your+profile";
        }

        String username = (String) session.getAttribute("AUTH_USER");
        User user = userRepository.findByUsername(username);

        String bio = (user.getBio() != null && !user.getBio().isBlank()) ? user.getBio() : "No bio added yet.";
        String avatar = (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isBlank())
                ? user.getProfilePictureUrl()
                : "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&h=150&q=80";
        List<Review> userReviews = reviewRepository.findByUserUsername(username);
        List<String> ownedList = (user.getGameOwned() != null && !user.getGameOwned().isBlank())
                ? new ArrayList<>(List.of(user.getGameOwned().split("\\s*,\\s*")))
                : new ArrayList<>();
        
        List<String> wishlistList = (user.getGameWishlist() != null && !user.getGameWishlist().isBlank())
                ? new ArrayList<>(List.of(user.getGameWishlist().split("\\s*,\\s*")))
                : new ArrayList<>();
        model.addAttribute("username", username);
        model.addAttribute("role", user.getRole());
        model.addAttribute("bio", bio);
        model.addAttribute("avatar", avatar);
        model.addAttribute("reviews", userReviews);

        model.addAttribute("ownedList", ownedList);
        model.addAttribute("wishlistList", wishlistList);

        //for followers and following:
        model.addAttribute("followerCount", userFollowRepository.countByFollowed(user));
        model.addAttribute("followingCount", userFollowRepository.countByFollower(user));

        return "profile";
    }

    @PostMapping("/profile/update-bio")
    public String updateBio(@RequestParam String bio, @RequestParam String profilePictureUrl, @RequestParam(required = false) String ownedGames, @RequestParam(required = false) String wishlistGames,
            HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("AUTH_USER") == null) {
            return "redirect:/login";
        }

        String username = (String) session.getAttribute("AUTH_USER");
        User user = userRepository.findByUsername(username);
        user.setBio(bio);
        user.setProfilePictureUrl(profilePictureUrl);

        if (ownedGames != null && !ownedGames.isBlank()) {
            String resolvedOwned = boardGameAutocompleteRepository
                    .resolveToExistingName(ownedGames)
                    .orElse(null);
            if (resolvedOwned != null && !resolvedOwned.isBlank()) {
                user.setGameOwned(appendUniqueGame(user.getGameOwned(), resolvedOwned));
            }
        }

        if (wishlistGames != null && !wishlistGames.isBlank()) {
            String resolvedWishlist = boardGameAutocompleteRepository
                    .resolveToExistingName(wishlistGames)
                    .orElse(null);
            if (resolvedWishlist != null && !resolvedWishlist.isBlank()) {
                user.setGameWishlist(appendUniqueGame(user.getGameWishlist(), resolvedWishlist));
            }
        }

        userRepository.save(user);
        return "redirect:/profile?success=Bio+updated+successfully";
    }

    //PROFILE SEARCH METHOD
        @GetMapping("/profile/search")
    public String searchProfiles(
            @RequestParam(name ="q", defaultValue ="") String query,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model,
            HttpServletRequest request
    ) {
        HttpSession session = request.getSession(false);
        if(session == null || session.getAttribute("AUTH_USER") == null) {
            return "redirect:/login?error=Please+log+in+to+search+profiles";
        }
 
        String trimmedQuery = query == null ? "" : query.trim();
        int currentPage = Math.max(page, 1) - 1;
        int pageSize = 12;
        PageRequest pageRequest = PageRequest.of(currentPage, pageSize);
 
        Page<User> resultsPage = trimmedQuery.isBlank()
        ? userRepository.findAll(pageRequest)
        : userRepository.findByUsernameContainingIgnoreCase(trimmedQuery, pageRequest);
 
        model.addAttribute("query", trimmedQuery);
        model.addAttribute("results", resultsPage.getContent());
        model.addAttribute("currentPage",resultsPage.getNumber()+ 1);
        model.addAttribute("totalPages",resultsPage.getTotalPages());
        return "profile-search";
    }

    //VIEWING ANOTHER USERS PROFILE
    @GetMapping("/profile/{username}")
    public String viewProfile(@PathVariable String username, Model model, HttpServletRequest request){
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("AUTH_USER") == null){
            return "redirect:/login?error=Please+log+in+to+view+profiles";
        }
        String currentUsername = (String) session.getAttribute("AUTH_USER");
        //keep user own profile separate from the viewing mechanism, as you can already view your own profile
        if (currentUsername.equalsIgnoreCase(username)){
            return "redirect:/profile";
        }
        User viewedUser = userRepository.findByUsername(username);
        if (viewedUser == null){
            return"redirect:/profile/search?error=User+not+found";
        }
        User currentUser = userRepository.findByUsername(currentUsername);
        String bio = (viewedUser.getBio() != null && !viewedUser.getBio().isBlank())
            ?viewedUser.getBio() : "No bio added yet.";
        String avatar = (viewedUser.getProfilePictureUrl() != null && !viewedUser.getProfilePictureUrl().isBlank())
            ? viewedUser.getProfilePictureUrl()
            :"https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&h=150&q=80";
 
        List<Review> userReviews = reviewRepository.findByUserUsername(viewedUser.getUsername());
        List<String> ownedList = (viewedUser.getGameOwned() != null && !viewedUser.getGameOwned().isBlank())
            ?new ArrayList<>(List.of(viewedUser.getGameOwned().split("\\s*,\\s*")))
            : new ArrayList<>();
        List<String> wishlistList = (viewedUser.getGameWishlist() != null && !viewedUser.getGameWishlist().isBlank())
            ? new ArrayList<>(List.of(viewedUser.getGameWishlist().split("\\s*,\\s*")))
            : new ArrayList<>();
 
        boolean isFollowing = userFollowRepository.existsByFollowerAndFollowed(currentUser, viewedUser);
        model.addAttribute("username", viewedUser.getUsername());
        model.addAttribute("role", viewedUser.getRole());
        model.addAttribute("bio", bio);
        model.addAttribute("avatar",avatar);
        model.addAttribute("reviews",userReviews);
        model.addAttribute("ownedList", ownedList);
        model.addAttribute("wishlistList",wishlistList);
        model.addAttribute("followerCount", userFollowRepository.countByFollowed(viewedUser));
        model.addAttribute("followingCount", userFollowRepository.countByFollower(viewedUser));
        model.addAttribute("isFollowing", isFollowing);
        return "profile-view";
    }
    //CURRENT SECTION WORKING ON---------------------
    //FOLLOW USER
    @PostMapping("/profile/{username}/follow")
    public String followUser(@PathVariable String username, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("AUTH_USER") == null) {
            return "redirect:/login?error=Please+log+in+to+follow+users";
        }
        String currentUsername = (String) session.getAttribute("AUTH_USER");
        if (currentUsername.equalsIgnoreCase(username)) {
            return "redirect:/profile?error=Cannot+follow+your+own+account";
        }
        User currentUser = userRepository.findByUsername(currentUsername);
        User targetUser = userRepository.findByUsername(username);
        if (targetUser == null){
            return "redirect:/profile/search?error=User+not+found";
        }
        if (!userFollowRepository.existsByFollowerAndFollowed(currentUser, targetUser)){
            userFollowRepository.save(new UserFollow(currentUser, targetUser));
        }
        return "redirect:/profile/" + targetUser.getUsername();
    }
    //----------------------------------------------  

    //UNFOLLOW USER!
    @PostMapping("/profile/{username}/unfollow")
    public String unfollowUser(@PathVariable String username, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("AUTH_USER") == null) {
            return "redirect:/login?error=Please+log+in+to+manage+your+followed+accounts";
        }
        String currentUsername = (String) session.getAttribute("AUTH_USER");
        User currentUser = userRepository.findByUsername(currentUsername);
        User targetUser = userRepository.findByUsername(username);
        if (targetUser == null) {
            return "redirect:/profile/search?error=User+not+found";
        }
        userFollowRepository.deleteByFollowerAndFollowed(currentUser, targetUser);
        return "redirect:/profile/" + targetUser.getUsername();
    }

    private String appendUniqueGame(String currentValue, String gameName) {
        if (currentValue == null || currentValue.isBlank()) {
            return gameName;
        }

        String normalizedNew = gameName.trim().toLowerCase(Locale.ROOT);
        String[] games = currentValue.split("\\s*,\\s*");
        for (String existing : games) {
            if (existing.trim().toLowerCase(Locale.ROOT).equals(normalizedNew)) {
                return currentValue;
            }
        }

        return currentValue + ", " + gameName;
    }


    //VIEW YOUR FOLLOWERS AND FOLLOWING
    @GetMapping("/profile/followers")
public String showFollowers(Model model, HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null || session.getAttribute("AUTH_USER") == null) {
        return "redirect:/login?error=Please+log+in+to+view+followers";
    }
    String username = (String) session.getAttribute("AUTH_USER");
    User user = userRepository.findByUsername(username);
    List<User> followers = userFollowRepository.findByFollowed(user).stream()
            .map(UserFollow::getFollower)
            .toList();
    model.addAttribute("connections", followers);
    model.addAttribute("listTitle", "Followers");
    return "connections";
}

@GetMapping("/profile/following")
public String showFollowing(Model model, HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null || session.getAttribute("AUTH_USER") == null) {
        return "redirect:/login?error=Please+log+in+to+view+following";
    }
    String username = (String) session.getAttribute("AUTH_USER");
    User user = userRepository.findByUsername(username);
    List<User> following = userFollowRepository.findByFollower(user).stream()
            .map(UserFollow::getFollowed)
            .toList();
    model.addAttribute("connections", following);
    model.addAttribute("listTitle", "Following");
    return "connections";
}
}
