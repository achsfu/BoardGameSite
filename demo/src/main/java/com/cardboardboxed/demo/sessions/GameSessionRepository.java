package com.cardboardboxed.demo.sessions;

import com.cardboardboxed.demo.useracounts.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GameSessionRepository extends JpaRepository<GameSession, Integer> {

    List<GameSession> findByHostOrderBySessionTimeAsc(User host);
}