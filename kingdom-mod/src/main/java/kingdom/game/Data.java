package kingdom.game;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/** Справочники: персонажи, эффекты, прозвища, свершения, концовки. */
public final class Data {
    private Data() {}

    public static final String[] STAT_NAMES = {"Церковь", "Народ", "Армия", "Казна"};
    public static final String[] STAT_KEYS = {"church", "people", "army", "gold"};
    public static final String[] SEASONS = {"Весна", "Лето", "Осень", "Зима"};

    // ── персонажи и события: имя, предмет-портрет, цвет щита (0 — событие без щита) ──
    public static final class Who {
        public final String name, item; public final int shield;
        Who(String name, String item, int shield) { this.name = name; this.item = item; this.shield = shield; }
        public boolean event() { return shield == 0; }
    }
    public static final Map<String, Who> WHO = new LinkedHashMap<String, Who>();
    private static void who(String id, String name, String item, int shield) { WHO.put(id, new Who(name, "minecraft:" + item, shield)); }
    static {
        who("herald", "Глашатай", "noteblock", 0xFF1F2B4D);
        who("chronicler", "Летописец Пахомий", "writable_book", 0xFF6B5A3E);
        who("treasurer", "Казначей Гвидо", "gold_ingot", 0xFFB8891C);
        who("general", "Генерал Борислава", "iron_sword", 0xFF9E2B25);
        who("bishop", "Епископ Амвросий", "torch", 0xFF5B3A6B);
        who("mila", "Крестьянка Мила", "wheat", 0xFF2F6B4F);
        who("jester", "Шут Пимен", "lit_pumpkin", 0xFF7A4B8C);
        who("merchant", "Купец Альберих", "emerald", 0xFF8A5A2B);
        who("alchemist", "Алхимик Зорн", "brewing_stand", 0xFF2D5E6E);
        who("captain", "Капитан стражи Рольф", "iron_helmet", 0xFF4A4F5C);
        who("envoy", "Посол Южной империи", "compass", 0xFF2A6A7A);
        who("abbess", "Аббатиса Ефросинья", "book", 0xFF5B3A6B);
        who("tikhon", "Крестьянин Тихон", "iron_hoe", 0xFF2F6B4F);
        who("guild", "Мастер-каменщик Ярослав", "stonebrick", 0xFF6B5A3E);
        who("scholar", "Учёный Фальк", "clock", 0xFF1F2B4D);
        who("princess", "Принцесса Агата", "red_flower", 0xFF9E2B25);
        who("healer", "Лекарка Ирма", "speckled_melon", 0xFF2F6B4F);
        who("bard", "Бард Лель", "jukebox", 0xFF8A5A2B);
        who("hunter", "Егерь Гнат", "bow", 0xFF3F5A34);
        who("clans", "Посланник Северных кланов", "bone", 0xFF4A4F5C);
        who("queen", "Королева Виола", "golden_helmet", 0xFF7A4B8C);
        who("miller", "Мельник Осип", "hay_block", 0xFF2D5E6E);
        who("seer", "Гадалка Зара", "ender_eye", 0xFF5B3A6B);
        who("duke", "Герцог Ратмир", "saddle", 0xFF4A4F5C);
        who("chancellor", "Канцлер Одо", "paper", 0xFF1F2B4D);
        who("savva", "Дьяк Савва", "spider_eye", 0xFF2A2016);
        who("yaga", "Баба-яга", "cauldron", 0xFF3F5A34);
        who("bird", "Жар-птица", "feather", 0xFFB8891C);
        who("night", "Ночной гость", "skull", 0xFF2A2016);
        who("flood", "Половодье", "water_bucket", 0);
        who("fair", "Весенняя ярмарка", "cake", 0);
        who("drought", "Засуха", "deadbush", 0);
        who("comet", "Хвостатая звезда", "fire_charge", 0);
        who("harvest", "Богатый урожай", "bread", 0);
        who("taxes", "Сбор податей", "gold_nugget", 0);
        who("winter", "Лютая зима", "ice", 0);
        who("carols", "Колядки", "nether_star", 0);
        who("fire", "Пожар в столице", "flint_and_steel", 0);
        who("eclipse", "Затмение", "ender_pearl", 0);
        who("treasure", "Клад", "chest", 0);
        who("circus", "Бродячий цирк", "fireworks", 0);
        who("locust", "Саранча", "web", 0);
        who("miracle", "Чудо", "glowstone_dust", 0);
        who("zmeyev", "Змей Горыныч", "dragon_egg", 0);
        who("port", "Мор в порту", "boat", 0);
    }

    // ── постоянные эффекты ──
    public static final class Status {
        public final String name, item, desc, off; public final BiFunction<Game, Integer, int[]> tick;
        Status(String name, String item, String desc, String off, BiFunction<Game, Integer, int[]> tick) {
            this.name = name; this.item = "minecraft:" + item; this.desc = desc; this.off = off; this.tick = tick;
        }
    }
    public static final Map<String, Status> STATUS = new LinkedHashMap<String, Status>();
    private static void st(String id, String n, String item, String d, String off, BiFunction<Game, Integer, int[]> tick) {
        STATUS.put(id, new Status(n, item, d, off, tick));
    }
    static {
        st("wall", "Каменная стена", "stonebrick", "Армия теряет на треть меньше. Достанется наследнику.", null, null);
        st("mills", "Мельницы", "wheat", "Каждую осень приносят зерно и золото. Достанутся наследнику.", null,
            (g, s) -> s == 2 ? new int[]{0, 3, 0, 5} : null);
        st("war", "Война", "iron_sword", "Война с Южной империей: каждый сезон тают армия и казна.", null,
            (g, s) -> new int[]{0, 0, -2, -3});
        st("plague", "Мор", "rotten_flesh", "Каждый сезон гибнут люди.", "Мор отступил.",
            (g, s) -> new int[]{0, -4, 0, 0});
        st("drought", "Засуха", "deadbush", "Каждый сезон народ беднеет.", "Наконец пошли дожди.",
            (g, s) -> new int[]{0, -3, 0, 0});
        st("debt", "Долг купцу", "paper", "Каждый сезон набегают проценты.", null,
            (g, s) -> new int[]{0, 0, 0, -2});
        st("tribute", "Дань змею", "gold_ingot", "Каждую зиму Змею Горынычу уходит сундук золота.", null,
            (g, s) -> s == 3 ? new int[]{0, -3, 0, -12} : null);
        st("alliance", "Союз с империей", "emerald", "Каждую весну приходят имперские полки. Войны с империей не будет.", null,
            (g, s) -> s == 0 ? new int[]{0, 0, 4, 0} : null);
        st("granary", "Полные амбары", "hay_block", "Выручат в засуху или в лютую зиму.", null, null);
        st("firebird", "Перо Жар-птицы", "blaze_powder", "Один раз спасёт от гибели.", null, null);
        st("sword", "Меч-кладенец", "diamond_sword", "С ним Змей Горыныч не страшен.", null, null);
        st("spy", "Тайный приказ", "ender_eye", "Видно, вырастет шкала или упадёт. Каждый сезон стоит золота.", null,
            (g, s) -> "cunning".equals(g.trait) ? null : new int[]{0, 0, 0, -1});
        st("heir", "Наследница Агата", "red_flower", "После вас править будет она.", null, null);
    }

    // ── прозвища ──
    public static final class Trait {
        public final String m, f, d;
        Trait(String m, String f, String d) { this.m = m; this.f = f; this.d = d; }
    }
    public static final Map<String, Trait> TRAITS = new LinkedHashMap<String, Trait>();
    static {
        TRAITS.put("warrior", new Trait("Воитель", "Воительница", "армия растёт в полтора раза быстрее"));
        TRAITS.put("pious", new Trait("Набожный", "Набожная", "церковь обижается вдвое слабее"));
        TRAITS.put("generous", new Trait("Щедрый", "Щедрая", "народ благодарит в полтора раза сильнее"));
        TRAITS.put("miser", new Trait("Скупой", "Скупая", "любые траты казны на треть меньше"));
        TRAITS.put("cunning", new Trait("Хитрый", "Хитрая", "видно, вырастет шкала или упадёт"));
        TRAITS.put("lucky", new Trait("Везучий", "Везучая", "рискованные решения удаются чаще"));
    }

    // ── свершения ──
    public static final Map<String, String[]> ACH = new LinkedHashMap<String, String[]>();
    static {
        ACH.put("y5", new String[]{"Пять лет на троне", "Продержаться пять лет"});
        ACH.put("y10", new String[]{"Десять лет на троне", "Продержаться десять лет"});
        ACH.put("jub", new String[]{"Юбилей", "Дожить до двенадцатого года правления"});
        ACH.put("old", new String[]{"Своя постель", "Умереть от старости"});
        ACH.put("zmey", new String[]{"Змееборец", "Одолеть Змея Горыныча"});
        ACH.put("plot", new String[]{"Раскрытый заговор", "Найти изменника при дворе"});
        ACH.put("duel", new String[]{"Сам себе стража", "Одолеть ночного убийцу своими руками"});
        ACH.put("warwin", new String[]{"Победа над империей", "Выиграть войну с Южной империей"});
        ACH.put("healer", new String[]{"Мор побеждён", "Остановить мор с помощью лекарки"});
        ACH.put("heir", new String[]{"Королева Агата", "Назвать принцессу наследницей"});
        ACH.put("bird", new String[]{"Перо Жар-птицы", "Отпустить Жар-птицу на волю"});
        ACH.put("builder", new String[]{"Строитель", "Иметь и стену, и мельницы"});
        ACH.put("powder", new String[]{"Порох", "Отдать изобретение алхимика армии"});
    }

    // ── концовки: предмет, текст, короткое название ──
    public static final class Ending {
        public final String item, text, name;
        Ending(String item, String text, String name) { this.item = "minecraft:" + item; this.text = text; this.name = name; }
    }
    public static final Map<String, Ending> ENDINGS = new LinkedHashMap<String, Ending>();
    private static void en(String id, String item, String text, String name) { ENDINGS.put(id, new Ending(item, text, name)); }
    static {
        en("church0", "lava_bucket", "Епископ объявил ваше правление ересью. Костёр на главной площади горел до рассвета.", "Костёр");
        en("church100", "bookshelf", "Церковь решила, что правит лучше вас. Вас тихо отправили в дальний монастырь.", "Монастырь");
        en("people0", "stone_axe", "Голодная толпа взяла дворец штурмом. Корону нашли в канаве.", "Бунт");
        en("people100", "potion", "Народ любит вас больше, чем знать. Знать подсыпала яд в ваш кубок.", "Яд");
        en("army0", "wool", "Без армии соседи взяли столицу за одну неделю.", "Завоевание");
        en("army100", "diamond_sword", "Генералы решили, что справятся без монарха. Переворот случился до обеда.", "Переворот");
        en("gold0", "paper", "Казна пуста. Кредиторы унесли трон, а заодно и корону.", "Банкротство");
        en("gold100", "gold_block", "Слухи о вашем золоте привлекли пиратов, воров и собственного казначея.", "Грабёж");
        en("assassin", "skull", "Стража не пришла: в казармах давно пусто. Заговорщики победили.", "Кинжал");
        en("jester", "lit_pumpkin", "Шут не вернул корону. Говорят, правит он неплохо — по крайней мере, весело.", "Шут на троне");
        en("abdicate", "sugar", "Вы передали корону в зените славы и уехали разводить пчёл. Мёд, говорят, вышел отменный.", "Отречение");
        en("oldage", "bed", "Вы умерли в своей постели, в глубокой старости. С монархами такое почти не случается.", "Старость");
    }

    public static final Map<String, String> LEGACY = new LinkedHashMap<String, String>();
    static {
        LEGACY.put("church0", "Церковь всё ещё косится на трон.");
        LEGACY.put("church100", "Епископы привыкли командовать.");
        LEGACY.put("people0", "Народ ещё не остыл после бунта.");
        LEGACY.put("people100", "Народ ждёт от вас чудес.");
        LEGACY.put("army0", "От армии остались одни знамёна.");
        LEGACY.put("army100", "Генералы привыкли решать сами.");
        LEGACY.put("gold0", "Казна после предшественника пуста.");
        LEGACY.put("gold100", "Сундуки ломятся — и все об этом знают.");
        LEGACY.put("assassin", "Заговорщики всё ещё при дворе, а стражи мало.");
        LEGACY.put("jester", "Шута с трудом уговорили вернуть корону.");
        LEGACY.put("abdicate", "Вам досталось крепкое королевство.");
        LEGACY.put("oldage", "Вам досталось крепкое королевство.");
    }

    public static final String[] NAMES = {"Казимир", "Владислав", "Ростислав", "Леопольд", "Мирослав", "Эдгар", "Святополк", "Бертольд", "Изяслав", "Годфрид"};
}
