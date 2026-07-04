package com.cardboardboxed.demo.controllers;

import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AdminModerationController {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public AdminModerationController(UserRepository userRepository, ReviewRepository reviewRepository) {
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    private User getLoggedInUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }

        String username = (String) session.getAttribute("AUTH_USER");

        if (username == null) {
            return null;
        }

        return userRepository.findByUsername(username);
    }

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private boolean isModeratorOrAdmin(User user) {
        return user != null &&
                ("ADMIN".equalsIgnoreCase(user.getRole()) ||
                 "MODERATOR".equalsIgnoreCase(user.getRole()));
    }

    @GetMapping("/admin")
    public String showAdminPage(Model model, HttpServletRequest request) {
        User currentUser = getLoggedInUser(request);

        if (!isAdmin(currentUser)) {
            return "redirect:/dashboard";
        }

        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("currentUsername", currentUser.getUsername());

        return "admin";
    }

    @PostMapping("/admin/users/{id}/role")
    public String updateUserRole(
            @PathVariable Integer id,
            @RequestParam String role,
            HttpServletRequest request
    ) {
        User currentUser = getLoggedInUser(request);

        if (!isAdmin(currentUser)) {
            return "redirect:/dashboard";
        }

        User user = userRepository.findById(id).orElse(null);

        if (user != null) {
            user.setRole(role);
            userRepository.save(user);
        }

        return "redirect:/admin";
    }

    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(@PathVariable Integer id, HttpServletRequest request) {
        User currentUser = getLoggedInUser(request);

        if (!isAdmin(currentUser)) {
            return "redirect:/dashboard";
        }

        User user = userRepository.findById(id).orElse(null);

        if (user != null && !user.getUsername().equals(currentUser.getUsername())) {
            reviewRepository.deleteAll(reviewRepository.findByUserUsername(user.getUsername()));
            userRepository.delete(user);
        }

        return "redirect:/admin";
    }

    @GetMapping("/moderation/reviews")
    public String showReviewModeration(Model model, HttpServletRequest request) {
        User currentUser = getLoggedInUser(request);

        if (!isModeratorOrAdmin(currentUser)) {
            return "redirect:/dashboard";
        }

        model.addAttribute("reviews", reviewRepository.findAll());

        return "moderation-reviews";
    }

    @PostMapping("/moderation/reviews/{id}/delete")
    public String deleteReview(@PathVariable Integer id, HttpServletRequest request) {
        User currentUser = getLoggedInUser(request);

        if (!isModeratorOrAdmin(currentUser)) {
            return "redirect:/dashboard";
        }

        reviewRepository.deleteById(id);

        return "redirect:/moderation/reviews";
    }
}