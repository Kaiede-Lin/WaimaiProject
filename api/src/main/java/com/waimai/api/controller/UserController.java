package com.waimai.api.controller;

import com.waimai.common.Result;
import com.waimai.common.entity.User;
import com.waimai.common.utils.UserContext;
import com.waimai.service.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/info")
    public Result<User> info() {
        return Result.ok(userService.getById(UserContext.getUserId()));
    }

    @PutMapping("/info")
    public Result<?> updateInfo(@RequestBody User user) {
        user.setId(UserContext.getUserId());
        userService.updateById(user);
        return Result.ok();
    }
}
