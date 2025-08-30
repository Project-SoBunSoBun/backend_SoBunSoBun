package com.sobunsobun.backend.controller.me;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController @RequestMapping("/api")
public class MeController {
    @GetMapping("/me")
    public Map<String,Object> me(Authentication auth){
        return Map.of("principal", auth.getPrincipal());
    }
}
