package com.cardboardboxed.demo.boardgames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BoardGameCsvImportServiceTest {

    @Test
    void thumbnailFallsBackToImagePath() {
        BoardGameRank game = new BoardGameRank();

        game.setImageUrl("https://example.com/game.jpg");

        assertEquals("https://example.com/game.jpg", game.getThumbnailUrl());
        assertEquals("https://example.com/game.jpg", game.getPreferredImageUrl());
        assertTrue(game.hasImage());
    }

    @Test
    void thumbnailSetterOnlyBackfillsWhenImageMissing() {
        BoardGameRank game = new BoardGameRank();

        game.setThumbnailUrl("https://example.com/thumb.jpg");
        assertEquals("https://example.com/thumb.jpg", game.getImageUrl());

        game.setImageUrl("https://example.com/full.jpg");
        game.setThumbnailUrl("https://example.com/ignored-thumb.jpg");

        assertEquals("https://example.com/full.jpg", game.getImageUrl());
        assertEquals("https://example.com/full.jpg", game.getThumbnailUrl());
    }

    @Test
    void displayDescriptionInsertsReadableSections() {
        BoardGameRank game = new BoardGameRank();
        game.setDescription("themeplayer buys things gameplayroll dice goalwin game backgroundold classic");

        String formatted = game.getDisplayDescription();

        assertTrue(formatted.contains("Theme: player buys things"));
        assertTrue(formatted.contains("Gameplay: roll dice"));
        assertTrue(formatted.contains("Goal: win game"));
        assertTrue(formatted.contains("Background: old classic"));
    }

    @Test
    void hasImageIsFalseWhenNoImageDataExists() {
        BoardGameRank game = new BoardGameRank();

        assertFalse(game.hasImage());
    }
}