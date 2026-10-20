package com.example.model

/**
 * الگوهای آماده و استاندارد صنعتی مدارات برق صنعتی و برنامه‌نویسی PLC سیمیار:
 * ۱. راه‌اندازی مستقیم (DOL): مدار قدرت کامل ۳فاز + رله حرارتی بی‌متال در مسیر قدرت + مدار فرمان خودنگهدار با بی‌متال و شستی قارچی
 * ۲. راه‌اندازی ستاره-مثلث (Star-Delta): مدار قدرت واقعی با ۳ کنتاکتور (KM1 خط، KM2 ستاره، KM3 مثلث)، رله حرارتی در مسیر خط، موتور با تخته‌کلمپ ۶ ترمیناله (U1-V1-W1 و U2-V2-W2) و مدار فرمان مجهز به تایمر تاخیر در وصل و اینترلاک دوطرفه
 * ۳. چپگرد / راستگرد (Forward-Reverse): مدار قدرت با جابجایی دو فاز، اینترلاک الکتریکی دوطرفه و خودنگهدار مجزا
 * ۴. برنامه پیش‌فرض لدر PLC: خودنگهدار واقعی (کنتاکت باز Q0.0 موازی با استارت I0.1)
 */
object DefaultCircuitTemplates {

    fun createDirectOnlineStarter(): CircuitProject {
        // ۱. مدار قدرت ۳ فاز استاندارد:
        // منبع ۳ فاز -> کنتاکت‌های قدرت KM1 -> رله حرارتی بی‌متال (عنصر قدرت ۱-۲, ۳-۴, ۵-۶) -> موتور سه‌فاز (U1, V1, W1)
        // ۲. مدار فرمان استاندارد:
        // فاز L1 -> مینیاتوری فرمان F1 -> شستی قارچی اضطراری E-Stop -> کنتاکت فرمان ۹۵-۹۶ بی‌متال -> شستی استپ -> شستی استارت موازی با خودنگهدار ۱۳-۱۴ KM1 -> بوبین KM1 و چراغ سیگنال -> نول N
        val comps = listOf(
            // مدار قدرت سمت چپ (X=40)
            CircuitComponent("src_3p", ComponentType.THREE_PHASE_SOURCE, "منبع ۳فاز (L1-L2-L3)", 40f, 60f),
            CircuitComponent("km1_pwr", ComponentType.MAIN_POWER_CONTACTS_3P, "کنتاکت قدرت KM1 (1-3-5)", 40f, 180f, tagBinding = "KM1"),
            CircuitComponent("ol_pwr", ComponentType.BIMETAL_OVERLOAD, "بی‌متال حرارتی FR1", 40f, 300f, tagBinding = "KM1"),
            CircuitComponent("m1", ComponentType.MOTOR_3PHASE, "الکتروموتور ۳فاز M1", 40f, 430f),

            // مدار فرمان سمت راست با چیدمان ستونی فشرده و استاندارد (X=170 و خودنگهدار X=270)
            CircuitComponent("l1", ComponentType.AC_PHASE, "فاز فرمان L1", 170f, 60f),
            CircuitComponent("f1", ComponentType.CIRCUIT_BREAKER_MCB, "مینیاتوری فرمان F1", 170f, 160f, isClosedOrPressed = true),
            CircuitComponent("e1", ComponentType.EMERGENCY_STOP, "استپ قارچی (E-Stop)", 170f, 260f),
            CircuitComponent("ol1", ComponentType.BIMETAL_OVERLOAD, "کنتاکت فرمان بی‌متال (95-96)", 170f, 360f),
            CircuitComponent("sb1", ComponentType.PUSH_BUTTON_NC, "شستی استپ SB1", 170f, 460f),
            CircuitComponent("sb2", ComponentType.PUSH_BUTTON_NO, "شستی استارت SB2", 170f, 560f),
            CircuitComponent("km1_aux", ComponentType.CONTACTOR_NO_AUX, "خودنگهدار KM1 (13-14)", 270f, 560f, tagBinding = "KM1"),
            CircuitComponent("km1_coil", ComponentType.CONTACTOR_COIL, "بوبین کنتاکتور KM1", 170f, 680f, tagBinding = "KM1"),
            CircuitComponent("hl1", ComponentType.PILOT_LAMP_GREEN, "چراغ سبز کارکرد HL1", 270f, 680f),
            CircuitComponent("n1", ComponentType.AC_NEUTRAL, "نول N", 170f, 800f)
        )

        val wires = listOf(
            // سیم‌بندی مدار قدرت: منبع به ورودی کنتاکتور
            WireConnection("wp1", "src_3p", "L1", "km1_pwr", "L1"),
            WireConnection("wp2", "src_3p", "L2", "km1_pwr", "L2"),
            WireConnection("wp3", "src_3p", "L3", "km1_pwr", "L3"),

            // خروجی کنتاکتور به ورودی عنصر حرارتی بی‌متال (1/L1, 3/L2, 5/L3)
            WireConnection("wp4", "km1_pwr", "T1", "ol_pwr", "1"),
            WireConnection("wp5", "km1_pwr", "T2", "ol_pwr", "3"),
            WireConnection("wp6", "km1_pwr", "T3", "ol_pwr", "5"),

            // خروجی بی‌متال (2/T1, 4/T2, 6/T3) به ترمینال‌های U1, V1, W1 موتور
            WireConnection("wp7", "ol_pwr", "2", "m1", "U"),
            WireConnection("wp8", "ol_pwr", "4", "m1", "V"),
            WireConnection("wp9", "ol_pwr", "6", "m1", "W"),

            // سیم‌بندی مدار فرمان
            WireConnection("w1", "l1", "L", "f1", "IN"),
            WireConnection("w2", "f1", "OUT", "e1", "1"),
            WireConnection("w3", "e1", "2", "ol1", "95"),
            WireConnection("w4", "ol1", "96", "sb1", "21"),
            WireConnection("w5", "sb1", "22", "sb2", "13"),
            WireConnection("w6", "sb1", "22", "km1_aux", "13"),
            WireConnection("w7", "sb2", "14", "km1_coil", "A1"),
            WireConnection("w8", "km1_aux", "14", "km1_coil", "A1"),
            WireConnection("w9", "km1_coil", "A1", "hl1", "X1"),
            WireConnection("w10", "km1_coil", "A2", "n1", "N"),
            WireConnection("w11", "hl1", "X2", "n1", "N")
        )

        return CircuitProject(
            id = "template_dol_starter",
            title = "مدار راه‌اندازی مستقیم موتور (DOL) - قدرت و فرمان ۳۸۰ ولت",
            description = "مدار استاندارد بین‌المللی با مسیر قدرت ۳فاز، کنتاکتور، رله حرارتی اضافه بار و خودنگهدار فرمان",
            components = comps,
            wires = wires
        )
    }

    fun createStarDeltaStarter(): CircuitProject {
        // مدار ستاره-مثلث کامل و واقعی مهندسی:
        // ۱. مدار قدرت:
        // - کنتاکتور خط اصلی KM1: منبع L1-L2-L3 به ورودی 1-3-5 KM1
        // - رله حرارتی بی‌متال FR1: ورودی 1-3-5 بی‌متال به خروجی 2-4-6 KM1، و خروجی 2-4-6 بی‌متال به U1, V1, W1 موتور
        // - تخته کلمپ ۶ ترمیناله موتور: U1, V1, W1 و U2, V2, W2
        // - کنتاکتور ستاره KM2: پایانه‌های U2, V2, W2 به ورودی‌های 1, 3, 5 وصل شده و خروجی‌های 2, 4, 6 به یکدیگر اتصال کوتاه (نقطه ستاره) می‌شوند
        // - کنتاکتور مثلث KM3: ورودی‌های 1, 3, 5 به ترتیب به فازهای L1, L2, L3 متصل شده و خروجی‌های 2, 4, 6 به W2, U2, V2 وصل می‌گردند (اتصال U1→W2، V1→U2، W1→V2)
        // ۲. مدار فرمان:
        // مسیر عملکرد: استارت -> KM1 و KM2 (ستاره) روشن -> پس از ۴ ثانیه KM2 قطع و KM3 وصل (مثلث) با اینترلاک دوطرفه NC
        val comps = listOf(
            // مدار قدرت (ستون‌های چپ و میانی قدرت)
            CircuitComponent("src_3p", ComponentType.THREE_PHASE_SOURCE, "منبع ۳فاز (L1-L2-L3)", 40f, 60f),
            CircuitComponent("km1_pwr", ComponentType.MAIN_POWER_CONTACTS_3P, "کنتاکت قدرت KM1 اصلی", 40f, 170f, tagBinding = "KM1"),
            CircuitComponent("ol_pwr", ComponentType.BIMETAL_OVERLOAD, "بی‌متال حرارتی FR1", 40f, 280f, tagBinding = "KM1"),
            CircuitComponent("m1", ComponentType.MOTOR_3PHASE, "الکتروموتور سه‌فاز M1", 40f, 400f),
            CircuitComponent("km3_pwr", ComponentType.MAIN_POWER_CONTACTS_3P, "قدرت مثلث KM3", 140f, 170f, tagBinding = "KM3"),
            CircuitComponent("km2_pwr", ComponentType.MAIN_POWER_CONTACTS_3P, "قدرت ستاره KM2", 140f, 280f, tagBinding = "KM2"),

            // مدار فرمان با چیدمان ستونی فشرده و منظم (شروع از X=250)
            CircuitComponent("l1", ComponentType.AC_PHASE, "فاز فرمان L1", 250f, 60f),
            CircuitComponent("f1", ComponentType.CIRCUIT_BREAKER_MCB, "مینیاتوری F1 (MCB)", 250f, 150f, isClosedOrPressed = true),
            CircuitComponent("ol1", ComponentType.BIMETAL_OVERLOAD, "کنتاکت فرمان بی‌متال (95-96)", 250f, 240f),
            CircuitComponent("sb1", ComponentType.PUSH_BUTTON_NC, "شستی استپ SB1", 250f, 330f),
            CircuitComponent("sb2", ComponentType.PUSH_BUTTON_NO, "شستی استارت SB2", 250f, 420f),
            CircuitComponent("km1_aux", ComponentType.CONTACTOR_NO_AUX, "خودنگهدار KM1 (13-14)", 350f, 420f, tagBinding = "KM1"),

            // بوبین اصلی و تایمر
            CircuitComponent("km1_coil", ComponentType.CONTACTOR_COIL, "کنتاکتور اصلی KM1", 250f, 540f, tagBinding = "KM1"),
            CircuitComponent("kt1_coil", ComponentType.TIMER_ON_DELAY_COIL, "تایمر KT1 (۴ ثانیه)", 350f, 540f, tagBinding = "KT1", delaySeconds = 4f),

            // شاخه ستاره (X=450)
            CircuitComponent("kt1_nc", ComponentType.TIMER_NC_CONTACT, "تیغه بسته تایمر KT1 (15-16)", 450f, 330f, tagBinding = "KT1"),
            CircuitComponent("km3_nc", ComponentType.CONTACTOR_NC_AUX, "اینترلاک بسته مثلث KM3 (21-22)", 450f, 430f, tagBinding = "KM3"),
            CircuitComponent("km2_coil", ComponentType.CONTACTOR_COIL, "کنتاکتور ستاره KM2", 450f, 540f, tagBinding = "KM2", interlockTag = "KM3"),

            // شاخه مثلث (X=550)
            CircuitComponent("kt1_no", ComponentType.TIMER_NO_CONTACT, "تیغه باز تایمر KT1 (15-18)", 550f, 330f, tagBinding = "KT1"),
            CircuitComponent("km2_nc", ComponentType.CONTACTOR_NC_AUX, "اینترلاک بسته ستاره KM2 (21-22)", 550f, 430f, tagBinding = "KM2"),
            CircuitComponent("km3_coil", ComponentType.CONTACTOR_COIL, "کنتاکتور مثلث KM3", 550f, 540f, tagBinding = "KM3", interlockTag = "KM2"),

            // نول خروجی
            CircuitComponent("n1", ComponentType.AC_NEUTRAL, "نول N", 400f, 680f)
        )

        val wires = listOf(
            // --- مدار قدرت ---
            // ۱. اتصال منبع ۳ فاز به ورودی کنتاکتور خط KM1 و کنتاکتور مثلث KM3
            WireConnection("wp1", "src_3p", "L1", "km1_pwr", "L1"),
            WireConnection("wp2", "src_3p", "L2", "km1_pwr", "L2"),
            WireConnection("wp3", "src_3p", "L3", "km1_pwr", "L3"),

            WireConnection("wp_m1", "src_3p", "L1", "km3_pwr", "L1"),
            WireConnection("wp_m2", "src_3p", "L2", "km3_pwr", "L2"),
            WireConnection("wp_m3", "src_3p", "L3", "km3_pwr", "L3"),

            // ۲. خروجی KM1 به ورودی بی‌متال FR1
            WireConnection("wp4", "km1_pwr", "T1", "ol_pwr", "1"),
            WireConnection("wp5", "km1_pwr", "T2", "ol_pwr", "3"),
            WireConnection("wp6", "km1_pwr", "T3", "ol_pwr", "5"),

            // ۳. خروجی بی‌متال به سرهای ابتدایی سیم‌پیچ‌های موتور (U1, V1, W1)
            WireConnection("wp7", "ol_pwr", "2", "m1", "U"),
            WireConnection("wp8", "ol_pwr", "4", "m1", "V"),
            WireConnection("wp9", "ol_pwr", "6", "m1", "W"),

            // ۴. اتصال سرهای انتهایی موتور (W2, U2, V2) به کنتاکتور مثلث KM3
            // اتصال استاندارد مثلث: U1 به W2، V1 به U2، W1 به V2
            WireConnection("wp_delta1", "km3_pwr", "T1", "m1", "W2"),
            WireConnection("wp_delta2", "km3_pwr", "T2", "m1", "U2"),
            WireConnection("wp_delta3", "km3_pwr", "T3", "m1", "V2"),

            // ۵. اتصال سرهای انتهایی به کنتاکتور ستاره KM2
            WireConnection("wp_star1", "m1", "U2", "km2_pwr", "L1"),
            WireConnection("wp_star2", "m1", "V2", "km2_pwr", "L2"),
            WireConnection("wp_star3", "m1", "W2", "km2_pwr", "L3"),

            // ۶. پل ستاره (اتصال کوتاه خروجی‌های کنتاکتور KM2 به هم در نقطه ستاره)
            WireConnection("wp_bridge1", "km2_pwr", "T1", "km2_pwr", "T2"),
            WireConnection("wp_bridge2", "km2_pwr", "T2", "km2_pwr", "T3"),

            // --- مدار فرمان ---
            WireConnection("w1", "l1", "L", "f1", "IN"),
            WireConnection("w2", "f1", "OUT", "ol1", "95"),
            WireConnection("w3", "ol1", "96", "sb1", "21"),
            WireConnection("w4", "sb1", "22", "sb2", "13"),
            WireConnection("w5", "sb1", "22", "km1_aux", "13"),

            // گره پس از استارت و خودنگهدار
            WireConnection("w6", "sb2", "14", "km1_coil", "A1"),
            WireConnection("w7", "km1_aux", "14", "km1_coil", "A1"),
            WireConnection("w8", "km1_coil", "A1", "kt1_coil", "A1"),

            // اتصال فرمان به شاخه‌های ستاره و مثلث
            WireConnection("w9", "km1_coil", "A1", "kt1_nc", "15"),
            WireConnection("w10", "kt1_nc", "16", "km3_nc", "21"),
            WireConnection("w11", "km3_nc", "22", "km2_coil", "A1"),

            WireConnection("w12", "km1_coil", "A1", "kt1_no", "17"),
            WireConnection("w13", "kt1_no", "18", "km2_nc", "21"),
            WireConnection("w14", "km2_nc", "22", "km3_coil", "A1"),

            // اتصال سر برگشت به نول
            WireConnection("w15", "km1_coil", "A2", "n1", "N"),
            WireConnection("w16", "kt1_coil", "A2", "n1", "N"),
            WireConnection("w17", "km2_coil", "A2", "n1", "N"),
            WireConnection("w18", "km3_coil", "A2", "n1", "N")
        )

        return CircuitProject(
            id = "template_star_delta",
            title = "مدار ستاره - مثلث (Star-Delta) با اینترلاک و تایمر TON",
            description = "راه‌اندازی دو مرحله‌ای موتور القایی ۳فاز: استارت با ستاره (KM1+KM2) و سوئیچ خودکار پس از ۴ ثانیه به مثلث (KM1+KM3)",
            components = comps,
            wires = wires
        )
    }

    fun createForwardReverseStarter(): CircuitProject {
        // مدار چپگرد / راستگرد کامل:
        // مدار قدرت با دو کنتاکتور قدرت و جابجایی دو فاز L1 و L3
        // مدار فرمان با اینترلاک الکتریکی متقابل دوطرفه و شستی‌های استارت مجزا
        val comps = listOf(
            // مدار قدرت سمت چپ با چیدمان جمع‌وجور
            CircuitComponent("src_3p", ComponentType.THREE_PHASE_SOURCE, "منبع ۳فاز (L1-L2-L3)", 60f, 60f),
            CircuitComponent("km1_pwr", ComponentType.MAIN_POWER_CONTACTS_3P, "قدرت راستگرد KM1", 40f, 220f, tagBinding = "KM1"),
            CircuitComponent("km2_pwr", ComponentType.MAIN_POWER_CONTACTS_3P, "قدرت چپگرد KM2", 140f, 220f, tagBinding = "KM2"),
            CircuitComponent("m1", ComponentType.MOTOR_3PHASE, "الکتروموتور سه‌فاز M1", 90f, 380f),

            // مدار فرمان با فاصله افقی فشرده و بهینه (X=260 برای فرمان مشترک)
            CircuitComponent("l1", ComponentType.AC_PHASE, "فاز فرمان L1", 260f, 60f),
            CircuitComponent("f1", ComponentType.CIRCUIT_BREAKER_MCB, "مینیاتوری F1 (MCB)", 260f, 150f, isClosedOrPressed = true),
            CircuitComponent("ol1", ComponentType.BIMETAL_OVERLOAD, "بی‌متال FR1 (95-96)", 260f, 240f),
            CircuitComponent("sb1", ComponentType.PUSH_BUTTON_NC, "شستی استپ عمومی SB1", 260f, 330f),

            // شاخه راستگرد (Forward: X=210 و خودنگهدار X=300)
            CircuitComponent("sb_fwd", ComponentType.PUSH_BUTTON_NO, "استارت راستگرد SB2", 210f, 430f),
            CircuitComponent("km1_aux", ComponentType.CONTACTOR_NO_AUX, "خودنگهدار KM1 (13-14)", 300f, 430f, tagBinding = "KM1"),
            CircuitComponent("km2_nc", ComponentType.CONTACTOR_NC_AUX, "اینترلاک بسته چپگرد KM2 (NC)", 255f, 530f, tagBinding = "KM2"),
            CircuitComponent("km1_coil", ComponentType.CONTACTOR_COIL, "بوبین راستگرد KM1", 255f, 640f, tagBinding = "KM1", interlockTag = "KM2"),

            // شاخه چپگرد (Reverse: X=390 و خودنگهدار X=480)
            CircuitComponent("sb_rev", ComponentType.PUSH_BUTTON_NO, "استارت چپگرد SB3", 390f, 430f),
            CircuitComponent("km2_aux", ComponentType.CONTACTOR_NO_AUX, "خودنگهدار KM2 (13-14)", 480f, 430f, tagBinding = "KM2"),
            CircuitComponent("km1_nc", ComponentType.CONTACTOR_NC_AUX, "اینترلاک بسته راستگرد KM1 (NC)", 435f, 530f, tagBinding = "KM1"),
            CircuitComponent("km2_coil", ComponentType.CONTACTOR_COIL, "بوبین چپگرد KM2", 435f, 640f, tagBinding = "KM2", interlockTag = "KM1"),

            // نول
            CircuitComponent("n1", ComponentType.AC_NEUTRAL, "نول N", 345f, 760f)
        )

        val wires = listOf(
            // مدار قدرت: تغذیه هر دو کنتاکتور از منبع ۳ فاز
            WireConnection("wp1", "src_3p", "L1", "km1_pwr", "L1"),
            WireConnection("wp2", "src_3p", "L2", "km1_pwr", "L2"),
            WireConnection("wp3", "src_3p", "L3", "km1_pwr", "L3"),

            WireConnection("wp4", "src_3p", "L1", "km2_pwr", "L1"),
            WireConnection("wp5", "src_3p", "L2", "km2_pwr", "L2"),
            WireConnection("wp6", "src_3p", "L3", "km2_pwr", "L3"),

            // اتصال KM1 (راستگرد: L1->U, L2->V, L3->W)
            WireConnection("wp7", "km1_pwr", "T1", "m1", "U"),
            WireConnection("wp8", "km1_pwr", "T2", "m1", "V"),
            WireConnection("wp9", "km1_pwr", "T3", "m1", "W"),

            // اتصال KM2 (چپگرد: جابجایی فاز ۱ و ۳: L1->W, L2->V, L3->U)
            WireConnection("wp10", "km2_pwr", "T1", "m1", "W"),
            WireConnection("wp11", "km2_pwr", "T2", "m1", "V"),
            WireConnection("wp12", "km2_pwr", "T3", "m1", "U"),

            // مدار فرمان: منبع تا بعد از استپ
            WireConnection("w1", "l1", "L", "f1", "IN"),
            WireConnection("w2", "f1", "OUT", "ol1", "95"),
            WireConnection("w3", "ol1", "96", "sb1", "21"),

            // تغذیه به استارت راستگرد و خودنگهدار آن
            WireConnection("w4", "sb1", "22", "sb_fwd", "13"),
            WireConnection("w5", "sb1", "22", "km1_aux", "13"),
            WireConnection("w6", "sb_fwd", "14", "km2_nc", "21"),
            WireConnection("w7", "km1_aux", "14", "km2_nc", "21"),
            WireConnection("w8", "km2_nc", "22", "km1_coil", "A1"),

            // تغذیه به استارت چپگرد و خودنگهدار آن
            WireConnection("w9", "sb1", "22", "sb_rev", "13"),
            WireConnection("w10", "sb1", "22", "km2_aux", "13"),
            WireConnection("w11", "sb_rev", "14", "km1_nc", "21"),
            WireConnection("w12", "km2_aux", "14", "km1_nc", "21"),
            WireConnection("w13", "km1_nc", "22", "km2_coil", "A1"),

            // اتصال انتهای هر دو کویل به نول
            WireConnection("w14", "km1_coil", "A2", "n1", "N"),
            WireConnection("w15", "km2_coil", "A2", "n1", "N")
        )

        return CircuitProject(
            id = "template_forward_reverse",
            title = "مدار چپگرد / راستگرد (Forward-Reverse) با اینترلاک الکتریکی",
            description = "تغییر جهت چرخش الکتروموتور سه‌فاز با جابجایی دو فاز، اینترلاک متقابل حفاظتی دوطرفه و خودنگهدار مجزا",
            components = comps,
            wires = wires
        )
    }

    fun createDefaultLadderProgram(): List<LadderRung> {
        return listOf(
            LadderRung(
                id = "rung_1",
                rungNumber = 1,
                comment = "روشن و خاموش کردن موتور با شستی استپ/استارت و خودنگهدار واقعی خروجی Q0.0",
                contacts = listOf(
                    LadderElement("c1", LadderElementType.NC_CONTACT, "I0.0", comment = "شستی استپ NC"),
                    LadderElement("c2", LadderElementType.NO_CONTACT, "I0.1", comment = "شستی استارت NO")
                ),
                branchContacts = listOf(
                    LadderElement("c_latch", LadderElementType.NO_CONTACT, "Q0.0", comment = "کنتاکت خودنگهدار Q0.0 موازی با استارت")
                ),
                coils = listOf(
                    LadderElement("q1", LadderElementType.NORMAL_COIL, "Q0.0", comment = "کویل کنتاکتور موتور M1")
                )
            ),
            LadderRung(
                id = "rung_2",
                rungNumber = 2,
                comment = "روشن شدن چراغ سیگنال سبز همزمان با خروجی Q0.0",
                contacts = listOf(
                    LadderElement("c3", LadderElementType.NO_CONTACT, "Q0.0", comment = "کنتاکت باز خروجی")
                ),
                coils = listOf(
                    LadderElement("q2", LadderElementType.NORMAL_COIL, "Q0.1", comment = "چراغ سبز H1")
                )
            ),
            LadderRung(
                id = "rung_3",
                rungNumber = 3,
                comment = "تایمر تاخیر در وصل (TON) به مدت ۵ ثانیه پس از فرمان",
                contacts = listOf(
                    LadderElement("c4", LadderElementType.NO_CONTACT, "Q0.0", comment = "فعال شدن تایمر")
                ),
                coils = listOf(
                    LadderElement("t1", LadderElementType.TIMER_TON, "T1", comment = "تایمر ۵ ثانیه", presetValue = 5)
                )
            )
        )
    }
}
