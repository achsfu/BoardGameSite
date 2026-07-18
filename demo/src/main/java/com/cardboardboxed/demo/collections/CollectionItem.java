package com.cardboardboxed.demo.collections;
import com.cardboardboxed.demo.useracounts.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/*
 * Collection item:
 *    - Connects one user to one board game
 *    - Stores the game as OWNED or WISHLIST
 *    - Prevents duplicate games for the same user
 *    - Records when the game was added
 */

//user's collection
@Entity
@Table(
        name = "collection_items",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "game_name"}
        )
)
public class CollectionItem{
    public enum CollectionType{
        OWNED, 
        WISHLIST;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "game_name", nullable = false)
    private String gameName;

    @Enumerated(EnumType.STRING)
    @Column(name = "collection_type", nullable = false)
    private CollectionType collectionType;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    public CollectionItem(){
    }

    public CollectionItem(
            User user,
            String gameName,
            CollectionType collectionType
    ){
        this.user = user;
        this.gameName = gameName;
        this.collectionType = collectionType;
        this.addedAt = LocalDateTime.now();
    }
    //Ensures addedAt is set before the item is first saved
    @PrePersist
    private void setAddedAt(){
        if(addedAt == null){
            addedAt = LocalDateTime.now();
        }
    }
    //Getter & Setter
    public Integer getId(){
        return id;
    }

    public User getUser(){
        return user;
    }

    public void setUser(User user){
        this.user = user;
    }

    public String getGameName(){
        return gameName;
    }

    public void setGameName(String gameName){
        this.gameName = gameName;
    }

    public CollectionType getCollectionType(){
        return collectionType;
    }

    public void setCollectionType(CollectionType collectionType){
        this.collectionType = collectionType;
    }

    public LocalDateTime getAddedAt(){
        return addedAt;
    }
}