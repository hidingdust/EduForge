package com.woniuxy.service.impl.animationImpl.service;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;

import java.util.Map;

public interface Text2AnimationService {
    Map<String, Object> text2Animation(OverAllState parentState) throws GraphRunnerException;
}
