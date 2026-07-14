package com.cardboardboxed.demo.boardgames;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardGameRankRepository extends JpaRepository<BoardGameRank, Integer> {

    BoardGameRank findByTitleIgnoreCase(String title);

    List<BoardGameRank> findAllByOrderByRankPositionAsc();

    List<BoardGameRank> findTop12ByTitleContainingIgnoreCaseOrderByRankPositionAsc(String title);
}