package com.dekalib.app.bot.tp_bot;

import com.dekalib.app.bot.Strings;
import com.dekalib.app.data.services.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.List;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TPBot extends TelegramLongPollingBot {
    private final TPBotConfig config;
    private final AddressService addressService;
    private final Logger logger = Logger.getLogger(TPBot.class.getName());

    @Value("${tp.id}")
    private long ADMIN_CHAT_ID;

    private final ConcurrentHashMap<Long, Boolean> awaitingAppealText = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Boolean> awaitingAppealMedia = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> appealTextMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> appealMessageToUser = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> appealMedia = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Boolean> awaitingAddress = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Boolean> awaitingReplyText = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Boolean> awaitingReplyMedia = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> replyTextMap = new ConcurrentHashMap<>();
    private Long replyToUserId = null;

    @Autowired
    public TPBot(TPBotConfig config, AddressService addressService) {
        this.config = config;
        this.addressService = addressService;
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);
        } catch (TelegramApiException e) {
            logger.severe("Error while registering bot");
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Long chatId = update.getMessage().getChatId();
            Long userId = update.getMessage().getFrom().getId();

            if (Boolean.TRUE.equals(awaitingAddress.get(userId))) {
                if (update.getMessage().hasText()) {
                    String addressText = update.getMessage().getText();
                    String promo = update.getMessage().getFrom().getUserName() != null ?
                            update.getMessage().getFrom().getUserName() : String.valueOf(userId);
                    addressService.insert(addressText, promo);
                    awaitingAddress.remove(userId);
                    sendMessage(SendMessage.builder()
                            .chatId(chatId)
                            .text("Адрес успешно добавлен.")
                            .parseMode(ParseMode.MARKDOWN)
                            .build());
                    return;
                }
            }

            if (chatId == ADMIN_CHAT_ID && replyToUserId != null && Boolean.TRUE.equals(awaitingReplyText.get(chatId))) {
                if (update.getMessage().hasText()) {
                    String text = update.getMessage().getText();
                    replyTextMap.put(chatId, text);
                    awaitingReplyText.remove(chatId);
                    awaitingReplyMedia.put(chatId, true);
                    sendMessage(SendMessage.builder()
                            .chatId(ADMIN_CHAT_ID)
                            .text("Отправьте фото, видео или документ (или напишите 'skip').")
                            .parseMode(ParseMode.MARKDOWN)
                            .build());
                    return;
                }
            }

            if (chatId == ADMIN_CHAT_ID && replyToUserId != null && Boolean.TRUE.equals(awaitingReplyMedia.get(chatId))) {
                if (update.getMessage().hasText() && "skip".equalsIgnoreCase(update.getMessage().getText())) {
                    sendReplyToUser(replyToUserId, replyTextMap.get(chatId), null);
                    clearReplyState(chatId);
                    return;
                } else if (update.getMessage().hasPhoto() || update.getMessage().hasVideo() || update.getMessage().hasDocument()) {
                    String mediaId = getMediaId(update.getMessage());
                    sendReplyToUser(replyToUserId, replyTextMap.get(chatId), mediaId);
                    clearReplyState(chatId);
                    return;
                }
            }

            if (Boolean.TRUE.equals(awaitingAppealText.get(userId))) {
                if (update.getMessage().hasText()) {
                    appealTextMap.put(userId, update.getMessage().getText());
                    awaitingAppealText.remove(userId);
                    awaitingAppealMedia.put(userId, true);
                    sendMessage(SendMessage.builder()
                            .chatId(chatId)
                            .text("Отправьте фото, видео или документ (или напишите 'skip').")
                            .parseMode(ParseMode.MARKDOWN)
                            .build());
                    return;
                }
            }

            if (Boolean.TRUE.equals(awaitingAppealMedia.get(userId))) {
                if (update.getMessage().hasText() && "skip".equalsIgnoreCase(update.getMessage().getText())) {
                    sendAppealToAdmin(userId, chatId, appealTextMap.get(userId), null, update);
                    clearAppealState(userId);
                    return;
                } else if (update.getMessage().hasPhoto() || update.getMessage().hasVideo() || update.getMessage().hasDocument()) {
                    String mediaId = getMediaId(update.getMessage());
                    sendAppealToAdmin(userId, chatId, appealTextMap.get(userId), mediaId, update);
                    clearAppealState(userId);
                    return;
                }
            }

            if (update.hasMessage() && update.getMessage().hasText()) {
                String text = update.getMessage().getText();
                switch (text) {
                    case "/start":
                        start(update);
                        break;
                    case "/create":
                        createAppeal(update);
                        break;
                    default:
                        msgNotSupported(update);
                }
            } else {
                msgNotSupported(update);
            }
        } else if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            Long userId = update.getCallbackQuery().getFrom().getId();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Long messageId = update.getCallbackQuery().getMessage().getMessageId().longValue();

            if ("create".equals(data)) {
                awaitingAppealText.put(userId, true);
                sendMessage(SendMessage.builder()
                        .chatId(chatId)
                        .text("Напишите текст обращения.")
                        .parseMode(ParseMode.MARKDOWN)
                        .build());
            } else if ("reply".equals(data) && chatId == ADMIN_CHAT_ID) {
                Long targetUserId = appealMessageToUser.get(messageId);
                if (targetUserId != null) {
                    replyToUserId = targetUserId;
                    awaitingReplyText.put(chatId, true);
                    sendMessage(SendMessage.builder()
                            .chatId(ADMIN_CHAT_ID)
                            .text("Введите текст ответа.")
                            .parseMode(ParseMode.MARKDOWN)
                            .build());
                }
            }
        }
    }

    private String getMediaId(Message message) {
        if (message.hasPhoto()) {
            return message.getPhoto().get(message.getPhoto().size() - 1).getFileId();
        } else if (message.hasVideo()) {
            return message.getVideo().getFileId();
        } else if (message.hasDocument()) {
            return message.getDocument().getFileId();
        }
        return null;
    }

    private void sendReplyToUser(Long userId, String text, String mediaId) {
        sendMessage(SendMessage.builder()
                .chatId(userId)
                .text("Ответ от администратора:\n" + text)
                .parseMode(ParseMode.MARKDOWN)
                .build());
        if (mediaId != null) {
            sendMedia(userId, mediaId);
        }
        sendMessage(SendMessage.builder()
                .chatId(ADMIN_CHAT_ID)
                .text("Ответ отправлен пользователю.")
                .parseMode(ParseMode.MARKDOWN)
                .build());
        replyToUserId = null;
    }

    private void sendMedia(Long chatId, String mediaId) {
        sendPhoto(SendPhoto.builder()
                .chatId(chatId.toString())
                .photo(new InputFile(mediaId))
                .build());
    }

    private void clearReplyState(Long chatId) {
        awaitingReplyText.remove(chatId);
        awaitingReplyMedia.remove(chatId);
        replyTextMap.remove(chatId);
        replyToUserId = null;
    }

    private void clearAppealState(Long userId) {
        awaitingAppealText.remove(userId);
        awaitingAppealMedia.remove(userId);
        appealTextMap.remove(userId);
        appealMedia.remove(userId);
    }

    private void start(Update update) {
        sendMessage(SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .text(Strings.TP_START_COMMAND_MESSAGE.formatted(
                        update.getMessage().getFrom().getFirstName(),
                        update.getMessage().getFrom().getUserName()))
                .parseMode(ParseMode.MARKDOWN)
                .disableWebPagePreview(true)
                .build());
    }

    public void msgNotSupported(Update update) {
        sendMessage(SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .text(Strings.TP_NOT_SUPPORTED_MESSAGE)
                .parseMode(ParseMode.MARKDOWN)
                .disableWebPagePreview(true)
                .build());
    }

    public void createAppeal(Update update) {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder()
                                .text("Создать обращение")
                                .callbackData("create")
                                .build()))
                .build();
        sendMessage(SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .text(Strings.TP_CREATE_APPEAL_MESSAGE)
                .replyMarkup(markup)
                .parseMode(ParseMode.MARKDOWN)
                .disableWebPagePreview(true)
                .build());
    }

    private void sendAppealToAdmin(Long userId, Long chatId, String appealText, String mediaId, Update update) {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder()
                                .text("Ответить")
                                .callbackData("reply")
                                .build()))
                .build();
        String userName = update.getMessage().getFrom().getUserName() != null ?
                "@" + update.getMessage().getFrom().getUserName() : "Пользователь";

        String message = String.format("Обращение от пользователя: [%s](tg://user?id=%d)\n%s",
                userName, userId, appealText);

        SendMessage sm = SendMessage.builder()
                .chatId(String.valueOf(ADMIN_CHAT_ID))
                .text(message)
                .replyMarkup(markup)
                .parseMode(ParseMode.MARKDOWN)
                .build();

        Message sentMessage = sendMessageWithReturn(sm);
        if (sentMessage != null) {
            appealMessageToUser.put(sentMessage.getMessageId().longValue(), userId);
            if (mediaId != null) {
                sendMedia(ADMIN_CHAT_ID, mediaId);
            }
        }
        sendMessage(SendMessage.builder()
                .chatId(chatId)
                .text("Обращение отправлено администратору.")
                .parseMode(ParseMode.MARKDOWN)
                .build());
    }

    @Override
    public String getBotUsername() {
        return config.getUsername();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    public void sendMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.severe("Error while sending message");
        }
    }

    private Message sendMessageWithReturn(SendMessage message) {
        try {
            return execute(message);
        } catch (TelegramApiException e) {
            logger.severe("Error while sending message");
            return null;
        }
    }

    private void sendPhoto(SendPhoto photo) {
        try {
            execute(photo);
        } catch (TelegramApiException e) {
            logger.severe("Error while sending photo");
        }
    }
}