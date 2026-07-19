package com.cardboardboxed.demo.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cardboardboxed.demo.boardgames.BoardGameCsvImportService;
import com.cardboardboxed.demo.boardgames.BoardGameCsvImportService.ImportResult;

@RestController
public class BggImportController {

    private final BoardGameCsvImportService importService;

    public BggImportController(
            BoardGameCsvImportService importService) {
        this.importService = importService;
    }

    @GetMapping("/admin/import-game-images")
    public ResponseEntity<String> importImages() throws Exception {
        ImportResult result = importService.importImages();

        String response = """
                CSV import completed.

                CSV rows inspected: %d
                Database games matched: %d
                Games updated: %d
                CSV rows without images: %d
                CSV games not in your database: %d
                """.formatted(
                    result.rowsInspected(),
                    result.matched(),
                    result.updated(),
                    result.missingImages(),
                    result.notInDatabase()
                );

        return ResponseEntity.ok(response);
    }
}