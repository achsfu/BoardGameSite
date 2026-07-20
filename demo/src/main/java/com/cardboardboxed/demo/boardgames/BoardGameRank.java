package com.cardboardboxed.demo.boardgames;

import com.cardboardboxed.demo.reviews.Review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.List;

@Entity
@Table(name = "board_games")
public class BoardGameRank {

    @Id
    @Column(name = "\"BGGId\"")
    private Integer id;

    @Column(name = "\"Name\"", nullable = false)
    private String title;

    @Column(name = "\"Rank:boardgame\"")
    private Integer rankPosition;

    @Column(name = "\"AvgRating\"")
    private Double communityScore;

    @Column(name = "\"Description\"", length = 10000)
    private String description;

    @Column(name = "\"ImagePath\"", length = 1000)
    private String imageUrl;

    @OneToMany(mappedBy = "game")
    private List<Review> reviews;

    @Transient
    private Boolean isExpansion;

    public BoardGameRank() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getBggId() {
        return id;
    }

    public void setBggId(Integer bggId) {
        this.id = bggId;
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

    public String getDisplayDescription() {
        if (description == null || description.isBlank()) {
            return description;
        }

        String normalized = description
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("\\s+", " ")
                .trim();

        normalized = insertSection(normalized, "theme", "Theme");
        normalized = insertSection(normalized, "gameplay", "Gameplay");
        normalized = insertSection(normalized, "goal", "Goal");
        normalized = insertSection(normalized, "cultural impact rules", "Cultural Impact And Rules");
        normalized = insertSection(normalized, "background", "Background");
        normalized = insertSection(normalized, "reimplement", "Reimplements");
        normalized = insertSection(normalized, "expande by", "Expanded By");
        normalized = insertSection(normalized, "expanded by", "Expanded By");

        return normalized.trim();
    }

    public String getThumbnailUrl() {
        return imageUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        if ((imageUrl == null || imageUrl.isBlank()) && thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            this.imageUrl = thumbnailUrl;
        }
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getIsExpansion() {
        return false;
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

    private String insertSection(String source, String token, String label) {
        return source.replaceFirst(
                "(?i)" + java.util.regex.Pattern.quote(token),
                java.util.regex.Matcher.quoteReplacement("\n\n" + label + ": ")
        );
    }
}