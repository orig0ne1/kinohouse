package com.dekalib.app.bot.main_bot;

import com.dekalib.app.bot.Strings;
import com.dekalib.app.data.entities.Addon;
import com.dekalib.app.data.services.AddressService;
import com.dekalib.app.data.services.DomenService;
import com.dekalib.app.data.services.TarifService;
import com.dekalib.app.data.services.UserService;
import com.dekalib.app.data.services.AddonService;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@Component
public class MainBot extends TelegramLongPollingBot {
    private final MainConfig config;
    private final UserService userService;
    private final DomenService domenService;
    private final TarifService tarifService;
    private final AddonService addonService;
    private final AddressService addressService;
    private final Map<Long, String> userState = new HashMap<>();
    private final Map<Long, Integer> userAddonState = new HashMap<>();
    private final Map<Long, String> userAddonName = new HashMap<>();
    private final Map<Long, Boolean> awaitingAddress = new HashMap<>();
    private final Logger logger = Logger.getLogger(MainBot.class.getName());

    @Autowired
    public MainBot(MainConfig config,
                   UserService userService,
                   DomenService domenService,
                   TarifService tarifService,
                   AddonService addonService,
                   AddressService addressService) {
        this.config = config;
        this.userService = userService;
        this.domenService = domenService;
        this.tarifService = tarifService;
        this.addonService = addonService;
        this.addressService = addressService;
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);
        } catch (TelegramApiException e) {
            logger.severe("Error while registering bot: " + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (awaitingAddress.containsKey(chatId) && awaitingAddress.get(chatId)) {
                handleAddressInput(chatId, text);
                return;
            }

            if (userState.containsKey(chatId)) {
                handleTarifValueInput(chatId, text);
                return;
            }

            if (userAddonState.containsKey(chatId)) {
                handleAddonInput(chatId, text);
                return;
            }

            switch (text) {
                case "/start":
                    startCommand(chatId);
                    break;
                case "/create":
                    createCommand(chatId);
                    break;
                case "/manage_tariffs":
                    changeTarifCommand(chatId);
                    break;
                case "/manage_addons":
                    addAddonCommand(chatId);
                    break;
                case "/manage_addresses":
                    manageAddressesCommand(chatId);
                    break;
                default:
                    send(SendMessage.builder()
                            .chatId(chatId)
                            .text("Неизвестная команда. Используйте кнопки меню.")
                            .replyMarkup(createMainMenu())
                            .parseMode(ParseMode.MARKDOWN)
                            .build());
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
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

    private void startCommand(long chatId) {
        send(SendMessage.builder()
                .chatId(chatId)
                .text(Strings.MAIN_START_MESSAGE)
                .replyMarkup(createMainMenu())
                .parseMode(ParseMode.MARKDOWN)
                .build());
    }

    private void createCommand(long chatId) {
        String promo = userService.getById(chatId).getPromo();
        String links = domenService.links(chatId);
        send(SendMessage.builder()
                .chatId(chatId)
                .text(Strings.MAIN_SHOW_LINKS.formatted(promo, links))
                .replyMarkup(createMainMenu())
                .parseMode(ParseMode.MARKDOWN)
                .build());
    }

    private void changeTarifCommand(long chatId) {
        send(SendMessage.builder()
                .chatId(chatId)
                .text("Выберите тариф для изменения:")
                .replyMarkup(createTarifMenu())
                .parseMode(ParseMode.MARKDOWN)
                .build());
    }

    private void addAddonCommand(long chatId) {
        userAddonState.put(chatId, 0);
        userAddonName.remove(chatId);
        send(SendMessage.builder()
                .chatId(chatId)
                .text("Введите название нового аддона:")
                .replyMarkup(null)
                .parseMode(ParseMode.MARKDOWN)
                .build());
    }

    private void manageAddressesCommand(long chatId) {
        send(SendMessage.builder()
                .chatId(chatId)
                .text("Выберите действие с адресами:")
                .replyMarkup(createAddressMenu())
                .parseMode(ParseMode.MARKDOWN)
                .build());
    }

    private void addAddressCommand(long chatId) {
        awaitingAddress.put(chatId, true);
        send(SendMessage.builder()
                .chatId(chatId)
                .text("Введите адрес для добавления:")
                .replyMarkup(null)
                .parseMode(ParseMode.MARKDOWN)
                .build());
    }

    private void showAddressesCommand(long chatId) {
        String promo = userService.getById(chatId).getPromo();
        String addresses = addressService.showAddresses(promo);
        send(SendMessage.builder()
                .chatId(chatId)
                .text(addresses.isEmpty() ? "Адреса не найдены." : addresses)
                .replyMarkup(createAddressMenu())
                .parseMode(ParseMode.MARKDOWN)
                .build());
    }

    private void showAddonsCommand(long chatId) {
        String promo = userService.getById(chatId).getPromo();
        List<Addon> addons = addonService.getByPromo(promo);
        StringBuilder addonList = new StringBuilder("Текущие аддоны:\n");
        if (addons.isEmpty()) {
            addonList.append("Аддоны не найдены.");
        } else {
            for (Addon addon : addons) {
                addonList.append("ID: ").append(addon.getAddonId())
                        .append(", Название: ").append(addon.getAddonName())
                        .append(", Цена: ").append(addon.getPrice())
                        .append("\n");
            }
        }
        send(SendMessage.builder()
                .chatId(chatId)
                .text(addonList.toString())
                .replyMarkup(createManageMenu())
                .parseMode(ParseMode.MARKDOWN)
                .build());
    }

    private void handleAddressInput(long chatId, String input) {
        String promo = userService.getById(chatId).getPromo();
        addressService.insert(input, promo);
        awaitingAddress.remove(chatId);
        send(SendMessage.builder()
                .chatId(chatId)
                .text("Адрес успешно добавлен!")
                .replyMarkup(createAddressMenu())
                .parseMode(ParseMode.MARKDOWN)
                .build());
    }

    private void handleTarifValueInput(long chatId, String input) {
        String tarifName = userState.get(chatId);
        try {
            int newTarif = Integer.parseInt(input);
            String promo = userService.getById(chatId).getPromo();
            tarifService.changeTarif(promo, tarifName, newTarif);
            send(SendMessage.builder()
                    .chatId(chatId)
                    .text("Тариф *" + tarifName + "* изменён на *" + newTarif + "*!")
                    .replyMarkup(createMainMenu())
                    .parseMode(ParseMode.MARKDOWN)
                    .build());
        } catch (NumberFormatException e) {
            send(SendMessage.builder()
                    .chatId(chatId)
                    .text("Ошибка: введите целое число.")
                    .replyMarkup(createMainMenu())
                    .parseMode(ParseMode.MARKDOWN)
                    .build());
        } finally {
            userState.remove(chatId);
        }
    }

    private void handleAddonInput(long chatId, String input) {
        int state = userAddonState.get(chatId);
        String promo = userService.getById(chatId).getPromo();
        if (state == 0) {
            userAddonName.put(chatId, input);
            userAddonState.put(chatId, 1);
            send(SendMessage.builder()
                    .chatId(chatId)
                    .text("Введите цену аддона (целое число):")
                    .replyMarkup(null)
                    .parseMode(ParseMode.MARKDOWN)
                    .build());
        } else if (state == 1) {
            try {
                int price = Integer.parseInt(input);
                String name = userAddonName.get(chatId);
                Addon addon = new Addon();
                addon.setId(UUID.randomUUID().getLeastSignificantBits());
                addon.setPromo(promo);
                addon.setAddonId(name.toLowerCase().replace(" ", "_"));
                addon.setAddonName(name);
                addon.setPrice(price);
                addonService.saveAddon(addon);
                send(SendMessage.builder()
                        .chatId(chatId)
                        .text("Аддон *" + name + "* (цена: *" + price + "*) добавлен!")
                        .replyMarkup(createMainMenu())
                        .parseMode(ParseMode.MARKDOWN)
                        .build());
            } catch (NumberFormatException e) {
                send(SendMessage.builder()
                        .chatId(chatId)
                        .text("Ошибка: введите целое число для цены.")
                        .replyMarkup(null)
                        .parseMode(ParseMode.MARKDOWN)
                        .build());
            } finally {
                userAddonState.remove(chatId);
                userAddonName.remove(chatId);
            }
        }
    }

    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String[] parts = callbackData.split(":");
        String command = parts[0];

        switch (command) {
            case "general":
                send(SendMessage.builder()
                        .chatId(chatId)
                        .text("Общие команды:")
                        .replyMarkup(createGeneralMenu())
                        .parseMode(ParseMode.MARKDOWN)
                        .build());
                break;
            case "manage":
                send(SendMessage.builder()
                        .chatId(chatId)
                        .text("Управление:")
                        .replyMarkup(createManageMenu())
                        .parseMode(ParseMode.MARKDOWN)
                        .build());
                break;
            case "create":
                createCommand(chatId);
                break;
            case "manage_tariffs":
                changeTarifCommand(chatId);
                break;
            case "manage_addons":
                addAddonCommand(chatId);
                break;
            case "manage_addresses":
                manageAddressesCommand(chatId);
                break;
            case "add_address":
                addAddressCommand(chatId);
                break;
            case "show_addresses":
                showAddressesCommand(chatId);
                break;
            case "show_addons":
                showAddonsCommand(chatId);
                break;
            case "changeTarif":
                if (parts.length > 1) {
                    String tarifName = parts[1];
                    userState.put(chatId, tarifName);
                    send(SendMessage.builder()
                            .chatId(chatId)
                            .text("Введите новое значение для тарифа *" + tarifName + "* (целое число):")
                            .replyMarkup(null)
                            .parseMode(ParseMode.MARKDOWN)
                            .build());
                } else {
                    changeTarifCommand(chatId);
                }
                break;
        }
    }

    private InlineKeyboardMarkup createMainMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Общие команды")
                        .callbackData("general")
                        .build(),
                InlineKeyboardButton.builder()
                        .text("Управление")
                        .callbackData("manage")
                        .build()));
        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup createGeneralMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Создать")
                        .callbackData("create")
                        .build()));
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Назад")
                        .callbackData("start")
                        .build()));
        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup createManageMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Управление тарифами")
                        .callbackData("manage_tariffs")
                        .build()));
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Управление аддонами")
                        .callbackData("manage_addons")
                        .build()));
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Управление адресами")
                        .callbackData("manage_addresses")
                        .build()));
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Показать аддоны")
                        .callbackData("show_addons")
                        .build()));
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Назад")
                        .callbackData("start")
                        .build()));
        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup createTarifMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Стандарт")
                        .callbackData("changeTarif:standart")
                        .build()));
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Плюс")
                        .callbackData("changeTarif:plus")
                        .build()));
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("VIP")
                        .callbackData("changeTarif:vip")
                        .build()));
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Назад")
                        .callbackData("manage")
                        .build()));
        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup createAddressMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Добавить адрес")
                        .callbackData("add_address")
                        .build()));
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Показать адреса")
                        .callbackData("show_addresses")
                        .build()));
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Назад")
                        .callbackData("manage")
                        .build()));
        markup.setKeyboard(rows);
        return markup;
    }

    private void send(SendMessage msg) {
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            logger.severe("Error while sending message: " + e.getMessage());
        }
    }
}