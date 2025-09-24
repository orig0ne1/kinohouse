package com.dekalib.app.bot;

import com.dekalib.app.bot.work_bot.Bot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;

@Component
public class Notifications {
    private final Bot bot;
    private final long adminChatId;

    @Autowired
    public Notifications(@Value("${admin_chat.id}") long adminChatId,
                         Bot bot) {
        this.bot = bot;
        this.adminChatId = adminChatId;
    }

    public void sendMessage(String chatId, String message) throws IOException, InterruptedException {
        bot.send(SendMessage.builder()
                .parseMode(ParseMode.MARKDOWN)
                .text(message)
                .chatId(chatId).build());
        bot.send(SendMessage.builder()
                .parseMode(ParseMode.MARKDOWN)
                .text(message)
                .chatId(adminChatId).build());
    }

    public void sendDocument(long userChatId, MultipartFile document, String caption) throws IOException, InterruptedException {
        // Create a temporary file to store the uploaded document
        File tempFile = File.createTempFile("temp", document.getOriginalFilename());
        document.transferTo(tempFile); // Transfer the MultipartFile to the temporary file

        // Send document to the user
        bot.sendDocument(String.valueOf(userChatId), document, caption);

        // Send document to the admin chat
        bot.sendDocument(String.valueOf(adminChatId), document, caption);

        // Delete the temporary file after sending
        tempFile.delete();
    }
}