package com.dekalib.app.bot.main_bot;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class MainConfig {
    private final String username;
    private final String token;

    @Autowired
    public MainConfig(@Value("${main_bot.token}") String token,
                      @Value("${main_bot.username}") String username) {
        this.token = token;
        this.username = username;
    }
}
