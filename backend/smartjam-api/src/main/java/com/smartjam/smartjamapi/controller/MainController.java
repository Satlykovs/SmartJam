package com.smartjam.smartjamapi.controller;

import java.security.Principal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/secured")
@Slf4j
public class MainController {

    @GetMapping("/user")
    public String userAccess(Principal principal) {
        log.info(principal.getName());

        log.info("userAccess called for: {}", principal.getName());
        return principal.getName();
    }

    @GetMapping("/hello")
//    @PreAuthorize()
    public String hello() {
        return "You are auth";
    }
}
