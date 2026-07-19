package com.cardboardboxed.demo.sessions;

import com.cardboardboxed.demo.useracounts.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GameSessionAttendeeRepository extends JpaRepository<GameSessionAttendee, Integer> {

    List<GameSessionAttendee> findBySession(GameSession session);

    List<GameSessionAttendee> findByUser(User user);

    Optional<GameSessionAttendee> findBySessionAndUser(GameSession session, User user);
}