package com.dekalib.app.bot.commands;

import com.dekalib.app.bot.work_bot.Bot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component("/start")
public class StartCommand implements Command{
    private final Bot bot;

    @Autowired
    public StartCommand(Bot bot) {
        this.bot = bot;
    }
    @Override
    public String getName() {
        return "/start";
    }

    @Override
    public void handle(Update update) {
        long chatId = update.getMessage().getChatId();
        bot.send(SendMessage.builder()
                .chatId(chatId)
                .text("test")
                .build());
    }
}
