package com.dekalib.app.bot;

import com.dekalib.app.data.entities.Card;
import com.dekalib.app.data.entities.Domen;
import com.dekalib.app.data.entities.UserApplication;
import com.dekalib.app.data.entities.YooKassa;
import com.dekalib.app.data.repositories.YooKassaRepository;
import com.dekalib.app.data.services.AdminService;
import com.dekalib.app.data.services.UserService;
import com.dekalib.app.data.repositories.CardRepository;
import com.dekalib.app.data.repositories.DomenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class AdminBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(AdminBot.class);
    private final UserService userService;
    private final AdminService adminService;
    private final CardRepository cardRepository;
    private final DomenRepository domenRepository;
    private final YooKassaRepository yooKassaRepository;
    private final AdminBotConfig config;
    @Value("${application_bot.owner_id}")
    private long ownerId;

    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("\\d{16}");
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9-]{1,61}[a-zA-Z0-9](?:\\.[a-zA-Z]{2,})+$");
    private final Map<Long, String> awaitingInput = new HashMap<>();
    private final Map<Long, Map<String, String>> tempData = new HashMap<>(); // For multi-step inputs

    @Autowired
    public AdminBot(UserService userService, AdminService adminService, CardRepository cardRepository,
                    DomenRepository domenRepository,
                    AdminBotConfig config, YooKassaRepository yooKassaRepository) {
        this.config = config;
        this.userService = userService;
        this.adminService = adminService;
        this.cardRepository = cardRepository;
        this.domenRepository = domenRepository;
        this.yooKassaRepository = yooKassaRepository;
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);
            logger.info("AdminBot successfully registered");
        } catch (TelegramApiException e) {
            logger.error("Failed to register AdminBot", e);
            throw new RuntimeException("AdminBot registration failed", e);
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

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            long userId = update.getMessage().getFrom().getId();

            try {
                var user = userService.getById(userId);
                if (userId != ownerId && !user.getRole().equals(Strings.ROLE_ADMIN) && !user.getRole().equals(Strings.ROLE_CARDER)) {
                    sendResponse(chatId, "Доступ запрещен. Только Admin или Carder могут использовать этого бота.");
                    return;
                }
                if (awaitingInput.containsKey(chatId)) {
                    handleInput(chatId, messageText, awaitingInput.get(chatId));
                    awaitingInput.remove(chatId);
                    return;
                }
                handleMessage(messageText, chatId, userId, user.getRole());
            } catch (TelegramApiException e) {
                logger.error("Error processing message for chatId: {}", chatId, e);
            } catch (Exception e) {
                logger.error("Unexpected error for chatId: {}", chatId, e);
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }

    private void handleInput(long chatId, String message, String inputType) throws TelegramApiException {
        try {
            switch (inputType) {
                case "addcard_id":
                    handleAddCardId(chatId, message);
                    break;
                case "addcard_bank":
                    handleAddCardBank(chatId, message);
                    break;
                case "addcard_country":
                    handleAddCardCountry(chatId, message);
                    break;
                case "deletecard":
                    handleDeleteCard(chatId, message);
                    break;
                case "adddomain":
                    handleAddDomain(chatId, message);
                    break;
                case "updatedomain":
                    handleUpdateDomain(chatId, message);
                    break;
                case "deletedomain":
                    handleDeleteDomain(chatId, message);
                    break;
                case "broadcast":
                    handleBroadcast(chatId, message);
                    break;
                case "changerole":
                    handleChangeRole(chatId, message);
                    break;
                case "getuser":
                    handleGetUser(chatId, message);
                    break;
                case "blockuser":
                    handleBlockUser(chatId, message);
                    break;
                case "unblockuser":
                    handleUnblockUser(chatId, message);
                    break;
                case "addkassa":
                    handleAddKassa(chatId, message);
                    break;
                case "deletekassa":
                    handleDeleteKassa(chatId, message);
                    break;
                default:
                    sendResponse(chatId, "Неизвестный тип ввода.");
            }
        } catch (Exception e) {
            logger.error("Error handling input for type: {}", inputType, e);
            sendResponse(chatId, "Ошибка при обработке ввода: " + e.getMessage());
        }
    }

    private void handleAddCardId(long chatId, String message) throws TelegramApiException {
        if (CARD_NUMBER_PATTERN.matcher(message).matches()) {
            tempData.computeIfAbsent(chatId, k -> new HashMap<>()).put("id", message);
            awaitingInput.put(chatId, "addcard_bank");
            sendResponse(chatId, "Введите банк:");
        } else {
            sendResponse(chatId, "Неверный формат номера карты. Введите 16 цифр.");
        }
    }

    private void handleAddCardBank(long chatId, String message) throws TelegramApiException {
        Map<String, String> cardData = tempData.get(chatId);
        if (cardData != null) {
            cardData.put("bank", message);
            awaitingInput.put(chatId, "addcard_country");
            sendResponse(chatId, "Введите страну:");
        } else {
            sendResponse(chatId, "Ошибка: данные карты не найдены.");
        }
    }

    private void handleAddCardCountry(long chatId, String message) throws TelegramApiException {
        Map<String, String> cardData = tempData.get(chatId);
        if (cardData != null) {
            Card card = new Card();
            card.setId(cardData.get("id"));
            card.setBank(cardData.get("bank"));
            card.setCountry(message);
            cardRepository.save(card);
            sendResponse(chatId, "Карта добавлена: " + maskCardNumber(card.getId()) + ", банк: " + card.getBank() + ", страна: " + card.getCountry());
            logger.info("Card added: {}", maskCardNumber(card.getId()));
            tempData.remove(chatId);
        } else {
            sendResponse(chatId, "Ошибка: данные карты не найдены.");
        }
    }

    private void handleDeleteCard(long chatId, String message) throws TelegramApiException {
        Optional<Card> cardOpt = cardRepository.findById(message);
        if (cardOpt.isPresent()) {
            cardRepository.deleteById(message);
            sendResponse(chatId, "Карта удалена: " + maskCardNumber(message));
            logger.info("Card deleted: {}", maskCardNumber(message));
        } else {
            sendResponse(chatId, "Карта не найдена.");
        }
    }

    private void handleAddDomain(long chatId, String message) throws TelegramApiException {
        Domen domen = new Domen();
        domen.setDomeName(message);
        domenRepository.save(domen);
        sendResponse(chatId, "Домен добавлен: " + message);
        logger.info("Domain added: {}", message);
    }

    private void handleUpdateDomain(long chatId, String message) throws TelegramApiException {
        String[] updateParts = message.split(" ", 2);
        if (updateParts.length == 2) {
            try {
                String id = updateParts[0];
                String newDomain = updateParts[1];
                Optional<Domen> domOpt = domenRepository.findById(id);
                if (domOpt.isPresent() && DOMAIN_PATTERN.matcher(newDomain).matches()) {
                    Domen domen = domOpt.get();
                    domen.setDomeName(newDomain);
                    domenRepository.save(domen);
                    sendResponse(chatId, "Домен обновлен: " + newDomain);
                    logger.info("Domain updated: ID {}, new name {}", id, newDomain);
                } else {
                    sendResponse(chatId, "Ошибка: домен не найден или неверный формат.");
                }
            } catch (NumberFormatException e) {
                sendResponse(chatId, "Неверный ID.");
            }
        } else {
            sendResponse(chatId, "Формат: ID новый_домен");
        }
    }

    private void handleDeleteDomain(long chatId, String message) throws TelegramApiException {
        try {
            String id = message;
            Optional<Domen> domOpt = domenRepository.findById(id);
            if (domOpt.isPresent()) {
                domenRepository.deleteById(id);
                sendResponse(chatId, "Домен удален: ID " + id);
                logger.info("Domain deleted: ID {}", id);
            } else {
                sendResponse(chatId, "Домен не найден.");
            }
        } catch (NumberFormatException e) {
            sendResponse(chatId, "Неверный ID.");
        }
    }

    private void handleBroadcast(long chatId, String message) throws TelegramApiException {
        List<UserApplication> users = userService.getUsers();
        for (UserApplication user : users) {
            try {
                sendResponse(user.getUserId(), message);
            } catch (TelegramApiException e) {
                logger.warn("Failed to send broadcast to user: {}", user.getUserId(), e);
            }
        }
        sendResponse(chatId, "Рассылка отправлена " + users.size() + " пользователям.");
        logger.info("Broadcast sent to {} users", users.size());
    }

    private void handleChangeRole(long chatId, String message) throws TelegramApiException {
        String[] roleParts = message.split(" ", 2);
        if (roleParts.length == 2) {
            try {
                long targetId = Long.parseLong(roleParts[0]);
                String newRole = roleParts[1].toUpperCase();
                if (List.of(Strings.ROLE_ADMIN, Strings.ROLE_CARDER, Strings.ROLE_USER, "BLOCKED").contains(newRole)) {
                    adminService.changeRole(targetId, newRole, ownerId);
                    sendResponse(chatId, "Роль изменена на " + newRole + " для пользователя " + targetId);
                    logger.info("Role changed for user {} to {}", targetId, newRole);
                } else {
                    sendResponse(chatId, "Недопустимая роль.");
                }
            } catch (NumberFormatException e) {
                sendResponse(chatId, "Неверный ID.");
            }
        } else {
            sendResponse(chatId, "Формат: ID роль");
        }
    }

    private void handleGetUser(long chatId, String message) throws TelegramApiException {
        try {
            long targetId = Long.parseLong(message);
            var user = userService.getById(targetId);
            if (user != null) {
                String userInfo = "Пользователь: ID " + user.getUserId() +
                        ", Роль: " + user.getRole() +
                        // Добавьте больше полей, если нужно, например: ", Имя: " + user.getUsername()
                        "";
                sendResponse(chatId, userInfo);
            } else {
                sendResponse(chatId, "Пользователь не найден.");
            }
        } catch (NumberFormatException e) {
            sendResponse(chatId, "Неверный ID.");
        }
    }

    private void handleBlockUser(long chatId, String message) throws TelegramApiException {
        try {
            long targetId = Long.parseLong(message);
            adminService.changeRole(targetId, "BLOCKED", ownerId);
            sendResponse(chatId, "Пользователь " + targetId + " заблокирован.");
            logger.info("User blocked: {}", targetId);
        } catch (NumberFormatException e) {
            sendResponse(chatId, "Неверный ID.");
        }
    }

    private void handleUnblockUser(long chatId, String message) throws TelegramApiException {
        try {
            long targetId = Long.parseLong(message);
            adminService.changeRole(targetId, Strings.ROLE_USER, ownerId);
            sendResponse(chatId, "Пользователь " + targetId + " разблокирован.");
            logger.info("User unblocked: {}", targetId);
        } catch (NumberFormatException e) {
            sendResponse(chatId, "Неверный ID.");
        }
    }

    private void handleAddKassa(long chatId, String message) throws TelegramApiException {
        String[] parts = message.split(" ");
        if (parts.length == 3) {
            String shopId = parts[0];
            String secretKey = parts[1];
            String idempotenceKey = parts[2];
            YooKassa kassa = new YooKassa();
            kassa.setShopId(shopId);
            kassa.setSecretKey(secretKey);
            kassa.setIdempotenceKey(idempotenceKey);
            yooKassaRepository.save(kassa);
            sendResponse(chatId, "Новая YooKassa добавлена: Shop ID " + shopId);
            logger.info("YooKassa added: Shop ID {}", shopId);
        } else {
            sendResponse(chatId, "Формат: shopId secretKey idempotenceKey");
        }
    }

    private void handleDeleteKassa(long chatId, String message) throws TelegramApiException {
        try {
            String id = message;
            Optional<YooKassa> kassaOpt = yooKassaRepository.findById(id);
            if (kassaOpt.isPresent()) {
                yooKassaRepository.deleteById(id);
                sendResponse(chatId, "YooKassa удалена: ID " + id);
                logger.info("YooKassa deleted: ID {}", id);
            } else {
                sendResponse(chatId, "YooKassa не найдена.");
            }
        } catch (NumberFormatException e) {
            sendResponse(chatId, "Неверный ID.");
        }
    }

    private void handleMessage(String message, long chatId, long userId, String role) throws TelegramApiException {
        if (message.startsWith("/")) {
            String[] parts = message.split(" ", 2);
            String command = parts[0].toLowerCase();
            switch (command) {
                case "/start":
                    sendResponse(chatId, "Добро пожаловать в AdminBot!", createMainMenu());
                    break;
                case "/changerole":
                    if (!role.equals(Strings.ROLE_ADMIN)) {
                        sendResponse(chatId, "Только Admin может менять роли.");
                        return;
                    }
                    if (parts.length > 1) {
                        handleChangeRole(chatId, parts[1]);
                    } else {
                        sendResponse(chatId, "Формат: /changerole <userId> <role>");
                    }
                    break;
                default:
                    sendResponse(chatId, "Неизвестная команда. Используйте кнопки меню.", createMainMenu());
            }
        } else {
            sendResponse(chatId, "Используйте команды или кнопки меню.", createMainMenu());
        }
    }

    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        try {
            switch (callbackData) {
                case "main":
                    handleMainMenu(chatId);
                    break;
                case "user_management":
                    handleUserManagement(chatId);
                    break;
                case "card_management":
                    handleCardManagement(chatId);
                    break;
                case "domain_management":
                    handleDomainManagement(chatId);
                    break;
                case "yookassa_management":
                    handleYooKassaManagement(chatId);
                    break;
                case "addcard":
                    handleAddCard(chatId);
                    break;
                case "listcards":
                    handleListCards(chatId);
                    break;
                case "deletecard":
                    handleDeleteCardPrompt(chatId);
                    break;
                case "adddomain":
                    handleAddDomainPrompt(chatId);
                    break;
                case "listdomains":
                    handleListDomains(chatId);
                    break;
                case "updatedomain":
                    handleUpdateDomainPrompt(chatId);
                    break;
                case "deletedomain":
                    handleDeleteDomainPrompt(chatId);
                    break;
                case "broadcast":
                    handleBroadcastPrompt(chatId);
                    break;
                case "changerole":
                    handleChangeRolePrompt(chatId);
                    break;
                case "getuser":
                    handleGetUserPrompt(chatId);
                    break;
                case "blockuser":
                    handleBlockUserPrompt(chatId);
                    break;
                case "unblockuser":
                    handleUnblockUserPrompt(chatId);
                    break;
                case "addkassa":
                    handleAddKassaPrompt(chatId);
                    break;
                case "listkassa":
                    handleListKassa(chatId);
                    break;
                case "deletekassa":
                    handleDeleteKassaPrompt(chatId);
                    break;
                default:
                    sendResponse(chatId, "Неизвестная команда.");
            }
        } catch (TelegramApiException e) {
            logger.error("Error processing callback query for chatId: {}", chatId, e);
            try {
                sendResponse(chatId, "Ошибка при обработке команды.");
            } catch (TelegramApiException ex) {
                logger.error("Failed to send error response to chatId: {}", chatId, ex);
            }
        }
    }

    private void handleMainMenu(long chatId) throws TelegramApiException {
        sendResponse(chatId, "Главное меню:", createMainMenu());
    }

    private void handleUserManagement(long chatId) throws TelegramApiException {
        sendResponse(chatId, "Управление пользователями:", createUserMenu());
    }

    private void handleCardManagement(long chatId) throws TelegramApiException {
        sendResponse(chatId, "Управление картами:", createCardMenu());
    }

    private void handleDomainManagement(long chatId) throws TelegramApiException {
        sendResponse(chatId, "Управление доменами:", createDomainMenu());
    }

    private void handleYooKassaManagement(long chatId) throws TelegramApiException {
        sendResponse(chatId, "Управление YooKassa:", createYooKassaMenu());
    }

    private void handleAddCard(long chatId) throws TelegramApiException {
        awaitingInput.put(chatId, "addcard_id");
        sendResponse(chatId, "Введите номер карты (16 цифр):");
    }

    private void handleListCards(long chatId) throws TelegramApiException {
        List<Card> cards = (List<Card>) cardRepository.findAll();
        if (cards.isEmpty()) {
            sendResponse(chatId, "Нет доступных карт.");
            return;
        }
        StringBuilder cardList = new StringBuilder("Список карт:\n");
        for (Card card : cards) {
            cardList.append("ID: ").append(maskCardNumber(card.getId()))
                    .append(", Банк: ").append(card.getBank())
                    .append(", Страна: ").append(card.getCountry()).append("\n");
        }
        sendResponse(chatId, cardList.toString());
    }

    private void handleDeleteCardPrompt(long chatId) throws TelegramApiException {
        awaitingInput.put(chatId, "deletecard");
        sendResponse(chatId, "Введите ID карты для удаления:");
    }

    private void handleAddDomainPrompt(long chatId) throws TelegramApiException {
        awaitingInput.put(chatId, "adddomain");
        sendResponse(chatId, "Введите домен (например, example.com):");
    }

    private void handleListDomains(long chatId) throws TelegramApiException {
        List<Domen> domains = (List<Domen>) domenRepository.findAll();
        if (domains.isEmpty()) {
            sendResponse(chatId, "Нет доступных доменов.");
            return;
        }
        StringBuilder domainList = new StringBuilder("Список доменов:\n");
        for (Domen domain : domains) {
            domainList.append("ID: ").append(domain.getId())
                    .append(", Название: ").append(domain.getDomeName()).append("\n");
        }
        sendResponse(chatId, domainList.toString());
    }

    private void handleUpdateDomainPrompt(long chatId) throws TelegramApiException {
        awaitingInput.put(chatId, "updatedomain");
        sendResponse(chatId, "Введите ID домена и новый домен (формат: ID новый_домен):");
    }

    private void handleDeleteDomainPrompt(long chatId) throws TelegramApiException {
        awaitingInput.put(chatId, "deletedomain");
        sendResponse(chatId, "Введите ID домена для удаления:");
    }

    private void handleBroadcastPrompt(long chatId) throws TelegramApiException {
        awaitingInput.put(chatId, "broadcast");
        sendResponse(chatId, "Введите сообщение для рассылки:");
    }

    private void handleChangeRolePrompt(long chatId) throws TelegramApiException {
        awaitingInput.put(chatId, "changerole");
        sendResponse(chatId, "Введите ID пользователя и новую роль (ADMIN, CARDER, USER, BLOCKED):");
    }

    private void handleGetUserPrompt(long chatId) throws TelegramApiException {
        awaitingInput.put(chatId, "getuser");
        sendResponse(chatId, "Введите ID пользователя для получения данных:");
    }

    private void handleBlockUserPrompt(long chatId) throws TelegramApiException {
        awaitingInput.put(chatId, "blockuser");
        sendResponse(chatId, "Введите ID пользователя для блокировки:");
    }

    private void handleUnblockUserPrompt(long chatId) throws TelegramApiException {
        awaitingInput.put(chatId, "unblockuser");
        sendResponse(chatId, "Введите ID пользователя для разблокировки:");
    }

    private void handleAddKassaPrompt(long chatId) throws TelegramApiException {
        awaitingInput.put(chatId, "addkassa");
        sendResponse(chatId, "Введите shopId secretKey idempotenceKey (через пробелы):");
    }

    private void handleListKassa(long chatId) throws TelegramApiException {
        List<YooKassa> kassas = (List<YooKassa>) yooKassaRepository.findAll();
        if (kassas.isEmpty()) {
            sendResponse(chatId, "Нет доступных YooKassa.");
            return;
        }
        StringBuilder kassaList = new StringBuilder("Список YooKassa:\n");
        for (YooKassa kassa : kassas) {
            kassaList.append("ID: ").append(kassa.getShopId())
                    .append(", Shop ID: ").append(kassa.getShopId())
                    .append(", Idempotence Key: ").append(kassa.getIdempotenceKey())
                    .append(" (Secret Key скрыт для безопасности)\n");
        }
        sendResponse(chatId, kassaList.toString());
    }

    private void handleDeleteKassaPrompt(long chatId) throws TelegramApiException {
        awaitingInput.put(chatId, "deletekassa");
        sendResponse(chatId, "Введите ID YooKassa для удаления:");
    }

    private void sendResponse(long chatId, String text) throws TelegramApiException {
        sendResponse(chatId, text, null);
    }

    private void sendResponse(long chatId, String text, InlineKeyboardMarkup markup) throws TelegramApiException {
        SendMessage messageObj = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .parseMode(ParseMode.MARKDOWN)
                .replyMarkup(markup)
                .build();
        execute(messageObj);
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber.length() < 16) return cardNumber;
        return cardNumber.substring(0, 4) + "****" + cardNumber.substring(12);
    }

    private InlineKeyboardMarkup createMainMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(InlineKeyboardButton.builder().text("Управление пользователями").callbackData("user_management").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("Управление картами").callbackData("card_management").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("Управление доменами").callbackData("domain_management").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("Управление YooKassa").callbackData("yookassa_management").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("Рассылка").callbackData("broadcast").build()));
        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup createUserMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(
                InlineKeyboardButton.builder().text("Изменить роль").callbackData("changerole").build(),
                InlineKeyboardButton.builder().text("Получить данные").callbackData("getuser").build()));
        rows.add(List.of(
                InlineKeyboardButton.builder().text("Заблокировать").callbackData("blockuser").build(),
                InlineKeyboardButton.builder().text("Разблокировать").callbackData("unblockuser").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("Назад").callbackData("main").build()));
        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup createCardMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(
                InlineKeyboardButton.builder().text("Добавить карту").callbackData("addcard").build(),
                InlineKeyboardButton.builder().text("Список карт").callbackData("listcards").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("Удалить карту").callbackData("deletecard").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("Назад").callbackData("main").build()));
        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup createDomainMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(
                InlineKeyboardButton.builder().text("Добавить домен").callbackData("adddomain").build(),
                InlineKeyboardButton.builder().text("Список доменов").callbackData("listdomains").build()));
        rows.add(List.of(
                InlineKeyboardButton.builder().text("Обновить домен").callbackData("updatedomain").build(),
                InlineKeyboardButton.builder().text("Удалить домен").callbackData("deletedomain").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("Назад").callbackData("main").build()));
        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup createYooKassaMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(
                InlineKeyboardButton.builder().text("Добавить YooKassa").callbackData("addkassa").build(),
                InlineKeyboardButton.builder().text("Список YooKassa").callbackData("listkassa").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("Удалить YooKassa").callbackData("deletekassa").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("Назад").callbackData("main").build()));
        markup.setKeyboard(rows);
        return markup;
    }
}