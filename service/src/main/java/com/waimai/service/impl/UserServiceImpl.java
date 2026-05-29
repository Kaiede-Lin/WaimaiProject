package com.waimai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.waimai.common.entity.User;
import com.waimai.service.mapper.UserMapper;
import com.waimai.service.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User loginByWechat(String openid, String nickname, String avatar) {
        User user = getByOpenid(openid);
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname(nickname);
            user.setAvatar(avatar);
            user.setStatus(1);
            save(user);
        }
        return user;
    }

    @Override
    public User getByOpenid(String openid) {
        return lambdaQuery().eq(User::getOpenid, openid).one();
    }
}
