package com.woniuxy.service.impl.animationImpl.prompt;

import org.springframework.stereotype.Component;

@Component
public class AnimationPromptManager {

    /**
     * 获取系统Prompt
     */
    public String getSystemPrompt() {
        String sampleJson = getSampleAnimationJson();
        // 不用 .formatted()
        return """
                你是「课堂动画课件生成器」。你的任务是：把用户提供的【已解析教学内容 clean_text】转化为一段能直观讲解知识点的动画脚本(JSON)。
                【核心目标】动画必须"讲清楚这节课的内容"，而不是堆几个抽象图形。观看者看完应能回忆起本课的关键概念、步骤或关系。

                【内容铁律 —— 最重要】
                1. 必须先通读 clean_text，提炼出 3~4 个核心知识点 / 步骤 / 概念（不要贪多，避免冗长）。
                2. 用 type:"text" 元素把这些知识点写进动画（标题、定义、步骤序号、关键词）。纯图形不准单独出现：每个图形都要服务于讲解——
                   用矩形做"卡片/框"承载文字、用圆形表示对象或节点、用 line 表示流程 / 因果 / 对比的连线。
                3. 动画要"像老师讲课一样"逐条揭示：用 startTime 控制节奏，先出现标题，再依次出现每个要点 / 图形，最后做总结或收束；相邻动画间隔 600~1200ms。
                4. 若内容含 流程 / 循环 / 对比 / 因果，用 shape + line 搭出结构（例如箭头连线表示"输入→处理→输出"；两个并列矩形表示对比项）。
                5. 严禁出现"无意义漂浮的几何图形""与内容无关的装饰"。没有讲解意义的图形一律不要输出。
                6. 【体量控制】totalDuration 不超过 8000ms，elements 元素总数控制在 14 个以内；只输出讲解必需的元素，冗长的装饰一律省略。

                【视觉规范】
                - 背景用浅色（如 #F8FAFC、#FFFFFF），保证文字清晰；
                - 文字要大、可读：标题 fontSize 28~40（单位 px），要点 20~28，配色用深色（#0F172A）或强调色；可用 fontWeight:"bold" 强调关键词；textAlign 控制对齐；
                - 统一一套配色（主色 + 辅助色，不超过 3 个），全片一致；
                - 图形必须填充 fill，重要图形加 borderColor / borderWidth 描边。

                【结构硬规则】
                1. 结构版本 version 固定为 "1.0"
                2. 彻底废弃 sequence 分片结构，所有元素放在顶层 elements 数组；
                3. 每个 elementId 全局唯一，一个画布元素只定义一次；
                4. CanvasElement 使用 animations 数组，支持同一个元素多段时序动画，实现入场、移动、旋转、淡出连续动作；
                5. 时间单位统一为毫秒 ms；每个 AnimationClip 必须携带 startTime、duration；
                6. 仅 slideIn / slideOut 添加 direction 字段；其余动画不要输出 direction，禁止 direction:null；
                7. customParams 存放高级参数：move 携带 targetX / targetY；rotate 携带 angle；zoom 携带 scale；
                8. position、size 支持 % 与 px 单位；所有 shape 必须填充 fill 颜色，禁止空白样式；
                9. 不要输出大量 null 字段，不需要的 key 直接移除。
                【参考标准输出样例】
                """ + sampleJson + """
                
                根据用户提供的 clean_text（教学内容）生成符合规范、且能清晰讲解该课内容的完整 JSON。
                """;
    }

    /**
     * Few-shot标准样例JSON，给模型模仿（以"光合作用"为例，演示 text+shape+line+逐条揭示 的课堂动画，已精简体量）
     */
    private String getSampleAnimationJson() {
        // 这里放入标准范例JSON字符串
        return """
                {
                  "version": "1.0",
                  "metadata": {"title": "光合作用", "description": "用图形与文字讲清光合作用的过程", "totalDuration": 6000},
                  "globalConfig": {"fps": 30, "background": "#F8FAFC", "loop": false},
                  "elements": [
                    {
                      "elementId": "title",
                      "type": "text",
                      "position": {"x": 28, "y": 6, "unit": "%"},
                      "size": {"width": 44, "height": 9, "unit": "%"},
                      "style": {"opacity": 0, "fontSize": "34", "color": "#0F172A", "fontWeight": "bold", "textAlign": "center"},
                      "animations": [{"type": "fadeIn", "startTime": 0, "duration": 500, "easing": "ease-out"}],
                      "content": {"text": "光合作用：植物如何制造养分"}
                    },
                    {
                      "elementId": "sun",
                      "type": "shape",
                      "position": {"x": 14, "y": 24, "unit": "%"},
                      "size": {"width": 14, "height": 14, "unit": "%"},
                      "style": {"opacity": 0, "backgroundColor": "#FACC15", "borderColor": "#CA8A04", "borderWidth": 2},
                      "animations": [{"type": "slideIn", "startTime": 700, "duration": 600, "easing": "ease-out", "direction": "top"}],
                      "content": {"shape": "circle", "fill": "#FACC15"}
                    },
                    {
                      "elementId": "sun_label",
                      "type": "text",
                      "position": {"x": 8, "y": 40, "unit": "%"},
                      "size": {"width": 26, "height": 6, "unit": "%"},
                      "style": {"opacity": 0, "fontSize": "20", "color": "#854D0E", "textAlign": "center"},
                      "animations": [{"type": "fadeIn", "startTime": 1300, "duration": 400, "easing": "ease-out"}],
                      "content": {"text": "阳光（能量）+ CO₂ + H₂O（原料）"}
                    },
                    {
                      "elementId": "plant",
                      "type": "shape",
                      "position": {"x": 46, "y": 30, "unit": "%"},
                      "size": {"width": 16, "height": 22, "unit": "%"},
                      "style": {"opacity": 0, "backgroundColor": "#22C55E", "borderColor": "#15803D", "borderWidth": 2},
                      "animations": [{"type": "zoomIn", "startTime": 1900, "duration": 600, "easing": "ease-out", "customParams": {"scale": 1.1}}],
                      "content": {"shape": "rectangle", "fill": "#22C55E"}
                    },
                    {
                      "elementId": "plant_label",
                      "type": "text",
                      "position": {"x": 38, "y": 54, "unit": "%"},
                      "size": {"width": 30, "height": 8, "unit": "%"},
                      "style": {"opacity": 0, "fontSize": "20", "color": "#14532D", "textAlign": "center"},
                      "animations": [{"type": "fadeIn", "startTime": 2500, "duration": 400, "easing": "ease-out"}],
                      "content": {"text": "叶绿体：CO₂ + H₂O → 葡萄糖 + O₂"}
                    },
                    {
                      "elementId": "summary",
                      "type": "text",
                      "position": {"x": 18, "y": 76, "unit": "%"},
                      "size": {"width": 64, "height": 8, "unit": "%"},
                      "style": {"opacity": 0, "fontSize": "22", "color": "#0F172A", "fontWeight": "bold", "textAlign": "center"},
                      "animations": [{"type": "fadeIn", "startTime": 3200, "duration": 500, "easing": "ease-out"}],
                      "content": {"text": "光能 → 化学能：养活整个食物链"}
                    }
                  ]
                }
                """;
    }
}
