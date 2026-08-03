package com.cardboardboxed.demo.reviews;

import com.cardboardboxed.demo.useracounts.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_replies")
public class ReviewReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reply_text", length = 1000, nullable = false)
    private String replyText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ReviewReply() {
    }

    public ReviewReply(
            Review review,
            User user,
            String replyText
    ) {
        this.review = review;
        this.user = user;
        this.replyText = replyText;
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

    public Review getReview() {
        return review;
    }

    public void setReview(Review review) {
        this.review = review;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getReplyText() {
        return replyText;
    }

    public void setReplyText(String replyText) {
        this.replyText = replyText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}