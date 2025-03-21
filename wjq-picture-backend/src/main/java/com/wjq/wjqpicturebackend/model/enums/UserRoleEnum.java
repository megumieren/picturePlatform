package com.wjq.wjqpicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Map;

@Getter
public enum UserRoleEnum {

    USER("用户","user"),
    ADMIN("管理员","admin");


    private final String role;

    private final String value;


    UserRoleEnum(String role, String value) {
        this.role = role;
        this.value = value;
    }

    public static UserRoleEnum getUserRoleEnumByValue(String value) {

        if(ObjUtil.isEmpty(value)){
            return null;
        }
        for (UserRoleEnum roleEnum : UserRoleEnum.values()) {
            if(roleEnum.getValue().equals(value)){
                return roleEnum;
            }
        }
        return null;
    }
}
