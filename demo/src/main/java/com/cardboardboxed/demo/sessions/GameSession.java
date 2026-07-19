package com.cardboardboxed.demo.sessions;

import java.time.LocalDateTime;
import java.util.*;
import com.cardboardboxed.demo.useracounts.User;
import jakarta.persistence.*;

/*
 * Session:
 *    - A planned game session hosted by one user, for one board game
 *    - Has many SessionAttendees (invited friends who can accept/decline)
 */

@Entity
@Table(name = "sessions")
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(name = "game_name", nullable = false)
    private String gameName;

    @Column(name = "session_time", nullable = false)
    private LocalDateTime sessionTime;

    @Column(name = "location")
    private String location;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameSessionAttendee> attendees = new ArrayList<>();

    public GameSession(){
    }

    public GameSession(User host, String gameName, LocalDateTime sessionTime, String location, String notes){
        this.host = host;
        this.gameName = gameName;
        this.sessionTime = sessionTime;
        this.location = location;
        this.notes = notes;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    private void setCreatedAt(){
        if (createdAt == null){
            createdAt = LocalDateTime.now();
        }
    }

    //Getters & setters

    public Integer getId(){
        return id;
    }

    public User getHost(){
        return host;
    }

    public void setHost(User host){
        this.host = host;
    }

    public void setGameName(String gameName){
        this.gameName = gameName;
    }

    public String getGameName(){
        return gameName;
    }

    public LocalDateTime getSessionTime(){
        return sessionTime;
    }

    public void setSessionTime(LocalDateTime sessionTime){
        this.sessionTime = sessionTime;
    }

    public String getLocation(){
        return location;
    }

    public void setLocation(String location){
        this.location = location;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<GameSessionAttendee> getAttendees(){
        return attendees;
    } 
}
