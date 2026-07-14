package com.cardboardboxed.demo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import com.cardboardboxed.demo.boardgames.BoardGameAutocompleteRepository;
import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.boardgames.BoardGameRankRepository;

@SpringBootTest
@Transactional
class DemoApplicationTests {

    @Autowired
    private BoardGameRankRepository boardGameRankRepository;

    @Autowired
    private BoardGameAutocompleteRepository boardGameAutocompleteRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void testSearchAndAutocompleteFilterExpansions() {
        var allGames = boardGameRankRepository.findAll();
        System.out.println("DEBUG: Games containing 'expansion' but is_expansion = FALSE:");
        allGames.stream()
            .filter(g -> g.getTitle().toLowerCase().contains("expansion") && Boolean.FALSE.equals(g.getIsExpansion()))
            .limit(20)
            .forEach(g -> System.out.println(" - " + g.getTitle()));
    }
}
