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

        int safeLimit = Math.max(1, Math.min(limit, 15));

        String sql = """
                SELECT name
                FROM (
                    SELECT name, MIN("rank") AS best_rank
                    FROM boardgames_ranks
                    WHERE name ILIKE ?
                    GROUP BY name
                ) ranked_games
                ORDER BY
                    CASE WHEN name ILIKE ? THEN 0 ELSE 1 END,
                    best_rank ASC NULLS LAST,
                    name ASC
                LIMIT ?
                """;

        try {
            String term = prefix.trim();
            return jdbcTemplate.queryForList(sql, String.class, "%" + term + "%", term + "%", safeLimit);
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
                SELECT name
                FROM (
                    SELECT name, MIN("rank") AS best_rank
                    FROM boardgames_ranks
                    WHERE lower(name) = lower(?)
                    GROUP BY name
                ) exact_match
                ORDER BY best_rank ASC NULLS LAST, name ASC
                LIMIT 1
                """;

        String nextAlphabeticalSql = """
                SELECT name
                FROM (
                    SELECT name, MIN("rank") AS best_rank
                    FROM boardgames_ranks
                    WHERE lower(name) >= lower(?)
                    GROUP BY name
                ) next_match
                ORDER BY lower(name) ASC, best_rank ASC NULLS LAST, name ASC
                LIMIT 1
                """;

        String previousAlphabeticalSql = """
                SELECT name
                FROM (
                    SELECT name, MIN("rank") AS best_rank
                    FROM boardgames_ranks
                    WHERE lower(name) < lower(?)
                    GROUP BY name
                ) previous_match
                ORDER BY lower(name) DESC, best_rank ASC NULLS LAST, name ASC
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
