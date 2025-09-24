package com.dekalib.app.bot.application_bot;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class ApplicationBotConfig {
    private final String token;
    private final String username;
    private final Long ownerId;

    public ApplicationBotConfig(@Value("${application_bot.token}") String token,
                                @Value("${application_bot.username}") String username,
                                @Value("${application_bot.owner_id}") Long ownerId) {
        this.token = token;
        this.username = username;
        this.ownerId = ownerId;
    }
}
