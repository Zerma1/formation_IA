package com.example.iaspringlab.web;

import com.example.iaspringlab.account.Role;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private static final String ADMIN_AUTHORITY = "ROLE_" + Role.ADMIN.name();

    @GetMapping("/")
    public String home(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> ADMIN_AUTHORITY.equals(authority.getAuthority()));
        return isAdmin ? "redirect:/admin/products" : "redirect:/shop";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
