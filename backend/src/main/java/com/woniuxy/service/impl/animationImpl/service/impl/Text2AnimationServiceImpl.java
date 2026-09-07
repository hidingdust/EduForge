package com.woniuxy.service.impl.animationImpl.service.impl;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.woniuxy.agent.DocParserAgentFactory;
import com.woniuxy.service.impl.animationImpl.dto.NewAnimationData;
import com.woniuxy.service.impl.animationImpl.service.Text2AnimationService;
import com.woniuxy.service.impl.animationImpl.validator.AnimationJsonValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class Text2AnimationServiceImpl implements Text2AnimationService {
    private final DocParserAgentFactory docParserAgentFactory;
    private final AnimationJsonValidator jsonValidator;

    /**
     * 子 agent 在调用时需要用到 overallstate 里的数据
     * 每个 Agent 执行周期，会生成独立的 OverAllState。
     * 默认情况下：父 Agent 的 State 和 子 Agent 的 State 是隔离的，互不互通。
     * 从父的 OverAllState 取出需要的数据，拼接进子 Agent 的用户输入 / 提示词传给子 Agent。
     * 父子各自维护独立状态，不会互相干扰，状态干净，调试简单。
     *
     * @throws GraphRunnerException
     */
    @Override
    public Map<String, Object> text2Animation(OverAllState parentState) throws GraphRunnerException {
        try {
            // 1. 读取全局状态
            String userTextAfterParsed = parentState.value("clean_text")
                    .orElseThrow(() -> new GraphRunnerException("缺少clean_text")).toString();
            System.out.println("clean_text的内容：" + userTextAfterParsed);

            // 2. 调用子Agent
            ReactAgent doc2AnimationAgent = docParserAgentFactory.doc2AnimationAgent();
            Optional<OverAllState> subStateOpt = doc2AnimationAgent.invoke(userTextAfterParsed);

            Object obj = subStateOpt.get().value("doc2Animation").get();
            AssistantMessage assistantMsg = (AssistantMessage) obj;
            // 重点！getText() 才是LLm输出的原生JSON字符串
            String animationJson = assistantMsg.getText();

            log.info("【子Agent原始animationJson】>>>{}<<<", animationJson);

            NewAnimationData animationData = jsonValidator.validateAndParse(animationJson);

            // 3. 返回需要更新到父状态的数据
            return Map.of("animation_json", animationData);
        } catch (GraphRunnerException e) {
            throw new RuntimeException("动画生成失败：" + e.getMessage());
        }
    }
}
