package com.example.model

object IndustrialComponentLibrary {

    data class LibraryItem(
        val type: ComponentType,
        val displayName: String,
        val category: String,
        val description: String,
        val defaultSpecs: TechnicalSpecs,
        val symbolAscii: String
    )

    val items: List<LibraryItem> = listOf(
        // منابع تغذیه
        LibraryItem(
            type = ComponentType.THREE_PHASE_SOURCE,
            displayName = "منبع تغذیه ۳ فاز (3-Phase L1-L2-L3)",
            category = "منابع تغذیه",
            description = "شبکه برق سه‌فاز صنعتی ۳۸۰/۲۲۰ ولت ۵۰ هرتز با سه فاز L1, L2, L3 و ارت حفاظتی PE (مقادیر نمونه قابل تنظیم)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۳۸۰/۲۲۰V AC (۵۰Hz)",
                ratedCurrent = "۳۲A",
                contactsNO = 0,
                contactsNC = 0,
                standardCode = "IEC 60364",
                isSampleValue = true
            ),
            symbolAscii = "[3P~]"
        ),
        LibraryItem(
            type = ComponentType.AC_PHASE,
            displayName = "ترمینال فاز متناوب (Phase L)",
            category = "منابع تغذیه",
            description = "تغذیه فاز خط ۲۲۰ ولت متناوب نسبت به نول در شبکه برق ۵۰ هرتز (مقدار نمونه قابل تنظیم)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۲۰V AC (۵۰Hz)",
                ratedCurrent = "۱۶A",
                contactsNO = 0,
                contactsNC = 0,
                standardCode = "IEC 60204-1",
                isSampleValue = true
            ),
            symbolAscii = "[L]"
        ),
        LibraryItem(
            type = ComponentType.AC_NEUTRAL,
            displayName = "شینه نول متناوب (Neutral N)",
            category = "منابع تغذیه",
            description = "هادی خنثی مدار جهت تکمیل حلقه جریان بوبین‌ها و مصرف‌کننده‌ها (مقدار نمونه قابل تنظیم)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۰V",
                ratedCurrent = "۱۶A",
                contactsNO = 0,
                contactsNC = 0,
                standardCode = "IEC 60204-1",
                isSampleValue = true
            ),
            symbolAscii = "[N]"
        ),
        LibraryItem(
            type = ComponentType.DC_PLUS,
            displayName = "منبع تغذیه مثبت ۲۴ ولت (+24V DC)",
            category = "منابع تغذیه",
            description = "ولتاژ مستقیم ایمن (SELV/PELV) جهت سنسورها و ورودی/خروجی‌های دیجیتال PLC طبق استاندارد IEC 61131-2 و IEC 60364-4-41 (مقادیر نمونه جریان و توان)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۴V DC",
                ratedCurrent = "۵A",
                powerRating = "۱۲۰W",
                standardCode = "IEC 61131-2 / IEC 60364-4-41",
                isSampleValue = true
            ),
            symbolAscii = "[+24V]"
        ),
        LibraryItem(
            type = ComponentType.DC_MINUS,
            displayName = "منبع تغذیه منفی ۰ ولت (0V DC)",
            category = "منابع تغذیه",
            description = "مشترک مرجع ولتاژ مستقیم مدار کنترل دیجیتال (SELV/PELV)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۰V DC",
                ratedCurrent = "۵A",
                standardCode = "IEC 61131-2 / IEC 60364-4-41",
                isSampleValue = true
            ),
            symbolAscii = "[0V]"
        ),
        LibraryItem(
            type = ComponentType.EARTH,
            displayName = "ارت حفاظتی (PE)",
            category = "منابع تغذیه",
            description = "هادی اتصال به زمین ایمنی جهت حفاظت در برابر برق‌گرفتگی",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۰V",
                ratedCurrent = "حفاظتی",
                standardCode = "IEC 60364-5-54"
            ),
            symbolAscii = "[PE]"
        ),

        // تجهیزات حفاظتی
        LibraryItem(
            type = ComponentType.CIRCUIT_BREAKER_MCB,
            displayName = "کلید مینیاتوری تک‌پل (MCB)",
            category = "تجهیزات حفاظتی",
            description = "حفاظت اضافه بار و اتصال کوتاه مدار فرمان بر پایه استاندارد IEC 60898-1 (جریان نامی مقدار نمونه قابل ویرایش)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۳۰/۴۰۰V AC",
                ratedCurrent = "۶A (تیپ C)",
                contactsNO = 1,
                contactsNC = 0,
                standardCode = "IEC 60898-1",
                isSampleValue = true
            ),
            symbolAscii = "-/ --"
        ),
        LibraryItem(
            type = ComponentType.CIRCUIT_BREAKER_MCB_3P,
            displayName = "کلید مینیاتوری سه‌پل (3P MCB)",
            category = "تجهیزات حفاظتی",
            description = "کلید مینیاتوری سه فاز صنعتی جهت حفاظت مغناطیسی اتصال کوتاه و حرارتی اضافه‌بار موتورها و کابل‌ها طبق IEC 60898-1 / IEC 60947-2 با قطع دستی همزمان هر سه فاز (پایانه‌های ورودی ۱-۳-۵ و خروجی ۲-۴-۶ با نشانه‎گذاری قطعی استاندارد)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۴۰۰V AC (۵۰Hz)",
                ratedCurrent = "۱۶A (تیپ C - مقدار نمونه قابل ویرایش)",
                contactsNO = 3,
                contactsNC = 0,
                standardCode = "IEC 60898-1 / IEC 60947-2",
                isSampleValue = true,
                voltageSource = SpecSource(SpecSourceType.STANDARD, "IEC 60898-1"),
                currentSource = SpecSource(SpecSourceType.SAMPLE, "مقدار نمونه ۱۶ آمپر قابل ویرایش"),
                standardCodeSource = SpecSource(SpecSourceType.STANDARD, "IEC 60898-1 / IEC 60947-2"),
                contactsSource = SpecSource(SpecSourceType.STANDARD, "IEC 60898-1")
            ),
            symbolAscii = "-/ - 3P"
        ),
        LibraryItem(
            type = ComponentType.CIRCUIT_BREAKER_MCCB_3P,
            displayName = "کلید اتوماتیک کمپکت سه‌پل (3P MCCB)",
            category = "تجهیزات حفاظتی",
            description = "کلید اتوماتیک قدرت بدنه تزریقی جهت حفاظت فیدر ورودی تابلو با رله حرارتی-مغناطیسی قابل تنظیم طبق IEC 60947-2 (پایانه‌های ۱-۳-۵ و ۲-۴-۶ با نشانه‎گذاری قطعی استاندارد؛ بازه جریان نمونه قابل تنظیم)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۴۰۰/۶۹۰V AC",
                ratedCurrent = "۱۰۰A (بازه جریان نمونه قابل تنظیم ۷۰A تا ۱۰۰A)",
                contactsNO = 3,
                contactsNC = 0,
                powerRating = "قدرت قطع ۳۶kA (نمونه)",
                standardCode = "IEC 60947-2",
                isSampleValue = true,
                voltageSource = SpecSource(SpecSourceType.STANDARD, "IEC 60947-2"),
                currentSource = SpecSource(SpecSourceType.SAMPLE, "بازه نمونه ۷۰A تا ۱۰۰A قابل ویرایش"),
                standardCodeSource = SpecSource(SpecSourceType.STANDARD, "IEC 60947-2 بند 4.3"),
                contactsSource = SpecSource(SpecSourceType.STANDARD, "IEC 60947-2")
            ),
            symbolAscii = "[>|- 3P]"
        ),
        LibraryItem(
            type = ComponentType.FUSE,
            displayName = "پایه فیوز استوانه‌ای مینیاتوری (Fuse)",
            category = "تجهیزات حفاظتی",
            description = "حفاظت اتصال کوتاه سریع با فیوز استوانه‌ای طبق IEC 60269 (جریان ۴A مقدار نمونه قابل ویرایش)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۵۰۰V AC",
                ratedCurrent = "۴A gG",
                standardCode = "IEC 60269",
                isSampleValue = true
            ),
            symbolAscii = "=[ ]="
        ),
        LibraryItem(
            type = ComponentType.FUSE_3P,
            displayName = "پایه فیوز سه‌پل (3P Fuse Base)",
            category = "تجهیزات حفاظتی",
            description = "پایه فیوز سه‌فاز صنعتی جهت حفاظت اتصال کوتاه خطوط ۳ فاز طبق استاندارد IEC 60269 (پایانه‌های ورودی ۱-۳-۵ و خروجی ۲-۴-۶ با نشانه‎گذاری قطعی استاندارد)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۵۰۰/۶۹۰V AC",
                ratedCurrent = "۱۶A gG (مقدار نمونه قابل ویرایش)",
                contactsNO = 3,
                contactsNC = 0,
                standardCode = "IEC 60269-1 / IEC 60269-2",
                isSampleValue = true,
                voltageSource = SpecSource(SpecSourceType.STANDARD, "IEC 60269-1"),
                currentSource = SpecSource(SpecSourceType.SAMPLE, "مقدار نمونه ۱۶A"),
                standardCodeSource = SpecSource(SpecSourceType.STANDARD, "IEC 60269-1")
            ),
            symbolAscii = "=[ ]= 3P"
        ),
        LibraryItem(
            type = ComponentType.RCD_4P,
            displayName = "کلید محافظ جان ۴ پل (4P RCD/RCCB)",
            category = "تجهیزات حفاظتی",
            description = "کلید جریان نشتی دیفرانسیلی ۴ پل سه فاز و نول طبق استاندارد IEC 61008-1 با دکمه تست و قطع دستی ۴ پل (پایانه‌های فاز ۱-۳-۵ و ۲-۴-۶ قطعی استاندارد؛ پایانه‌های نول N و دکمه تست نشانه‎گذاری عمومی)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۳۰/۴۰۰V AC",
                ratedCurrent = "۲۵A (جریان نشتی عامل نامی ۳۰mA - مقدار نمونه)",
                contactsNO = 4,
                contactsNC = 0,
                standardCode = "IEC 61008-1",
                isSampleValue = true,
                voltageSource = SpecSource(SpecSourceType.STANDARD, "IEC 61008-1"),
                currentSource = SpecSource(SpecSourceType.SAMPLE, "جریان عامل ۳۰mA نمونه قابل ویرایش"),
                standardCodeSource = SpecSource(SpecSourceType.STANDARD, "IEC 61008-1 بند 5.2"),
                contactsSource = SpecSource(SpecSourceType.STANDARD, "IEC 61008-1")
            ),
            symbolAscii = "(ΔI) -/ - 4P"
        ),
        LibraryItem(
            type = ComponentType.PHASE_SEQUENCE_RELAY,
            displayName = "رله کنترل فاز و توالی فاز (Phase Relay)",
            category = "تجهیزات حفاظتی",
            description = "رله نظارت بر ولتاژ شبکه جهت تشخیص قطع فاز، عدم تقارن و توالی صحیح فازهای سه‌گانه با ورودی‌های L1-L2-L3 (نشانه‎گذاری عمومی) و کنتاکت خروجی تبدیلی ۱۵ (مشترک)، ۱۶ (بسته NC) و ۱۸ (باز NO) مطابق استاندارد قطعی IEC 60947-5-1 و DIN 46199",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۳۸۰/۴۰۰V AC (سه‌فاز)",
                ratedCurrent = "۵A (جریان نامی کنتاکت خروجی فرمان - مقدار نمونه)",
                contactsNO = 1,
                contactsNC = 1,
                standardCode = "IEC 60947-5-1 / DIN 46199",
                isSampleValue = true,
                voltageSource = SpecSource(SpecSourceType.STANDARD, "IEC 60947-5-1"),
                currentSource = SpecSource(SpecSourceType.SAMPLE, "جریان کنتاکت خروجی ۵A نمونه"),
                standardCodeSource = SpecSource(SpecSourceType.STANDARD, "IEC 60947-5-1 / DIN 46199"),
                contactsSource = SpecSource(SpecSourceType.STANDARD, "DIN 46199")
            ),
            symbolAscii = "[Phase L1-L2-L3]"
        ),
        LibraryItem(
            type = ComponentType.BIMETAL_OVERLOAD,
            displayName = "رله حرارتی بی‌متال (Overload Relay)",
            category = "تجهیزات حفاظتی",
            description = "رله حرارتی اضافه‌بار الکتروموتور با کنتاکت فرمان ۹۵-۹۶ (NC) و ۹۷-۹۸ (NO) و ترمینال‌های عبور جریان قدرت ۱-۳-۵ و ۲-۴-۶. بازه تنظیم مطابق مدل تأییدشده Schneider LRD10 (۴ تا ۶A) یا مدل متناسب با جریان نامی موتور قالب (۷ تا ۱۰A)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۶۹۰V AC",
                ratedCurrent = "بازه تنظیم ۷ تا ۱۰A (شامل جریان نامی ۸.۵A موتور قالب)",
                contactsNO = 1,
                contactsNC = 1,
                standardCode = "IEC 60947-4-1",
                manufacturerModel = "Schneider TeSys LRD14 (7-10A) / LRD10 (4-6A)",
                datasheetUrl = "https://eshop.se.com/za/lrd10-tesys-lrd-thermal-overload-relays-4-6-a-class-10a-lrd10.html",
                voltageSource = SpecSource(SpecSourceType.DATASHEET, "https://eshop.se.com/za/lrd10-tesys-lrd-thermal-overload-relays-4-6-a-class-10a-lrd10.html"),
                currentSource = SpecSource(SpecSourceType.SAMPLE, "مقدار نمونه (قابل ویرایش)"),
                standardCodeSource = SpecSource(SpecSourceType.STANDARD, "IEC 60947-4-1 بند 4.3"),
                modelSource = SpecSource(SpecSourceType.DATASHEET, "https://eshop.se.com/za/lrd10-tesys-lrd-thermal-overload-relays-4-6-a-class-10a-lrd10.html"),
                contactsSource = SpecSource(SpecSourceType.STANDARD, "IEC 60947-4-1 بند 4.3")
            ),
            symbolAscii = "-[==]-"
        ),
        LibraryItem(
            type = ComponentType.EMERGENCY_STOP,
            displayName = "شستی قارچی اضطراری (E-Stop)",
            category = "تجهیزات حفاظتی",
            description = "شستی کله‌قارچی قرمز ایمنی با کنتاکت بسته ۱-۲ (NC) قفل‌شونده با عملکرد باز شدن مستقیم (Direct Opening Action) طبق استاندارد ISO 13850 و IEC 60947-5-5؛ ریست مکانیکی با چرخاندن (Turn to Reset)، کشیدن (Pull to Reset) یا کلید (Key Release)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۴۰V AC",
                ratedCurrent = "۳A (AC-15)",
                contactsNO = 0,
                contactsNC = 1,
                standardCode = "ISO 13850 / IEC 60947-5-5",
                isSampleValue = true
            ),
            symbolAscii = "-(O)-"
        ),

        // شستی‌ها و کنترل
        LibraryItem(
            type = ComponentType.PUSH_BUTTON_NO,
            displayName = "شستی استارت فشاری (Start Button NO)",
            category = "شستی و کنترل",
            description = "شستی فرمان سبز رنگ با کنتاکت باز ۱۳-۱۴ برای راه‌اندازی و وصل بوبین کنتاکتور (جریان نامی مقدار نمونه قابل ویرایش)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۴۰V AC",
                ratedCurrent = "۶A (AC-15)",
                contactsNO = 1,
                contactsNC = 0,
                standardCode = "IEC 60947-5-1",
                isSampleValue = true
            ),
            symbolAscii = "-|/|-"
        ),
        LibraryItem(
            type = ComponentType.PUSH_BUTTON_NC,
            displayName = "شستی استپ فشاری (Stop Button NC)",
            category = "شستی و کنترل",
            description = "شستی فرمان قرمز رنگ با کنتاکت بسته ۲۱-۲۲ برای قطع مدار تغذیه بوبین (جریان نامی مقدار نمونه قابل ویرایش)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۴۰V AC",
                ratedCurrent = "۶A (AC-15)",
                contactsNO = 0,
                contactsNC = 1,
                standardCode = "IEC 60947-5-1",
                isSampleValue = true
            ),
            symbolAscii = "-|\\|-"
        ),
        LibraryItem(
            type = ComponentType.SELECTOR_SWITCH_2POS,
            displayName = "کلید سلکتوری دوحالته (Selector Switch)",
            category = "شستی و کنترل",
            description = "کلید چرخشی تثبیت‌شونده دستی برای تعیین وضعیت کار دستی یا اتوماتیک (جریان نامی مقدار نمونه قابل ویرایش)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۴۰V AC",
                ratedCurrent = "۱۰A Ith",
                contactsNO = 1,
                contactsNC = 0,
                standardCode = "IEC 60947-5-1",
                isSampleValue = true
            ),
            symbolAscii = "-/ - 2P"
        ),

        // کنتاکتور و رله
        LibraryItem(
            type = ComponentType.CONTACTOR_COIL,
            displayName = "کویل / بوبین کنتاکتور (Contactor Coil)",
            category = "کنتاکتور و رله",
            description = "سیم‌پیچ مغناطیسی کنتاکتور با پایانه‌های A1-A2. مدل دیتاشیت رسمی Schneider LC1D09M7 با ولتاژ نامی ۲۲۰V AC (۵۰/۶۰Hz)؛ توان مصرفی بوبین: ۷۰VA تحریک لحظه‌ای (Inrush) و ۷VA نگهداری در ۵۰Hz (۷.۵VA در ۶۰Hz)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۲۰V AC (۵۰/۶۰Hz)",
                ratedCurrent = "-",
                contactsNO = 1,
                contactsNC = 0,
                powerRating = "تحریک ۷۰VA / نگهداری ۷VA (۵۰Hz) و ۷.۵VA (۶۰Hz)",
                standardCode = "IEC 60947-4-1",
                manufacturerModel = "Schneider TeSys D LC1D09M7",
                datasheetUrl = "https://eshop.se.com/id/lc1d09m7-tesys-d-kontactor-3p3-no-ac-3-440-v-9-a-220-v-ac-coil.html",
                voltageSource = SpecSource(SpecSourceType.DATASHEET, "https://eshop.se.com/id/lc1d09m7-tesys-d-kontactor-3p3-no-ac-3-440-v-9-a-220-v-ac-coil.html"),
                currentSource = SpecSource(SpecSourceType.UNKNOWN, "نامشخص"),
                powerSource = SpecSource(SpecSourceType.DATASHEET, "https://eshop.se.com/id/lc1d09m7-tesys-d-kontactor-3p3-no-ac-3-440-v-9-a-220-v-ac-coil.html"),
                standardCodeSource = SpecSource(SpecSourceType.STANDARD, "IEC 60947-4-1 بند 1"),
                modelSource = SpecSource(SpecSourceType.DATASHEET, "https://eshop.se.com/id/lc1d09m7-tesys-d-kontactor-3p3-no-ac-3-440-v-9-a-220-v-ac-coil.html"),
                contactsSource = SpecSource(SpecSourceType.STANDARD, "IEC 60947-4-1 بند 1")
            ),
            symbolAscii = "[ KM ]"
        ),
        LibraryItem(
            type = ComponentType.CONTACTOR_NO_AUX,
            displayName = "کنتاکت کمکی باز (Aux Contact NO 13-14)",
            category = "کنتاکتور و رله",
            description = "تیغه فرمان باز ۱۳-۱۴ برای خودنگهدار (Latch) شستی استارت طبق IEC 60947-5-1",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۴۰V AC",
                ratedCurrent = "۱۰A Ith (مقدار نمونه)",
                contactsNO = 1,
                contactsNC = 0,
                standardCode = "IEC 60947-5-1",
                isSampleValue = true
            ),
            symbolAscii = "--| |--"
        ),
        LibraryItem(
            type = ComponentType.CONTACTOR_NC_AUX,
            displayName = "کنتاکت کمکی بسته (Aux Contact NC 21-22)",
            category = "کنتاکتور و رله",
            description = "تیغه فرمان بسته ۲۱-۲۲ جهت اینترلاک الکتریکی بین دو کنتاکتور چپگرد/راستگرد طبق IEC 60947-5-1",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۴۰V AC",
                ratedCurrent = "۱۰A Ith (مقدار نمونه)",
                contactsNO = 0,
                contactsNC = 1,
                standardCode = "IEC 60947-5-1",
                isSampleValue = true
            ),
            symbolAscii = "--|/|--"
        ),
        LibraryItem(
            type = ComponentType.MAIN_POWER_CONTACTS_3P,
            displayName = "کنتاکت‌های قدرت ۳ فاز (Main Power Contacts)",
            category = "کنتاکتور و رله",
            description = "تیغه‌های قدرت ۳ فاز ۱/L1-۲/T1، ۳/L2-۴/T2، ۵/L3-۶/T3 کنتاکتور",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۴۰۰/۴۴۰V AC",
                ratedCurrent = "۹A (AC-3) بر مبنای LC1D09",
                contactsNO = 3,
                contactsNC = 0,
                powerRating = "۴kW در ۴۰۰V AC",
                standardCode = "IEC 60947-4-1",
                manufacturerModel = "Schneider TeSys D LC1D09M7",
                datasheetUrl = "https://eshop.se.com/id/lc1d09m7-tesys-d-kontactor-3p3-no-ac-3-440-v-9-a-220-v-ac-coil.html",
                voltageSource = SpecSource(SpecSourceType.DATASHEET, "https://eshop.se.com/id/lc1d09m7-tesys-d-kontactor-3p3-no-ac-3-440-v-9-a-220-v-ac-coil.html"),
                currentSource = SpecSource(SpecSourceType.DATASHEET, "https://eshop.se.com/id/lc1d09m7-tesys-d-kontactor-3p3-no-ac-3-440-v-9-a-220-v-ac-coil.html"),
                powerSource = SpecSource(SpecSourceType.DATASHEET, "https://eshop.se.com/id/lc1d09m7-tesys-d-kontactor-3p3-no-ac-3-440-v-9-a-220-v-ac-coil.html"),
                standardCodeSource = SpecSource(SpecSourceType.STANDARD, "IEC 60947-4-1 بند 1"),
                modelSource = SpecSource(SpecSourceType.DATASHEET, "https://eshop.se.com/id/lc1d09m7-tesys-d-kontactor-3p3-no-ac-3-440-v-9-a-220-v-ac-coil.html"),
                contactsSource = SpecSource(SpecSourceType.STANDARD, "IEC 60947-4-1 بند 1")
            ),
            symbolAscii = "[ ||| ]"
        ),
        LibraryItem(
            type = ComponentType.CONTROL_RELAY_1CO,
            displayName = "رله کمکی ۱ کنتاکت تبدیلی (1-CO Relay)",
            category = "کنتاکتور و رله",
            description = "رله واسطه و فرمان با یک کنتاکت چنج‌اور تبدیلی (پایه مشترک ۱۱، کنتاکت بسته ۱۲، کنتاکت باز ۱۴) و بوبین A1-A2 طبق استاندارد EN 50005 و IEC 60947-5-1 (نشانه‎گذاری قطعی استاندارد)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۲۰V AC (یا ۲۴V DC - مقدار نمونه)",
                ratedCurrent = "۱۰A (AC-1 / مقدار نمونه قابل ویرایش)",
                contactsNO = 1,
                contactsNC = 1,
                standardCode = "IEC 60947-5-1 / EN 50005",
                isSampleValue = true,
                voltageSource = SpecSource(SpecSourceType.SAMPLE, "ولتاژ نمونه ۲۲۰V AC"),
                currentSource = SpecSource(SpecSourceType.SAMPLE, "جریان نامی نمونه ۱۰A"),
                standardCodeSource = SpecSource(SpecSourceType.STANDARD, "EN 50005 / IEC 60947-5-1"),
                contactsSource = SpecSource(SpecSourceType.STANDARD, "EN 50005")
            ),
            symbolAscii = "[KA 1-CO]"
        ),
        LibraryItem(
            type = ComponentType.CONTROL_RELAY_2CO,
            displayName = "رله کمکی ۲ کنتاکت تبدیلی (2-CO Relay)",
            category = "کنتاکتور و رله",
            description = "رله فرمان با دو کنتاکت چنج‌اور تبدیلی مجزا (کنتاکت اول ۱۱-۱۲-۱۴ و کنتاکت دوم ۲۱-۲۲-۲۴) و بوبین A1-A2 طبق استاندارد EN 50005 و IEC 60947-5-1 (نشانه‎گذاری قطعی استاندارد)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۲۰V AC (یا ۲۴V DC - مقدار نمونه)",
                ratedCurrent = "۸A (AC-1 / مقدار نمونه قابل ویرایش)",
                contactsNO = 2,
                contactsNC = 2,
                standardCode = "IEC 60947-5-1 / EN 50005",
                isSampleValue = true,
                voltageSource = SpecSource(SpecSourceType.SAMPLE, "ولتاژ نمونه ۲۲۰V AC"),
                currentSource = SpecSource(SpecSourceType.SAMPLE, "جریان نامی نمونه ۸A"),
                standardCodeSource = SpecSource(SpecSourceType.STANDARD, "EN 50005 / IEC 60947-5-1"),
                contactsSource = SpecSource(SpecSourceType.STANDARD, "EN 50005")
            ),
            symbolAscii = "[KA 2-CO]"
        ),

        // تایمرها
        LibraryItem(
            type = ComponentType.TIMER_ON_DELAY_COIL,
            displayName = "تایمر تاخیر در وصل (Timer ON-Delay TON)",
            category = "تایمرها",
            description = "تایمر صنعتی الکترونیکی با عملکرد تأخیر در وصل (پس از برق‌دار شدن بوبین A1-A2 زمان تنظیم‌شده سپری می‌شود و سپس تیغه‌ها تغییر وضعیت می‌دهند). تیغه بسته ۱۵-۱۶ و تیغه باز ۱۵-۱۸",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۴-۲۴۰V AC/DC",
                ratedCurrent = "رله خروجی ۵A (مقدار نمونه)",
                contactsNO = 1,
                contactsNC = 1,
                powerRating = "بازه تاخیر نمونه: ۰.۱ ثانیه تا ۱۰ دقیقه",
                standardCode = "IEC 61812-1",
                isSampleValue = true
            ),
            symbolAscii = "[ TON ]"
        ),
        LibraryItem(
            type = ComponentType.TIMER_OFF_DELAY_COIL,
            displayName = "تایمر تاخیر در قطع (Timer OFF-Delay TOF)",
            category = "تایمرها",
            description = "تایمر تأخیر در قطع (با قطع ولتاژ ورودی، خروجی تا اتمام زمان تنظیم‌شده وصل می‌ماند)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۲۰V AC",
                ratedCurrent = "رله خروجی ۵A (مقدار نمونه)",
                contactsNO = 1,
                contactsNC = 1,
                standardCode = "IEC 61812-1",
                isSampleValue = true
            ),
            symbolAscii = "[ TOF ]"
        ),
        LibraryItem(
            type = ComponentType.TIMER_NO_CONTACT,
            displayName = "کنتاکت باز تایمر (Timer NO 15-18)",
            category = "تایمرها",
            description = "تیغه فرمان باز تایمر با شماره‌گذاری استاندارد صنعتی ۱۵ (مشترک) و ۱۸ (NO)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۴۰V AC",
                ratedCurrent = "۵A (مقدار نمونه)",
                contactsNO = 1,
                contactsNC = 0,
                standardCode = "IEC 61812-1",
                isSampleValue = true
            ),
            symbolAscii = "--[ | ]--"
        ),
        LibraryItem(
            type = ComponentType.TIMER_NC_CONTACT,
            displayName = "کنتاکت بسته تایمر (Timer NC 15-16)",
            category = "تایمرها",
            description = "تیغه فرمان بسته تایمر با شماره‌گذاری استاندارد صنعتی ۱۵ (مشترک) و ۱۶ (NC)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۴۰V AC",
                ratedCurrent = "۵A (مقدار نمونه)",
                contactsNO = 0,
                contactsNC = 1,
                standardCode = "IEC 61812-1",
                isSampleValue = true
            ),
            symbolAscii = "--[ / ]--"
        ),

        // سنسورها
        LibraryItem(
            type = ComponentType.LIMIT_SWITCH_NO,
            displayName = "میکروسوئیچ باز (Limit Switch NO 13-14)",
            category = "سنسورها",
            description = "کلید مکانیکی تعیین موقعیت و حد کورس با کنتاکت باز ۱۳-۱۴ (یا ۳-۴)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۴۰V AC",
                ratedCurrent = "۵A Ith (مقدار نمونه)",
                contactsNO = 1,
                contactsNC = 0,
                standardCode = "IEC 60947-5-1",
                isSampleValue = true
            ),
            symbolAscii = "o/ o"
        ),
        LibraryItem(
            type = ComponentType.LIMIT_SWITCH_NC,
            displayName = "میکروسوئیچ بسته (Limit Switch NC 11-12)",
            category = "سنسورها",
            description = "کلید مکانیکی تعیین موقعیت و حد کورس با کنتاکت بسته ۱۱-۱۲ (یا ۱-۲)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۴۰V AC",
                ratedCurrent = "۵A Ith (مقدار نمونه)",
                contactsNO = 0,
                contactsNC = 1,
                standardCode = "IEC 60947-5-1",
                isSampleValue = true
            ),
            symbolAscii = "o\\ o"
        ),
        LibraryItem(
            type = ComponentType.PROXIMITY_SENSOR,
            displayName = "سنسور القایی مجاورتی (Proximity Sensor)",
            category = "سنسورها",
            description = "سنسور مجاورتی تشخیص فلز با پیشوند استاندارد B (مطابق با IEC 81346-2 برای ترانسدیوسرها و حسگرها)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۱۰-۳۰V DC",
                ratedCurrent = "۲۰۰mA خروجی ترانزیستوری NPN/PNP (مقدار نمونه)",
                contactsNO = 1,
                contactsNC = 0,
                standardCode = "IEC 60947-5-2 / IEC 81346-2",
                isSampleValue = true
            ),
            symbolAscii = "<(B)>"
        ),

        // نشانگرها و خروجی‌ها
        LibraryItem(
            type = ComponentType.PILOT_LAMP_GREEN,
            displayName = "چراغ سیگنال سبز (Pilot Lamp Green)",
            category = "خروجی و نشانگر",
            description = "نشانگر وضعیت کارکرد عادی و وصل بودن تغذیه موتور (جریان تقریبی ۳.۶mA با توان ۰.۸W در ۲۲۰V AC)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۲۰V AC (۵۰Hz)",
                ratedCurrent = "۳.۶mA LED",
                powerRating = "۰.۸W",
                standardCode = "IEC 60947-5-1",
                isSampleValue = true
            ),
            symbolAscii = "( X ) Green"
        ),
        LibraryItem(
            type = ComponentType.PILOT_LAMP_RED,
            displayName = "چراغ سیگنال قرمز (Pilot Lamp Red)",
            category = "خروجی و نشانگر",
            description = "نشانگر وضعیت خاموشی یا تریپ اضافه‌بار در مدار (جریان تقریبی ۳.۶mA با توان ۰.۸W در ۲۲۰V AC)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۲۰V AC (۵۰Hz)",
                ratedCurrent = "۳.۶mA LED",
                powerRating = "۰.۸W",
                standardCode = "IEC 60947-5-1",
                isSampleValue = true
            ),
            symbolAscii = "( X ) Red"
        ),
        LibraryItem(
            type = ComponentType.PILOT_LAMP_YELLOW,
            displayName = "چراغ سیگنال زرد (Pilot Lamp Yellow)",
            category = "خروجی و نشانگر",
            description = "نشانگر وضعیت هشدار یا آماده‌باش مدار (جریان تقریبی ۳.۶mA با توان ۰.۸W در ۲۲۰V AC)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۲۰V AC (۵۰Hz)",
                ratedCurrent = "۳.۶mA LED",
                powerRating = "۰.۸W",
                standardCode = "IEC 60947-5-1",
                isSampleValue = true
            ),
            symbolAscii = "( X ) Yellow"
        ),
        LibraryItem(
            type = ComponentType.ALARM_BUZZER,
            displayName = "آژیر هشدار صوتی (Alarm Buzzer)",
            category = "خروجی و نشانگر",
            description = "هشدار صوتی با پایانه‌های X1-X2. مشخصات نمونه قابل ویرایش: شدت صدا حدود ۸۵dB در فاصله ۱ متر، فرکانس حدود ۲.۴kHz و مصرف الکتریکی حدود ۵VA در ۲۲۰V AC",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۲۰V AC (۵۰Hz)",
                ratedCurrent = "۲۵mA (مقدار نمونه)",
                powerRating = "۵VA",
                standardCode = "IEC 60947-5-1",
                isSampleValue = true
            ),
            symbolAscii = "<| Buzzer"
        ),

        // مصرف‌کننده‌ها
        LibraryItem(
            type = ComponentType.MOTOR_3PHASE,
            displayName = "الکتروموتور سه‌فاز القایی (3-Phase Motor)",
            category = "مصرف‌کننده‌ها",
            description = "الکتروموتور آسنکرون روتور قفس‌سنجابی سه‌فاز با تخته‌کلمپ ۶ ترمیناله (U1-V1-W1 و U2-V2-W2) و ارت PE مطابق IEC 60034-1 و IEC 60034-8",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۳۸۰/۴۰۰V AC (سه‌فاز)",
                ratedCurrent = "۸.۵A (جریان نامی در بار کامل - مقدار نمونه)",
                powerRating = "۴kW (۵.۵ اسب بخار) ۱۴۴۰ دور در دقیقه (مقدار نمونه)",
                standardCode = "IEC 60034-1 / IEC 60034-8",
                isSampleValue = true
            ),
            symbolAscii = "( M 3~ )"
        ),
        LibraryItem(
            type = ComponentType.MOTOR_1PHASE,
            displayName = "الکتروموتور تک‌فاز (Single Phase Motor)",
            category = "مصرف‌کننده‌ها",
            description = "موتور تک‌فاز ۲۲۰ ولت با خازن راه‌انداز جهت بارهای سبک کارگاهی (مقادیر نمونه قابل ویرایش)",
            defaultSpecs = TechnicalSpecs(
                ratedVoltage = "۲۲۰V AC (۵۰Hz)",
                ratedCurrent = "۶.۲A (مقدار نمونه)",
                powerRating = "۱.۱kW (۱.۵ اسب بخار) (مقدار نمونه)",
                standardCode = "IEC 60034-1",
                isSampleValue = true
            ),
            symbolAscii = "( M 1~ )"
        )
    )

    fun getByCategory(): Map<String, List<LibraryItem>> {
        return items.groupBy { it.category }
    }
}
