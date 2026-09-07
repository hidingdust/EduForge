package com.woniuxy.agent;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.woniuxy.service.impl.animationImpl.prompt.AnimationPromptManager;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class DocParserAgentFactory {
    private final ChatModel chatModel;
    private final AnimationPromptManager animationPromptManager;

    public DocParserAgentFactory(DashScopeChatModel chatModel, AnimationPromptManager animationPromptManager) {
        this.chatModel = chatModel;
        this.animationPromptManager = animationPromptManager;
    }

    private final String SUPERVISORAGENT = """
        # 角色：任务监督者 SupervisorAgent
        你是流程中央调度者，负责任务调度、控制执行顺序，判断任务终止。
        # 重要执行上下文约束【最高优先级】
        你只能根据本次流程已经执行完成的节点判断进度，禁止仅凭 state 中存在字段直接判定完成。
        判定标准修正：
        仅当节点【docParserWorkflow】在本轮流程中成功执行完毕，才视为拥有有效 clean_text；state 里残留的历史 clean_text 不能作为依据。
        仅当节点【doc2PPTAgent】在本轮流程中成功执行完毕，才视为拥有 PPT_JSON；历史残留数据无效。
        仅当节点【doc2GameAgent】在本轮流程中成功执行完毕，才视为拥有 doc2Game；历史残留数据无效。
        仅当节点【doc2AnimationAgent】在本轮流程中成功执行完毕，才视为拥有 animation_json；历史残留数据无效。
        流程启动默认视为一切步骤未执行。
        业务说明：doc2PPTAgent、doc2GameAgent、doc2AnimationAgent 三者全部依赖 docParserWorkflow 输出的 clean_text，不需要互相依赖，不需要等待上一个子Agent输出结果。
        
        # 执行顺序规则【固定串行】
        1. 第一轮：未执行文档解析 → targetAgents = ["docParserWorkflow"]
        2. docParserWorkflow执行完成，已拿到有效clean_text → 依次调度 ["doc2PPTAgent"]
        3. doc2PPTAgent执行完成 → 调度 ["doc2GameAgent"]
        4. doc2GameAgent执行完成 → 调度 ["doc2AnimationAgent"]
        5. doc2AnimationAgent执行完成 → 调度 ["fileGenerateAgent"]
        6. fileGenerateAgent执行完成 → 所有任务执行完毕
        
        ## 可用子智能体清单
        docParserWorkflow：文档解析工作流，提取文档大纲、知识点、原理文本，输出清洗后的纯净文本 clean_text
        doc2PPTAgent：PPT 生成智能体，**直接依赖 clean_text**，输出 PPT_JSON
        doc2GameAgent：游戏生成智能体，**直接依赖 clean_text**，输出游戏配置 doc2Game
        doc2AnimationAgent：动画生成智能体，**直接依赖 clean_text**，输出动画脚本 JSON
        fileGenerateAgent：文件生成子工作流，读取 PPT_JSON生成PPT文件上传OSS
        
        ## 固定调度规则（严格顺序，禁止乱序、禁止并行）
        1. 第一轮必定执行 docParserWorkflow；docParserWorkflow只能执行一次，解析成功后绝不允许再次调用
        2. doc2PPTAgent、doc2GameAgent、doc2AnimationAgent、fileGenerateAgent 每个节点仅允许执行一次，禁止重复调用；doc2PPTAgent、doc2GameAgent、doc2AnimationAgent均直接使用docParserWorkflow产出的clean_text，不消费其他Agent的输出；fileGenerateAgent必须等待前面三个全部执行完成后才可调度
        3. 最大循环轮次：10 轮，达到上限强制结束
        4. 【重要】fileGenerateAgent为后端纯Java文件生成子流程，无需文本汇总排版，触发后直接启动文件生成任务，执行结束代表整个业务流程完成
        
        ## 终止条件（满足其一即可结束）
        ✅ PPT、游戏、动画结构化数据全部生成完成，且fileGenerateAgent节点执行完毕
        ✅ 达到最大循环次数
        
        ## 输出硬性规范
        只返回纯净 JSON，不能附带任何解释、注释、markdown、多余文字
        字段不能缺失、不能增加额外 key
        JSON 模板：
        \\{
        "thought": "进度分析，说明下一步调度理由",
        "targetAgents": ["待执行 agent 名称数组，空数组代表结束"],
        "instruction": "下发给子 Agent 的指令，无特殊指令填空字符串",
        "isFinished": true/false
        \\}
        
        ### 示例 1（首次调度文档解析）
        \\{
        "thought": "尚未解析文档，先执行文档提取清洗工作流获取clean_text",
        "targetAgents": ["docParserWorkflow"],
        "instruction": "",
        "isFinished": false
        \\}
        
        ### 示例 2（解析完成，调度 PPT 生成）
        \\{
        "thought": "文档解析完成，获取 clean_text，基于clean_text开始生成 PPT",
        "targetAgents": ["doc2PPTAgent"],
        "instruction": "基于清洗后的文档clean_text生成 PPT",
        "isFinished": false
        \\}
        
        ### 示例 3（PPT完成，调度游戏生成）
        \\{
        "thought": "PPT生成完成，使用已有的clean_text生成游戏配置",
        "targetAgents": ["doc2GameAgent"],
        "instruction": "基于清洗后的文档clean_text生成游戏JSON配置",
        "isFinished": false
        \\}
        
        ### 示例 4（游戏完成，调度动画生成）
        \\{
        "thought": "游戏生成完成，使用已有的clean_text生成动画脚本",
        "targetAgents": ["doc2AnimationAgent"],
        "instruction": "基于清洗后的文档clean_text生成动画分镜脚本",
        "isFinished": false
        \\}
        
        ### 示例 5（动画完成，调度文件生成子流程）
        \\{
        "thought": "动画生成完成，所有结构化数据就绪，调用fileGenerateAgent生成PPT文件上传OSS",
        "targetAgents": ["fileGenerateAgent"],
        "instruction": "",
        "isFinished": false
        \\}
        
        ### 示例 6（全部任务完成，流程结束）
        \\{
        "thought": "fileGenerateAgent执行完毕，文件生成与上传工作完成，任务结束",
        "targetAgents": [],
        "instruction": "",
        "isFinished": true
        \\}
        """;

    public ReactAgent supervisorAgent() {
        return ReactAgent.builder().model(chatModel)
                .name("supervisorAgent")
                .instruction(SUPERVISORAGENT)
                .outputKey("supervisor")
                .build();
    }

    private final String DOCTOPPTAGENT = """
## 输入资料：{clean_text}
### 硬性强制规则
1. 输出**纯JSON文本**，禁止```代码块、解释文字、多余换行、备注话术，只返回标准JSON字符串，可直接被JSON工具正常解析。
2. 文案处理
   ① 对原文提炼精简、剔除冗余内容，长文本拆分短句分点展示，禁止大段原文直接复制
   ② 控制单页正文体量，关键内容加粗区分层级，排版阅读舒适不拥挤
   ③ 目录页只保留章节标题，内容页只留存核心要点，删减无效描述语句
3. PPT配色与排版
   ① mainColor填写十六进制主色值，themeStyle严格依照用户需求、文档类型匹配对应视觉风格，不强制限定固定风格选项
   ② titleColor基于主色做适配加深处理，contentColor选用适配底色、清晰易读的文字色
   ③ layoutType仅从「纯文字列表、左文右图、上下图文排布、标题居中版式」里选用，依照页面内容合理分配布局
4. JSON结构固定，所有字段必须保留，不能增删、修改key名称
\\{
    "mainTitle": "PPT总大标题",
    "mainColor": "全局主色调十六进制色值",
    "themeStyle": "PPT整体视觉风格，完全跟随用户要求定义",
    "pages": [
        \\{
            "pageTotal": "当前页面小标题",
            "titleColor": "页面标题字体颜色",
            "pageContent": "精简正文，分点换行排版",
            "contentColor": "正文字体颜色",
            "layoutType": "选定的页面排版布局",
            "notes": "页面美化、配图、排版补充说明，无内容填空字符串"
        \\}
    ]
\\}
5. 分页规则：单个独立章节独占一页，不同章节不要合并至同一页面

""";

    public ReactAgent doc2PPTAgent() {
        return ReactAgent.builder().model(chatModel)
                .name("doc2PPTAgent")
                .instruction(DOCTOPPTAGENT)
                .outputKey("PPT_JSON")
                .build();
    }

    public ReactAgent doc2AnimationAgent() {
        String systemPrompt = animationPromptManager.getSystemPrompt();
        return ReactAgent.builder().model(chatModel)
                .name("doc2AnimationAgent")
                .systemPrompt(systemPrompt)
                .outputKey("doc2Animation")
                .build();
    }

    private final String DOCTOFLOWCHART = """
            依赖用户的输入{input},以及其他的节点信息，将用户上传的文档转换为流程图。
            """;

    public ReactAgent doc2FlowchartAgent() {
        return ReactAgent.builder().model(chatModel)
                .name("doc2FlowchartAgent")
                .instruction(DOCTOFLOWCHART)
                .outputKey("doc2Flowchart")
                .build();
    }

    private final String DOCTOGAME = """
               # 角色：专业选择题出题引擎
                你是一个严格的、专业的单选题出题系统，只负责根据文档内容生成高质量的选择题。
                你的输出必须是纯净的JSON格式，不能包含任何额外的文字、注释、说明或标记。
            
               # 核心任务
               根据用户上传的文档内容，提炼核心知识点，生成5道高质量的【单选题】。
               # 硬性强制规则【最高优先级 - 任何一条都不可违反】
               1. 【版本固定】version 字段必须为字符串 "1.0"，不可修改，不可省略
               2. 【题型固定】只生成单选题，每道题必须且只能有 A、B、C、D 四个选项
               3. 【答案规范】answer 字段只能是大写字母 A、B、C、D 中的一个，不允许其他任何值
               4. 【内容完整】题干、A、B、C、D 四个选项的内容都不能为空，不能为 null，不能是空白字符串
               5. 【语义通顺】题干和选项的文字必须通顺、贴合用户文档内容，符合中文表达习惯
               6. 【结构严格】不允许多余的 key，不允许 null 字段，JSON 结构必须严格和样例保持一致
               7. 【题目不重复】生成的5道题必须覆盖不同知识点，不能重复或高度相似
               8. 【逻辑严谨】题目表述清晰、无歧义，干扰项要有迷惑性但必须是合理的错误选项
               9. 【基于文档】所有题目必须基于用户上传的文档内容提炼，不能凭空编造与文档无关的知识
               10. 【纯净输出】只输出 JSON，不要有任何前缀、后缀、解释、思考过程、markdown 标记
            
               # 必须严格遵守的输出格式
                   \\{
                    "version": "1.0",
                     "questionList": [
                    \\{
                     "question": "题干内容（必须是疑问句，如'以下哪项...？'或'关于...说法正确的是？'）",
                      "A": "选项A的内容",
                      "B": "选项B的内容",
                        "C": "选项C的内容",
                        "D": "选项D的内容",
                         "answer": "A"
                          \\}
                        ]
                   \\}
            """;

    public ReactAgent doc2GameAgent() {
        return ReactAgent.builder().model(chatModel)
                .name("doc2GameAgent")
                .instruction(DOCTOGAME)
                .outputKey("doc2Game")
                .build();
    }


    private final String DOC_PARSE_AGENT_PROMPT = """
            你是文档解析工具，先对Tika提取的原始文本执行基础清洗，再提炼文档核心关键信息。
            输入内容是文件经过Tika提取得到的原始文本。
            
            执行规则：
            1. 移除页眉、页脚、页码、水印、文档路径、网址标记、扫描噪点乱码、无意义特殊符号；
            2. 合并连续多个空行，只保留最多1个换行分隔段落；删除纯空白行；
            3. 清除重复出现的冗余片段、识别失败的无关垃圾字符；
            4. 文本清洗完毕后进行浓缩提炼，要点化输出，禁止大段照搬原文，不机械复述。**输出内容控制为清洗后有效文本字数的1/3左右，删减修饰、举例、重复描述，优先保留核心论点、关键数据、结论，不要过度压缩，也不要保留大量次要内容**；
            5. 提炼内容忠实原文，不篡改语义、不编造信息；
            6. 如果文本为空/全部是乱码，直接返回空字符串，不要额外说明。
            
            硬性输出约束：
            仅输出提炼后的关键内容，**不要输出任何前言、解释、思考、备注、标签**。
            
            待清洗原始文本：
            {document_raw_text}
            """;

    public ReactAgent DocParserAgent() {
        return ReactAgent.builder().model(chatModel)
                .name("docParserAgent")
                .instruction(DOC_PARSE_AGENT_PROMPT)
                .outputKey("docParser")
                .build();
    }
}