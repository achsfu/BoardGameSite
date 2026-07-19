package com.cardboardboxed.demo.sessions;

import java.time.LocalDateTime;

import com.cardboardboxed.demo.useracounts.User;

import jakarta.persistence.*;


/*
 * SessionAttendee:
 *    - Links one user to one session
 *    - Tracks RSVP status: INVITED (default), ACCEPTED, DECLINED
 *    - Prevents duplicate invites for the same user/session pair
 */

@Entity
@Table(
        name = "session_attendees",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"session_id", "user_id"}
        )
)
public class GameSessionAttendee {

    //RSVP Status
    public enum RsvpStatus{
        INVITED,
        ACCEPTED,
        DECLINED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession session;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RsvpStatus status;

    @Column(name = "invited_at", nullable = false)
    private LocalDateTime invitedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public GameSessionAttendee(){

    }

    public GameSessionAttendee(GameSession session, User user){
        this.session = session;
        this.user = user;
        this.status = RsvpStatus.INVITED;
        this.invitedAt = LocalDateTime.now();
    }

    @PrePersist
    private void setInvitedAt(){
        if(invitedAt == null){
            invitedAt = LocalDateTime.now();
        }
    }

    //Getters & Setters
    public Integer getId(){
        return id;
    }

    public GameSession getSession(){
        return session;
    }

    public void setSession(GameSession session){
        this.session = session;
    }

    public User getUser(){
        return user;
    }

    public void setUser(User user){
        this.user = user;
    }

    public RsvpStatus getStatus(){
        return status;
    }

    public void setStatus(RsvpStatus status){
        this.status = status;
        this.respondedAt = LocalDateTime.now();
    }

    public LocalDateTime getInvitedAt(){
        return invitedAt;
    }

    public LocalDateTime getRespondedAt(){
        return respondedAt;
    }

    
}
