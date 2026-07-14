package com.cardboardboxed.demo.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cardboardboxed.demo.boardgames.BoardGameAutocompleteRepository;

@RestController
@RequestMapping("/api/boardgames")
public class BoardGameApiController {

    private final BoardGameAutocompleteRepository boardGameAutocompleteRepository;

    public BoardGameApiController(BoardGameAutocompleteRepository boardGameAutocompleteRepository) {
        this.boardGameAutocompleteRepository = boardGameAutocompleteRepository;
    }

    @GetMapping("/autocomplete")
    public List<String> autocomplete(@RequestParam("q") String query) {
        return boardGameAutocompleteRepository.findByPrefix(query, 8);
    }

    @GetMapping("/resolve")
    public Map<String, Object> resolve(@RequestParam("q") String query) {
        Map<String, Object> response = new HashMap<>();
        String resolvedName = boardGameAutocompleteRepository.resolveToExistingName(query).orElse("");
        boolean exactMatch = !resolvedName.isEmpty() && resolvedName.equalsIgnoreCase(query.trim());
        response.put("resolvedName", resolvedName);
        response.put("exactMatch", exactMatch);
        return response;
    }
}
