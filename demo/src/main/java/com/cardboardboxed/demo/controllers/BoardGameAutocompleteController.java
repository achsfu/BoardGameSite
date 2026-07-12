package com.cardboardboxed.demo.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cardboardboxed.demo.boardgames.BoardGameAutocompleteRepository;

@RestController
public class BoardGameAutocompleteController {

    private final BoardGameAutocompleteRepository autocompleteRepository;

    public BoardGameAutocompleteController(BoardGameAutocompleteRepository autocompleteRepository) {
        this.autocompleteRepository = autocompleteRepository;
    }

    @GetMapping("/api/boardgames/autocomplete")
    public List<String> autocomplete(@RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "limit", defaultValue = "8") int limit) {
        return autocompleteRepository.findByPrefix(query, limit);
    }

    @GetMapping("/api/boardgames/resolve")
    public ResolveResponse resolve(@RequestParam(name = "q", defaultValue = "") String query) {
        Optional<String> resolved = autocompleteRepository.resolveToExistingName(query);
        boolean exactMatch = resolved.isPresent() && query != null && resolved.get().equalsIgnoreCase(query.trim());
        return new ResolveResponse(resolved.orElse(""), exactMatch);
    }

    public record ResolveResponse(String resolvedName, boolean exactMatch) {
    }
}
