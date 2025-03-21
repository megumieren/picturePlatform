package com.wjq.wjqpicturebackend.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class PictureTagCategory {
    /**
     * 分类列表
     */
    private List<String> CategoryList;

    /**
     * 标签列表
     */
    private List<String> TagList;
}
