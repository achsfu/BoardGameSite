package com.cardboardboxed.demo.controllers;

import com.cardboardboxed.demo.boardgames.BoardGameAutocompleteRepository;
import com.cardboardboxed.demo.collections.CollectionItem;
import com.cardboardboxed.demo.collections.CollectionItem.CollectionType;
import com.cardboardboxed.demo.collections.CollectionItemRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CollectionController{

    private final CollectionItemRepository collectionItemRepository;
    private final UserRepository userRepository;
    private final BoardGameAutocompleteRepository boardGameAutocompleteRepository;

    public CollectionController(
            CollectionItemRepository collectionItemRepository,
            UserRepository userRepository,
            BoardGameAutocompleteRepository boardGameAutocompleteRepository
    ){
        this.collectionItemRepository = collectionItemRepository;
        this.userRepository = userRepository;
        this.boardGameAutocompleteRepository = boardGameAutocompleteRepository;
    }
    //Adds a game or moves
    @PostMapping("/collection/add")
    public String addGame(
            @RequestParam String gameName,
            @RequestParam CollectionType collectionType,
            HttpServletRequest request
    ){
        HttpSession session = request.getSession(false);
        // User must be logged in
        if(session == null || session.getAttribute("AUTH_USER") == null)
            return "redirect:/login";
        

        String username = (String) session.getAttribute("AUTH_USER");
        User user = userRepository.findByUsername(username);
        
        String resolvedName = boardGameAutocompleteRepository.resolveToExistingName(gameName).orElse(null);
        // Match with an existing game
        if(resolvedName == null || resolvedName.isBlank())
            return "redirect:/profile?error=Game+not+found";
        

        CollectionItem item = collectionItemRepository.findByUserAndGameNameIgnoreCase(user, resolvedName).orElse(null);

        if(item == null)
            item = new CollectionItem(user, resolvedName, collectionType); 
        else
            item.setCollectionType(collectionType);
        

        collectionItemRepository.save(item);

        return "redirect:/profile?success=Collection+updated";
    }
    // Removes a game 
    @PostMapping("/collection/remove")
    public String removeGame(
            @RequestParam Integer id,
            HttpServletRequest request
    ){
        HttpSession session = request.getSession(false);

        if(session == null || session.getAttribute("AUTH_USER") == null)
            return "redirect:/login";
        

        String username = (String) session.getAttribute("AUTH_USER");
        User user = userRepository.findByUsername(username);

        collectionItemRepository.findByIdAndUser(id, user).ifPresent(collectionItemRepository::delete);
        return "redirect:/profile?success=Game+removed";
    }

    // Moves a game between Owned and Wishlist
    @PostMapping("/collection/move")
    public String moveGame(
            @RequestParam Integer id,
            @RequestParam CollectionType collectionType,
            HttpServletRequest request
    ){
        HttpSession session = request.getSession(false);

        if(session == null || session.getAttribute("AUTH_USER") == null)
            return "redirect:/login";
        

        String username = (String) session.getAttribute("AUTH_USER");
        User user = userRepository.findByUsername(username);

        CollectionItem item = collectionItemRepository
                .findByIdAndUser(id, user)
                .orElse(null);

        if(item != null){
            item.setCollectionType(collectionType);
            collectionItemRepository.save(item);
        }

        return "redirect:/profile?success=Collection+updated";
    }
}