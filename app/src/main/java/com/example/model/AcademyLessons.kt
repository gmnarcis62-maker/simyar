package com.example.model

data class AcademyLesson(
    val id: Int,
    val title: String,
    val level: String, // مقدماتی، متوسط، پیشرفته
    val description: String,
    val objectiveText: String,
    val initialProject: CircuitProject,
    val isVipOnly: Boolean = false,
    val isComingSoon: Boolean = false,
    val validationHint: String
)

object AcademyLessonsData {

    val lessons: List<AcademyLesson> = listOf(
        // درس ۱: راه‌اندازی شستی و لامپ
        AcademyLesson(
            id = 1,
            title = "۱. مدار روشن کردن ساده لامپ با شستی NO",
            level = "مقدماتی",
            description = "آشنایی با مبانی مدار فرمان، عبور فاز از شستی استارت و اتصال به نول.",
            objectiveText = "سیم‌کشی فاز به شستی استارت (SB1) و اتصال خروجی آن به چراغ سیگنال سبز (HL1) و در نهایت به نول.",
            initialProject = CircuitProject(
                id = "lesson_1",
                title = "تمرین ۱: مدار پایه شستی و لامپ",
                description = "اتصال شستی به لامپ",
                components = listOf(
                    CircuitComponent("l1", ComponentType.AC_PHASE, "فاز L", 100f, 80f),
                    CircuitComponent("sb1", ComponentType.PUSH_BUTTON_NO, "استارت SB1", 100f, 220f),
                    CircuitComponent("hl1", ComponentType.PILOT_LAMP_GREEN, "چراغ HL1", 100f, 360f),
                    CircuitComponent("n1", ComponentType.AC_NEUTRAL, "نول N", 100f, 500f)
                ),
                wires = listOf(
                    WireConnection("w1", "l1", "L", "sb1", "13")
                )
            ),
            isVipOnly = false,
            validationHint = "سیم خروجی ترمینال ۱۴ شستی را به ترمینال X1 چراغ، و X2 چراغ را به نول وصل کنید."
        ),

        // درس ۲: مدار استپ / استارت مستقیم با خودنگهدار (DOL)
        AcademyLesson(
            id = 2,
            title = "۲. خودنگهدار کنتاکتور (Self-Hold Latch)",
            level = "مقدماتی",
            description = "اصلی‌ترین مدار برق صنعتی: چگونه بوبین کنتاکتور پس از رها شدن شستی استارت روشن می‌ماند؟",
            objectiveText = "موازی کردن کنتاکت باز کمکی ۱۳-۱۴ کنتاکتور با شستی استارت جهت ایجاد فیدبک خودنگهدار.",
            initialProject = DefaultCircuitTemplates.createDirectOnlineStarter(),
            isVipOnly = false,
            validationHint = "کنتاکت ۱۳-۱۴ تیغه KM1 را به صورت موازی با دو سر شستی استارت SB2 وصل نمایید."
        ),

        // درس ۳: حفاظت حرارتی با رله بی‌متال
        AcademyLesson(
            id = 3,
            title = "۳. حفاظت الکتروموتور با رله حرارتی بی‌متال",
            level = "مقدماتی",
            description = "جلوگیری از سوختن موتور در حالت اضافه‌بار با استفاده از کنتاکت بسته ۹۵-۹۶ بی‌متال.",
            objectiveText = "قرار دادن کنتاکت ۹۵-۹۶ بی‌متال در مسیر اصلی تغذیه مدار فرمان قبل از شستی استپ.",
            initialProject = CircuitProject(
                id = "lesson_3",
                title = "تمرین ۳: مدار حفاظت بی‌متال",
                description = "سری کردن کنتاکت بسته بی‌متال با بوبین",
                components = listOf(
                    CircuitComponent("l1", ComponentType.AC_PHASE, "فاز L", 120f, 60f),
                    CircuitComponent("ol1", ComponentType.BIMETAL_OVERLOAD, "بی‌متال ۹۵-۹۶", 120f, 180f),
                    CircuitComponent("sb1", ComponentType.PUSH_BUTTON_NC, "استپ SB1", 120f, 300f),
                    CircuitComponent("km1", ComponentType.CONTACTOR_COIL, "بوبین KM1", 120f, 440f, tagBinding = "KM1"),
                    CircuitComponent("n1", ComponentType.AC_NEUTRAL, "نول N", 120f, 560f)
                ),
                wires = listOf(
                    WireConnection("w1", "l1", "L", "ol1", "95"),
                    WireConnection("w2", "ol1", "96", "sb1", "21"),
                    WireConnection("w3", "sb1", "22", "km1", "A1"),
                    WireConnection("w4", "km1", "A2", "n1", "N")
                )
            ),
            isVipOnly = false,
            validationHint = "با تحریک بی‌متال کنتاکت ۹۵-۹۶ باز شده و مدار فرمان کنتاکتور را قطع می‌کند."
        ),

        // درس ۴: مدار فرمان از دو نقطه (کنترل راه دور و محلی)
        AcademyLesson(
            id = 4,
            title = "۴. کنترل موتور از دو نقطه (موازی و سری)",
            level = "متوسط",
            description = "روشن و خاموش کردن موتور از تابلوی محلی و اتاق فرمان دوردست.",
            objectiveText = "سری کردن استپ‌ها با یکدیگر و موازی کردن استارت‌ها با یکدیگر.",
            initialProject = CircuitProject(
                id = "lesson_4",
                title = "تمرین ۴: کنترل از دو محل",
                description = "استارت‌های موازی و استپ‌های سری",
                components = listOf(
                    CircuitComponent("l1", ComponentType.AC_PHASE, "فاز L", 120f, 60f),
                    CircuitComponent("sb1", ComponentType.PUSH_BUTTON_NC, "استپ محلی ۱", 120f, 160f),
                    CircuitComponent("sb2", ComponentType.PUSH_BUTTON_NC, "استپ کنترل ۲", 120f, 260f),
                    CircuitComponent("sb3", ComponentType.PUSH_BUTTON_NO, "استارت محلی ۱", 80f, 380f),
                    CircuitComponent("sb4", ComponentType.PUSH_BUTTON_NO, "استارت کنترل ۲", 220f, 380f),
                    CircuitComponent("km1", ComponentType.CONTACTOR_COIL, "بوبین KM1", 120f, 520f, tagBinding = "KM1"),
                    CircuitComponent("n1", ComponentType.AC_NEUTRAL, "نول N", 120f, 640f)
                ),
                wires = listOf(
                    WireConnection("w1", "l1", "L", "sb1", "21"),
                    WireConnection("w2", "sb1", "22", "sb2", "21"),
                    WireConnection("w3", "sb2", "22", "sb3", "13"),
                    WireConnection("w4", "sb2", "22", "sb4", "13"),
                    WireConnection("w5", "sb3", "14", "km1", "A1"),
                    WireConnection("w6", "sb4", "14", "km1", "A1"),
                    WireConnection("w7", "km1", "A2", "n1", "N")
                )
            ),
            isVipOnly = false,
            validationHint = "با زدن هر یک از دو شستی استارت مدار روشن و با هر کدام از استپ‌ها خاموش می‌شود."
        ),

        // درس ۵: اینترلاک الکتریکی چپگرد / راستگرد (Reversing Starter)
        AcademyLesson(
            id = 5,
            title = "۵. اینترلاک الکتریکی چپگرد و راستگرد",
            level = "متوسط",
            description = "جلوگیری قطعی از اتصال کوتاه دوفاز با قرار دادن کنتاکت NC هر کنتاکتور در مسیر کویل کنتاکتور دیگر.",
            objectiveText = "استفاده از تیغه‌های بسته ۲۱-۲۲ کنتاکتورهای KM1 و KM2 در مسیر یکدیگر.",
            initialProject = DefaultCircuitTemplates.createForwardReverseStarter(),
            isVipOnly = false,
            validationHint = "هنگام کار کنتاکتور راستگرد، فشار دادن استارت چپگرد نباید کنتاکتور چپگرد را روشن کند."
        ),

        // درس ۶: مدار ستاره - مثلث اتوماتیک با تایمر تاخیر در وصل
        AcademyLesson(
            id = 6,
            title = "۶. مدار ستاره - مثلث اتوماتیک با تایمر تاخیر در وصل",
            level = "پیشرفته",
            description = "راه‌اندازی موتور سه‌فاز توان بالا ابتدا در اتصال ستاره با جریان کمتر و سپس سوئیچ به مثلث.",
            objectiveText = "استفاده از تایمر تاخیر در وصل TON و اینترلاک بین کنتاکتورهای ستاره و مثلث.",
            initialProject = DefaultCircuitTemplates.createStarDeltaStarter(),
            isVipOnly = true,
            isComingSoon = false,
            validationHint = "تایمر پس از ۴ ثانیه وضعیت کنتاکتور ستاره را قطع و مثلث را وارد مدار می‌کند."
        ),

        // درس ۷: راه‌اندازی یکی پس از دیگری (توالی موتورها)
        AcademyLesson(
            id = 7,
            title = "۷. راه‌اندازی یکی پس از دیگری (توالی موتورها)",
            level = "پیشرفته",
            description = "موتور دوم تنها پس از روشن شدن موتور اول مجاز به راه‌اندازی باشد (مانند خطوط انتقال نوار نقاله).",
            objectiveText = "قرار دادن تیغه باز کمکی ۱۳-۱۴ موتور اول (KM1) در مسیر استارت موتور دوم (KM2).",
            initialProject = CircuitProject(
                id = "lesson_7",
                title = "تمرین ۷: مدار توالی موتورها",
                description = "استارت ترتیبی دو موتور",
                components = listOf(
                    CircuitComponent("l1", ComponentType.AC_PHASE, "فاز L", 150f, 60f),
                    CircuitComponent("sb_stop", ComponentType.PUSH_BUTTON_NC, "استپ کلی", 150f, 150f),
                    CircuitComponent("sb_start1", ComponentType.PUSH_BUTTON_NO, "استارت موتور ۱", 80f, 250f),
                    CircuitComponent("km1_aux", ComponentType.CONTACTOR_NO_AUX, "خودنگهدار KM1", 170f, 250f, tagBinding = "KM1"),
                    CircuitComponent("km1_coil", ComponentType.CONTACTOR_COIL, "بوبین موتور ۱", 120f, 360f, tagBinding = "KM1"),
                    CircuitComponent("km1_perm", ComponentType.CONTACTOR_NO_AUX, "مجوز KM1 برای موتور ۲", 280f, 250f, tagBinding = "KM1"),
                    CircuitComponent("sb_start2", ComponentType.PUSH_BUTTON_NO, "استارت موتور ۲", 280f, 360f),
                    CircuitComponent("km2_coil", ComponentType.CONTACTOR_COIL, "بوبین موتور ۲", 280f, 480f, tagBinding = "KM2"),
                    CircuitComponent("n1", ComponentType.AC_NEUTRAL, "نول N", 200f, 600f)
                ),
                wires = listOf(
                    WireConnection("w1", "l1", "L", "sb_stop", "21"),
                    WireConnection("w2", "sb_stop", "22", "sb_start1", "13"),
                    WireConnection("w3", "sb_stop", "22", "km1_aux", "13"),
                    WireConnection("w4", "sb_start1", "14", "km1_coil", "A1"),
                    WireConnection("w5", "km1_aux", "14", "km1_coil", "A1"),
                    WireConnection("w6", "sb_stop", "22", "km1_perm", "13"),
                    WireConnection("w7", "km1_perm", "14", "sb_start2", "13"),
                    WireConnection("w8", "sb_start2", "14", "km2_coil", "A1"),
                    WireConnection("w9", "km1_coil", "A2", "n1", "N"),
                    WireConnection("w10", "km2_coil", "A2", "n1", "N")
                )
            ),
            isVipOnly = true,
            isComingSoon = false,
            validationHint = "موتور دوم را بدون روشن کردن موتور اول تست کنید؛ نباید استارت بخورد."
        ),

        // درس ۸: راه‌اندازی یکی به جای دیگری (اتوماسیون پمپ‌ها)
        AcademyLesson(
            id = 8,
            title = "۸. راه‌اندازی یکی به جای دیگری (اتوماسیون پمپ‌ها)",
            level = "متوسط",
            description = "با روشن شدن پمپ دوم، پمپ اول به صورت خودکار از مدار خارج گردد.",
            objectiveText = "استفاده از تیغه بسته ۲۱-۲۲ کنتاکتور دوم در مسیر کویل کنتاکتور اول.",
            initialProject = CircuitProject(
                id = "lesson_8",
                title = "تمرین ۸: یکی به جای دیگری",
                description = "خروج خودکار پمپ اول با وصل پمپ دوم",
                components = listOf(
                    CircuitComponent("l1", ComponentType.AC_PHASE, "فاز L", 160f, 60f),
                    CircuitComponent("sb_stop", ComponentType.PUSH_BUTTON_NC, "استپ کلی", 160f, 150f),
                    CircuitComponent("sb_start1", ComponentType.PUSH_BUTTON_NO, "استارت پمپ ۱", 80f, 260f),
                    CircuitComponent("km1_aux", ComponentType.CONTACTOR_NO_AUX, "خودنگهدار KM1", 160f, 260f, tagBinding = "KM1"),
                    CircuitComponent("km2_nc", ComponentType.CONTACTOR_NC_AUX, "قطع کننده KM2", 80f, 380f, tagBinding = "KM2"),
                    CircuitComponent("km1_coil", ComponentType.CONTACTOR_COIL, "پمپ ۱ (KM1)", 80f, 500f, tagBinding = "KM1"),
                    CircuitComponent("sb_start2", ComponentType.PUSH_BUTTON_NO, "استارت پمپ ۲", 260f, 260f),
                    CircuitComponent("km2_aux", ComponentType.CONTACTOR_NO_AUX, "خودنگهدار KM2", 340f, 260f, tagBinding = "KM2"),
                    CircuitComponent("km2_coil", ComponentType.CONTACTOR_COIL, "پمپ ۲ (KM2)", 260f, 500f, tagBinding = "KM2"),
                    CircuitComponent("n1", ComponentType.AC_NEUTRAL, "نول N", 170f, 620f)
                ),
                wires = listOf(
                    WireConnection("w1", "l1", "L", "sb_stop", "21"),
                    WireConnection("w2", "sb_stop", "22", "sb_start1", "13"),
                    WireConnection("w3", "sb_stop", "22", "km1_aux", "13"),
                    WireConnection("w4", "sb_start1", "14", "km2_nc", "21"),
                    WireConnection("w5", "km1_aux", "14", "km2_nc", "21"),
                    WireConnection("w6", "km2_nc", "22", "km1_coil", "A1"),
                    WireConnection("w7", "sb_stop", "22", "sb_start2", "13"),
                    WireConnection("w8", "sb_stop", "22", "km2_aux", "13"),
                    WireConnection("w9", "sb_start2", "14", "km2_coil", "A1"),
                    WireConnection("w10", "km2_aux", "14", "km2_coil", "A1"),
                    WireConnection("w11", "km1_coil", "A2", "n1", "N"),
                    WireConnection("w12", "km2_coil", "A2", "n1", "N")
                )
            ),
            isVipOnly = true,
            isComingSoon = false,
            validationHint = "ابتدا پمپ ۱ را روشن کنید؛ سپس با زدن استارت پمپ ۲، پمپ ۱ خاموش شده و پمپ ۲ روشن می‌ماند."
        ),

        // درس ۹: مدار چپگرد و راستگرد با میکروسوئیچ
        AcademyLesson(
            id = 9,
            title = "۹. مدار چپگرد و راستگرد با میکروسوئیچ (Limit Switch)",
            level = "پیشرفته",
            description = "توقف یا معکوس‌سازی جهت حرکت ماشین‌آلات متحرک در انتهای کورس با میکروسوئیچ‌های SQ1 و SQ2.",
            objectiveText = "قرار دادن کنتاکت‌های بسته ۱۱-۱۲ میکروسوئیچ‌ها در مسیر بوبین‌های مربوطه جهت قطع در انتهای کورس.",
            initialProject = CircuitProject(
                id = "lesson_9",
                title = "تمرین ۹: کنترل حد کورس با میکروسوئیچ",
                description = "حفاظت کورس با میکروسوئیچ‌های حد",
                components = listOf(
                    CircuitComponent("l1", ComponentType.AC_PHASE, "فاز L", 160f, 60f),
                    CircuitComponent("sb_stop", ComponentType.PUSH_BUTTON_NC, "استپ کلی", 160f, 150f),
                    CircuitComponent("sb_fwd", ComponentType.PUSH_BUTTON_NO, "استارت جلو", 80f, 260f),
                    CircuitComponent("km1_aux", ComponentType.CONTACTOR_NO_AUX, "خودنگهدار KM1", 160f, 260f, tagBinding = "KM1"),
                    CircuitComponent("sq_fwd", ComponentType.LIMIT_SWITCH_NC, "میکروسوئیچ انتهای جلو SQ1", 80f, 380f),
                    CircuitComponent("km1_coil", ComponentType.CONTACTOR_COIL, "راستگرد KM1", 80f, 500f, tagBinding = "KM1"),
                    CircuitComponent("sb_rev", ComponentType.PUSH_BUTTON_NO, "استارت عقب", 260f, 260f),
                    CircuitComponent("km2_aux", ComponentType.CONTACTOR_NO_AUX, "خودنگهدار KM2", 340f, 260f, tagBinding = "KM2"),
                    CircuitComponent("sq_rev", ComponentType.LIMIT_SWITCH_NC, "میکروسوئیچ انتهای عقب SQ2", 260f, 380f),
                    CircuitComponent("km2_coil", ComponentType.CONTACTOR_COIL, "چپگرد KM2", 260f, 500f, tagBinding = "KM2"),
                    CircuitComponent("n1", ComponentType.AC_NEUTRAL, "نول N", 170f, 620f)
                ),
                wires = listOf(
                    WireConnection("w1", "l1", "L", "sb_stop", "21"),
                    WireConnection("w2", "sb_stop", "22", "sb_fwd", "13"),
                    WireConnection("w3", "sb_stop", "22", "km1_aux", "13"),
                    WireConnection("w4", "sb_fwd", "14", "sq_fwd", "11"),
                    WireConnection("w5", "km1_aux", "14", "sq_fwd", "11"),
                    WireConnection("w6", "sq_fwd", "12", "km1_coil", "A1"),
                    WireConnection("w7", "sb_stop", "22", "sb_rev", "13"),
                    WireConnection("w8", "sb_stop", "22", "km2_aux", "13"),
                    WireConnection("w9", "sb_rev", "14", "sq_rev", "11"),
                    WireConnection("w10", "km2_aux", "14", "sq_rev", "11"),
                    WireConnection("w11", "sq_rev", "12", "km2_coil", "A1"),
                    WireConnection("w12", "km1_coil", "A2", "n1", "N"),
                    WireConnection("w13", "km2_coil", "A2", "n1", "N")
                )
            ),
            isVipOnly = true,
            isComingSoon = false,
            validationHint = "با لمس میکروسوئیچ حد کورس، کنتاکتور جهت متناظر فورا قطع می‌شود."
        ),

        // درس ۱۰: کنترل مخزن و پمپ با کلید سلکتوری و سنسور القایی
        AcademyLesson(
            id = 10,
            title = "۱۰. کنترل سطح مخزن با کلید سلکتوری و سنسور",
            level = "متوسط",
            description = "روشن و خاموش کردن پمپ آب بر اساس سنسور موقعیت و انتخاب وضعیت دستی/خودکار با کلید سلکتوری.",
            objectiveText = "طراحی مدار فرمان پمپ با قابلیت انتخاب حالت دستی (Manual) یا حسگر اتوماتیک (Auto).",
            initialProject = CircuitProject(
                id = "lesson_10",
                title = "تمرین ۱۰: کنترل پمپ با سنسور و کلید سلکتوری",
                description = "حالت کاری دستی و اتوماتیک پمپ",
                components = listOf(
                    CircuitComponent("l1", ComponentType.AC_PHASE, "فاز L", 120f, 60f),
                    CircuitComponent("sa1", ComponentType.SELECTOR_SWITCH_2POS, "سلکتور دستی/اتومات", 120f, 160f),
                    CircuitComponent("sb_man", ComponentType.PUSH_BUTTON_NO, "استارت دستی", 60f, 280f),
                    CircuitComponent("sq_level", ComponentType.LIMIT_SWITCH_NO, "سوئیچ فلوتر سطح آب", 200f, 280f),
                    CircuitComponent("km_pump", ComponentType.CONTACTOR_COIL, "بوبین پمپ", 120f, 420f, tagBinding = "KM1"),
                    CircuitComponent("hl_run", ComponentType.PILOT_LAMP_GREEN, "چراغ پمپ", 220f, 420f),
                    CircuitComponent("n1", ComponentType.AC_NEUTRAL, "نول N", 170f, 550f)
                ),
                wires = listOf(
                    WireConnection("w1", "l1", "L", "sa1", "IN"),
                    WireConnection("w2", "sa1", "OUT", "sb_man", "13"),
                    WireConnection("w3", "sa1", "OUT", "sq_level", "13"),
                    WireConnection("w4", "sb_man", "14", "km_pump", "A1"),
                    WireConnection("w5", "sq_level", "14", "km_pump", "A1"),
                    WireConnection("w6", "km_pump", "A1", "hl_run", "X1"),
                    WireConnection("w7", "km_pump", "A2", "n1", "N"),
                    WireConnection("w8", "hl_run", "X2", "n1", "N")
                )
            ),
            isVipOnly = true,
            isComingSoon = false,
            validationHint = "با فعال کردن سلکتور و سوئیچ سطح، پمپ و چراغ نشانگر روشن می‌شوند."
        ),

        // درس ۱۱: مدار فرمان تایمر چشمک‌زن (پالس‌ساز فلاشر)
        AcademyLesson(
            id = 11,
            title = "۱۱. مدار فرمان تایمر فلاشر (پالس‌ساز هشدار)",
            level = "پیشرفته",
            description = "ایجاد پالس متناوب خاموش و روشن با تایمر تاخیر در وصل جهت آلارم خطر و چراغ زرد سیگنال.",
            objectiveText = "تغذیه متقابل تیغه تایمر برای ساخت چرخه پیوسته پالس زمانی.",
            initialProject = CircuitProject(
                id = "lesson_11",
                title = "تمرین ۱۱: پالس‌ساز فلاشر",
                description = "مدار فلاشر تایمری",
                components = listOf(
                    CircuitComponent("l1", ComponentType.AC_PHASE, "فاز L", 120f, 60f),
                    CircuitComponent("kt1", ComponentType.TIMER_ON_DELAY_COIL, "تایمر KT1", 120f, 200f, tagBinding = "KT1", delaySeconds = 2f),
                    CircuitComponent("kt1_contact", ComponentType.TIMER_NO_CONTACT, "تیغه باز KT1 (15-18)", 120f, 340f, tagBinding = "KT1"),
                    CircuitComponent("hl_alarm", ComponentType.PILOT_LAMP_YELLOW, "فلاشر زرد HL1", 120f, 460f),
                    CircuitComponent("n1", ComponentType.AC_NEUTRAL, "نول N", 120f, 580f)
                ),
                wires = listOf(
                    WireConnection("w1", "l1", "L", "kt1", "A1"),
                    WireConnection("w2", "kt1", "A2", "n1", "N"),
                    WireConnection("w3", "l1", "L", "kt1_contact", "15"),
                    WireConnection("w4", "kt1_contact", "18", "hl_alarm", "X1"),
                    WireConnection("w5", "hl_alarm", "X2", "n1", "N")
                )
            ),
            isVipOnly = true,
            isComingSoon = false,
            validationHint = "با اتمام زمان تنظیم شده تایمر، چراغ سیگنال زرد روشن می‌شود."
        ),

        // درس ۱۲: ترمز تزریق DC (به‌زودی - نیازمند قطعه یکسوکننده ترمز DC)
        AcademyLesson(
            id = 12,
            title = "۱۲. ترمز جریان مستقیم الکتروموتور (DC Injection Braking)",
            level = "پیشرفته",
            description = "توقف سریع محور موتور با تزریق ولتاژ DC به سیم‌پیچ‌های استاتور پس از قطع تغذیه متناوب.",
            objectiveText = "به‌زودی: این آموزش پس از افزودن ماژول ترمز یکسوکننده اختصاصی در به‌روزرسانی بعدی اضافه می‌شود.",
            initialProject = CircuitProject("lesson_12", "ترمز تزریق DC (به‌زودی)", "نیازمند ماژول ترمز", emptyList(), emptyList()),
            isVipOnly = false,
            isComingSoon = true,
            validationHint = "این محتوا در دست تولید است و به‌زودی در دسترس قرار می‌گیرد."
        ),

        // درس ۱۳: شبیه‌سازی منطق نردبانی PLC با ورودی‌های تابلو
        AcademyLesson(
            id = 13,
            title = "۱۳. اتصال شبیه‌ساز PLC به مدار فرمان",
            level = "پیشرفته",
            description = "ارتباط مستقیم ورودی‌های دیجیتال PLC با شستی‌ها و کنترل خروجی Q0.0 جهت فرمان به بوبین تابلو.",
            objectiveText = "استفاده از تگ‌های PLC در قطعات بوم جهت پیوند زنده با موتور شبیه‌سازی لدر.",
            initialProject = CircuitProject(
                id = "lesson_13",
                title = "تمرین ۱۳: کنترل هوشمند با PLC",
                description = "فرمان به بوبین از طریق خروجی دیجیتال PLC",
                components = listOf(
                    CircuitComponent("l1", ComponentType.AC_PHASE, "فاز L", 100f, 60f),
                    CircuitComponent("sb_plc", ComponentType.PUSH_BUTTON_NO, "استارت ورودی I0.1", 100f, 180f, plcTagBinding = "I0.1"),
                    CircuitComponent("km_plc", ComponentType.CONTACTOR_COIL, "کنتاکتور خروجی Q0.0", 100f, 320f, tagBinding = "KM1", plcTagBinding = "Q0.0"),
                    CircuitComponent("hl_plc", ComponentType.PILOT_LAMP_GREEN, "چراغ خروجی Q0.1", 220f, 320f, plcTagBinding = "Q0.1"),
                    CircuitComponent("n1", ComponentType.AC_NEUTRAL, "نول N", 160f, 460f)
                ),
                wires = listOf(
                    WireConnection("w1", "l1", "L", "km_plc", "A1"),
                    WireConnection("w2", "km_plc", "A2", "n1", "N"),
                    WireConnection("w3", "l1", "L", "hl_plc", "X1"),
                    WireConnection("w4", "hl_plc", "X2", "n1", "N")
                ),
                ladderRungs = DefaultCircuitTemplates.createDefaultLadderProgram()
            ),
            isVipOnly = true,
            isComingSoon = false,
            validationHint = "با اجرای برنامه لدر و فشردن استارت ورودی، خروجی‌های متناظر در مدار روشن می‌شوند."
        ),

        // درس ۱۴: راه‌اندازی نرم با سافت‌استارتر (به‌زودی - نیازمند قطعه سافت‌استارتر)
        AcademyLesson(
            id = 14,
            title = "۱۴. مدار راه‌اندازی نرم (Soft Starter) و بای‌پس",
            level = "پیشرفته",
            description = "راه‌اندازی آرام موتور با کنترل زاویه آتش تریستورها و سپس سوئیچ به خط مستقیم با کنتاکتور بای‌پس.",
            objectiveText = "به‌زودی: این آموزش پس از افزودن کامپوننت سافت‌استارتر در به‌روزرسانی آتی منتشر می‌گردد.",
            initialProject = CircuitProject("lesson_14", "سافت‌استارتر (به‌زودی)", "نیازمند قطعه سافت‌استارتر", emptyList(), emptyList()),
            isVipOnly = false,
            isComingSoon = true,
            validationHint = "این محتوا در دست تولید است و به‌زودی در دسترس قرار می‌گیرد."
        ),

        // درس ۱۵: کنترل هوشمند تهویه با اینورتر و سنسور گاز (به‌زودی - نیازمند اینورتر)
        AcademyLesson(
            id = 15,
            title = "۱۵. مدار هوشمند تهویه با درایو اینورتر و حسگر",
            level = "پیشرفته",
            description = "کنترل فرکانسی دور موتور اگزاست‌فن با درایو سرعت متغیر (VFD) و حسگرهای محیطی.",
            objectiveText = "به‌زودی: این آموزش پس از اضافه شدن قطعه اینورتر در به‌روزرسانی آتی فعال خواهد شد.",
            initialProject = CircuitProject("lesson_15", "درایو VFD (به‌زودی)", "نیازمند کامپوننت اینورتر", emptyList(), emptyList()),
            isVipOnly = false,
            isComingSoon = true,
            validationHint = "این محتوا در دست تولید است و به‌زودی در دسترس قرار می‌گیرد."
        )
    )
}

object AcademyRepository {
    fun getLessons(): List<AcademyLesson> = AcademyLessonsData.lessons

    fun getDefaultSpecsForType(type: ComponentType): TechnicalSpecs {
        return IndustrialComponentLibrary.items.find { it.type == type }?.defaultSpecs
            ?: TechnicalSpecs()
    }
}
