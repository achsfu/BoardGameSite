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

    @Column(name = "\"YearPublished\"")
    private Integer yearPublished;

    @Column(name = "\"MinPlayers\"")
    private Integer minPlayers;

    @Column(name = "\"MaxPlayers\"")
    private Integer maxPlayers;

    @Column(name = "\"MfgPlaytime\"")
    private Integer manufacturerPlaytime;

    @Column(name = "\"ComMinPlaytime\"")
    private Integer communityMinPlaytime;

    @Column(name = "\"ComMaxPlaytime\"")
    private Integer communityMaxPlaytime;

    @Column(name = "\"MfgAgeRec\"")
    private Integer recommendedAge;

    @Column(name = "\"GameWeight\"")
    private Double gameWeight;

    @Column(name = "\"NumUserRatings\"")
    private Integer numberOfUserRatings;

    @Column(name = "\"NumOwned\"")
    private Integer numberOwned;

    @Column(name = "\"NumWish\"")
    private Integer numberWished;

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

        normalized = insertSection(
                normalized,
                "theme",
                "Theme"
        );

        normalized = insertSection(
                normalized,
                "gameplay",
                "Gameplay"
        );

        normalized = insertSection(
                normalized,
                "goal",
                "Goal"
        );

        normalized = insertSection(
                normalized,
                "cultural impact rules",
                "Cultural Impact And Rules"
        );

        normalized = insertSection(
                normalized,
                "background",
                "Background"
        );

        normalized = insertSection(
                normalized,
                "reimplement",
                "Reimplements"
        );

        normalized = insertSection(
                normalized,
                "expande by",
                "Expanded By"
        );

        normalized = insertSection(
                normalized,
                "expanded by",
                "Expanded By"
        );

        return normalized.trim();
    }

    public String getThumbnailUrl() {
        return imageUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        if ((imageUrl == null || imageUrl.isBlank())
                && thumbnailUrl != null
                && !thumbnailUrl.isBlank()) {

            this.imageUrl = thumbnailUrl;
        }
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getYearPublished() {
        return yearPublished;
    }

    public void setYearPublished(Integer yearPublished) {
        this.yearPublished = yearPublished;
    }

    public Integer getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(Integer minPlayers) {
        this.minPlayers = minPlayers;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public Integer getManufacturerPlaytime() {
        return manufacturerPlaytime;
    }

    public void setManufacturerPlaytime(Integer manufacturerPlaytime) {
        this.manufacturerPlaytime = manufacturerPlaytime;
    }

    public Integer getCommunityMinPlaytime() {
        return communityMinPlaytime;
    }

    public void setCommunityMinPlaytime(Integer communityMinPlaytime) {
        this.communityMinPlaytime = communityMinPlaytime;
    }

    public Integer getCommunityMaxPlaytime() {
        return communityMaxPlaytime;
    }

    public void setCommunityMaxPlaytime(Integer communityMaxPlaytime) {
        this.communityMaxPlaytime = communityMaxPlaytime;
    }

    public Integer getRecommendedAge() {
        return recommendedAge;
    }

    public void setRecommendedAge(Integer recommendedAge) {
        this.recommendedAge = recommendedAge;
    }

    public Double getGameWeight() {
        return gameWeight;
    }

    public void setGameWeight(Double gameWeight) {
        this.gameWeight = gameWeight;
    }

    public Integer getNumberOfUserRatings() {
        return numberOfUserRatings;
    }

    public void setNumberOfUserRatings(Integer numberOfUserRatings) {
        this.numberOfUserRatings = numberOfUserRatings;
    }

    public Integer getNumberOwned() {
        return numberOwned;
    }

    public void setNumberOwned(Integer numberOwned) {
        this.numberOwned = numberOwned;
    }

    public Integer getNumberWished() {
        return numberWished;
    }

    public void setNumberWished(Integer numberWished) {
        this.numberWished = numberWished;
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

    public boolean hasImage() {
        return isNotBlank(imageUrl)
                || isNotBlank(getThumbnailUrl());
    }

    public String getPreferredImageUrl() {
        if (isNotBlank(imageUrl)) {
            return imageUrl;
        }

        return getThumbnailUrl();
    }

    public boolean hasPlayerCount() {
        return minPlayers != null
                && minPlayers > 0;
    }

    public String getPlayerCountDisplay() {
        if (!hasPlayerCount()) {
            return null;
        }

        if (maxPlayers == null
                || maxPlayers <= 0
                || minPlayers.equals(maxPlayers)) {

            return minPlayers + " players";
        }

        return minPlayers
                + "–"
                + maxPlayers
                + " players";
    }

    public String getPlaytimeDisplay() {
        boolean hasCommunityMinimum =
                communityMinPlaytime != null
                        && communityMinPlaytime > 0;

        boolean hasCommunityMaximum =
                communityMaxPlaytime != null
                        && communityMaxPlaytime > 0;

        if (hasCommunityMinimum && hasCommunityMaximum) {
            if (communityMinPlaytime.equals(
                    communityMaxPlaytime
            )) {
                return communityMinPlaytime + " min";
            }

            return communityMinPlaytime
                    + "–"
                    + communityMaxPlaytime
                    + " min";
        }

        if (hasCommunityMinimum) {
            return communityMinPlaytime + " min";
        }

        if (hasCommunityMaximum) {
            return communityMaxPlaytime + " min";
        }

        if (manufacturerPlaytime != null
                && manufacturerPlaytime > 0) {

            return manufacturerPlaytime + " min";
        }

        return null;
    }

    public String getComplexityDisplay() {
        if (gameWeight == null || gameWeight <= 0) {
            return null;
        }

        return String.format(
                java.util.Locale.US,
                "%.2f / 5",
                gameWeight
        );
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String insertSection(
            String source,
            String token,
            String label
    ) {
        return source.replaceFirst(
                "(?i)"
                        + java.util.regex.Pattern.quote(token),
                java.util.regex.Matcher.quoteReplacement(
                        "\n\n"
                                + label
                                + ": "
                )
        );
    }
}