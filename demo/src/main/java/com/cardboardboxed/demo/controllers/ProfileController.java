package com.cardboardboxed.demo.controllers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.cardboardboxed.demo.boardgames.BoardGameAutocompleteRepository;
import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.boardgames.BoardGameRankRepository;
import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.reviews.ReviewLikeRepository;
import com.cardboardboxed.demo.reviews.ReviewReply;
import com.cardboardboxed.demo.reviews.ReviewReplyRepository;
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
import org.springframework.web.util.UriUtils;
import com.cardboardboxed.demo.useracounts.UserFollow;
import com.cardboardboxed.demo.useracounts.UserFollowRepository;
import org.springframework.web.bind.annotation.PathVariable;
import com.cardboardboxed.demo.collections.CollectionItem;
import com.cardboardboxed.demo.collections.CollectionItem.CollectionType;
import com.cardboardboxed.demo.collections.CollectionItemRepository;

@Controller
public class ProfileController {

    private static final int COLLECTION_PAGE_SIZE = 20;

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final BoardGameAutocompleteRepository boardGameAutocompleteRepository;
    private final BoardGameRankRepository boardGameRankRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewReplyRepository reviewReplyRepository;
    //NEW FIELD FOR USER LOOKUP AND FOLLOW FUNCTION
    private final UserFollowRepository userFollowRepository;
    private final CollectionItemRepository collectionItemRepository;

    //add parameter for userfollowrepository in the constructor
    public ProfileController(UserRepository userRepository, ReviewRepository reviewRepository,
            BoardGameAutocompleteRepository boardGameAutocompleteRepository,
            BoardGameRankRepository boardGameRankRepository,
            ReviewLikeRepository reviewLikeRepository,
            ReviewReplyRepository reviewReplyRepository,
            UserFollowRepository userFollowRepository,
            CollectionItemRepository collectionItemRepository) {
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.boardGameAutocompleteRepository = boardGameAutocompleteRepository;
        this.boardGameRankRepository = boardGameRankRepository;
        this.reviewLikeRepository = reviewLikeRepository;
        this.reviewReplyRepository = reviewReplyRepository;
        this.userFollowRepository = userFollowRepository;
        this.collectionItemRepository = collectionItemRepository;
    }

    @GetMapping("/profile")
    public String showProfile(
        Model model,
        HttpServletRequest request,
        @RequestParam(defaultValue = "added") String sort
    ){
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

        if(!sort.equals("rating") && !sort.equals("reviewed")){
            sort = "added";
        }

        Map<String, Review> latestReviewByGame = new HashMap<>();

        for(Review review : userReviews){
            if(review.getGameTitle() == null){
                continue;
            }

            String gameName = review.getGameTitle()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            Review savedReview = latestReviewByGame.get(gameName);

            if(savedReview == null
                    || savedReview.getCreatedAt() == null
                    || (review.getCreatedAt() != null
                    && review.getCreatedAt().isAfter(savedReview.getCreatedAt()))){
                latestReviewByGame.put(gameName, review);
            }
        }

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

        Comparator<CollectionItem> comparator = Comparator.comparing(
                CollectionItem::getAddedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        );

        if(sort.equals("rating")){
            comparator = Comparator.comparing(
                    (CollectionItem item) -> {
                        String gameName = item.getGameName()
                                .trim()
                                .toLowerCase(Locale.ROOT);

                        Review review = latestReviewByGame.get(gameName);

                        return review == null ? null : review.getRating();
                    },
                    Comparator.nullsLast(Comparator.reverseOrder())
            ).thenComparing(
                    CollectionItem::getAddedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
        } else if(sort.equals("reviewed")){
            comparator = Comparator.comparing(
                    (CollectionItem item) -> {
                        String gameName = item.getGameName()
                                .trim()
                                .toLowerCase(Locale.ROOT);

                        Review review = latestReviewByGame.get(gameName);

                        return review == null ? null : review.getCreatedAt();
                    },
                    Comparator.nullsLast(Comparator.reverseOrder())
            ).thenComparing(
                    CollectionItem::getAddedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
        }

        ownedItems.sort(comparator);
        wishlistItems.sort(comparator);

        Map<Integer, BoardGameRank> ownedGameCatalog =
                buildCollectionGameCatalog(ownedItems);

        Map<Integer, BoardGameRank> wishlistGameCatalog =
                buildCollectionGameCatalog(wishlistItems);

        Map<Integer, BoardGameRank> reviewGameCatalog =
                buildReviewGameCatalog(userReviews);

        List<User> followers = userFollowRepository.findByFollowed(user).stream()
            .map(UserFollow::getFollower)
            .toList();

        List<User> following = userFollowRepository.findByFollower(user).stream()
            .map(UserFollow::getFollowed)
            .toList();

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
        model.addAttribute("followers", followers);
        model.addAttribute("following", following);
        model.addAttribute("ownedItems", ownedItems);
        model.addAttribute("wishlistItems", wishlistItems);
        model.addAttribute("ownedGameCatalog", ownedGameCatalog);
        model.addAttribute("wishlistGameCatalog", wishlistGameCatalog);
        model.addAttribute("reviewGameCatalog", reviewGameCatalog);
        model.addAttribute("ownedCount", ownedItems.size());
        model.addAttribute("wishlistCount", wishlistItems.size());
        model.addAttribute("reviewCount", userReviews.size());
        model.addAttribute("sort", sort);

        return "profile";
    }

    @GetMapping("/collection")
    public String showCollection(
            Model model,
            HttpServletRequest request,
            @RequestParam(name = "name", defaultValue = "") String name,
            @RequestParam(name = "playersMin", required = false) Integer playersMin,
            @RequestParam(name = "playersMax", required = false) Integer playersMax,
            @RequestParam(name = "complexityMin", required = false) Double complexityMin,
            @RequestParam(name = "complexityMax", required = false) Double complexityMax,
            @RequestParam(name = "timeMin", required = false) Integer timeMin,
            @RequestParam(name = "timeMax", required = false) Integer timeMax,
            @RequestParam(name = "sort", defaultValue = "name") String sort,
            @RequestParam(name = "dir", defaultValue = "asc") String dir,
            @RequestParam(name = "page", defaultValue = "1") int page
    ) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("AUTH_USER") == null) {
            return "redirect:/login?error=Please+log+in+to+view+your+collection";
        }

        String username = (String) session.getAttribute("AUTH_USER");
        User user = userRepository.findByUsername(username);

        String safeSort = sanitizeSort(sort);
        String safeDir = sanitizeDirection(dir);
        String normalizedNameFilter = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);

        List<CollectionItem> items = collectionItemRepository.findByUserOrderByAddedAtDesc(user);
        List<Review> userReviews = reviewRepository.findByUserUsername(username);

        Set<String> normalizedTitles = new HashSet<>();
        for (CollectionItem item : items) {
            String normalized = normalizeGameName(item.getGameName());
            if (!normalized.isBlank()) {
                normalizedTitles.add(normalized);
            }
        }

        Map<String, BoardGameRank> gameByNormalizedTitle = new HashMap<>();
        if (!normalizedTitles.isEmpty()) {
            List<BoardGameRank> games = boardGameRankRepository.findByNormalizedTitles(new ArrayList<>(normalizedTitles));
            for (BoardGameRank game : games) {
                String normalized = normalizeGameName(game.getTitle());
                gameByNormalizedTitle.putIfAbsent(normalized, game);
            }
        }

        Map<String, CollectionReviewStats> reviewStatsByTitle = new HashMap<>();
        for (Review review : userReviews) {
            String reviewTitle = coalesceReviewGameTitle(review);
            String normalizedTitle = normalizeGameName(reviewTitle);
            if (normalizedTitle.isBlank()) {
                continue;
            }

            CollectionReviewStats stats = reviewStatsByTitle.computeIfAbsent(
                    normalizedTitle,
                    key -> new CollectionReviewStats()
            );

            if (review.getRating() != null) {
                stats.reviewCount += 1;
                stats.ratingTotal += review.getRating();
            }

            if (review.getCreatedAt() != null
                    && (stats.lastReviewDate == null || review.getCreatedAt().isAfter(stats.lastReviewDate))) {
                stats.lastReviewDate = review.getCreatedAt();
            }
        }

        List<CollectionRow> rows = new ArrayList<>();
        for (CollectionItem item : items) {
            String normalizedTitle = normalizeGameName(item.getGameName());
            BoardGameRank game = gameByNormalizedTitle.get(normalizedTitle);
            CollectionReviewStats stats = reviewStatsByTitle.get(normalizedTitle);

            Double averageReviewScore = null;
            LocalDateTime lastReviewDate = null;

            if (stats != null) {
                if (stats.reviewCount > 0) {
                    averageReviewScore = (double) stats.ratingTotal / (double) stats.reviewCount;
                }
                lastReviewDate = stats.lastReviewDate;
            }

            rows.add(new CollectionRow(
                    item.getId(),
                    item.getGameName(),
                    item.getAddedAt(),
                    item.getCollectionType(),
                    game == null ? null : game.getMinPlayers(),
                    game == null ? null : game.getMaxPlayers(),
                    game == null ? null : game.getGameWeight(),
                    resolveSortablePlaytime(game),
                    averageReviewScore,
                    lastReviewDate,
                    game == null ? "N/A" : safeDisplay(game.getPlayerCountDisplay()),
                    game == null ? "N/A" : safeDisplay(game.getComplexityDisplay()),
                    game == null ? "N/A" : safeDisplay(game.getPlaytimeDisplay())
            ));
        }

        int globalPlayersMin = rows.stream()
                .map(row -> row.getMinPlayers() == null ? row.getMaxPlayers() : row.getMinPlayers())
                .filter(value -> value != null && value > 0)
                .min(Integer::compareTo)
                .orElse(1);

        int globalPlayersMax = rows.stream()
                .map(row -> row.getMaxPlayers() == null ? row.getMinPlayers() : row.getMaxPlayers())
                .filter(value -> value != null && value > 0)
                .max(Integer::compareTo)
                .orElse(globalPlayersMin);
        if (globalPlayersMax < globalPlayersMin) {
            globalPlayersMax = globalPlayersMin;
        }

        double globalComplexityMin = rows.stream()
                .map(CollectionRow::getComplexityValue)
                .filter(value -> value != null && value > 0)
                .min(Double::compareTo)
                .orElse(0.0);

        double globalComplexityMax = rows.stream()
                .map(CollectionRow::getComplexityValue)
                .filter(value -> value != null && value > 0)
                .max(Double::compareTo)
                .orElse(Math.max(globalComplexityMin, 5.0));
        if (globalComplexityMax < globalComplexityMin) {
            globalComplexityMax = globalComplexityMin;
        }

        int globalTimeMin = rows.stream()
                .map(CollectionRow::getTimeToPlayValue)
                .filter(value -> value != null && value > 0)
                .min(Integer::compareTo)
                .orElse(1);

        int globalTimeMax = rows.stream()
                .map(CollectionRow::getTimeToPlayValue)
                .filter(value -> value != null && value > 0)
                .max(Integer::compareTo)
                .orElse(globalTimeMin);
        if (globalTimeMax < globalTimeMin) {
            globalTimeMax = globalTimeMin;
        }

        int safePlayersMinValue = clampInt(
            playersMin == null ? globalPlayersMin : playersMin,
            globalPlayersMin,
            globalPlayersMax
        );
        int safePlayersMaxValue = clampInt(
            playersMax == null ? globalPlayersMax : playersMax,
            globalPlayersMin,
            globalPlayersMax
        );
        if (safePlayersMinValue > safePlayersMaxValue) {
            int temp = safePlayersMinValue;
            safePlayersMinValue = safePlayersMaxValue;
            safePlayersMaxValue = temp;
        }

        double safeComplexityMinValue = clampDouble(
            complexityMin == null ? globalComplexityMin : complexityMin,
            globalComplexityMin,
            globalComplexityMax
        );
        double safeComplexityMaxValue = clampDouble(
            complexityMax == null ? globalComplexityMax : complexityMax,
            globalComplexityMin,
            globalComplexityMax
        );
        if (safeComplexityMinValue > safeComplexityMaxValue) {
            double temp = safeComplexityMinValue;
            safeComplexityMinValue = safeComplexityMaxValue;
            safeComplexityMaxValue = temp;
        }

        int safeTimeMinValue = clampInt(
            timeMin == null ? globalTimeMin : timeMin,
            globalTimeMin,
            globalTimeMax
        );
        int safeTimeMaxValue = clampInt(
            timeMax == null ? globalTimeMax : timeMax,
            globalTimeMin,
            globalTimeMax
        );
        if (safeTimeMinValue > safeTimeMaxValue) {
            int temp = safeTimeMinValue;
            safeTimeMinValue = safeTimeMaxValue;
            safeTimeMaxValue = temp;
        }

        final int safePlayersMin = safePlayersMinValue;
        final int safePlayersMax = safePlayersMaxValue;
        final double safeComplexityMin = safeComplexityMinValue;
        final double safeComplexityMax = safeComplexityMaxValue;
        final int safeTimeMin = safeTimeMinValue;
        final int safeTimeMax = safeTimeMaxValue;

        List<CollectionRow> filteredRows = rows.stream()
                .filter(row -> normalizedNameFilter.isBlank()
                        || row.getName().toLowerCase(Locale.ROOT).contains(normalizedNameFilter))
            .filter(row -> row.matchesPlayerRange(safePlayersMin, safePlayersMax))
            .filter(row -> row.getComplexityValue() != null
                && row.getComplexityValue() >= safeComplexityMin
                && row.getComplexityValue() <= safeComplexityMax)
            .filter(row -> row.getTimeToPlayValue() != null
                && row.getTimeToPlayValue() >= safeTimeMin
                && row.getTimeToPlayValue() <= safeTimeMax)
                .toList();

        List<CollectionRow> sortedRows = new ArrayList<>(filteredRows);
        sortedRows.sort(buildCollectionComparator(safeSort, safeDir));

        int totalItems = sortedRows.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) COLLECTION_PAGE_SIZE));
        int currentPage = Math.min(Math.max(page, 1), totalPages);
        int startIndex = (currentPage - 1) * COLLECTION_PAGE_SIZE;
        int endIndex = Math.min(startIndex + COLLECTION_PAGE_SIZE, totalItems);

        List<CollectionRow> pageRows = startIndex >= endIndex
                ? List.of()
                : sortedRows.subList(startIndex, endIndex);

        model.addAttribute("username", username);
        model.addAttribute("role", user.getRole());
        model.addAttribute("rows", pageRows);
        model.addAttribute("sort", safeSort);
        model.addAttribute("dir", safeDir);
        model.addAttribute("name", name == null ? "" : name.trim());
        model.addAttribute("playersMin", safePlayersMin);
        model.addAttribute("playersMax", safePlayersMax);
        model.addAttribute("complexityMin", safeComplexityMin);
        model.addAttribute("complexityMax", safeComplexityMax);
        model.addAttribute("timeMin", safeTimeMin);
        model.addAttribute("timeMax", safeTimeMax);
        model.addAttribute("playersGlobalMin", globalPlayersMin);
        model.addAttribute("playersGlobalMax", globalPlayersMax);
        model.addAttribute("complexityGlobalMin", globalComplexityMin);
        model.addAttribute("complexityGlobalMax", globalComplexityMax);
        model.addAttribute("timeGlobalMin", globalTimeMin);
        model.addAttribute("timeGlobalMax", globalTimeMax);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);

        return "collection";
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

        List<CollectionItem> ownedItems = new ArrayList<>(
                collectionItemRepository
                        .findByUserAndCollectionTypeOrderByAddedAtDesc(
                                viewedUser,
                                CollectionType.OWNED
                        )
        );

        List<CollectionItem> wishlistItems = new ArrayList<>(
                collectionItemRepository
                        .findByUserAndCollectionTypeOrderByAddedAtDesc(
                                viewedUser,
                                CollectionType.WISHLIST
                        )
        );

        Map<Integer, BoardGameRank> ownedGameCatalog =
                buildCollectionGameCatalog(ownedItems);

        Map<Integer, BoardGameRank> wishlistGameCatalog =
                buildCollectionGameCatalog(wishlistItems);

        Map<Integer, BoardGameRank> reviewGameCatalog =
                buildReviewGameCatalog(userReviews);

        Map<String, String> ownedGameDetailLinks = buildGameDetailLinks(ownedList);
        Map<String, String> wishlistGameDetailLinks = buildGameDetailLinks(wishlistList);
        Map<Integer, Long> reviewLikeCounts = buildReviewLikeCounts(userReviews);
        Set<Integer> likedReviewIds = findLikedReviewIds(userReviews, currentUser);
        Map<Integer, List<ReviewReply>> reviewReplies = buildReviewReplies(userReviews);
        Map<Integer, Long> reviewReplyCounts = buildReviewReplyCounts(userReviews);

        List<User> followers = userFollowRepository
                .findByFollowed(viewedUser)
                .stream()
                .map(UserFollow::getFollower)
                .filter(java.util.Objects::nonNull)
                .toList();

        List<User> following = userFollowRepository
                .findByFollower(viewedUser)
                .stream()
                .map(UserFollow::getFollowed)
                .filter(java.util.Objects::nonNull)
                .toList();

        boolean isFollowing = userFollowRepository.existsByFollowerAndFollowed(currentUser, viewedUser);
        model.addAttribute("username", viewedUser.getUsername());
        model.addAttribute("role", viewedUser.getRole());
        model.addAttribute("bio", bio);
        model.addAttribute("avatar",avatar);
        model.addAttribute("reviews",userReviews);
        model.addAttribute("ownedList", ownedList);
        model.addAttribute("wishlistList",wishlistList);
        model.addAttribute("ownedItems", ownedItems);
        model.addAttribute("wishlistItems", wishlistItems);
        model.addAttribute("ownedGameCatalog", ownedGameCatalog);
        model.addAttribute("wishlistGameCatalog", wishlistGameCatalog);
        model.addAttribute("reviewGameCatalog", reviewGameCatalog);
        model.addAttribute("ownedCount", ownedItems.size());
        model.addAttribute("wishlistCount", wishlistItems.size());
        model.addAttribute("reviewCount", userReviews.size());
        model.addAttribute("ownedGameDetailLinks", ownedGameDetailLinks);
        model.addAttribute("wishlistGameDetailLinks", wishlistGameDetailLinks);
        model.addAttribute("reviewLikeCounts", reviewLikeCounts);
        model.addAttribute("likedReviewIds", likedReviewIds);
        model.addAttribute("reviewReplies", reviewReplies);
        model.addAttribute("reviewReplyCounts", reviewReplyCounts);
        model.addAttribute("followerCount", userFollowRepository.countByFollowed(viewedUser));
        model.addAttribute("followingCount", userFollowRepository.countByFollower(viewedUser));
        model.addAttribute("followers", followers);
        model.addAttribute("following", following);
        model.addAttribute("isFollowing", isFollowing);
        return "profile-view";
    }

    private Map<Integer, BoardGameRank> buildCollectionGameCatalog(
            List<CollectionItem> collectionItems
    ) {
        Map<Integer, BoardGameRank> gameCatalog = new HashMap<>();

        if (collectionItems == null || collectionItems.isEmpty()) {
            return gameCatalog;
        }

        Set<String> normalizedTitles = new HashSet<>();

        for (CollectionItem item : collectionItems) {
            if (item == null
                    || item.getGameName() == null
                    || item.getGameName().isBlank()) {
                continue;
            }

            normalizedTitles.add(normalizeGameName(item.getGameName()));
        }

        Map<String, BoardGameRank> gamesByTitle = new HashMap<>();

        if (!normalizedTitles.isEmpty()) {
            List<BoardGameRank> games = boardGameRankRepository
                    .findByNormalizedTitles(new ArrayList<>(normalizedTitles));

            for (BoardGameRank game : games) {
                if (game == null || game.getTitle() == null) {
                    continue;
                }

                gamesByTitle.putIfAbsent(
                        normalizeGameName(game.getTitle()),
                        game
                );
            }
        }

        for (CollectionItem item : collectionItems) {
            if (item == null || item.getId() == null) {
                continue;
            }

            BoardGameRank game = gamesByTitle.get(
                    normalizeGameName(item.getGameName())
            );

            if (game == null
                    && item.getGameName() != null
                    && !item.getGameName().isBlank()) {
                game = boardGameRankRepository
                        .findFirstByTitleIgnoreCaseOrderByRankPositionAsc(
                                item.getGameName().trim()
                        )
                        .orElse(null);
            }

            gameCatalog.put(item.getId(), game);
        }

        return gameCatalog;
    }

    private Map<Integer, BoardGameRank> buildReviewGameCatalog(
            List<Review> reviews
    ) {
        Map<Integer, BoardGameRank> gameCatalog = new HashMap<>();

        if (reviews == null || reviews.isEmpty()) {
            return gameCatalog;
        }

        for (Review review : reviews) {
            if (review == null || review.getId() == null) {
                continue;
            }

            BoardGameRank game = review.getGame();

            if (game == null
                    && review.getGameTitle() != null
                    && !review.getGameTitle().isBlank()) {
                game = boardGameRankRepository
                        .findFirstByTitleIgnoreCaseOrderByRankPositionAsc(
                                review.getGameTitle().trim()
                        )
                        .orElse(null);
            }

            gameCatalog.put(review.getId(), game);
        }

        return gameCatalog;
    }

    private Map<String, String> buildGameDetailLinks(List<String> gameTitles) {
        Map<String, String> detailLinks = new HashMap<>();

        if (gameTitles == null) {
            return detailLinks;
        }

        for (String gameTitle : gameTitles) {
            if (gameTitle == null || gameTitle.isBlank()) {
                continue;
            }

            String normalizedTitle = gameTitle.trim();
            BoardGameRank matchingGame = boardGameRankRepository
                    .findFirstByTitleIgnoreCaseOrderByRankPositionAsc(normalizedTitle)
                    .orElse(null);

            if (matchingGame != null && matchingGame.getId() != null) {
                detailLinks.put(
                        normalizedTitle,
                        "/games/id/" + matchingGame.getId()
                );
            } else {
                detailLinks.put(
                        normalizedTitle,
                        "/games/search?q=" + UriUtils.encodeQueryParam(
                                normalizedTitle,
                                StandardCharsets.UTF_8
                        )
                );
            }
        }

        return detailLinks;
    }

    private Map<Integer, Long> buildReviewLikeCounts(List<Review> reviews) {
        Map<Integer, Long> likeCounts = new HashMap<>();

        for (Review review : reviews) {
            if (review == null || review.getId() == null) {
                continue;
            }

            likeCounts.put(review.getId(), reviewLikeRepository.countByReview(review));
        }

        return likeCounts;
    }

    private Set<Integer> findLikedReviewIds(List<Review> reviews, User currentUser) {
        Set<Integer> likedReviewIds = new HashSet<>();

        if (currentUser == null) {
            return likedReviewIds;
        }

        for (Review review : reviews) {
            if (review == null || review.getId() == null) {
                continue;
            }

            if (reviewLikeRepository.existsByReviewAndUser(review, currentUser)) {
                likedReviewIds.add(review.getId());
            }
        }

        return likedReviewIds;
    }

    private Map<Integer, List<ReviewReply>> buildReviewReplies(List<Review> reviews) {
        Map<Integer, List<ReviewReply>> repliesByReview = new HashMap<>();

        for (Review review : reviews) {
            if (review == null || review.getId() == null) {
                continue;
            }

            repliesByReview.put(
                    review.getId(),
                    reviewReplyRepository.findByReviewOrderByCreatedAtAsc(review)
            );
        }

        return repliesByReview;
    }

    private Map<Integer, Long> buildReviewReplyCounts(List<Review> reviews) {
        Map<Integer, Long> replyCounts = new HashMap<>();

        for (Review review : reviews) {
            if (review == null || review.getId() == null) {
                continue;
            }

            replyCounts.put(review.getId(), reviewReplyRepository.countByReview(review));
        }

        return replyCounts;
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

    private Comparator<CollectionRow> buildCollectionComparator(String sort, String dir) {
        Comparator<CollectionRow> comparator;

        switch (sort) {
            case "acquired":
                comparator = Comparator.comparing(
                        CollectionRow::getAcquiredAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                );
                break;
            case "players":
                comparator = Comparator.comparing(
                        CollectionRow::getMinPlayers,
                        Comparator.nullsLast(Comparator.naturalOrder())
                );
                break;
            case "complexity":
                comparator = Comparator.comparing(
                        CollectionRow::getComplexityValue,
                        Comparator.nullsLast(Comparator.naturalOrder())
                );
                break;
            case "time":
                comparator = Comparator.comparing(
                        CollectionRow::getTimeToPlayValue,
                        Comparator.nullsLast(Comparator.naturalOrder())
                );
                break;
            case "lastReview":
                comparator = Comparator.comparing(
                        CollectionRow::getLastReviewDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                );
                break;
            case "avgScore":
                comparator = Comparator.comparing(
                        CollectionRow::getAverageReviewScore,
                        Comparator.nullsLast(Comparator.naturalOrder())
                );
                break;
            case "name":
            default:
                comparator = Comparator.comparing(
                        CollectionRow::getName,
                        String.CASE_INSENSITIVE_ORDER
                );
                break;
        }

        if ("desc".equals(dir)) {
            comparator = comparator.reversed();
        }

        return comparator.thenComparing(CollectionRow::getName, String.CASE_INSENSITIVE_ORDER);
    }

    private String sanitizeSort(String sort) {
        Set<String> allowedSorts = Set.of(
                "name",
                "acquired",
                "players",
                "complexity",
                "time",
                "lastReview",
                "avgScore"
        );

        return allowedSorts.contains(sort) ? sort : "name";
    }

    private String sanitizeDirection(String direction) {
        return "desc".equalsIgnoreCase(direction) ? "desc" : "asc";
    }

    private String normalizeGameName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        return name.trim().toLowerCase(Locale.ROOT);
    }

    private String coalesceReviewGameTitle(Review review) {
        if (review.getGameTitle() != null && !review.getGameTitle().isBlank()) {
            return review.getGameTitle();
        }

        if (review.getGame() != null) {
            return review.getGame().getTitle();
        }

        return "";
    }

    private Integer resolveSortablePlaytime(BoardGameRank game) {
        if (game == null) {
            return null;
        }

        if (game.getCommunityMaxPlaytime() != null && game.getCommunityMaxPlaytime() > 0) {
            return game.getCommunityMaxPlaytime();
        }

        if (game.getCommunityMinPlaytime() != null && game.getCommunityMinPlaytime() > 0) {
            return game.getCommunityMinPlaytime();
        }

        if (game.getManufacturerPlaytime() != null && game.getManufacturerPlaytime() > 0) {
            return game.getManufacturerPlaytime();
        }

        return null;
    }

    private int defaultIfNull(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private double defaultIfNull(Double value, double fallback) {
        return value == null ? fallback : value;
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String safeDisplay(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private static class CollectionReviewStats {
        private int reviewCount;
        private int ratingTotal;
        private LocalDateTime lastReviewDate;
    }

    public static class CollectionRow {
        private final Integer itemId;
        private final String name;
        private final LocalDateTime acquiredAt;
        private final CollectionType collectionType;
        private final Integer minPlayers;
        private final Integer maxPlayers;
        private final Double complexityValue;
        private final Integer timeToPlayValue;
        private final Double averageReviewScore;
        private final LocalDateTime lastReviewDate;
        private final String playersDisplay;
        private final String complexityDisplay;
        private final String timeToPlayDisplay;

        public CollectionRow(
            Integer itemId,
                String name,
                LocalDateTime acquiredAt,
                CollectionType collectionType,
                Integer minPlayers,
                Integer maxPlayers,
                Double complexityValue,
                Integer timeToPlayValue,
                Double averageReviewScore,
                LocalDateTime lastReviewDate,
                String playersDisplay,
                String complexityDisplay,
                String timeToPlayDisplay
        ) {
            this.itemId = itemId;
            this.name = name;
            this.acquiredAt = acquiredAt;
            this.collectionType = collectionType;
            this.minPlayers = minPlayers;
            this.maxPlayers = maxPlayers;
            this.complexityValue = complexityValue;
            this.timeToPlayValue = timeToPlayValue;
            this.averageReviewScore = averageReviewScore;
            this.lastReviewDate = lastReviewDate;
            this.playersDisplay = playersDisplay;
            this.complexityDisplay = complexityDisplay;
            this.timeToPlayDisplay = timeToPlayDisplay;
        }

        public Integer getItemId() {
            return itemId;
        }

        public String getName() {
            return name;
        }

        public LocalDateTime getAcquiredAt() {
            return acquiredAt;
        }

        public CollectionType getCollectionType() {
            return collectionType;
        }

        public Integer getMinPlayers() {
            return minPlayers;
        }

        public Integer getMaxPlayers() {
            return maxPlayers;
        }

        public Double getComplexityValue() {
            return complexityValue;
        }

        public Integer getTimeToPlayValue() {
            return timeToPlayValue;
        }

        public Double getAverageReviewScore() {
            return averageReviewScore;
        }

        public LocalDateTime getLastReviewDate() {
            return lastReviewDate;
        }

        public String getPlayersDisplay() {
            return playersDisplay;
        }

        public String getComplexityDisplay() {
            return complexityDisplay;
        }

        public String getTimeToPlayDisplay() {
            return timeToPlayDisplay;
        }

        public boolean matchesPlayerRange(int selectedMinPlayers, int selectedMaxPlayers) {
            if (minPlayers == null && maxPlayers == null) {
                return false;
            }

            int gameMin = minPlayers == null ? selectedMinPlayers : minPlayers;
            int gameMax = maxPlayers == null ? selectedMaxPlayers : maxPlayers;

            if (gameMax < gameMin) {
                int temp = gameMin;
                gameMin = gameMax;
                gameMax = temp;
            }

            return gameMax >= selectedMinPlayers
                    && gameMin <= selectedMaxPlayers;
        }
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