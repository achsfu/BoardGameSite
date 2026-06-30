package com.cardboardboxed.demo.controllers;

import com.cardboardboxed.demo.useracounts.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    System.out.println("Found cookie: " + name + " with value: " + cookie.getValue());
                    return cookie.getValue();
                }
            }
        }
        System.out.println("Cookie not found: " + name);
        return null;
    }

    @GetMapping("/login")
    public String showLoginForm(HttpServletRequest request) {
        if (getCookie(request, "AUTH-TOKEN") != null) {
            System.out.println("User is already logged in, redirecting to dashboard.");
            return "redirect:/dashboard";
        }
        return "login.html";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String username, @RequestParam String password, HttpServletRequest request, HttpServletResponse response) {
        
        if (getCookie(request, "AUTH-TOKEN") != null) {
            // If the token is present, it means the user is already logged in
            return "redirect:/dashboard";
        }

        User user = userRepository.findByUsername(username);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            // Successful login - set cookie
            Cookie cookie = new Cookie("AUTH-TOKEN", username);
            cookie.setPath("/");
            cookie.setMaxAge((int) Duration.ofDays(1).getSeconds());
            cookie.setHttpOnly(true);
            cookie.setSecure(false);
            response.addCookie(cookie);
            return "redirect:/dashboard";
        } else {
            // Failed login
            return "redirect:/login.html?error=Invalid+username+or+password";
        }
    }

    @GetMapping("/register")
    public String showRegistrationForm(HttpServletRequest request) {
        if (getCookie(request, "AUTH-TOKEN") != null) {
            System.out.println("User is already logged in, redirecting to dashboard.");
            return "redirect:/dashboard";
        }
        return "register.html";
    }

    @PostMapping("/register")
    public String processRegistration(@RequestParam String username, @RequestParam String password, @RequestParam String password2, HttpServletRequest request) {

        if (getCookie(request, "AUTH-TOKEN") != null) {
            // If the token is present, it means the user is already logged in
            return "redirect:/dashboard";
        }
        if (!password.equals(password2)) {
            return "redirect:/register.html?error=Passwords+do+not+match";
        }
        try {
            userService.registerUser(username, password);
        } catch (IllegalArgumentException e) {
            return "redirect:/register.html?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
        return "redirect:/login.html?success=Registration+successful%21+Please+log+in.";
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model, HttpServletRequest request) {
        if (getCookie(request, "AUTH-TOKEN") == null) {
            // If the token is not present, it means the user is not logged in
            return "redirect:/login.html?error=Please+log+in+to+access+the+dashboard";
        }

        model.addAttribute("username", getCookie(request, "AUTH-TOKEN"));
        // Add any necessary attributes to the model for the dashboard view
        return "dashboard.html"; // Return the name of the dashboard view template
    }
    
}
