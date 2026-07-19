package com.cardboardboxed.demo.boardgames;

import com.cardboardboxed.demo.reviews.Review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "boardgames_ranks")
public class BoardGameRank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /*
     * BoardGameGeek's unique identifier.
     *
     * This is used when requesting game information,
     * thumbnails and full-sized images from the BGG API.
     */
    @Column(name = "bgg_id", unique = true)
    private Integer bggId;

    @Column(name = "name", unique = true, nullable = false)
    private String title;

    @Column(name = "rank")
    private Integer rankPosition;

    @Column(name = "average")
    private Double communityScore;

    @Column(length = 10000)
    private String description;

    /*
     * Smaller BoardGameGeek image.
     *
     * This can be used when loading compact poster cards.
     */
    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    /*
     * Full-sized BoardGameGeek image.
     *
     * This is the preferred image for game pages and
     * larger homepage posters.
     */
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "is_expansion")
    private Boolean isExpansion;

    @OneToMany(mappedBy = "game")
    private List<Review> reviews;

    public BoardGameRank() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getBggId() {
        return bggId;
    }

    public void setBggId(Integer bggId) {
        this.bggId = bggId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getRankPosition() {
        return rankPosition;
    }

    public void setRankPosition(Integer rankPosition) {
        this.rankPosition = rankPosition;
    }

    public Double getCommunityScore() {
        return communityScore;
    }

    public void setCommunityScore(Double communityScore) {
        this.communityScore = communityScore;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getIsExpansion() {
        return isExpansion;
    }

    public void setIsExpansion(Boolean isExpansion) {
        this.isExpansion = isExpansion;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    /*
     * Returns true when at least one usable image URL
     * has already been stored for this game.
     */
    public boolean hasImage() {
        return isNotBlank(imageUrl) || isNotBlank(thumbnailUrl);
    }

    /*
     * Returns the full-sized image when available,
     * otherwise falls back to the thumbnail.
     */
    public String getPreferredImageUrl() {
        if (isNotBlank(imageUrl)) {
            return imageUrl;
        }

        return thumbnailUrl;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}