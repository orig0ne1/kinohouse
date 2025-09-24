package com.dekalib.app.bot.application_bot;

import com.dekalib.app.bot.Strings;
import com.dekalib.app.data.entities.UserApplication;
import com.dekalib.app.data.repositories.UserApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Component
public class ApplicationBot extends TelegramLongPollingBot {
    private static final Logger logger = Logger.getLogger(ApplicationBot.class.getName());
    private final ApplicationBotConfig config;
    private final UserApplicationRepository userRepository;
    private enum State {
        ASK_SOURCE,
        ASK_EXPERIENCE,
        COMPLETED
    }

    private final Map<Long, State> userStates = new HashMap<>();

    @Autowired
    public ApplicationBot(ApplicationBotConfig config,
                          UserApplicationRepository userRepository) {
        this.config = config;
        this.userRepository = userRepository;
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);
        } catch (TelegramApiException e) {
            logger.severe("Error registering bot: " + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long userId = update.getMessage().getFrom().getId();
            String username = update.getMessage().getFrom().getUserName();
            String messageText = update.getMessage().getText();
            State state = userStates.getOrDefault(userId, null);

            try {
                if (state == null && messageText.equals("/start")) {
                    userStates.put(userId, State.ASK_SOURCE);
                    UserApplication userApp = new UserApplication();
                    userApp.setUserId(userId);
                    userApp.setUsername(username);
                    userApp.setRole(Strings.ROLE_USER);
                    userApp.setRegistrationDate(System.currentTimeMillis());
                    userRepository.save(userApp);
                    sendTextMessage(userId, Strings.APP_BOT_START_MESSAGE);
                    sendTextMessage(userId, Strings.APP_FIRST_QUESTION);
                } else if (state == State.ASK_SOURCE) {
                    UserApplication userApp = userRepository.findById(userId).orElseThrow();
                    userApp.setSourceInfo(messageText);
                    userRepository.save(userApp);
                    userStates.put(userId, State.ASK_EXPERIENCE);
                    sendTextMessage(userId, Strings.APP_SECOND_QUESTION);
                } else if (state == State.ASK_EXPERIENCE) {
                    UserApplication userApp = userRepository.findById(userId).orElseThrow();
                    userApp.setExperience(messageText);
                    userRepository.save(userApp);
                    userStates.put(userId, State.COMPLETED);
                    sendTextMessage(userId, Strings.APP_APPLICATION_WAIT);
                    sendAdminNotification(userApp);
                } else if (state == State.COMPLETED) {
                    sendTextMessage(userId, Strings.APP_ALSO_COMPLETED);
                }
            } catch (TelegramApiException e) {
                logger.severe("Error processing message: " + e.getMessage());
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            Long userId = Long.parseLong(callbackData.split("_")[1]);
            try {
                UserApplication userApp = userRepository.findById(userId).orElseThrow();
                if (callbackData.startsWith("accept_")) {
                    userApp.setRole(Strings.ROLE_USER);
                    userRepository.save(userApp);
                    sendTextMessage(userId, Strings.APP_APPLICATION_CONFIRMED);
                } else if (callbackData.startsWith("reject_")) {
                    userRepository.delete(userApp);
                    sendTextMessage(userId, Strings.APP_APPLICATION_REJECTED);
                }
                userStates.remove(userId);
            } catch (TelegramApiException e) {
                logger.severe("Error processing callback: " + e.getMessage());
            }
        }
    }

    private void sendAdminNotification(UserApplication userApp) throws TelegramApiException {
        String message = String.format(Strings.APP_OWNER_NOTIFICATION,
                userApp.getUserId(), userApp.getUsername(), userApp.getSourceInfo(), userApp.getExperience());
        SendMessage adminMessage = new SendMessage();
        adminMessage.setChatId(config.getOwnerId().toString());
        adminMessage.setText(message);
        adminMessage.setParseMode(ParseMode.MARKDOWN);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = List.of(
                List.of(
                        InlineKeyboardButton.builder().text("Принять").callbackData("accept_" + userApp.getUserId()).build(),
                        InlineKeyboardButton.builder().text("Отклонить").callbackData("reject_" + userApp.getUserId()).build()
                )
        );
        markup.setKeyboard(rows);
        adminMessage.setReplyMarkup(markup);
        execute(adminMessage);
    }

    private void sendTextMessage(Long chatId, String text) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode(ParseMode.MARKDOWN);
        execute(message);
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
            logger.severe("Error while sending Message: " + e.getMessage());
        }
    }
}