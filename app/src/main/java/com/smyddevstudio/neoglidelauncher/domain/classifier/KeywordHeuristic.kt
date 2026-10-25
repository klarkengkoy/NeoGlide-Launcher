package com.smyddevstudio.neoglidelauncher.domain.classifier

import com.smyddevstudio.neoglidelauncher.domain.model.AppCategory
import java.util.Locale

/**
 * Keyword Heuristic v2
 *
 * MATCH RULES
 *   1. Plain ASCII keyword -> EXACT token match.
 *   2. "stem*"             -> token PREFIX match.
 *   3. Non-ASCII keyword   -> SUBSTRING match on the lowercased label (CJK text has no spaces).
 *   4. Brand compounds     -> adjacent tokens are also joined ("Google Maps" -> "googlemaps").
 *   5. Category dilution   -> a keyword in N categories yields 1/N point per category.
 */
internal object KeywordHeuristic {

    private const val LABEL_WEIGHT = 1.0f
    private const val PACKAGE_WEIGHT = 0.5f
    private const val PREFIX_FACTOR = 0.7f
    private const val PER_SOURCE_CAP = 2.0f

    private val GENERIC_TOKENS = setOf(
        "app", "apps", "application", "android", "mobile", "free", "pro", "lite", "beta", "official",
        "new", "my", "the", "one", "plus", "go", "now", "live", "best", "super", "easy", "quick", "fast",
        "simple", "mini", "hd", "max", "ultra", "premium", "play", "store", "show", "smart", "version",
        "edition", "online", "offline", "global", "classic", "original", "vip", "hub", "for", "and", "of", "to"
    )

    private val PACKAGE_NOISE = setOf(
        "com", "org", "net", "io", "co", "me", "de", "jp", "kr", "cn", "uk", "in", "id", "ph", "th", "vn", "sg",
        "app", "apps", "android", "mobile", "client", "ui", "main", "free", "lite", "pro", "sec", "release", "prod"
    )

    val KEYWORD_MAP: Map<AppCategory, List<String>> = mapOf(
        AppCategory.SOCIAL to listOf(
            "social", "friend", "friends", "follow", "followers", "community", "forum", "dating", "singles", "sns",
            "facebook", "instagram", "twitter", "threads", "bluesky", "mastodon", "tumblr", "pinterest", "snapchat",
            "tiktok", "musically", "linkedin", "reddit", "quora", "weibo", "vk", "vkontakte", "lemon8", "bereal",
            "nextdoor", "tinder", "bumble", "hinge", "badoo", "meetup"
        ),
        AppCategory.COMMUNICATION to listOf(
            "chat", "chats", "message", "messages", "messaging", "messenger", "sms", "mms", "talk", "call", "calls",
            "dialer", "phone", "contacts", "voip", "voicemail", "rcs", "meet", "conference", "webinar",
            "mail", "email", "gmail", "outlook", "inbox", "protonmail",
            "whatsapp", "telegram", "viber", "wechat", "skype", "zoom", "discord", "facetime", "hangouts", "imo",
            "botim", "zalo", "threema", "linecorp", "kakao", "kakaotalk"
        ),
        AppCategory.PRODUCTIVITY to listOf(
            "office", "doc", "docs", "document", "documents", "note", "notes", "notepad", "memo", "task", "tasks",
            "todo", "checklist", "reminder", "reminders", "planner", "calendar", "agenda", "habit", "habits",
            "pomodoro", "focus", "whiteboard", "mindmap", "pdf", "scan", "scanner", "ocr", "sign", "esign",
            "docusign", "sheet", "sheets", "slide", "slides", "excel", "powerpoint", "spreadsheet", "drive",
            "cloud", "sync", "dropbox", "onedrive", "evernote", "notion", "onenote", "obsidian", "todoist",
            "ticktick", "simplenote", "joplin", "wps", "assistant", "chatgpt", "gpt", "copilot", "gemini",
            "claude", "perplexity", "deepseek"
        ),
        AppCategory.BUSINESS to listOf(
            "work", "workplace", "workspace", "business", "enterprise", "corporate", "crm", "erp", "management",
            "project", "projects", "invoice", "invoices", "billing", "payroll", "hr", "hrm", "recruit*", "hiring",
            "job", "jobs", "career", "careers", "resume", "cv", "attendance", "timesheet", "freelance", "freelancer",
            "upwork", "fiverr", "indeed", "glassdoor", "workday", "salesforce", "hubspot", "zoho", "jira", "trello",
            "asana", "confluence", "slack", "teams", "webex", "sap", "oracle", "quickbooks", "xero", "sales",
            "marketing", "analytics", "dashboard", "kpi", "inventory", "pos", "meeting", "meetings", "b2b", "saas",
            "hustle"
        ),
        AppCategory.ENTERTAINMENT to listOf(
            "video", "videos", "watch", "stream", "streaming", "livestream", "movie", "movies", "film", "films", "tv",
            "television", "series", "anime", "episode", "episodes", "drama", "cartoon", "clip", "clips", "shorts",
            "vod", "ott", "iptv", "trailer", "cinema", "theater", "theatre", "comedy", "meme", "memes", "gif", "gifs",
            "netflix", "youtube", "hulu", "disney", "hbo", "primevideo", "crunchyroll", "viki", "iqiyi", "bilibili",
            "niconico", "abema", "twitch", "vimeo", "dailymotion", "plex", "kodi", "jellyfin", "vlc", "mxplayer",
            "tubi", "peacock", "paramount", "appletv", "hotstar", "viu", "vidio", "wetv", "catchplay", "imdb",
            "letterboxd"
        ),
        AppCategory.GAMING to listOf(
            "game", "games", "gaming", "gamer", "arcade", "puzzle", "trivia", "rpg", "mmorpg", "moba", "fps",
            "battle", "royale", "brawl", "arena", "clash", "craft", "quest", "runner", "jump", "shoot", "shooter",
            "shooting", "sniper", "race", "racing", "tower", "defense", "casino", "poker", "slot", "slots",
            "solitaire", "chess", "sudoku", "crossword", "wordle", "bingo", "mahjong", "blackjack", "roulette",
            "bubble", "match3", "merge", "idle", "tycoon", "simulator", "survival", "zombie", "strike", "warfare",
            "war", "legends", "heroes", "hero", "saga", "adventure", "empire", "kingdom", "dragon", "ninja",
            "fighter", "fighting", "gacha", "ludo", "carrom", "domino", "dominoes", "checkers", "backgammon",
            "billiards", "bowling", "candy", "crush", "escape", "mystery", "detective", "steam", "playstation",
            "xbox", "nintendo", "roblox", "minecraft", "fortnite", "pubg", "genshin", "supercell", "garena",
            "mihoyo", "emulator", "gamepad"
        ),
        AppCategory.FINANCE to listOf(
            "bank", "banks", "banking", "money", "cash", "finance", "financial", "invest", "investing", "investment",
            "investments", "stock", "stocks", "trading", "trade", "broker", "brokerage", "crypto", "bitcoin", "btc",
            "ethereum", "forex", "etf", "funds", "budget", "expense", "expenses", "loan", "loans", "credit", "tax",
            "taxes", "accounting", "bookkeeping", "ledger", "insurance", "mortgage", "savings", "retirement",
            "pension", "wealth", "portfolio", "dividend", "fintech", "currency", "exchange", "bill", "bills",
            "coinbase", "binance", "robinhood", "etoro", "tradingview", "webull", "revolut", "monzo", "hsbc",
            "citi", "dbs", "ocbc", "uob", "maybank", "ynab", "sofi", "mint"
        ),
        AppCategory.WALLET to listOf(
            "pay", "payment", "payments", "wallet", "wallets", "gpay", "googlepay", "samsungpay", "applepay",
            "paypal", "venmo", "cashapp", "zelle", "alipay", "wechatpay", "paytm", "phonepe", "gcash", "maya",
            "grabpay", "touchngo", "tng", "dana", "ovo", "gopay", "linepay", "paypay", "rakutenpay", "merpay",
            "upi", "bhim", "stripe", "klarna", "afterpay", "remit", "remittance", "sendmoney", "moneytransfer",
            "remitly", "westernunion", "moneygram", "visa", "mastercard", "amex", "creditcard", "debitcard",
            "contactless", "qrpay", "suica", "pasmo", "icoca", "octopus", "ezlink", "loyalty", "rewards", "twint",
            "blik"
        ),
        AppCategory.SHOPPING to listOf(
            "shop", "shops", "shopping", "shopper", "buy", "buying", "sell", "selling", "seller", "marketplace",
            "market", "mall", "cart", "checkout", "wishlist", "order", "orders", "deal", "deals", "coupon", "coupons",
            "voucher", "vouchers", "discount", "discounts", "sale", "price", "prices", "merchant", "retail",
            "wholesale", "outlet", "cashback", "giftcard", "thrift", "secondhand", "preloved", "auction",
            "classifieds", "delivery", "parcel", "parcels", "shipment", "shipping", "courier", "17track",
            "aftership", "ebay", "amazon", "lazada", "shopee", "aliexpress", "alibaba", "taobao", "temu", "shein",
            "walmart", "costco", "ikea", "etsy", "wish", "wayfair", "zalando", "asos", "tokopedia", "bukalapak",
            "flipkart", "myntra", "meesho", "coupang", "rakuten", "mercari", "zozotown", "olx", "carousell",
            "craigslist", "offerup"
        ),
        AppCategory.PHOTOGRAPHY to listOf(
            "photo", "photos", "photography", "photographer", "camera", "cameras", "cam", "webcam", "gallery",
            "pic", "pics", "picture", "pictures", "image", "images", "selfie", "selfies", "filter", "filters",
            "retouch", "crop", "collage", "snapshot", "lens", "portrait", "panorama", "hdr", "photoeditor",
            "videoeditor", "imageeditor", "sketch", "draw", "drawing", "paint", "painting", "lightroom",
            "photoshop", "snapseed", "vsco", "picsart", "canva", "capcut", "inshot", "pixlr", "facetune",
            "prisma", "procreate"
        ),
        AppCategory.HEALTH to listOf(
            "health", "healthcare", "healthconnect", "heart", "heartrate", "sleep", "insomnia", "meditat*",
            "mindful*", "calm", "headspace", "doctor", "doctors", "clinic", "hospital", "pharmacy", "medicine",
            "medical", "medication", "pill", "pills", "prescription", "symptom", "symptoms", "telehealth",
            "telemedicine", "vaccine", "covid", "firstaid", "period", "pregnancy", "pregnant", "fertility",
            "ovulation", "menstrual", "bloodpressure", "glucose", "diabetes", "spo2", "ecg", "bmi", "therapy",
            "therapist", "mental", "anxiety", "depression", "wellbeing", "wellness", "dental", "dentist"
        ),
        AppCategory.FITNESS to listOf(
            "fitness", "fit", "workout", "workouts", "exercise", "exercises", "gym", "run", "running", "jogging",
            "marathon", "step", "steps", "pedometer", "stepcounter", "calorie", "calories", "nutrition", "diet",
            "dieting", "yoga", "pilates", "training", "trainer", "weight", "weightloss", "hiit", "strength",
            "cardio", "cycling", "hiking", "swim", "swimming", "stretch", "stretching", "abs", "plank", "pushup",
            "pushups", "squat", "squats", "muscle", "bodybuilding", "crossfit", "zumba", "tabata", "wearable",
            "strava", "fitbit", "garmin", "whoop", "oura", "peloton", "myfitnesspal"
        ),
        AppCategory.MAPS to listOf(
            "map", "maps", "navigate", "navigation", "gps", "route", "routes", "routing", "direction", "directions",
            "traffic", "location", "locator", "geo", "atlas", "earth", "satellite", "streetview", "coordinates",
            "latitude", "longitude", "offlinemaps", "waze", "googlemaps", "osmand", "mapsme", "komoot", "alltrails",
            "mapquest", "tomtom", "sygic", "mapbox", "findmy", "life360", "amap", "gaode", "baidumap"
        ),
        AppCategory.NAVIGATION to listOf(
            "commute", "commuter", "transit", "transport", "transportation", "timetable", "subway", "metro", "tram",
            "rail", "railway", "train", "trains", "bus", "buses", "ferry", "taxi", "cab", "ride", "rides",
            "rideshare", "ridehailing", "carpool", "carshare", "carsharing", "bike", "bikes", "bikeshare",
            "scooter", "scooters", "parking", "toll", "tolls", "fuel", "petrol", "driving", "driver", "dashcam",
            "obd", "obd2", "androidauto", "carplay", "mirrorlink", "evcharging", "uber", "grab", "lyft", "bolt",
            "gojek", "didi", "ola", "careem", "freenow", "cabify", "indriver", "citymapper", "navitime",
            "transitapp", "moovit", "chargepoint", "plugshare"
        ),
        AppCategory.FOOD to listOf(
            "food", "foods", "recipe", "recipes", "cook", "cooking", "cookbook", "restaurant", "restaurants",
            "dining", "reservation", "reservations", "eat", "eating", "meal", "meals", "mealprep", "delivery",
            "takeout", "takeaway", "menu", "kitchen", "grocery", "groceries", "drink", "drinks", "coffee", "tea",
            "snack", "bakery", "baking", "bake", "chef", "cafe", "pizza", "burger", "sushi", "cocktail", "cocktails",
            "wine", "beer", "starbucks", "mcdonalds", "kfc", "foodpanda", "grabfood", "ubereats", "doordash",
            "grubhub", "deliveroo", "zomato", "swiggy", "gofood", "lineman", "menulog", "instacart", "hellofresh",
            "yelp", "opentable", "tabelog", "gurunavi", "cookpad", "kurashiru", "tasty", "allrecipes", "yummly"
        ),
        AppCategory.EDUCATION to listOf(
            "learn", "learning", "learner", "study", "studies", "student", "students", "school", "schools", "course",
            "courses", "lesson", "lessons", "tutor", "tutoring", "quiz", "language", "languages", "math", "maths",
            "algebra", "calculus", "geometry", "physics", "chemistry", "biology", "science", "education", "edu",
            "university", "college", "academy", "classroom", "homework", "exam", "exams", "lecture", "lectures",
            "mooc", "teacher", "teachers", "campus", "vocabulary", "vocab", "grammar", "flashcard", "flashcards",
            "dictionary", "thesaurus", "translate", "translator", "encyclopedia", "wikipedia", "english", "spanish",
            "japanese", "chinese", "korean", "french", "german", "ielts", "toefl", "toeic", "coursera", "udemy",
            "edx", "khan", "khanacademy", "duolingo", "babbel", "memrise", "busuu", "quizlet", "anki", "brainly",
            "photomath", "mathway", "wolfram", "desmos", "geogebra", "scholar", "moodle", "blackboard", "sololearn"
        ),
        AppCategory.TOOLS to listOf(
            "tool", "tools", "toolbox", "cleaner", "clean", "cleanup", "booster", "boost", "optimizer", "optimize",
            "optimise", "junk", "battery", "cpu", "ram", "storage", "memory", "file", "files", "manager",
            "filemanager", "explorer", "backup", "restore", "wifi", "wlan", "hotspot", "tethering", "bluetooth",
            "nfc", "network", "keyboard", "gboard", "flashlight", "torch", "qr", "barcode", "compress", "compressor",
            "zip", "unzip", "rar", "7zip", "archive", "extract", "speedtest", "ping", "converter", "convert", "unit",
            "units", "ruler", "protractor", "level", "magnifier", "magnify", "mirror", "screenshot", "screencast",
            "screenrecorder", "audiorecorder", "voicerecorder", "recorder", "recording", "compass", "altimeter",
            "cast", "miracast", "screenmirror", "shareit", "xender", "filetransfer", "sendanywhere", "apk",
            "uninstall", "uninstaller", "usb", "otg", "sdcard"
        ),
        AppCategory.UTILITIES to listOf(
            "settings", "setting", "system", "sysui", "accessibility", "talkback", "clock", "alarm", "alarms",
            "timer", "stopwatch", "calculator", "calc", "worldclock", "timezone", "util", "utils", "utility",
            "utilities", "installer", "packageinstaller", "setup", "wizard", "carrier", "esim", "sim", "emergency",
            "clipboard", "notification", "notifications", "display", "brightness", "volume"
        ),
        AppCategory.WEATHER to listOf(
            "weather", "forecast", "forecasts", "temp", "temperature", "rain", "radar", "snow", "sun", "sunny",
            "sunrise", "sunset", "storm", "wind", "humidity", "climate", "uv", "aqi", "airquality", "pollen",
            "hurricane", "typhoon", "tornado", "earthquake", "quake", "thermometer", "barometer", "tide", "tides",
            "meteo", "accuweather", "weatherbug", "windy", "wunderground", "rainviewer", "weathernews"
        ),
        AppCategory.NEWS to listOf(
            "news", "newspaper", "newspapers", "headline", "headlines", "article", "articles", "feed", "rss",
            "blog", "blogs", "press", "daily", "bulletin", "magazine", "magazines", "breaking", "nyt", "nytimes",
            "bbc", "cnn", "reuters", "bloomberg", "guardian", "washingtonpost", "wsj", "foxnews", "aljazeera",
            "apnews", "flipboard", "smartnews", "newsbreak", "inshorts", "dailyhunt", "msn", "feedly", "medium",
            "substack", "pocket", "instapaper", "nhk", "nikkei"
        ),
        AppCategory.MUSIC to listOf(
            "music", "song", "songs", "audio", "radio", "podcast", "podcasts", "playlist", "playlists", "beat",
            "beats", "dj", "listen", "listening", "lyrics", "karaoke", "guitar", "piano", "drum", "drums", "synth",
            "synthesizer", "midi", "daw", "instrument", "instruments", "tuner", "metronome", "equalizer", "eq",
            "mp3", "flac", "vinyl", "singing", "sing", "spotify", "soundcloud", "shazam", "deezer", "tidal",
            "pandora", "youtubemusic", "applemusic", "amazonmusic", "audiomack", "jiosaavn", "saavn", "gaana",
            "joox", "bandcamp", "napster", "qobuz", "pocketcasts", "overcast", "castbox", "iheart", "tunein",
            "garageband", "flstudio", "musixmatch", "lastfm"
        ),
        AppCategory.TRAVEL to listOf(
            "travel", "traveler", "travelling", "traveling", "hotel", "hotels", "trip", "trips", "tour", "tours",
            "vacation", "holiday", "holidays", "airline", "airlines", "airport", "flight", "flights", "flightradar*",
            "evisa", "passport", "hostel", "hostels", "resort", "resorts", "cruise", "itinerary", "luggage",
            "baggage", "boarding", "checkin", "carrental", "camping", "campsite", "destination", "destinations",
            "sightseeing", "attraction", "attractions", "airbnb", "booking", "expedia", "agoda", "trivago", "kayak",
            "skyscanner", "tripadvisor", "traveloka", "tiket", "klook", "kkday", "hopper", "vrbo", "tripit",
            "wanderlog", "airasia"
        ),
        AppCategory.LIFESTYLE to listOf(
            "lifestyle", "style", "fashion", "beauty", "makeup", "skincare", "hair", "salon", "nail", "outfit",
            "outfits", "wedding", "zodiac", "horoscope", "astrology", "tarot", "numerology", "fortune", "pet",
            "pets", "dog", "dogs", "cat", "cats", "puppy", "plant", "plants", "garden", "gardening", "decor",
            "homedecor", "interior", "diy", "hobby", "hobbies", "quotes", "quote", "affirmation", "affirmations",
            "journal", "diary", "bible", "quran", "prayer", "azan", "adhan", "church", "mosque", "realestate",
            "apartment", "rent", "rental", "zillow"
        ),
        AppCategory.BOOKS to listOf(
            "book", "books", "reading", "ebook", "ebooks", "kindle", "audiobook", "audiobooks", "novel", "novels",
            "comic", "comics", "manga", "webtoon", "library", "bookshelf", "epub", "mobi", "cbz", "fiction",
            "poetry", "literature", "libby", "overdrive", "audible", "scribd", "wattpad", "goodreads", "kobo",
            "tachiyomi", "mangadex"
        ),
        AppCategory.SPORTS to listOf(
            "sport", "sports", "soccer", "football", "basketball", "baseball", "tennis", "golf", "score", "scores",
            "nfl", "nba", "fifa", "mlb", "nhl", "epl", "ufc", "wwe", "f1", "formula1", "cricket", "rugby", "hockey",
            "volleyball", "badminton", "boxing", "mma", "olympic", "olympics", "league", "livescore", "fantasy",
            "fixtures", "standings", "athlete", "stadium", "espn", "sofascore", "fotmob", "flashscore",
            "onefootball", "dazn", "cricbuzz", "cricinfo", "bleacher"
        ),
        AppCategory.HOME to listOf(
            "home", "smarthome", "smartthings", "thinq", "homekit", "homeassistant", "googlehome", "alexa", "iot",
            "lights", "lighting", "bulb", "doorbell", "thermostat", "ecobee", "nest", "hue", "ring", "tuya",
            "smartlife", "mihome", "yeelight", "wyze", "arlo", "eufy", "roomba", "irobot", "vacuum", "sonos",
            "zigbee", "zwave", "ipcam", "cctv", "surveillance", "garage", "appliance", "appliances", "aircon",
            "hvac", "smartlock", "doorlock", "switchbot", "meross", "kasa", "tapo"
        ),
        AppCategory.KIDS to listOf(
            "kids", "kid", "child", "children", "baby", "babies", "parent", "parents", "parenting", "toy", "toys",
            "storybook", "nursery", "preschool", "kindergarten", "toddler", "toddlers", "lullaby", "lullabies",
            "coloring", "colouring", "abc", "alphabet", "phonics", "rhymes", "cartoon", "cartoons", "disney",
            "parentalcontrol", "babymonitor", "familylink", "youtubekids", "kidoz", "peppa", "cocomelon", "bluey",
            "pinkfong", "babyshark", "pororo", "tayo", "sesame", "lego", "toca"
        ),
        AppCategory.BROWSER to listOf(
            "browser", "browsers", "chrome", "firefox", "opera", "brave", "vivaldi", "duckduckgo", "samsunginternet",
            "ucbrowser", "puffin", "bing", "yandex", "msedge", "webview"
        ),
        AppCategory.SECURITY to listOf(
            "security", "secure", "privacy", "private", "lock", "applock", "locker", "vault", "password",
            "passwords", "passkey", "authenticator", "authentication", "2fa", "otp", "totp", "biometric",
            "fingerprint", "antivirus", "antimalware", "malware", "virus", "firewall", "vpn", "proxy", "dns",
            "encrypt", "encryption", "encrypted", "protect", "protection", "guard", "shield", "bitwarden",
            "lastpass", "1password", "keepass", "dashlane", "nordvpn", "expressvpn", "proton", "surfshark",
            "windscribe", "mullvad", "avast", "avg", "norton", "mcafee", "kaspersky", "malwarebytes", "eset",
            "bitdefender", "lookout", "authy"
        ),
        AppCategory.PERSONALIZATION to listOf(
            "launcher", "wallpaper", "wallpapers", "livewallpaper", "lockscreen", "theme", "themes", "icon", "icons",
            "iconpack", "widget", "widgets", "font", "fonts", "emoji", "emojis", "sticker", "stickers", "ringtone",
            "ringtones", "amoled", "aod", "alwayson", "customize", "customise", "customization", "personalize",
            "personalization", "zedge", "muzei", "kwgt", "klwp", "nova", "niagara", "lawnchair"
        ),
        AppCategory.DEVELOPER to listOf(
            "developer", "developers", "dev", "devtools", "adb", "terminal", "termux", "console", "git", "github",
            "gitlab", "ssh", "sftp", "ftp", "ide", "sdk", "api", "compiler", "python", "java", "kotlin",
            "javascript", "html", "css", "json", "regex", "linux", "bash", "shell", "debug", "debugger", "logcat",
            "coding", "programming", "postman", "scrcpy", "shizuku", "magisk", "lsposed", "xposed", "firebase",
            "docker", "kubernetes"
        )
    )

    val LOCALIZED_KEYWORD_MAP: Map<AppCategory, List<String>> = mapOf(
        AppCategory.SOCIAL to listOf("ソーシャル", "社交", "소셜", "出会い", "交友", "커뮤니티", "掲示板"),
        AppCategory.COMMUNICATION to listOf("電話", "电话", "전화", "メッセージ", "消息", "메시지", "チャット", "聊天", "채팅", "通話", "메신저", "メール", "邮件", "메일"),
        AppCategory.PRODUCTIVITY to listOf("ノート", "笔记", "メモ", "메모", "カレンダー", "日历", "캘린더", "タスク", "任务", "文書", "文档", "办公"),
        AppCategory.FINANCE to listOf("銀行", "银行", "은행", "投資", "投资", "투자", "証券", "证券", "증권", "家計簿", "记账", "股票", "주식", "ローン", "保険", "保险", "보험", "仮想通貨", "加密货币", "가상화폐"),
        AppCategory.WALLET to listOf("決済", "支付", "결제", "钱包", "지갑", "ウォレット"),
        AppCategory.SHOPPING to listOf("ショッピング", "购物", "쇼핑", "通販", "商城", "フリマ", "クーポン", "优惠", "쿠폰"),
        AppCategory.PHOTOGRAPHY to listOf("写真", "照片", "사진", "カメラ", "相机", "카메라", "ギャラリー", "相册", "갤러리", "フォト"),
        AppCategory.HEALTH to listOf("健康", "医療", "医疗", "건강", "病院", "医院", "병원", "薬局", "药店", "睡眠", "수면", "瞑想", "冥想", "명상", "生理", "月经"),
        AppCategory.FITNESS to listOf("運動", "运动", "운동", "フィットネス", "健身", "歩数", "步数", "ランニング", "跑步", "달리기", "ヨガ", "瑜伽", "筋トレ", "ダイエット", "减肥"),
        AppCategory.MAPS to listOf("地図", "地图", "지도", "ナビ", "导航", "내비", "マップ"),
        AppCategory.NAVIGATION to listOf("乗換", "换乘", "환승", "電車", "地铁", "지하철", "タクシー", "出租", "택시", "公交", "버스", "時刻表"),
        AppCategory.FOOD to listOf("レシピ", "食谱", "레시피", "料理", "美食", "요리", "グルメ", "外卖", "出前", "배달", "レストラン", "餐厅"),
        AppCategory.EDUCATION to listOf("学習", "学习", "학습", "勉強", "辞書", "词典", "사전", "英語", "英语", "영어", "翻訳", "翻译", "번역", "課程", "课程", "강의"),
        AppCategory.WEATHER to listOf("天気", "天气", "날씨", "気象", "气象", "雨雲", "雨云"),
        AppCategory.NEWS to listOf("ニュース", "新闻", "新聞", "뉴스", "速報", "头条"),
        AppCategory.MUSIC to listOf("音楽", "音乐", "음악", "ラジオ", "广播", "라디오", "ポッドキャスト", "播客", "歌詞", "歌词", "가사"),
        AppCategory.TRAVEL to listOf("旅行", "여행", "ホテル", "酒店", "호텔", "航空", "航班", "항공", "観光", "旅游"),
        AppCategory.BOOKS to listOf("電子書籍", "电子书", "전자책", "漫画", "マンガ", "만화", "小説", "小说", "소설", "読書", "阅读", "웹툰", "図書館"),
        AppCategory.SPORTS to listOf("スポーツ", "体育", "스포츠", "野球", "足球", "축구", "야구", "サッカー", "篮球", "농구"),
        AppCategory.GAMING to listOf("ゲーム", "游戏", "게임", "パズル", "益智", "퍼즐", "麻雀", "棋牌"),
        AppCategory.ENTERTAINMENT to listOf("動画", "视频", "동영상", "アニメ", "动漫", "애니", "映画", "电影", "영화", "ドラマ", "电视剧", "드라마", "配信", "直播", "방송"),
        AppCategory.TOOLS to listOf("ツール", "工具", "도구", "クリーナー", "清理", "클리너", "バッテリー", "电池", "배터리", "ファイル", "文件", "파일", "懐中電灯", "手电筒", "손전등")
    )

    private enum class Kind { EXACT, PREFIX, SUBSTRING }

    private class Rule(val key: String, val category: AppCategory, val kind: Kind, val weight: Float)

    private fun normalize(raw: String) = raw.trim().lowercase(Locale.ROOT)

    private val rules: List<Rule> by lazy {
        val sources = listOf(KEYWORD_MAP, LOCALIZED_KEYWORD_MAP)

        val owners = HashMap<String, MutableSet<AppCategory>>()
        for (map in sources) for ((category, words) in map) for (word in words) {
            owners.getOrPut(normalize(word)) { mutableSetOf() }.add(category)
        }

        sources.flatMap { map ->
            map.flatMap { (category, words) ->
                words.map { normalize(it) }.distinct().map { w ->
                    val kind = when {
                        w.endsWith("*") -> Kind.PREFIX
                        w.any { it.code > 127 } -> Kind.SUBSTRING
                        else -> Kind.EXACT
                    }
                    Rule(w.removeSuffix("*"), category, kind, 1f / owners.getValue(w).size)
                }
            }
        }
    }

    private val exactRules by lazy { rules.filter { it.kind == Kind.EXACT }.groupBy { it.key } }
    private val prefixRules by lazy { rules.filter { it.kind == Kind.PREFIX } }
    private val substringRules by lazy { rules.filter { it.kind == Kind.SUBSTRING } }

    private val CAMEL_BOUNDARY = Regex("(?<=[a-z0-9])(?=[A-Z])")
    private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")

    private fun rawTokens(text: String): List<String> =
        CAMEL_BOUNDARY.replace(text, " ")
            .split(NON_WORD)
            .map { it.lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }

    private fun candidates(tokens: List<String>): Set<String> {
        val out = LinkedHashSet<String>()
        tokens.filterTo(out) { it.length >= 2 && it !in GENERIC_TOKENS }
        tokens.zipWithNext { a, b -> a + b }.forEach { out.add(it) }
        return out
    }

    private fun labelCandidates(label: String) = candidates(rawTokens(label))

    private fun packageCandidates(packageName: String): Set<String> =
        candidates(
            packageName.lowercase(Locale.ROOT)
                .split('.', '_')
                .filter { it.isNotEmpty() && it !in PACKAGE_NOISE }
        )

    private fun scoreSource(
        tokens: Set<String>,
        fullTextLower: String?,
        sourceWeight: Float
    ): Map<AppCategory, Float> {
        val out = HashMap<AppCategory, Float>()

        fun add(rule: Rule, factor: Float) {
            out[rule.category] = (out[rule.category] ?: 0f) + rule.weight * factor * sourceWeight
        }

        for (token in tokens) {
            exactRules[token]?.forEach { add(it, 1f) }
            for (rule in prefixRules) if (token.startsWith(rule.key)) add(rule, PREFIX_FACTOR)
        }
        if (fullTextLower != null) {
            for (rule in substringRules) if (fullTextLower.contains(rule.key)) add(rule, 1f)
        }

        val cap = PER_SOURCE_CAP * sourceWeight
        return out.mapValues { (_, v) -> minOf(v, cap) }
    }

    fun score(packageName: String, label: String): Map<AppCategory, Float> {
        val fromLabel = scoreSource(labelCandidates(label), label.lowercase(Locale.ROOT), LABEL_WEIGHT)
        val fromPackage = scoreSource(packageCandidates(packageName), null, PACKAGE_WEIGHT)
        val total = HashMap<AppCategory, Float>(fromLabel)
        for ((category, v) in fromPackage) total[category] = (total[category] ?: 0f) + v
        return total
    }
}
