package com.cardboardboxed.demo.boardgames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cardboardboxed.demo.boardgames.BoardGameCsvImportService.ImportResult;

@ExtendWith(MockitoExtension.class)
class BoardGameCsvImportServiceTest {

    @Mock
    private BoardGameRankRepository boardGameRankRepository;

    private BoardGameCsvImportService importService;

    @BeforeEach
    void setUp() {
        importService =
                new BoardGameCsvImportService(
                        boardGameRankRepository
                );
    }

    @Test
    void importImagesUpdatesMatchingGameUsingBggId()
            throws Exception {

        BoardGameRank game = new BoardGameRank();

        game.setBggId(1);
        game.setTitle("Die Macher");

        when(boardGameRankRepository.findAll())
                .thenReturn(List.of(game));

        ImportResult result =
                importService.importImages();

        assertTrue(result.rowsInspected() > 0);
        assertEquals(1, result.matched());
        assertEquals(1, result.updated());

        assertEquals(
                "https://cf.geekdo-images.com/"
                + "rpwCZAjYLD940NWwP3SRoA__original/"
                + "img/yR0aoBVKNrAmmCuBeSzQnMflLYg=/"
                + "0x0/filters:format(jpeg)/pic4718279.jpg",
                game.getImageUrl()
        );

        assertEquals(
                game.getImageUrl(),
                game.getThumbnailUrl()
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BoardGameRank>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(boardGameRankRepository)
                .saveAll(captor.capture());

        List<BoardGameRank> savedGames =
                captor.getValue();

        assertEquals(1, savedGames.size());
        assertEquals(game, savedGames.get(0));
    }
}