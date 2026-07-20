package com.cardboardboxed.demo.controllers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.cardboardboxed.demo.boardgames.BoardGameAutocompleteRepository;
import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.boardgames.BoardGameRankRepository;
import com.cardboardboxed.demo.collections.CollectionItem;
import com.cardboardboxed.demo.collections.CollectionItem.CollectionType;
import com.cardboardboxed.demo.collections.CollectionItemRepository;
import com.cardboardboxed.demo.profile.ProfileGameView;
import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserFollow;
import com.cardboardboxed.demo.useracounts.UserFollowRepository;
import com.cardboardboxed.demo.useracounts.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class ProfileController {

    private static final String DEFAULT_AVATAR =
            "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde"
            + "?auto=format&fit=crop&w=150&h=150&q=80";

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final BoardGameAutocompleteRepository boardGameAutocompleteRepository;
    private final BoardGameRankRepository boardGameRankRepository;
    private final UserFollowRepository userFollowRepository;
    private final CollectionItemRepository collectionItemRepository;

    public ProfileController(
            UserRepository userRepository,
            ReviewRepository reviewRepository,
            BoardGameAutocompleteRepository boardGameAutocompleteRepository,
            BoardGameRankRepository boardGameRankRepository,
            UserFollowRepository userFollowRepository,
            CollectionItemRepository collectionItemRepository
    ) {
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.boardGameAutocompleteRepository =
                boardGameAutocompleteRepository;
        this.boardGameRankRepository = boardGameRankRepository;
        this.userFollowRepository = userFollowRepository;
        this.collectionItemRepository = collectionItemRepository;
    }

    /*
     * Displays the currently logged-in user's profile.
     */
    @GetMapping("/profile")
    public String showProfile(
            Model model,
            HttpServletRequest request,
            @RequestParam(defaultValue = "added") String sort
    ) {
        User user = getLoggedInUser(request);

        if (user == null) {
            return "redirect:/login"
                    + "?error=Please+log+in+to+view+your+profile";
        }

        String normalizedSort = normalizeSort(sort);

        addProfileInformationToModel(
                user,
                normalizedSort,
                model
        );

        model.addAttribute("isOwnProfile", true);

        return "profile";
    }

    /*
     * Updates the user's bio and profile-picture URL.
     */
    @PostMapping("/profile/update-bio")
    public String updateBio(
            @RequestParam String bio,
            @RequestParam(defaultValue = "") String profilePictureUrl,
            @RequestParam(required = false) String ownedGames,
            @RequestParam(required = false) String wishlistGames,
            HttpServletRequest request
    ) {
        User user = getLoggedInUser(request);

        if (user == null) {
            return "redirect:/login";
        }

        user.setBio(
                bio == null
                        ? ""
                        : bio.trim()
        );

        user.setProfilePictureUrl(
                profilePictureUrl == null
                        ? ""
                        : profilePictureUrl.trim()
        );

        /*
         * Compatibility with the older profile form.
         *
         * New collection additions are handled by CollectionController.
         */
        addLegacyCollectionGame(
                user,
                ownedGames,
                CollectionType.OWNED
        );

        addLegacyCollectionGame(
                user,
                wishlistGames,
                CollectionType.WISHLIST
        );

        userRepository.save(user);

        return "redirect:/profile"
                + "?success=Profile+updated+successfully";
    }

    /*
     * Searches for user profiles.
     */
    @GetMapping("/profile/search")
    public String searchProfiles(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model,
            HttpServletRequest request
    ) {
        User currentUser = getLoggedInUser(request);

        if (currentUser == null) {
            return "redirect:/login"
                    + "?error=Please+log+in+to+search+profiles";
        }

        String trimmedQuery = query == null
                ? ""
                : query.trim();

        int currentPage = Math.max(page, 1) - 1;
        int pageSize = 12;

        PageRequest pageRequest = PageRequest.of(
                currentPage,
                pageSize
        );

        Page<User> resultsPage = trimmedQuery.isBlank()
                ? userRepository.findAll(pageRequest)
                : userRepository.findByUsernameContainingIgnoreCase(
                        trimmedQuery,
                        pageRequest
                );

        model.addAttribute("query", trimmedQuery);
        model.addAttribute("results", resultsPage.getContent());

        model.addAttribute(
                "currentPage",
                resultsPage.getNumber() + 1
        );

        model.addAttribute(
                "totalPages",
                resultsPage.getTotalPages()
        );

        return "profile-search";
    }

    /*
     * Displays another user's public profile.
     */
    @GetMapping("/profile/{username}")
    public String viewProfile(
            @PathVariable String username,
            @RequestParam(defaultValue = "added") String sort,
            Model model,
            HttpServletRequest request
    ) {
        User currentUser = getLoggedInUser(request);

        if (currentUser == null) {
            return "redirect:/login"
                    + "?error=Please+log+in+to+view+profiles";
        }

        if (currentUser.getUsername().equalsIgnoreCase(username)) {
            return "redirect:/profile";
        }

        User viewedUser = userRepository.findByUsername(username);

        if (viewedUser == null) {
            return "redirect:/profile/search"
                    + "?error=User+not+found";
        }

        String normalizedSort = normalizeSort(sort);

        addProfileInformationToModel(
                viewedUser,
                normalizedSort,
                model
        );

        boolean isFollowing =
                userFollowRepository.existsByFollowerAndFollowed(
                        currentUser,
                        viewedUser
                );

        model.addAttribute("isFollowing", isFollowing);
        model.addAttribute("isOwnProfile", false);

        return "profile-view";
    }

    /*
     * Follows another user.
     */
    @PostMapping("/profile/{username}/follow")
    public String followUser(
            @PathVariable String username,
            HttpServletRequest request
    ) {
        User currentUser = getLoggedInUser(request);

        if (currentUser == null) {
            return "redirect:/login"
                    + "?error=Please+log+in+to+follow+users";
        }

        if (currentUser.getUsername().equalsIgnoreCase(username)) {
            return "redirect:/profile"
                    + "?error=Cannot+follow+your+own+account";
        }

        User targetUser = userRepository.findByUsername(username);

        if (targetUser == null) {
            return "redirect:/profile/search"
                    + "?error=User+not+found";
        }

        boolean alreadyFollowing =
                userFollowRepository.existsByFollowerAndFollowed(
                        currentUser,
                        targetUser
                );

        if (!alreadyFollowing) {
            userFollowRepository.save(
                    new UserFollow(
                            currentUser,
                            targetUser
                    )
            );
        }

        return "redirect:/profile/"
                + targetUser.getUsername();
    }

    /*
     * Unfollows another user.
     */
    @PostMapping("/profile/{username}/unfollow")
    public String unfollowUser(
            @PathVariable String username,
            HttpServletRequest request
    ) {
        User currentUser = getLoggedInUser(request);

        if (currentUser == null) {
            return "redirect:/login"
                    + "?error=Please+log+in+to+manage"
                    + "+your+followed+accounts";
        }

        User targetUser = userRepository.findByUsername(username);

        if (targetUser == null) {
            return "redirect:/profile/search"
                    + "?error=User+not+found";
        }

        userFollowRepository.deleteByFollowerAndFollowed(
                currentUser,
                targetUser
        );

        return "redirect:/profile/"
                + targetUser.getUsername();
    }

    /*
     * Displays the current user's followers.
     */
    @GetMapping("/profile/followers")
    public String showFollowers(
            Model model,
            HttpServletRequest request
    ) {
        User user = getLoggedInUser(request);

        if (user == null) {
            return "redirect:/login"
                    + "?error=Please+log+in+to+view+followers";
        }

        List<User> followers = userFollowRepository
                .findByFollowed(user)
                .stream()
                .map(UserFollow::getFollower)
                .toList();

        model.addAttribute("connections", followers);
        model.addAttribute("listTitle", "Followers");

        return "connections";
    }

    /*
     * Displays the accounts followed by the current user.
     */
    @GetMapping("/profile/following")
    public String showFollowing(
            Model model,
            HttpServletRequest request
    ) {
        User user = getLoggedInUser(request);

        if (user == null) {
            return "redirect:/login"
                    + "?error=Please+log+in+to+view+following";
        }

        List<User> following = userFollowRepository
                .findByFollower(user)
                .stream()
                .map(UserFollow::getFollowed)
                .toList();

        model.addAttribute("connections", following);
        model.addAttribute("listTitle", "Following");

        return "connections";
    }

    /*
     * Adds all shared profile data to the model.
     *
     * Used for both the logged-in user's profile and public profiles.
     */
    private void addProfileInformationToModel(
            User user,
            String sort,
            Model model
    ) {
        String username = user.getUsername();

        String bio = user.getBio() != null
                && !user.getBio().isBlank()
                ? user.getBio()
                : "No bio added yet.";

        String avatar = user.getProfilePictureUrl() != null
                && !user.getProfilePictureUrl().isBlank()
                ? user.getProfilePictureUrl()
                : DEFAULT_AVATAR;

        List<Review> userReviews =
                reviewRepository.findByUserUsername(username);

        if (userReviews == null) {
            userReviews = new ArrayList<>();
        }

        Map<String, Review> latestReviewByGame =
                buildLatestReviewMap(userReviews);

        List<CollectionItem> ownedItems = new ArrayList<>(
                collectionItemRepository
                        .findByUserAndCollectionTypeOrderByAddedAtDesc(
                                user,
                                CollectionType.OWNED
                        )
        );

        List<CollectionItem> wishlistItems = new ArrayList<>(
                collectionItemRepository
                        .findByUserAndCollectionTypeOrderByAddedAtDesc(
                                user,
                                CollectionType.WISHLIST
                        )
        );

        Comparator<CollectionItem> comparator =
                createCollectionComparator(
                        sort,
                        latestReviewByGame
                );

        ownedItems.sort(comparator);
        wishlistItems.sort(comparator);

        List<ProfileGameView> ownedGames =
                buildProfileGameViews(
                        ownedItems,
                        latestReviewByGame
                );

        List<ProfileGameView> wishlistGames =
                buildProfileGameViews(
                        wishlistItems,
                        latestReviewByGame
                );

        double averageRating =
                calculateAverageRating(userReviews);

        model.addAttribute("username", username);
        model.addAttribute("role", user.getRole());
        model.addAttribute("bio", bio);
        model.addAttribute("avatar", avatar);
        model.addAttribute("reviews", userReviews);

        model.addAttribute("ownedGames", ownedGames);
        model.addAttribute("wishlistGames", wishlistGames);

        /*
         * Retained for compatibility with older profile templates.
         */
        model.addAttribute("ownedItems", ownedItems);
        model.addAttribute("wishlistItems", wishlistItems);

        model.addAttribute(
                "ownedList",
                ownedItems.stream()
                        .map(CollectionItem::getGameName)
                        .toList()
        );

        model.addAttribute(
                "wishlistList",
                wishlistItems.stream()
                        .map(CollectionItem::getGameName)
                        .toList()
        );

        model.addAttribute(
                "followerCount",
                userFollowRepository.countByFollowed(user)
        );

        model.addAttribute(
                "followingCount",
                userFollowRepository.countByFollower(user)
        );

        model.addAttribute(
                "reviewCount",
                userReviews.size()
        );

        model.addAttribute(
                "ownedCount",
                ownedGames.size()
        );

        model.addAttribute(
                "wishlistCount",
                wishlistGames.size()
        );

        model.addAttribute(
                "averageRating",
                averageRating
        );

        model.addAttribute("sort", sort);
    }

    /*
     * Converts collection records into rich view objects for Thymeleaf.
     */
    private List<ProfileGameView> buildProfileGameViews(
            List<CollectionItem> collectionItems,
            Map<String, Review> latestReviewByGame
    ) {
        List<ProfileGameView> profileGames =
                new ArrayList<>();

        for (CollectionItem collectionItem : collectionItems) {
            String gameName =
                    collectionItem.getGameName();

            BoardGameRank catalogGame = null;

            if (gameName != null && !gameName.isBlank()) {
                catalogGame = boardGameRankRepository
                        .findFirstByTitleIgnoreCaseOrderByRankPositionAsc(
                                gameName.trim()
                        )
                        .orElse(null);
            }

            Review latestReview =
                    latestReviewByGame.get(
                            normalizeGameName(gameName)
                    );

            profileGames.add(
                    new ProfileGameView(
                            collectionItem,
                            catalogGame,
                            latestReview
                    )
            );
        }

        return profileGames;
    }

    /*
     * Builds a map containing the newest review for each game.
     */
    private Map<String, Review> buildLatestReviewMap(
            List<Review> reviews
    ) {
        Map<String, Review> latestReviewByGame =
                new HashMap<>();

        for (Review review : reviews) {
            String gameName = normalizeGameName(
                    review.getGameTitle()
            );

            if (gameName.isBlank()) {
                continue;
            }

            Review savedReview =
                    latestReviewByGame.get(gameName);

            if (savedReview == null
                    || savedReview.getCreatedAt() == null
                    || (
                        review.getCreatedAt() != null
                        && review.getCreatedAt().isAfter(
                                savedReview.getCreatedAt()
                        )
                    )) {

                latestReviewByGame.put(
                        gameName,
                        review
                );
            }
        }

        return latestReviewByGame;
    }

    /*
     * Creates the selected collection sorting behavior.
     */
    private Comparator<CollectionItem> createCollectionComparator(
            String sort,
            Map<String, Review> latestReviewByGame
    ) {
        Comparator<CollectionItem> addedComparator =
                Comparator.comparing(
                        CollectionItem::getAddedAt,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                );

        if ("rating".equals(sort)) {
            return Comparator.comparing(
                    (CollectionItem item) -> {
                        Review review =
                                latestReviewByGame.get(
                                        normalizeGameName(
                                                item.getGameName()
                                        )
                                );

                        return review == null
                                ? null
                                : review.getRating();
                    },
                    Comparator.nullsLast(
                            Comparator.reverseOrder()
                    )
            ).thenComparing(addedComparator);
        }

        if ("reviewed".equals(sort)) {
            return Comparator.comparing(
                    (CollectionItem item) -> {
                        Review review =
                                latestReviewByGame.get(
                                        normalizeGameName(
                                                item.getGameName()
                                        )
                                );

                        return review == null
                                ? null
                                : review.getCreatedAt();
                    },
                    Comparator.nullsLast(
                            Comparator.reverseOrder()
                    )
            ).thenComparing(addedComparator);
        }

        return addedComparator;
    }

    /*
     * Calculates the user's average rating.
     */
    private double calculateAverageRating(
            List<Review> reviews
    ) {
        if (reviews == null || reviews.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        int ratingCount = 0;

        for (Review review : reviews) {
            if (review.getRating() != null) {
                total += review.getRating();
                ratingCount++;
            }
        }

        if (ratingCount == 0) {
            return 0.0;
        }

        return total / ratingCount;
    }

    /*
     * Supports the previous profile form.
     *
     * Normal collection changes are handled by CollectionController.
     */
    private void addLegacyCollectionGame(
            User user,
            String gameName,
            CollectionType collectionType
    ) {
        if (gameName == null || gameName.isBlank()) {
            return;
        }

        String resolvedName =
                boardGameAutocompleteRepository
                        .resolveToExistingName(gameName)
                        .orElse(null);

        if (resolvedName == null
                || resolvedName.isBlank()) {
            return;
        }

        CollectionItem item =
                collectionItemRepository
                        .findByUserAndGameNameIgnoreCase(
                                user,
                                resolvedName
                        )
                        .orElse(null);

        if (item == null) {
            item = new CollectionItem(
                    user,
                    resolvedName,
                    collectionType
            );
        } else {
            item.setCollectionType(collectionType);
        }

        collectionItemRepository.save(item);
    }

    /*
     * Returns the currently authenticated user.
     */
    private User getLoggedInUser(
            HttpServletRequest request
    ) {
        HttpSession session =
                request.getSession(false);

        if (session == null) {
            return null;
        }

        Object authenticatedUsername =
                session.getAttribute("AUTH_USER");

        if (!(authenticatedUsername instanceof String username)
                || username.isBlank()) {
            return null;
        }

        return userRepository.findByUsername(username);
    }

    private String normalizeSort(String sort) {
        if ("rating".equalsIgnoreCase(sort)) {
            return "rating";
        }

        if ("reviewed".equalsIgnoreCase(sort)) {
            return "reviewed";
        }

        return "added";
    }

    private String normalizeGameName(
            String gameName
    ) {
        if (gameName == null) {
            return "";
        }

        return gameName
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}