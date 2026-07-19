package com.cardboardboxed.demo.controllers;
import com.cardboardboxed.demo.sessions.GameSession;
import com.cardboardboxed.demo.sessions.GameSessionAttendee;
import com.cardboardboxed.demo.sessions.GameSessionAttendeeRepository;
import com.cardboardboxed.demo.sessions.GameSessionRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserFollow;
import com.cardboardboxed.demo.useracounts.UserFollowRepository;
import com.cardboardboxed.demo.useracounts.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class SessionController {
    private final GameSessionRepository sessionRepository;
    private final GameSessionAttendeeRepository sessionAttendeeRepository;
    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;

    public SessionController(GameSessionRepository sessionRepository, GameSessionAttendeeRepository sessionAttendeeRepository, UserRepository userRepository, UserFollowRepository userFollowRepository ){
        this.sessionRepository = sessionRepository;
        this.sessionAttendeeRepository = sessionAttendeeRepository;
        this.userRepository = userRepository;
        this.userFollowRepository = userFollowRepository;
    }

    @GetMapping("/sessions")
    public String mySessions(
            @RequestParam(required = false) String error,
            HttpServletRequest request, Model model
    ) {
        User currentUser = requireCurrentUser(request);
        if (currentUser == null) {
            return "redirect:/login?error=Please+log+in+to+view+sessions";
        }

        model.addAttribute("hostedSessions", sessionRepository.findByHostOrderBySessionTimeAsc(currentUser));
        model.addAttribute("invitedSessions", sessionAttendeeRepository.findByUser(currentUser));
        model.addAttribute("error", error);
        return "my-sessions";
    }

    // GET /sessions/new - form for creating a session, with followers as invite candidates
    @GetMapping("/sessions/new")
    public String newSessionForm(
            @RequestParam(required = false) String error,
            HttpServletRequest request, Model model
    ) {
        User currentUser = requireCurrentUser(request);
        if (currentUser == null) {
            return "redirect:/login?error=Please+log+in+to+create+a+session";
        }

        List<User> followedUsers = userFollowRepository.findByFollower(currentUser)
                .stream()
                .map(UserFollow::getFollowed)
                .collect(Collectors.toList());

        model.addAttribute("followedUsers", followedUsers);
        model.addAttribute("error", error);
        return "new-session";
    }
    // POST /sessions - create a session and invite selected users
    @PostMapping("/sessions")
    public String createSession(
            @RequestParam String gameName,
            @RequestParam String sessionDate,       
            @RequestParam String sessionTimeOfDay,   
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String notes,
            @RequestParam(name = "inviteeIds", required = false) List<Integer> inviteeIds,
            HttpServletRequest request
    ) {
        User currentUser = requireCurrentUser(request);
        if (currentUser == null) {
            return "redirect:/login?error=Please+log+in+to+create+a+session";
        }

        if (gameName == null || gameName.isBlank()) {
            return "redirect:/sessions/new?error=Please+choose+a+board+game";
        }

        LocalDateTime parsedTime;
        try {
            parsedTime = LocalDateTime.parse(sessionDate + "T" + sessionTimeOfDay);
        } catch (Exception e) {
            return "redirect:/sessions/new?error=Please+choose+a+valid+date+and+time";
        }

        GameSession session = new GameSession(currentUser, gameName, parsedTime, location, notes);
        sessionRepository.save(session);

        if (inviteeIds != null) {
            for (Integer inviteeId : inviteeIds) {
                userRepository.findById(inviteeId).ifPresent(invitee -> {
                    if (!invitee.getId().equals(currentUser.getId())) {
                        sessionAttendeeRepository.save(new GameSessionAttendee(session, invitee));
                    }
                });
            }
        }

        return "redirect:/sessions/" + session.getId();
    }

    // GET /sessions/{id} - session detail view
    @GetMapping("/sessions/{id}")
    public String sessionDetail(
            @PathVariable Integer id,
            @RequestParam(required = false) String error,
            HttpServletRequest request, Model model
    ) {
        User currentUser = requireCurrentUser(request);
        if (currentUser == null) {
            return "redirect:/login?error=Please+log+in+to+view+this+session";
        }

        Optional<GameSession> sessionOpt = sessionRepository.findById(id);
        if (sessionOpt.isEmpty()) {
            return "redirect:/sessions?error=Session+not+found";
        }
        GameSession session = sessionOpt.get();

        boolean isHost = session.getHost().getId().equals(currentUser.getId());

        model.addAttribute("gameSession", session);
        model.addAttribute("attendees", sessionAttendeeRepository.findBySession(session));
        model.addAttribute("isHost", session.getHost().getId().equals(currentUser.getId()));
        model.addAttribute("myAttendance",
                sessionAttendeeRepository.findBySessionAndUser(session, currentUser).orElse(null));
        model.addAttribute("error", error);
        if (isHost) {
            List<Integer> alreadyInvitedIds = sessionAttendeeRepository.findBySession(session)
                    .stream()
                    .map(a -> a.getUser().getId())
                    .collect(Collectors.toList());

            List<User> inviteCandidates = userFollowRepository.findByFollower(currentUser)
                    .stream()
                    .map(UserFollow::getFollowed)
                    .filter(u -> !alreadyInvitedIds.contains(u.getId()))
                    .collect(Collectors.toList());

            model.addAttribute("inviteCandidates", inviteCandidates);
        }
        return "session-detail";
    }

    // POST /sessions/{id}/invite - host invites another user
    @PostMapping("/sessions/{id}/invite")
    public String inviteToSession(
            @PathVariable Integer id,
            @RequestParam Integer inviteeId,
            HttpServletRequest request
    ) {
        User currentUser = requireCurrentUser(request);
        if (currentUser == null) {
            return "redirect:/login?error=Please+log+in";
        }

        Optional<GameSession> sessionOpt = sessionRepository.findById(id);
        if (sessionOpt.isEmpty()) {
            return "redirect:/sessions?error=Session+not+found";
        }
        GameSession session = sessionOpt.get();

        if (!session.getHost().getId().equals(currentUser.getId())) {
            return "redirect:/sessions/" + id + "?error=Only+the+host+can+invite+people";
        }

        Optional<User> inviteeOpt = userRepository.findById(inviteeId);
        if (inviteeOpt.isEmpty()) {
            return "redirect:/sessions/" + id + "?error=User+not+found";
        }
        User invitee = inviteeOpt.get();

        boolean alreadyInvited = sessionAttendeeRepository.findBySessionAndUser(session, invitee).isPresent();
        if (!alreadyInvited) {
            sessionAttendeeRepository.save(new GameSessionAttendee(session, invitee));
        }

        return "redirect:/sessions/" + id;
    }

    // POST /sessions/{id}/rsvp - invitee accepts or declines
    @PostMapping("/sessions/{id}/rsvp")
    public String rsvp(
            @PathVariable Integer id,
            @RequestParam String response, 
            HttpServletRequest request
    ) {
        User currentUser = requireCurrentUser(request);
        if (currentUser == null) {
            return "redirect:/login?error=Please+log+in";
        }

        Optional<GameSession> sessionOpt = sessionRepository.findById(id);
        if (sessionOpt.isEmpty()) {
            return "redirect:/sessions?error=Session+not+found";
        }
        GameSession session = sessionOpt.get();

        Optional<GameSessionAttendee> attendanceOpt = sessionAttendeeRepository.findBySessionAndUser(session, currentUser);
        if (attendanceOpt.isEmpty()) {
            return "redirect:/sessions/" + id + "?error=You+were+not+invited+to+this+session";
        }
        GameSessionAttendee attendance = attendanceOpt.get();

        if ("accept".equalsIgnoreCase(response)) {
            attendance.setStatus(GameSessionAttendee.RsvpStatus.ACCEPTED);
        } else if ("decline".equalsIgnoreCase(response)) {
            attendance.setStatus(GameSessionAttendee.RsvpStatus.DECLINED);
        } else {
            return "redirect:/sessions/" + id + "?error=Invalid+response";
        }
        sessionAttendeeRepository.save(attendance);

        return "redirect:/sessions/" + id;
    }

    // Mirrors the AUTH_USER session-lookup pattern used in ReviewController
    private User requireCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        String username = (String) session.getAttribute("AUTH_USER");
        if (username == null) {
            return null;
        }
        return userRepository.findByUsername(username);
    }
}
