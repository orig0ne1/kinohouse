package com.dekalib.app.bot.commands;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface Command {
    String getName();
    void handle(Update update);
}
