package com.dekalib.app.bot.work_bot;

import com.dekalib.app.bot.commands.Command;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

@Component
public class Bot extends TelegramLongPollingBot {
    private final BotConfig config;
    private static final Logger logger = Logger.getLogger(Bot.class.getName());

    @Autowired
    public Bot(BotConfig config) {
        this.config = config;
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);
        } catch (TelegramApiException e) {
            logger.severe("Error registering bot: " + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            ApplicationContext context = new AnnotationConfigApplicationContext("com.dekalib.app");
            Command command = (Command) context.getBean(text);
            command.handle(update);
        }
    }

    @Override
    public String getBotUsername() {
        return config.getUsername();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    public void send(SendMessage msg) {
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            logger.severe("Error sending message: " + e.getMessage() + ", message: " + msg.toString());
        }
    }

    public void sendDocument(String chatId, MultipartFile file, String caption) {
        try {
            SendDocument document = new SendDocument();
            document.setChatId(String.valueOf(chatId));
            document.setDocument(new InputFile(file.getInputStream(), file.getOriginalFilename()));
            document.setCaption(caption);
            execute(document);
        } catch (TelegramApiException e) {
            logger.severe("Error sending document: " + e.getMessage());
        } catch (IOException e) {
            logger.severe("Error reading file: " + e.getMessage());
        }
    }
}