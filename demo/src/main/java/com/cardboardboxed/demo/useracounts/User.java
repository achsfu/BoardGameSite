package com.cardboardboxed.demo.useracounts;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String username;
    private String password;

    // Simple role field for iteration 1 role-based views.
    // Example values: PLAYER, ORGANIZER
    private String role;
    private String bio; // New field for user bio

    private String profilePictureUrl;
    private String gameOwned;
    private String gameWishlist;

    

    public User() {
    }

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        if (role == null || role.isBlank()) {
            return "PLAYER";
        }
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getBio(){
        return bio;
    }

    public void setBio(String bio){
        this.bio = bio;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getGameOwned() {
        return gameOwned;
    }

    public void setGameOwned(String gameOwned) {
        this.gameOwned = gameOwned;
    }

    public String getGameWishlist() {
        return gameWishlist;
    }

    public void setGameWishlist(String gameWishlist) {
        this.gameWishlist = gameWishlist;
    }
}