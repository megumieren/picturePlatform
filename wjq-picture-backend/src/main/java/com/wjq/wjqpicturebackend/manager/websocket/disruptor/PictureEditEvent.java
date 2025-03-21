package com.wjq.wjqpicturebackend.manager.websocket.disruptor;

import com.wjq.wjqpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.wjq.wjqpicturebackend.model.domain.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;
    
    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片 id
     */
    private Long pictureId;

}
