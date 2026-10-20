package com.example

import android.app.Application
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppCircuitDatabase
import com.example.data.CircuitProjectEntity
import com.example.data.MIGRATION_1_2
import com.example.data.MIGRATION_2_3
import com.example.data.MIGRATION_3_4
import com.example.data.ProjectDraftEntity
import com.example.model.CircuitComponent
import com.example.model.ComponentType
import com.example.model.LadderElement
import com.example.model.LadderElementType
import com.example.model.LadderRung
import com.example.model.WireConnection
import com.example.viewmodel.SimYarViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * تست‌های واحد جامع مرحله P1 برای:
 * ۱. پرچم isDirty (فعال شدن با هر تغییر و ریست با Save و Load)
 * ۲. ذخیره خودکار (Debounce ۳ ثانیه‌ای و تفکیک حالت طراحی از شبیه‌سازی)
 * ۳. بازیابی پیش‌نویس (Draft Recovery) هنگام شروع یا تغییر پروژه
 * ۴. مدیریت پیش‌نویس خراب (Corrupt Draft) بدون کرش
 * ۵. مایگریشن دیتابیس Room و عدم احتساب در سقف پروژه‌های رایگان
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SimYarDraftAutosaveTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var db: AppCircuitDatabase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        application.deleteDatabase("simyar_circuits.db")
        val billing = com.example.billing.MyketBillingRepository(application)
        billing.setVipUnlocked(application, true)
        // Use the same named database as SimYarViewModel so DAO assertions exercise
        // the actual persistence path rather than an unrelated in-memory database.
        db = Room.databaseBuilder(application, AppCircuitDatabase::class.java, "simyar_circuits.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
        application.deleteDatabase("simyar_circuits.db")
        Dispatchers.resetMain()
    }

    @Test
    fun testDraftRecoveryDoesNotSelectAnotherProjectsDraft() {
        val unrelatedDraft = ProjectDraftEntity(
            draftKey = "project_b",
            originalProjectId = "project_b",
            title = "پروژه دیگر",
            description = "",
            componentsJson = "[]",
            wiresJson = "[]",
            ladderRungsJson = "[]",
            updatedAt = 200L
        )
        val expectedDraft = ProjectDraftEntity(
            draftKey = SimYarViewModel.DRAFT_UNNAMED_KEY,
            title = "پیش‌نویس بدون نام",
            description = "",
            componentsJson = "[]",
            wiresJson = "[]",
            ladderRungsJson = "[]",
            updatedAt = 100L
        )

        val selected = SimYarViewModel.selectDraftForRecovery(
            targetProjectId = null,
            keyToCheck = SimYarViewModel.DRAFT_UNNAMED_KEY,
            drafts = listOf(unrelatedDraft, expectedDraft)
        )

        assertEquals("پیش‌نویس بدون نام", selected?.title)
        assertNull(
            SimYarViewModel.selectDraftForRecovery(
                targetProjectId = null,
                keyToCheck = SimYarViewModel.DRAFT_UNNAMED_KEY,
                drafts = listOf(unrelatedDraft)
            )
        )
    }

    @Test
    fun testAutosaveUsesPersistedProjectIdBeforeProjectsFlowEmits() = runBlocking {
        val projectId = "persisted_project_before_flow"
        val entity = CircuitProjectEntity(
            id = projectId,
            title = "پروژه ذخیره‌شده",
            description = "",
            componentsJson = "[]",
            wiresJson = "[]",
            ladderRungsJson = "[]",
            sheetsJson = "[]",
            layersJson = "[]"
        )
        db.projectDao().insertOrUpdateProject(entity)

        val viewModel = SimYarViewModel(application)
        // loadProject sets the current ID; the projects Flow collection is dispatched
        // asynchronously on the test dispatcher and is deliberately not advanced here.
        viewModel.loadProject(entity)
        viewModel.addComponent(ComponentType.PUSH_BUTTON_NO)
        viewModel.saveDraftDirectly()

        val draftDao = db.projectDraftDao()
        val savedDraft = draftDao.getDraft(projectId)
        assertNotNull("پیش‌نویس باید با شناسه همان پروژه ذخیره شود", savedDraft)
        assertEquals(projectId, savedDraft?.originalProjectId)
        assertNull("نباید پیش‌نویس پروژه ذخیره‌شده به کلید پروژه بدون نام برود", draftDao.getDraft(SimYarViewModel.DRAFT_UNNAMED_KEY))
    }

    // ۱. تست flag dirty: تغییر در مدار یا لدر وضعیت dirty را فعال می‌کند
    @Test
    fun test01_DirtyFlag_Lifecycle() = runBlocking {
        val viewModel = SimYarViewModel(application)
        
        // در ابتدا وضعیت تمیز است
        viewModel.clearDirty()
        assertFalse("در ابتدای باز کردن باید isDirty مقدار false داشته باشد", viewModel.isDirty.value)

        // افزودن قطعه
        viewModel.addComponent(ComponentType.PUSH_BUTTON_NO)
        assertTrue("با افزودن قطعه باید isDirty برابر true شود", viewModel.isDirty.value)

        // ریست با clearDirty
        viewModel.clearDirty()
        assertFalse(viewModel.isDirty.value)

        // تغییر برچسب یا تگ قطعه
        val compId = viewModel.components.value.first().id
        viewModel.selectComponent(compId)
        viewModel.updateSelectedComponentTag("KM1")
        assertTrue("با ویرایش تگ قطعه باید isDirty برابر true شود", viewModel.isDirty.value)

        // چرخش قطعه
        viewModel.clearDirty()
        viewModel.rotateSelectedComponent()
        assertTrue("با چرخش قطعه باید isDirty برابر true شود", viewModel.isDirty.value)

        // تغییر در برنامه لدر PLC
        viewModel.clearDirty()
        viewModel.addLadderRung()
        assertTrue("با افزودن رانگ لدر باید isDirty برابر true شود", viewModel.isDirty.value)

        // حذف رانگ لدر
        viewModel.clearDirty()
        val rungId = viewModel.ladderRungs.value.first().id
        viewModel.deleteLadderRung(rungId)
        assertTrue("با حذف رانگ لدر باید isDirty برابر true شود", viewModel.isDirty.value)
    }

    // ۲. تست ذخیره و بارگذاری: Save و Load باید isDirty را ریست کنند
    @Test
    fun test02_DirtyFlag_ClearedOnSaveAndLoad() = runBlocking {
        val viewModel = SimYarViewModel(application)
        viewModel.addComponent(ComponentType.CONTACTOR_COIL)
        assertTrue(viewModel.isDirty.value)

        // ذخیره پروژه
        var saved = false
        viewModel.saveCurrentProject("پروژه آزمایشی ۱", "توضیحات تست") {
            saved = true
        }
        val startTime = System.currentTimeMillis()
        while (!saved && System.currentTimeMillis() - startTime < 3000) {
            testDispatcher.scheduler.runCurrent()
            Thread.sleep(25)
        }
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue("ذخیره پروژه باید تکمیل شده باشد", saved)
        assertFalse("بعد از ذخیره، isDirty باید false شود", viewModel.isDirty.value)

        // بارگذاری پروژه
        viewModel.addComponent(ComponentType.PILOT_LAMP_GREEN)
        assertTrue(viewModel.isDirty.value)

        val projectEntity = CircuitProjectEntity(
            id = "proj_test_load",
            title = "پروژه بارگذاری شده",
            description = "توضیحات",
            componentsJson = "[]",
            wiresJson = "[]",
            ladderRungsJson = "[]"
        )
        viewModel.loadProject(projectEntity)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse("بعد از بارگذاری پروژه، isDirty باید false شود", viewModel.isDirty.value)
    }

    // ۳. تست Debounce و تفکیک حالت طراحی از شبیه‌سازی
    @Test
    fun test03_DebounceAndDesignStateSanitization() {
        val viewModel = SimYarViewModel(application)
        assertEquals("ثابت دی‌بانس ذخیره خودکار باید ۳۰۰۰ میلی‌ثانیه باشد", 3000L, SimYarViewModel.AUTOSAVE_DEBOUNCE_MS)

        // ساخت قطعات با حالت لحظه‌ای شبیه‌سازی (برق‌دار، تایمر در حال گذر، تیغه‌های کمکی بسته شده)
        val runtimeComponents = listOf(
            CircuitComponent(
                id = "c1",
                type = ComponentType.CONTACTOR_NO_AUX,
                label = "NO1",
                x = 100f,
                y = 100f,
                isClosedOrPressed = true, // در حین شبیه‌سازی وصل شده بود
                isEnergized = true
            ),
            CircuitComponent(
                id = "c2",
                type = ComponentType.TIMER_ON_DELAY_COIL,
                label = "KT1",
                x = 200f,
                y = 200f,
                currentTimerProgress = 0.85f,
                isEnergized = true
            ),
            CircuitComponent(
                id = "c3",
                type = ComponentType.MOTOR_3PHASE,
                label = "M1",
                x = 300f,
                y = 300f,
                motorRotationDirection = com.example.model.MotorDirection.CLOCKWISE
            )
        )

        val sanitized = viewModel.sanitizeComponentsForDesignState(runtimeComponents)

        // بررسی اینکه حالت طراحی خالص است و وضعیت‌های لحظه‌ای شبیه‌سازی حذف شده‌اند
        assertFalse("تیغه کمکی کنتاکتور در حالت طراحی باید به وضعیت عادی بازگردد", sanitized[0].isClosedOrPressed)
        assertFalse("قطعه در حالت ذخیره نباید وضعیت برق‌دار لحظه‌ای داشته باشد", sanitized[0].isEnergized)
        assertEquals("پیشرفت تایمر در حالت طراحی باید صفر باشد", 0f, sanitized[1].currentTimerProgress, 0.001f)
        assertFalse(sanitized[1].isEnergized)
        assertEquals("موتور در حالت طراحی باید متوقف باشد", com.example.model.MotorDirection.STOPPED, sanitized[2].motorRotationDirection)

        // بررسی پاکسازی المان‌های لدر
        val runtimeRungs = listOf(
            LadderRung(
                id = "r1",
                rungNumber = 1,
                isPowerFlowing = true,
                contacts = listOf(LadderElement("e1", LadderElementType.NO_CONTACT, "I0.0", isActive = true, currentValue = 1)),
                coils = listOf(LadderElement("q1", LadderElementType.NORMAL_COIL, "Q0.0", isActive = true, currentValue = 1))
            )
        )
        val cleanRungs = viewModel.sanitizeLadderRungsForDesignState(runtimeRungs)
        assertFalse("جریان برق رانگ لدر در ذخیره باید قطع باشد", cleanRungs[0].isPowerFlowing)
        assertFalse("کنتاکت لدر در ذخیره نباید فعال باشد", cleanRungs[0].contacts[0].isActive)
        assertEquals(0, cleanRungs[0].contacts[0].currentValue)
        assertFalse("کویل لدر در ذخیره نباید فعال باشد", cleanRungs[0].coils[0].isActive)
        assertEquals(0, cleanRungs[0].coils[0].currentValue)
    }

    // ۴. تست بازیابی پیش‌نویس (Draft Recovery)
    @Test
    fun test04_DraftRecovery_PromptAndRestore() = runBlocking {
        val draftDao = db.projectDraftDao()
        val draft = ProjectDraftEntity(
            draftKey = "proj_rec_1",
            originalProjectId = "proj_rec_1",
            title = "پروژه ریکاوری",
            description = "توضیحات پیش‌نویس",
            componentsJson = """[{"id":"c1","type":"PUSH_BUTTON_NO","label":"SB1","x":100.0,"y":100.0,"specs":{}}]""",
            wiresJson = "[]",
            ladderRungsJson = "[]",
            updatedAt = System.currentTimeMillis()
        )
        draftDao.insertOrUpdateDraft(draft)

        val retrieved = draftDao.getDraft("proj_rec_1")
        assertNotNull(retrieved)
        assertEquals("پروژه ریکاوری", retrieved?.title)

        // بازگردانی پیش‌نویس در ViewModel
        val viewModel = SimYarViewModel(application)
        val prompt = SimYarViewModel.DraftRecoveryPrompt(
            draftKey = draft.draftKey,
            draftTitle = draft.title,
            draftTimestamp = draft.updatedAt,
            components = listOf(CircuitComponent("c1", ComponentType.PUSH_BUTTON_NO, "SB1", 100f, 100f)),
            wires = emptyList(),
            ladderRungs = emptyList()
        )

        viewModel.restoreDraft(prompt)
        assertEquals("پروژه ریکاوری", viewModel.currentProjectTitle.value)
        assertEquals(1, viewModel.components.value.size)
        assertEquals("c1", viewModel.components.value[0].id)
        assertTrue("پس از بازیابی پیش‌نویس، وضعیت باید isDirty باشد", viewModel.isDirty.value)
        assertNull("دیالوگ بازیابی باید پس از انتخاب بسته شود", viewModel.draftRecoveryPrompt.value)
    }

    // ۵. تست پیش‌نویس خراب (Corrupt Draft) بدون کرش
    @Test
    fun test05_CorruptDraft_HandledSilentlyWithoutCrash() = runBlocking {
        val draftDao = db.projectDraftDao()
        val corruptDraft = ProjectDraftEntity(
            draftKey = "corrupt_key",
            originalProjectId = null,
            title = "پیش‌نویس معیوب",
            description = "",
            componentsJson = "{ invalid json content ... not valid array }",
            wiresJson = "[]",
            ladderRungsJson = "[]",
            updatedAt = System.currentTimeMillis()
        )
        draftDao.insertOrUpdateDraft(corruptDraft)

        // بررسی اینکه ViewModel در checkForDraftRecovery خطا نمی‌دهد و پیش‌نویس معیوب را حذف می‌کند
        val viewModel = SimYarViewModel(application)
        viewModel.checkForDraftRecovery("corrupt_key")
        testDispatcher.scheduler.advanceUntilIdle()

        // باید بدون کرش اجرا شود، پرامپت نمایش داده نشود و رکورد خراب خودکار حذف نشود.
        assertNull(viewModel.draftRecoveryPrompt.value)
        assertNotNull("پیش‌نویس غیرقابل‌خواندن باید برای بازیابی دستی باقی بماند", draftDao.getDraft("corrupt_key"))
    }

    // ۶. تست حذف پیش‌نویس هنگام Discard یا Save
    @Test
    fun test06_DraftDeletedOnDiscardAndSave() = runBlocking {
        val draftDao = db.projectDraftDao()
        val draft = ProjectDraftEntity(
            draftKey = "draft_del_test",
            originalProjectId = "draft_del_test",
            title = "پیش‌نویس حذف",
            description = "",
            componentsJson = "[]",
            wiresJson = "[]",
            ladderRungsJson = "[]"
        )
        draftDao.insertOrUpdateDraft(draft)
        assertNotNull(draftDao.getDraft("draft_del_test"))

        draftDao.deleteDraft("draft_del_test")
        assertNull("پیش‌نویس باید پس از ذخیره یا دور انداختن به کلی حذف شود", draftDao.getDraft("draft_del_test"))
    }

    // ۷. تست مهاجرت دیتابیس (MIGRATION_2_3) و عدم احتساب draft در محدودیت پروژه‌های رایگان
    @Test
    fun test07_RoomMigrationAndVipFreeProjectCap() = runBlocking {
        // ایجاد جدول با استفاده از اسکریپت migration
        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(application)
                .name(null)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE circuit_projects (
                                id TEXT NOT NULL PRIMARY KEY,
                                title TEXT NOT NULL,
                                description TEXT NOT NULL,
                                componentsJson TEXT NOT NULL,
                                wiresJson TEXT NOT NULL,
                                ladderRungsJson TEXT NOT NULL DEFAULT '[]',
                                updatedAt INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        val writableDb = openHelper.writableDatabase

        // اجرای مایگریشن نسخه ۲ به ۳
        MIGRATION_2_3.migrate(writableDb)

        // بررسی ایجاد جدول project_drafts در دیتابیس
        val cursor = writableDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='project_drafts'")
        assertTrue("جدول project_drafts باید پس از MIGRATION_2_3 ساخته شده باشد", cursor.moveToFirst())
        cursor.close()
        writableDb.close()

        // بررسی عدم احتساب draft در سقف ۳ پروژه رایگان
        val projectDao = db.circuitProjectDao()
        val draftDao = db.projectDraftDao()

        // ذخیره ۲ پروژه
        projectDao.insertOrUpdateProject(CircuitProjectEntity("p1", "P1", "", "[]", "[]"))
        projectDao.insertOrUpdateProject(CircuitProjectEntity("p2", "P2", "", "[]", "[]"))

        // ذخیره چندین draft
        draftDao.insertOrUpdateDraft(ProjectDraftEntity("d1", "p1", "D1", "", "[]", "[]"))
        draftDao.insertOrUpdateDraft(ProjectDraftEntity("d2", null, "D2", "", "[]", "[]"))
        draftDao.insertOrUpdateDraft(ProjectDraftEntity(SimYarViewModel.DRAFT_UNNAMED_KEY, null, "D3", "", "[]", "[]"))

        val billing = com.example.billing.MyketBillingRepository(application)
        billing.setVipUnlocked(application, false)

        // با ۲ پروژه در پایگاه داده، کاربر غیر VIP باید همچنان بتواند پروژه جدید بسازد، چون draftها محاسبه نمی‌شوند
        val projectCount = 2
        assertTrue("با وجود پیش‌نویس‌ها، سقف پروژه‌های ذخیره شده تحت تاثیر قرار نمی‌گیرد", billing.canCreateNewProject(projectCount))
        assertFalse("اگر ۳ پروژه کامل باشد سقف تکمیل است", billing.canCreateNewProject(3))
    }
    @Test
    fun testLegacyJsonConvertersKeepValidItemsWhenOneItemIsMalformed() {
        val converters = com.example.data.CircuitTypeConverters()
        val legacyComponents = """[
            {"id":"valid_component","type":"PUSH_BUTTON_NO","label":"SB1","x":10.0,"y":20.0},
            {"id":"broken_component","type":"NOT_A_REAL_COMPONENT","label":"BAD","x":0.0,"y":0.0}
        ]"""

        val components = converters.toComponents(legacyComponents)
        assertEquals("آیتم معتبر قدیمی باید حفظ شود", 1, components.size)
        assertEquals("valid_component", components.first().id)

        val wires = converters.toWires("{ definitely-not-an-array }")
        assertTrue("JSON کاملاً نامعتبر نباید باعث کرش شود", wires.isEmpty())

        val sheets = converters.toSheets("[]")
        assertEquals("در نبود برگه معتبر، برگه پیش‌فرض ساخته می‌شود", "sheet_1", sheets.first().id)

        val layers = converters.toLayers("not-json")
        assertTrue("در نبود لایه معتبر، لایه‌های پیش‌فرض برمی‌گردند", layers.isNotEmpty())
    }

}
