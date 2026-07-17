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

import com.cardboardboxed.demo.collections.CollectionItem;
import com.cardboardboxed.demo.collections.CollectionItem.CollectionType;
import com.cardboardboxed.demo.collections.CollectionItemRepository;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final BoardGameAutocompleteRepository boardGameAutocompleteRepository;
    private final CollectionItemRepository collectionItemRepository;

    public ProfileController(UserRepository userRepository, ReviewRepository reviewRepository,
            BoardGameAutocompleteRepository boardGameAutocompleteRepository,
            CollectionItemRepository collectionItemRepository) {
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.boardGameAutocompleteRepository = boardGameAutocompleteRepository;
        this.collectionItemRepository = collectionItemRepository;
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

        List<CollectionItem> ownedItems = collectionItemRepository
                .findByUserAndCollectionTypeOrderByAddedAtDesc(
                        user,
                        CollectionType.OWNED
                );

        List<CollectionItem> wishlistItems = collectionItemRepository
                .findByUserAndCollectionTypeOrderByAddedAtDesc(
                        user,
                        CollectionType.WISHLIST
                );

        model.addAttribute("username", username);
        model.addAttribute("role", user.getRole());
        model.addAttribute("bio", bio);
        model.addAttribute("avatar", avatar);
        model.addAttribute("reviews", userReviews);

        model.addAttribute("ownedList", ownedList);
        model.addAttribute("wishlistList", wishlistList);

        model.addAttribute("ownedItems", ownedItems);
        model.addAttribute("wishlistItems", wishlistItems);

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

}