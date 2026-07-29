package com.cardboardboxed.demo.boardgames;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BoardGameAutocompleteRepository {

    private final JdbcTemplate jdbcTemplate;

    public BoardGameAutocompleteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findByPrefix(String prefix, int limit) {
        if (prefix == null || prefix.isBlank()) {
            return Collections.emptyList();
        }

        String term = prefix.trim();
        if (term.length() < 2) {
            return Collections.emptyList();
        }

        int safeLimit = Math.max(1, Math.min(limit, 8));

        String sql = """
                SELECT "Name"
                FROM board_games
            WHERE "Name" ILIKE ?
                ORDER BY
                    CASE WHEN "Name" ILIKE ? THEN 0 ELSE 1 END,
                    CASE WHEN "Rank:boardgame" = 0 THEN 1 ELSE 0 END,
                    "Rank:boardgame" ASC NULLS LAST,
                    "Name" ASC
                LIMIT ?
                """;

        try {
            String searchTerm = term + "%";
            return jdbcTemplate.queryForList(sql, String.class, searchTerm, searchTerm, safeLimit);
        } catch (DataAccessException ex) {
            return Collections.emptyList();
        }
    }

    public Optional<String> resolveToExistingName(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        String term = input.trim();

        String exactSql = """
                SELECT "Name"
                FROM (
                    SELECT "Name", MIN("Rank:boardgame") AS best_rank
                    FROM board_games
                    WHERE lower("Name") = lower(?)
                    GROUP BY "Name"
                ) exact_match
                ORDER BY
                    CASE WHEN best_rank = 0 THEN 1 ELSE 0 END,
                    best_rank ASC NULLS LAST,
                    "Name" ASC
                LIMIT 1
                """;

        String nextAlphabeticalSql = """
                SELECT "Name"
                FROM (
                    SELECT "Name", MIN("Rank:boardgame") AS best_rank
                    FROM board_games
                    WHERE lower("Name") >= lower(?)
                    GROUP BY "Name"
                ) next_match
                ORDER BY
                    lower("Name") ASC,
                    CASE WHEN best_rank = 0 THEN 1 ELSE 0 END,
                    best_rank ASC NULLS LAST,
                    "Name" ASC
                LIMIT 1
                """;

        String previousAlphabeticalSql = """
                SELECT "Name"
                FROM (
                    SELECT "Name", MIN("Rank:boardgame") AS best_rank
                    FROM board_games
                    WHERE lower("Name") < lower(?)
                    GROUP BY "Name"
                ) previous_match
                ORDER BY
                    lower("Name") DESC,
                    CASE WHEN best_rank = 0 THEN 1 ELSE 0 END,
                    best_rank ASC NULLS LAST,
                    "Name" ASC
                LIMIT 1
                """;

        try {
            List<String> exact = jdbcTemplate.queryForList(exactSql, String.class, term);
            if (!exact.isEmpty()) {
                return Optional.of(exact.get(0));
            }

            List<String> next = jdbcTemplate.queryForList(nextAlphabeticalSql, String.class, term);
            if (!next.isEmpty()) {
                return Optional.of(next.get(0));
            }

            List<String> previous = jdbcTemplate.queryForList(previousAlphabeticalSql, String.class, term);
            if (!previous.isEmpty()) {
                return Optional.of(previous.get(0));
            }

            return Optional.empty();
        } catch (DataAccessException ex) {
            return Optional.empty();
        }
    }
}
