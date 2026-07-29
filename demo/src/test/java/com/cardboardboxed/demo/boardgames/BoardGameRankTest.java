package com.cardboardboxed.demo.boardgames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BoardGameRankTest {

    @Test
    void playerCountDisplayShowsRangeWhenMinimumAndMaximumDiffer() {
        BoardGameRank game = new BoardGameRank();

        game.setMinPlayers(2);
        game.setMaxPlayers(4);

        assertEquals("2–4 players", game.getPlayerCountDisplay());
    }

    @Test
    void playerCountDisplayShowsSingleValueWhenMinimumAndMaximumMatch() {
        BoardGameRank game = new BoardGameRank();

        game.setMinPlayers(2);
        game.setMaxPlayers(2);

        assertEquals("2 players", game.getPlayerCountDisplay());
    }

    @Test
    void playerCountDisplayReturnsNullWhenMinimumPlayersIsMissing() {
        BoardGameRank game = new BoardGameRank();

        game.setMaxPlayers(4);

        assertNull(game.getPlayerCountDisplay());
    }

    @Test
    void playtimeDisplayUsesCommunityRangeWhenAvailable() {
        BoardGameRank game = new BoardGameRank();

        game.setCommunityMinPlaytime(60);
        game.setCommunityMaxPlaytime(90);
        game.setManufacturerPlaytime(120);

        assertEquals("60–90 min", game.getPlaytimeDisplay());
    }

    @Test
    void playtimeDisplayShowsSingleCommunityValueWhenTimesMatch() {
        BoardGameRank game = new BoardGameRank();

        game.setCommunityMinPlaytime(45);
        game.setCommunityMaxPlaytime(45);

        assertEquals("45 min", game.getPlaytimeDisplay());
    }

    @Test
    void playtimeDisplayFallsBackToManufacturerPlaytime() {
        BoardGameRank game = new BoardGameRank();

        game.setManufacturerPlaytime(75);

        assertEquals("75 min", game.getPlaytimeDisplay());
    }

    @Test
    void playtimeDisplayReturnsNullWhenNoValidTimeExists() {
        BoardGameRank game = new BoardGameRank();

        assertNull(game.getPlaytimeDisplay());
    }

    @Test
    void complexityDisplayFormatsWeightToTwoDecimalPlaces() {
        BoardGameRank game = new BoardGameRank();

        game.setGameWeight(3.257);

        assertEquals("3.26 / 5", game.getComplexityDisplay());
    }

    @Test
    void complexityDisplayReturnsNullWhenWeightIsMissing() {
        BoardGameRank game = new BoardGameRank();

        assertNull(game.getComplexityDisplay());
    }

    @Test
    void preferredImageUrlReturnsImageUrl() {
        BoardGameRank game = new BoardGameRank();

        game.setImageUrl("https://example.com/game.jpg");

        assertTrue(game.hasImage());
        assertEquals(
            "https://example.com/game.jpg",
            game.getPreferredImageUrl()
        );
    }

    @Test
    void hasImageReturnsFalseWhenImageIsMissing() {
        BoardGameRank game = new BoardGameRank();

        assertFalse(game.hasImage());
    }

    @Test
    void displayDescriptionReturnsNullWhenDescriptionIsNull() {
        BoardGameRank game = new BoardGameRank();

        assertNull(game.getDisplayDescription());
    }

    @Test
    void displayDescriptionNormalizesExtraWhitespace() {
        BoardGameRank game = new BoardGameRank();

        game.setDescription(
            "A strategy game   with multiple\n\nspaces."
        );

        assertEquals(
            "A strategy game with multiple spaces.",
            game.getDisplayDescription()
        );
    }
}