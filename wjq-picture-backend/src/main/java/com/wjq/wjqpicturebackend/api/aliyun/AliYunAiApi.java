package com.wjq.wjqpicturebackend.api.aliyun;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.wjq.wjqpicturebackend.api.aliyun.model.CreateOutPaintingTaskRequest;
import com.wjq.wjqpicturebackend.api.aliyun.model.CreateOutPaintingTaskResponse;
import com.wjq.wjqpicturebackend.api.aliyun.model.GetOutPaintingTaskResponse;
import com.wjq.wjqpicturebackend.exception.BusinessException;
import com.wjq.wjqpicturebackend.exception.ErrorCode;
import com.wjq.wjqpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunAiApi {
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建任务
     * @param createOutPaintingTaskRequest
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        ThrowUtils.throwIf(createOutPaintingTaskRequest == null, ErrorCode.PARAMS_ERROR,"扩图参数为空");
        // 发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("X-DashScope-Async", "enable")
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));
        //执行完自动释放连接
        try(HttpResponse httpResponse = httpRequest.execute()){
            if(!httpResponse.isOk()){
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }
            CreateOutPaintingTaskResponse createOutPaintingTaskResponse = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            String errorCode = createOutPaintingTaskResponse.getCode();
            if(StrUtil.isNotBlank(errorCode)){
                String errorMessage = createOutPaintingTaskResponse.getMessage();
                log.error("AI 扩图失败，errorCode:{}, errorMessage:{}", errorCode, errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图接口响应异常");
            }
            return createOutPaintingTaskResponse;
        }
    }

    /**
     * 查询创建的任务
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if(StrUtil.isBlank(taskId)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        try(HttpResponse httpResponse = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL,taskId))
                    .header("Authorization","Bearer" + apiKey)
                    .execute()){
            if (!httpResponse.isOk()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    }
}
