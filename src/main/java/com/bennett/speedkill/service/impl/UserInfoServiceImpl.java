package com.bennett.speedkill.service.impl;

import com.bennett.speedkill.entity.UserInfo;
import com.bennett.speedkill.mapper.UserInfoMapper;
import com.bennett.speedkill.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户信息表 服务实现类
 * </p>
 *
 * @author bennett
 * @since 2020-11-04
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
