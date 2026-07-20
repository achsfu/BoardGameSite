package com.cardboardboxed.demo.profile;

import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.collections.CollectionItem;
import com.cardboardboxed.demo.reviews.Review;

/*
 * View model used by the profile templates.
 *
 * Combines:
 *   - the user's collection entry
 *   - the matching BoardGameGeek catalog entry
 *   - the user's latest review for that game
 */
public class ProfileGameView {

    private final CollectionItem collectionItem;
    private final BoardGameRank game;
    private final Review latestReview;

    public ProfileGameView(
            CollectionItem collectionItem,
            BoardGameRank game,
            Review latestReview
    ) {
        this.collectionItem = collectionItem;
        this.game = game;
        this.latestReview = latestReview;
    }

    public CollectionItem getCollectionItem() {
        return collectionItem;
    }

    public BoardGameRank getGame() {
        return game;
    }

    public Review getLatestReview() {
        return latestReview;
    }

    public Integer getCollectionItemId() {
        if (collectionItem == null) {
            return null;
        }

        return collectionItem.getId();
    }

    public String getGameName() {
        if (game != null
                && game.getTitle() != null
                && !game.getTitle().isBlank()) {
            return game.getTitle();
        }

        if (collectionItem != null
                && collectionItem.getGameName() != null
                && !collectionItem.getGameName().isBlank()) {
            return collectionItem.getGameName();
        }

        return "Unknown game";
    }

    public Integer getGameId() {
        if (game == null) {
            return null;
        }

        return game.getId();
    }

    public boolean hasCatalogGame() {
        return game != null;
    }

    public boolean hasImage() {
        return game != null && game.hasImage();
    }

    public String getImageUrl() {
        if (!hasImage()) {
            return null;
        }

        return game.getPreferredImageUrl();
    }

    /*
     * BoardGameRank currently does not map a publication-year column.
     * Returning null allows the Thymeleaf templates to hide the year.
     */
    public Integer getYearPublished() {
        return null;
    }

    public Integer getRankPosition() {
        if (game == null) {
            return null;
        }

        return game.getRankPosition();
    }

    public Double getCommunityScore() {
        if (game == null) {
            return null;
        }

        return game.getCommunityScore();
    }

    public Integer getUserRating() {
        if (latestReview == null) {
            return null;
        }

        return latestReview.getRating();
    }

    public boolean hasUserReview() {
        return latestReview != null;
    }

    public CollectionItem.CollectionType getCollectionType() {
        if (collectionItem == null) {
            return null;
        }

        return collectionItem.getCollectionType();
    }

    public boolean isOwned() {
        return getCollectionType()
                == CollectionItem.CollectionType.OWNED;
    }

    public boolean isWishlist() {
        return getCollectionType()
                == CollectionItem.CollectionType.WISHLIST;
    }
}