package com.smartjam.smartjamapi.controller;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/secured")
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @GetMapping("/user")
    public String userAccess(Principal principal) {
        System.out.println(principal);
        if (principal == null) {
            return "Anonymous";
        }

        log.error("Call userAccess");
        return principal.getName();
    }

    @GetMapping("/hello")
    public String hello() {
        return "You are auth";
    }
}
