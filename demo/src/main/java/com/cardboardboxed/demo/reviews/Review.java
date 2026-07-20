package com.cardboardboxed.demo.reviews;
import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.useracounts.User;
import jakarta.persistence.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDateTime;


@Entity
@Table(name = "reviews")
public class Review {
    
    //associated fields
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "game_title")
    private String gameTitle;
    private Integer rating;

    @Column(length = 2000)
    private String reviewText;
    private LocalDateTime createdAt;

    //define a database relationship where multiple entity records associate
    //with a single record in another identity
    @ManyToOne
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private BoardGameRank game;

    //simple getter and setter methods
    public Review(){
        this.createdAt = LocalDateTime.now();
    }
    public Integer getId(){
        return id;
    }
    public String getGameTitle(){
        return gameTitle;
    }
    public void setGameTitle(String gameTitle){
        this.gameTitle = gameTitle;
    }
    public BoardGameRank getGame(){
        return game;
    }
    public void setGame(BoardGameRank game){
        this.game = game;
        if (game != null) {
            this.gameTitle = game.getTitle();
        }
    }
    public Integer getRating(){
        return rating;
    }
    public void setRating(Integer rating){
        this.rating = rating;
    }
    public String getReviewText(){
        return reviewText;
    }
    public void setReviewText(String reviewText){
        this.reviewText = reviewText;
    }
    public LocalDateTime getCreatedAt(){
        return createdAt;
    }
    public User getUser(){
        return user;
    }
    public void setUser(User user){
        this.user = user;
    }
}
