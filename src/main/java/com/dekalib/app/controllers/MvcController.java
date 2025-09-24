package com.dekalib.app.controllers;

import com.dekalib.app.bot.Notifications;
import com.dekalib.app.bot.Strings;
import com.dekalib.app.bot.work_bot.Bot;
import com.dekalib.app.data.entities.Addon;
import com.dekalib.app.data.entities.Tarif;
import com.dekalib.app.data.repositories.CardRepository;
import com.dekalib.app.data.services.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

@Controller
public class MvcController {
    private final Bot bot;
    private final UserService userService;
    private static final Logger logger = Logger.getLogger(MvcController.class.getName());
    private final CardService cardService;
    private final Notifications notifications;
    private final AddonService addonService;
    private final TarifService tarifService;
    private final DomenService domenService;

    @Autowired
    public MvcController(Bot bot, UserService userService,
                         CardService cardService, Notifications notifications,
                         AddonService addonService, TarifService tarifService,
                         DomenService domenService) {
        this.domenService = domenService;
        this.addonService = addonService;
        this.tarifService = tarifService;
        this.notifications = notifications;
        this.bot = bot;
        this.userService = userService;
        this.cardService = cardService;
    }

    @GetMapping("/main")
    public String getMain(@RequestParam("promo") String promo, Model model,
                          HttpServletRequest request) {
        model.addAttribute("promo", promo);
        try {
            notifications.sendMessage(String.valueOf(userService.getIdByPromo(promo)),
                    Strings.WEB_MAIN_NOTIFICATION.formatted(IP.get(request), promo));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "/main";
    }

    @GetMapping("/book")
    public String book(@RequestParam("promo") String promo, Model model,
                       @RequestParam("tariff") String tariff,
                       @RequestParam("totalPrice") String totalPrice,
                       HttpServletRequest request) {
        model.addAttribute("promo", promo);
        model.addAttribute("tariff", tariff);
        model.addAttribute("totalPrice", totalPrice);
        try {
            notifications.sendMessage(String.valueOf(userService.getIdByPromo(promo)),
                    Strings.WEB_BOOK_NOTIFICATION.formatted(IP.get(request), promo));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "/book";
    }

    @GetMapping("/payment")
    public String payment(@RequestParam("promo") String promo,
                          @RequestParam("fullname") String fullname,
                          @RequestParam("phone") String phone,
                          @RequestParam("email") String email,
                          @RequestParam("address") String address, Model model,
                          @RequestParam("totalPrice") String totalPrice,
                          @RequestParam("tariff") String tariff,
                          HttpServletRequest request) {
        model.addAttribute("promo", promo);
        model.addAttribute("card", cardService.getCard());
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("return_url", domenService.current());
        model.addAttribute("shop_id", cardService.getKassa().getShopId());
        model.addAttribute("secret_key", cardService.getKassa().getSecretKey());
        try {
            notifications.sendMessage(String.valueOf(userService.getIdByPromo(promo)),
                    Strings.WEB_CONTACT_INFO_NOTIFICATION.formatted(IP.get(request), userService.getIdByPromo(promo), fullname, phone, email, address, totalPrice, tariff));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "/payment";
    }

    @PostMapping("/uploadReceipt")
    public String uploadReceipt(@RequestParam("promo") String promo,
                                @RequestParam("file") MultipartFile file,
                                Model model) {
        try {
            if (!file.isEmpty() && (file.getContentType().equals("image/jpeg") ||
                    file.getContentType().equals("image/png") ||
                    file.getContentType().equals("application/pdf"))) {
                if (file.getSize() <= 5 * 1024 * 1024) {
                    long userId = userService.getIdByPromo(promo);
                    SendDocument document = SendDocument.builder()
                            .chatId(String.valueOf(userId))
                            .document(new InputFile(new ByteArrayInputStream(file.getBytes()), file.getOriginalFilename()))
                            .caption("Чек от пользователя (promo: " + promo + ")")
                            .build();
                    bot.sendDocument(String.valueOf(userId), file, "Чек от пользователя (promo: " + promo + ")");
                    notifications.sendDocument(userService.getIdByPromo(promo),
                            file, """
                                    Поступил Чек!
                                    promo = %s
                                    """.formatted(promo));
                    model.addAttribute("message", "Чек успешно отправлен!");
                } else {
                    model.addAttribute("error", "Файл превышает максимальный размер (5 МБ).");
                }
            } else {
                model.addAttribute("error", "Поддерживаются только JPEG, PNG или PDF файлы.");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка при загрузке файла: " + e.getMessage());
        }

        model.addAttribute("promo", promo);
        return "/payment";
    }

    @GetMapping("/films")
    public String films(@RequestParam("promo") String promo,
                        Model model, HttpServletRequest request) {
        model.addAttribute("promo", promo);
        try {
            notifications.sendMessage(String.valueOf(userService.getIdByPromo(promo)),
                    Strings.WEB_FILM_NOTIFICATION.formatted(IP.get(request), promo));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "/films";
    }

    @GetMapping("/about")
    public String about(@RequestParam("promo") String promo,
                        HttpServletRequest request,
                        Model model) {
        model.addAttribute("promo", promo);
        try {
            notifications.sendMessage(String.valueOf(userService.getIdByPromo(promo)),
                    Strings.WEB_ABOUT_NOTIFICATION.formatted(IP.get(request), promo));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "/about";
    }

    @GetMapping("/tarifs")
    public String tarifs(@RequestParam("promo") String promo,
                         HttpServletRequest request, Model model) {
        Tarif tarif = tarifService.getByPromo(promo);
        List<Addon> addons = addonService.getByPromo(promo);
        model.addAttribute("tarif", tarif);
        model.addAttribute("addons", addons);
        model.addAttribute("promo", promo);
        model.addAttribute("tariffList", Arrays.asList("standart", "plus", "vip")); // Add tariff list
        try {
            notifications.sendMessage(String.valueOf(userService.getIdByPromo(promo)),
                    Strings.WEB_TARIF_NOTIFICATION.formatted(IP.get(request), promo));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "/tarifs";
    }

    @GetMapping("/yookassa")
    public String yookassa(@RequestParam("totalPrice") String totalPrice,
                           Model model) {
        model.addAttribute("totalPrice", totalPrice);
        return "/yookassa";
    }



}