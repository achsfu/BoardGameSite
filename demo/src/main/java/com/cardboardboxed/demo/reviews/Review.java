package com.cardboardboxed.demo.reviews;

import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.useracounts.User;

import jakarta.persistence.*;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "game_title")
    private String gameTitle;

    private Integer rating;

    @Column(length = 2000)
    private String reviewText;

    private LocalDateTime createdAt;

    /*
     * The user who created the review.
     */
    @ManyToOne
    private User user;

    /*
     * The board game connected to the review.
     */
    @ManyToOne
    @JoinColumn(name = "game_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private BoardGameRank game;

    /*
     * Likes attached to this review.
     *
     * When a review is deleted, its likes are also deleted.
     */
    @OneToMany(
            mappedBy = "review",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ReviewLike> likes = new ArrayList<>();

    /*
     * Replies attached to this review.
     *
     * When a review is deleted, its replies are also deleted.
     */
    @OneToMany(
            mappedBy = "review",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ReviewReply> replies = new ArrayList<>();

    public Review() {
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    private void setCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public String getGameTitle() {
        return gameTitle;
    }

    public void setGameTitle(String gameTitle) {
        this.gameTitle = gameTitle;
    }

    public BoardGameRank getGame() {
        return game;
    }

    public void setGame(BoardGameRank game) {
        this.game = game;

        if (game != null) {
            this.gameTitle = game.getTitle();
        }
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    /*
     * ==========================
     * Likes
     * ==========================
     */

    public List<ReviewLike> getLikes() {
        return likes;
    }

    public void setLikes(List<ReviewLike> likes) {
        this.likes = likes != null
                ? likes
                : new ArrayList<>();
    }

    public int getLikeCount() {
        return likes != null
                ? likes.size()
                : 0;
    }

    /*
     * ==========================
     * Replies
     * ==========================
     */

    public List<ReviewReply> getReplies() {
        return replies;
    }

    public void setReplies(List<ReviewReply> replies) {
        this.replies = replies != null
                ? replies
                : new ArrayList<>();
    }

    public int getReplyCount() {
        return replies != null
                ? replies.size()
                : 0;
    }
}