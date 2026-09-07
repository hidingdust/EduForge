//package com.woniuxy.animation.validator;
//
//import com.alibaba.fastjson.JSON;
//import com.woniuxy.animation.dto.NewAnimationData;
//import org.springframework.stereotype.Component;
//
//@Component
//public class AnimationJsonValidator {
//
//    /**
//     * 尝试解析JSON，校验合法性
//     */
//    public NewAnimationData validateAndParse(String jsonStr) {
//        // 简单预处理：剔除markdown ```json 标记
//        jsonStr = cleanMarkdownCodeBlock(jsonStr);
//        // 解析得到NewAnimationData对象，变量名data
//        NewAnimationData data = JSON.parseObject(jsonStr, NewAnimationData.class);
//
//        // 结构版本校验
//        if (!"1.0".equals(data.getVersion())) {
//            throw new IllegalArgumentException("动画结构版本不匹配");
//        }
//        // 追加基础业务校验
//        if(data.getMetadata() == null){
//            throw new IllegalArgumentException("缺少metadata元信息");
//        }
//        if(data.getElements() == null || data.getElements().isEmpty()){
//            throw new IllegalArgumentException("动画元素列表不能为空");
//        }
//
//        return data;
//    }
//
//    private String cleanMarkdownCodeBlock(String raw) {
//        raw = raw.trim();
//        if (raw.startsWith("```json")) {
//            raw = raw.substring(7);
//        }
//        if (raw.endsWith("```")) {
//            raw = raw.substring(0, raw.length() - 3);
//        }
//        return raw.trim();
//    }
//}
package com.woniuxy.service.impl.animationImpl.validator;

import com.alibaba.fastjson.JSON;
import com.woniuxy.service.impl.animationImpl.dto.NewAnimationData;
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class AnimationJsonValidator {

    private static final Pattern AUTO_VALUE_PATTERN = Pattern.compile("\"(width|height)\"\\s*:\\s*\"auto\"");

    public NewAnimationData validateAndParse(String jsonStr) {
        jsonStr = cleanMarkdownCodeBlock(jsonStr);
        // 正则替换，兼容key和value之间存在空格的场景
        jsonStr = AUTO_VALUE_PATTERN.matcher(jsonStr).replaceAll("\"$1\":100");

        NewAnimationData data = JSON.parseObject(jsonStr, NewAnimationData.class);

        if (!"1.0".equals(data.getVersion())) {
            throw new IllegalArgumentException("动画结构版本不匹配");
        }
        if(data.getMetadata() == null){
            throw new IllegalArgumentException("缺少metadata元信息");
        }
        if(data.getElements() == null || data.getElements().isEmpty()){
            throw new IllegalArgumentException("动画元素列表不能为空");
        }
        return data;
    }

    private String cleanMarkdownCodeBlock(String raw) {
        raw = raw.trim();
        if (raw.startsWith("```json")) {
            raw = raw.substring(7);
        }
        if (raw.endsWith("```")) {
            raw = raw.substring(0, raw.length() - 3);
        }
        return raw.trim();
    }
}