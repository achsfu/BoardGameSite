package com.cardboardboxed.demo.controllers;

import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.boardgames.BoardGameRankRepository;
import com.cardboardboxed.demo.reviews.Review;
import com.cardboardboxed.demo.reviews.ReviewRepository;
import com.cardboardboxed.demo.useracounts.User;
import com.cardboardboxed.demo.useracounts.UserRepository;
import com.cardboardboxed.demo.useracounts.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BoardGameRankRepository boardGameRankRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private HttpSession getSession(HttpServletRequest request) {
        return request.getSession(false);
    }

    private String getLoggedInUsername(HttpServletRequest request) {
        HttpSession session = getSession(request);

        if (session == null) {
            return null;
        }

        return (String) session.getAttribute("AUTH_USER");
    }

    private User getLoggedInUser(HttpServletRequest request) {
        String username = getLoggedInUsername(request);

        if (username == null) {
            return null;
        }

        return userRepository.findByUsername(username);
    }

    /*
     * Public homepage.
     *
     * Both logged-in and logged-out users may view this page.
     * Logged-in users receive personalized navigation options.
     */
    @GetMapping("/")
    public String showLandingPage(
            Model model,
            HttpServletRequest request
    ) {
        String username = getLoggedInUsername(request);
        boolean loggedIn = username != null;

        List<BoardGameRank> popularGames =
                boardGameRankRepository.findHomepagePopularGames(
                        PageRequest.of(0, 6)
                );

        List<BoardGameRank> highestRatedGames =
                boardGameRankRepository.findHighestRatedGames(
                        PageRequest.of(0, 6)
                );

        List<Review> recentReviews =
                reviewRepository.findRecentHomepageReviews(
                        PageRequest.of(0, 6)
                );

        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("username", username);
        model.addAttribute("popularGames", popularGames);
        model.addAttribute("highestRatedGames", highestRatedGames);
        model.addAttribute("recentReviews", recentReviews);

        return "index";
    }

    @GetMapping("/login")
    public String showLoginForm(HttpServletRequest request) {
        if (getLoggedInUsername(request) != null) {
            return "redirect:/dashboard";
        }

        return "login";
    }

    @PostMapping("/login")
    public String processLogin(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request
    ) {
        if (getLoggedInUsername(request) != null) {
            return "redirect:/dashboard";
        }

        User user = userRepository.findByUsername(username);

        if (user != null
                && passwordEncoder.matches(password, user.getPassword())) {

            HttpSession session = request.getSession(true);

            session.setAttribute("AUTH_USER", username);
            session.setMaxInactiveInterval(
                    (int) Duration.ofDays(1).getSeconds()
            );

            return "redirect:/dashboard";
        }

        return "redirect:/login?error=Invalid+username+or+password";
    }

    @GetMapping("/register")
    public String showRegistrationForm(HttpServletRequest request) {
        if (getLoggedInUsername(request) != null) {
            return "redirect:/dashboard";
        }

        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String password2,
            HttpServletRequest request
    ) {
        if (getLoggedInUsername(request) != null) {
            return "redirect:/dashboard";
        }

        if (!password.equals(password2)) {
            return "redirect:/register?error=Passwords+do+not+match";
        }

        try {
            userService.registerUser(username, password);
        } catch (IllegalArgumentException e) {
            return "redirect:/register?error="
                    + URLEncoder.encode(
                            e.getMessage(),
                            StandardCharsets.UTF_8
                    );
        }

        return "redirect:/login"
                + "?success=Registration+successful%21+Please+log+in.";
    }

    @GetMapping("/dashboard")
    public String showDashboard(
            Model model,
            HttpServletRequest request
    ) {
        User user = getLoggedInUser(request);

        if (user == null) {
            return "redirect:/login"
                    + "?error=Please+log+in+to+access+the+dashboard";
        }

        model.addAttribute("username", user.getUsername());
        String role = user.getRole();
        model.addAttribute(
            "role",
            role == null || role.isBlank()
                ? "PLAYER"
                : role
        );

        return "dashboard";
    }
}