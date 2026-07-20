package com.cardboardboxed.demo.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.collections.CollectionItem;
import com.cardboardboxed.demo.collections.CollectionItem.CollectionType;
import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.useracounts.User;

class ProfileGameViewTest {

    @Test
    void profileGameViewStoresCollectionItem() {

        User user = new User();
        user.setUsername("alice");

        CollectionItem item = new CollectionItem(
                user,
                "Catan",
                CollectionType.OWNED
        );

        BoardGameRank game = new BoardGameRank();
        game.setId(15);
        game.setTitle("Catan");
        game.setRankPosition(300);
        game.setCommunityScore(7.3);

        Review review = new Review();
        review.setRating(5);

        ProfileGameView view = new ProfileGameView(
                item,
                game,
                review
        );

        assertEquals("Catan", view.getGameName());
        assertEquals(15, view.getGameId());
        assertEquals(300, view.getRankPosition());
        assertEquals(7.3, view.getCommunityScore());
        assertEquals(5, view.getUserRating());

        /*
         * BoardGameRank currently does not contain a publication-year field,
         * so the view should return null.
         */
        assertNull(view.getYearPublished());

        assertTrue(view.isOwned());
        assertFalse(view.isWishlist());
    }

    @Test
    void wishlistCollectionTypeWorks() {

        User user = new User();
        user.setUsername("bob");

        CollectionItem item = new CollectionItem(
                user,
                "Root",
                CollectionType.WISHLIST
        );

        ProfileGameView view = new ProfileGameView(
                item,
                null,
                null
        );

        assertTrue(view.isWishlist());
        assertFalse(view.isOwned());
    }

    @Test
    void missingGameMetadataHandledGracefully() {

        User user = new User();

        CollectionItem item = new CollectionItem(
                user,
                "Unknown Game",
                CollectionType.OWNED
        );

        ProfileGameView view = new ProfileGameView(
                item,
                null,
                null
        );

        assertNull(view.getGameId());
        assertNull(view.getImageUrl());
        assertNull(view.getCommunityScore());
        assertNull(view.getRankPosition());
        assertNull(view.getYearPublished());
    }

    @Test
    void reviewRatingReturnedCorrectly() {

        User user = new User();

        CollectionItem item = new CollectionItem(
                user,
                "Terraforming Mars",
                CollectionType.OWNED
        );

        Review review = new Review();
        review.setRating(4);

        ProfileGameView view = new ProfileGameView(
                item,
                null,
                review
        );

        assertEquals(4, view.getUserRating());
        assertTrue(view.hasUserReview());
    }

    @Test
    void noReviewReturnsNullRating() {

        User user = new User();

        CollectionItem item = new CollectionItem(
                user,
                "Pandemic",
                CollectionType.OWNED
        );

        ProfileGameView view = new ProfileGameView(
                item,
                null,
                null
        );

        assertNull(view.getUserRating());
        assertFalse(view.hasUserReview());
    }

    @Test
    void catalogGamePropertiesReturned() {

        User user = new User();

        CollectionItem item = new CollectionItem(
                user,
                "Brass Birmingham",
                CollectionType.OWNED
        );

        BoardGameRank game = new BoardGameRank();
        game.setId(999);
        game.setTitle("Brass Birmingham");
        game.setRankPosition(1);
        game.setCommunityScore(8.8);
        game.setImageUrl("https://example.com/brass.jpg");

        ProfileGameView view = new ProfileGameView(
                item,
                game,
                null
        );

        assertEquals(999, view.getGameId());
        assertEquals(1, view.getRankPosition());
        assertEquals(8.8, view.getCommunityScore());
        assertEquals(
                "https://example.com/brass.jpg",
                view.getImageUrl()
        );
        assertNull(view.getYearPublished());
        assertTrue(view.hasCatalogGame());
        assertTrue(view.hasImage());
    }

    @Test
    void collectionItemNameUsedWhenCatalogGameIsMissing() {

        User user = new User();

        CollectionItem item = new CollectionItem(
                user,
                "Azul",
                CollectionType.OWNED
        );

        ProfileGameView view = new ProfileGameView(
                item,
                null,
                null
        );

        assertEquals("Azul", view.getGameName());
    }
}