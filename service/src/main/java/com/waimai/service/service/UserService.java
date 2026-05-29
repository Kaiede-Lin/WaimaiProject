package com.waimai.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.waimai.common.entity.User;

public interface UserService extends IService<User> {

    User loginByWechat(String openid, String nickname, String avatar);

    User getByOpenid(String openid);
}
