package com.example.chatbot.config;

import org.springframework.ai.ollama.api.ThinkOption;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * application.yml 의 문자열을 Ollama 의 think 설정으로 바꾼다.
 *
 * Spring AI 1.1.8 은 spring.ai.ollama.chat.options.think-option 이라는 프로퍼티를
 * 만들어 두고 JSON 직렬화기까지 넣어 놨지만, 설정 바인딩용 Converter 는 빠뜨렸다.
 * 그래서 yml 에 값을 적으면 "No converter found" 로 기동이 멈춘다.
 * 이 클래스 하나로 그 구멍을 메운다.
 *
 * 이걸 자바 코드(AiConfig)에서 직접 부르지 않고 굳이 설정으로 남기는 이유는,
 * model · temperature · num-ctx 같은 다른 모델 옵션이 전부 yml 에 있기 때문이다.
 * 모델을 손보려는 사람이 두 군데를 뒤지게 만들지 않는다.
 */
@Component
@ConfigurationPropertiesBinding
public class ThinkOptionConverter implements Converter<String, ThinkOption> {

    @Override
    public ThinkOption convert(String source) {
        return switch (source.trim().toLowerCase()) {
            case "true"   -> ThinkOption.ThinkBoolean.ENABLED;
            case "false"  -> ThinkOption.ThinkBoolean.DISABLED;
            case "low"    -> ThinkOption.ThinkLevel.LOW;
            case "medium" -> ThinkOption.ThinkLevel.MEDIUM;
            case "high"   -> ThinkOption.ThinkLevel.HIGH;
            default -> throw new IllegalArgumentException(
                    "think-option 은 true · false · low · medium · high 중 하나여야 한다: " + source);
        };
    }
}
