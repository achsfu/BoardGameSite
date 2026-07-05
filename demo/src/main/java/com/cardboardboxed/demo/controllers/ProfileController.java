package com.cardboardboxed.demo.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.UserRepository;
import com.cardboardboxed.demo.useracounts.User;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public ProfileController(UserRepository userRepository, ReviewRepository reviewRepository) {
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
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
        String currentOwned = user.getGameOwned();
        if (currentOwned != null && !currentOwned.isBlank()) {
            if (!currentOwned.contains(ownedGames.trim())) {
                user.setGameOwned(currentOwned + ", " + ownedGames.trim());
            }
        } else {
            user.setGameOwned(ownedGames.trim());
        }
        }

        if (wishlistGames != null && !wishlistGames.isBlank()) {
        String currentWishlist = user.getGameWishlist();
        if (currentWishlist != null && !currentWishlist.isBlank()) {
            if (!currentWishlist.contains(wishlistGames.trim())) {
                user.setGameWishlist(currentWishlist + ", " + wishlistGames.trim());
            }
        } else {
            user.setGameWishlist(wishlistGames.trim());
        }
    }
        userRepository.save(user);
        return "redirect:/profile?success=Bio+updated+successfully";
    }

}
