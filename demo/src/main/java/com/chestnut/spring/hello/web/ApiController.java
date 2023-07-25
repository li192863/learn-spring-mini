package com.chestnut.spring.hello.web;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chestnut.spring.hello.User;
import com.chestnut.spring.hello.service.UserService;
import com.chestnut.spring.annotation.Autowired;
import com.chestnut.spring.annotation.GetMapping;
import com.chestnut.spring.annotation.PathVariable;
import com.chestnut.spring.annotation.RestController;
import com.chestnut.spring.exception.DataAccessException;

@RestController
public class ApiController {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    UserService userService;

    @GetMapping("/api/user/{email}")
    Map<String, Boolean> userExist(@PathVariable("email") String email) {
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
        try {
            userService.getUser(email);
            return Map.of("result", Boolean.TRUE);
        } catch (DataAccessException e) {
            return Map.of("result", Boolean.FALSE);
        }
    }

    @GetMapping("/api/users")
    List<User> users() {
        return userService.getUsers();
    }
}
