package com.wjq.wjqpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = -4094836293108510068L;

    private String userAccount;

    private String userPassword;

    private String checkPassword;

    
}
