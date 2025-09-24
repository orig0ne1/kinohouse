package com.dekalib.app.bot.commands;

import com.dekalib.app.bot.application_bot.ApplicationBot;
import com.dekalib.app.data.services.DomenService;
import com.dekalib.app.data.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component("/create")
public class CreateCommand implements Command{
    private final ApplicationBot bot;
    private final UserService userService;
    private final DomenService domenService;
    @Autowired
    public CreateCommand(ApplicationBot bot,
                         UserService userService,
                         DomenService domenService) {
        this.bot = bot;
        this.userService = userService;
        this.domenService = domenService;
    }
    @Override
    public String getName() {
        return "/create";
    }

    @Override
    public void handle(Update update) {
        long chatId = update.getMessage().getChatId();
        String message = """
                Ваш промокод: %s
                Ваши ссылки:
                %s
                """.formatted(userService.getById(chatId),
                domenService.links(chatId));
        bot.send(SendMessage.builder()
                .chatId(chatId)
                .text(message)
                .build());
    }
}
