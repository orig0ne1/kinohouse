package com.dekalib.app.bot.tp_bot;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class TPBotConfig {
    private final String token;
    private final String username;
    @Autowired
    public TPBotConfig(@Value("${tp.bot.token}") String token,
                       @Value("${tp_bot.username}") String username) {
        this.token = token;
        this.username = username;
    }
}
