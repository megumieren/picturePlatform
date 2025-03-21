package com.wjq.wjqpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginRequest implements Serializable {
    private static final long serialVersionUID = -1732962297057529123L;

    private String userAccount;

    private String userPassword;

}
