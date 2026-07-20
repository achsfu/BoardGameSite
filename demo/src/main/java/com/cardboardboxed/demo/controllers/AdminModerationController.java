package com.cardboardboxed.demo.controllers;

import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Controller
public class AdminModerationController {

    private static final Set<String> VALID_ROLES = Set.of(
            "PLAYER",
            "ORGANIZER",
            "MODERATOR",
            "ADMIN"
    );

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public AdminModerationController(
            UserRepository userRepository,
            ReviewRepository reviewRepository
    ) {
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    /*
     * Returns the currently logged-in user.
     */
    private User getLoggedInUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }

        String username = (String) session.getAttribute("AUTH_USER");

        if (username == null || username.isBlank()) {
            return null;
        }

        return userRepository.findByUsername(username);
    }

    /*
     * Checks whether the user has the administrator role.
     */
    private boolean isAdmin(User user) {
        return user != null
                && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    /*
     * Checks whether the user can moderate reviews.
     */
    private boolean isModeratorOrAdmin(User user) {
        return user != null
                && (
                    "MODERATOR".equalsIgnoreCase(user.getRole())
                    || "ADMIN".equalsIgnoreCase(user.getRole())
                );
    }

    /*
     * Null-safe, case-insensitive text matching.
     */
    private boolean containsIgnoreCase(
            String source,
            String searchTerm
    ) {
        if (source == null || searchTerm == null) {
            return false;
        }

        return source
                .toLowerCase(Locale.ROOT)
                .contains(searchTerm.toLowerCase(Locale.ROOT));
    }

    /*
     * Administrator-only user management page.
     */
    @GetMapping("/admin")
    public String showAdminPage(
            @RequestParam(required = false) String keyword,
            Model model,
            HttpServletRequest request
    ) {
        User currentUser = getLoggedInUser(request);

        if (!isAdmin(currentUser)) {
            return "redirect:/dashboard";
        }

        String searchTerm = keyword == null
                ? ""
                : keyword.trim();

        List<User> users = userRepository.findAll();

        if (!searchTerm.isBlank()) {
            users = users.stream()
                    .filter(user ->
                            containsIgnoreCase(
                                    user.getUsername(),
                                    searchTerm
                            )
                    )
                    .toList();
        }

        model.addAttribute(
                "admins",
                users.stream()
                        .filter(user ->
                                "ADMIN".equalsIgnoreCase(user.getRole())
                        )
                        .toList()
        );

        model.addAttribute(
                "moderators",
                users.stream()
                        .filter(user ->
                                "MODERATOR".equalsIgnoreCase(user.getRole())
                        )
                        .toList()
        );

        model.addAttribute(
                "organizers",
                users.stream()
                        .filter(user ->
                                "ORGANIZER".equalsIgnoreCase(user.getRole())
                        )
                        .toList()
        );

        model.addAttribute(
                "players",
                users.stream()
                        .filter(user ->
                                "PLAYER".equalsIgnoreCase(user.getRole())
                        )
                        .toList()
        );

        model.addAttribute("keyword", keyword);
        model.addAttribute(
                "currentUsername",
                currentUser.getUsername()
        );

        return "admin";
    }

    /*
     * Administrator-only role updates.
     */
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

        if (role == null || role.isBlank()) {
            return "redirect:/admin";
        }

        String normalizedRole = role
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!VALID_ROLES.contains(normalizedRole)) {
            return "redirect:/admin";
        }

        User user = userRepository.findById(id).orElse(null);

        if (user != null) {
            user.setRole(normalizedRole);
            userRepository.save(user);
        }

        return "redirect:/admin";
    }

    /*
     * Administrator-only user deletion.
     *
     * The logged-in administrator cannot delete their own account.
     */
    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(
            @PathVariable Integer id,
            HttpServletRequest request
    ) {
        User currentUser = getLoggedInUser(request);

        if (!isAdmin(currentUser)) {
            return "redirect:/dashboard";
        }

        User user = userRepository.findById(id).orElse(null);

        if (
                user != null
                && user.getUsername() != null
                && !user.getUsername().equalsIgnoreCase(
                        currentUser.getUsername()
                )
        ) {
            reviewRepository.deleteAll(
                    reviewRepository.findByUserUsername(
                            user.getUsername()
                    )
            );

            userRepository.delete(user);
        }

        return "redirect:/admin";
    }

    /*
     * Review moderation page.
     *
     * Moderators and administrators can access this page.
     */
    @GetMapping("/moderation/reviews")
    public String showReviewModeration(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer rating,
            Model model,
            HttpServletRequest request
    ) {
        User currentUser = getLoggedInUser(request);

        if (!isModeratorOrAdmin(currentUser)) {
            return "redirect:/dashboard";
        }

        String searchTerm = keyword == null
                ? ""
                : keyword.trim();

        Integer selectedRating = rating;

        if (
                selectedRating != null
                && (selectedRating < 1 || selectedRating > 5)
        ) {
            selectedRating = null;
        }

        List<Review> reviews = reviewRepository.findAll();

        if (!searchTerm.isBlank()) {
            reviews = reviews.stream()
                    .filter(review -> {
                        String reviewerUsername =
                                review.getUser() == null
                                        ? null
                                        : review.getUser().getUsername();

                        return containsIgnoreCase(
                                    review.getGameTitle(),
                                    searchTerm
                                )
                                || containsIgnoreCase(
                                    review.getReviewText(),
                                    searchTerm
                                )
                                || containsIgnoreCase(
                                    reviewerUsername,
                                    searchTerm
                                );
                    })
                    .toList();
        }

        if (selectedRating != null) {
            Integer finalRating = selectedRating;

            reviews = reviews.stream()
                    .filter(review ->
                            finalRating.equals(review.getRating())
                    )
                    .toList();
        }

        model.addAttribute("reviews", reviews);
        model.addAttribute("reviewCount", reviews.size());
        model.addAttribute("keyword", searchTerm);
        model.addAttribute("selectedRating", selectedRating);

        model.addAttribute(
                "hasActiveFilters",
                !searchTerm.isBlank() || selectedRating != null
        );

        model.addAttribute(
                "username",
                currentUser.getUsername()
        );

        model.addAttribute(
                "role",
                currentUser.getRole().toUpperCase(Locale.ROOT)
        );

        return "moderation-reviews";
    }

    /*
     * Moderators and administrators may delete individual reviews.
     */
    @PostMapping("/moderation/reviews/{id}/delete")
    public String deleteReview(
            @PathVariable Integer id,
            HttpServletRequest request
    ) {
        User currentUser = getLoggedInUser(request);

        if (!isModeratorOrAdmin(currentUser)) {
            return "redirect:/dashboard";
        }

        Review review = reviewRepository
                .findById(id)
                .orElse(null);

        if (review != null) {
            reviewRepository.delete(review);
        }

        return "redirect:/moderation/reviews";
    }

    /*
     * Only administrators may delete every review.
     */
    @PostMapping("/moderation/reviews/delete-all")
    public String deleteAllReviews(
            HttpServletRequest request
    ) {
        User currentUser = getLoggedInUser(request);

        if (!isAdmin(currentUser)) {
            return "redirect:/dashboard";
        }

        reviewRepository.deleteAllInBatch();

        return "redirect:/moderation/reviews";
    }
}