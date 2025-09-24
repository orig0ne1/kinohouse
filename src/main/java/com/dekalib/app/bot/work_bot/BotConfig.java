package com.dekalib.app.bot.work_bot;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class BotConfig {
    private final String token;
    private final String username;

    public BotConfig(@Value("${bot.token}") String token,
                     @Value("${bot.username}") String username) {
        this.token = token;
        this.username = username;
    }


}
