package com.skala.minutes;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.skala.minutes.config.TokenUsageAdvisor;
import com.skala.minutes.support.FakeChatModel;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Advisor 가 체인에서 실제로 실행되는지 확인한다. */
class TokenUsageAdvisorTest {

    private static ListAppender<ILoggingEvent> attachAppender() {
        ch.qos.logback.classic.Logger logger =
                ((LoggerContext) LoggerFactory.getILoggerFactory())
                        .getLogger(TokenUsageAdvisor.class);
        logger.setLevel(Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static ChatClient client() {
        return ChatClient.builder(new FakeChatModel("요약 결과"))
                .defaultAdvisors(new TokenUsageAdvisor())
                .build();
    }

    @Test
    void call_경로에서_Advisor가_실행된다() {
        ListAppender<ILoggingEvent> logs = attachAppender();

        client().prompt().user("회의록").call().content();

        List<String> messages = logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).isNotEmpty();
        assertThat(messages.get(0)).contains("[call]");
    }

    @Test
    void stream_경로에서_Advisor가_실행된다() {
        ListAppender<ILoggingEvent> logs = attachAppender();

        client().prompt().user("회의록").stream().content().blockLast();

        List<String> messages = logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).isNotEmpty();
        assertThat(messages.get(0)).contains("[stream]");
    }
}
