package com.cardboardboxed.demo.boardgames;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardGameCsvImportService {

    private final BoardGameRankRepository boardGameRankRepository;

    public BoardGameCsvImportService(
            BoardGameRankRepository boardGameRankRepository) {
        this.boardGameRankRepository = boardGameRankRepository;
    }

    @Transactional
    public ImportResult importImages() throws Exception {

        List<BoardGameRank> databaseGames =
                boardGameRankRepository.findAll();

        Map<Integer, BoardGameRank> gamesByBggId =
                new HashMap<>();

        for (BoardGameRank game : databaseGames) {
            if (game.getBggId() != null) {
                gamesByBggId.put(game.getBggId(), game);
            }
        }

        int rowsInspected = 0;
        int matched = 0;
        int updated = 0;
        int missingImages = 0;
        int notInDatabase = 0;

        List<BoardGameRank> gamesToSave =
                new ArrayList<>();

        ClassPathResource resource =
                new ClassPathResource("data/games.csv");

        try (
            Reader reader = new InputStreamReader(
                    resource.getInputStream(),
                    StandardCharsets.UTF_8
            );

            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreSurroundingSpaces(true)
                    .setTrim(true)
                    .get()
                    .parse(reader)
        ) {

            for (CSVRecord record : parser) {
                rowsInspected++;

                String bggIdText = record.get("BGGId");
                String imagePath = record.get("ImagePath");

                if (bggIdText == null || bggIdText.isBlank()) {
                    continue;
                }

                Integer bggId;

                try {
                    bggId = Integer.parseInt(
                            bggIdText.trim()
                    );
                } catch (NumberFormatException exception) {
                    continue;
                }

                BoardGameRank game =
                        gamesByBggId.get(bggId);

                if (game == null) {
                    notInDatabase++;
                    continue;
                }

                matched++;

                if (imagePath == null ||
                        imagePath.isBlank()) {
                    missingImages++;
                    continue;
                }

                String normalizedImageUrl =
                        normalizeImageUrl(imagePath);

                game.setImageUrl(normalizedImageUrl);
                game.setThumbnailUrl(normalizedImageUrl);

                gamesToSave.add(game);
                updated++;
            }
        }

        boardGameRankRepository.saveAll(gamesToSave);

        return new ImportResult(
                rowsInspected,
                matched,
                updated,
                missingImages,
                notInDatabase
        );
    }

    private String normalizeImageUrl(String imageUrl) {
        String cleaned = imageUrl.trim();

        if (cleaned.startsWith("//")) {
            return "https:" + cleaned;
        }

        return cleaned;
    }

    public record ImportResult(
            int rowsInspected,
            int matched,
            int updated,
            int missingImages,
            int notInDatabase
    ) {
    }
}