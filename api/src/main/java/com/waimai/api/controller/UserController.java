package com.waimai.api.controller;

import com.waimai.common.Result;
import com.waimai.common.entity.User;
import com.waimai.common.entity.UserAddress;
import com.waimai.common.utils.UserContext;
import com.waimai.service.service.UserAddressService;
import com.waimai.service.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final UserAddressService userAddressService;

    public UserController(UserService userService, UserAddressService userAddressService) {
        this.userService = userService;
        this.userAddressService = userAddressService;
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

    @GetMapping("/address")
    public Result<List<UserAddress>> listAddress() {
        return Result.ok(userAddressService.listByUserId(UserContext.getUserId()));
    }

    @PostMapping("/address")
    public Result<?> addAddress(@RequestBody UserAddress addr) {
        userAddressService.saveAddress(UserContext.getUserId(), addr);
        return Result.ok();
    }

    @PutMapping("/address/{id}")
    public Result<?> updateAddress(@PathVariable Long id, @RequestBody UserAddress addr) {
        userAddressService.updateAddress(UserContext.getUserId(), id, addr);
        return Result.ok();
    }

    @DeleteMapping("/address/{id}")
    public Result<?> deleteAddress(@PathVariable Long id) {
        userAddressService.deleteAddress(UserContext.getUserId(), id);
        return Result.ok();
    }
}
