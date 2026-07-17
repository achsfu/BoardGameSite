package com.cardboardboxed.demo.collections;

import com.cardboardboxed.demo.collections.CollectionItem.CollectionType;
import com.cardboardboxed.demo.useracounts.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/*
 * Collection repository:
 *    - Saves & deletes items
 *    - Finds a user's OWNED or WISHLIST games
 *    - Checks whether a game is already in the user's collection
 *    - Finds an item only when it belongs to the correct user
 */
public interface CollectionItemRepository
        extends JpaRepository<CollectionItem, Integer>{

    List<CollectionItem> findByUserAndCollectionTypeOrderByAddedAtDesc(
            User user,
            CollectionType collectionType
    );

    Optional<CollectionItem> findByUserAndGameNameIgnoreCase(
            User user,
            String gameName
    );

    Optional<CollectionItem> findByIdAndUser(
            Integer id,
            User user
    );
}