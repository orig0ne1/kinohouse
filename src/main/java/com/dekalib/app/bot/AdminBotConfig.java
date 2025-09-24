package com.dekalib.app.bot;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AdminBotConfig {
    @Value("${admin_bot.token}")
    private String token;

    @Value("${admin_bot.username}")
    private String username;

}
