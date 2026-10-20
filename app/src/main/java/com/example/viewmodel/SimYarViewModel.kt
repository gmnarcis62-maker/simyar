package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.billing.BillingRepository
import com.example.billing.MyketBillingRepository
import com.example.canvas.CanvasFitCalculator
import com.example.canvas.CircuitBoundingBox
import com.example.data.AppCircuitDatabase
import com.example.data.CircuitProjectEntity
import com.example.data.CircuitTypeConverters
import com.example.data.MIGRATION_1_2
import com.example.data.MIGRATION_2_3
import com.example.data.MIGRATION_3_4
import com.example.data.ProjectDraftDao
import com.example.data.ProjectDraftEntity
import com.example.model.AcademyLesson
import com.example.model.AcademyRepository
import com.example.model.CircuitComponent
import com.example.model.CircuitComponent.Companion.COMPONENT_HEIGHT
import com.example.model.CircuitComponent.Companion.COMPONENT_WIDTH
import com.example.model.CircuitLayer
import com.example.model.CircuitProject
import com.example.model.CircuitReferenceValidator
import com.example.model.CircuitSheet
import com.example.model.ComponentType
import com.example.model.DefaultCircuitTemplates
import com.example.model.LadderElement
import com.example.model.LadderElementType
import com.example.model.LadderRung
import com.example.model.PlcMemory
import com.example.model.TechnicalSpecs
import com.example.model.WireConnection
import com.example.navigation.AppNavScreen
import com.example.simulation.CircuitSimulationEngine
import com.example.simulation.PlcLadderEngine
import com.example.canvas.WireRouter
import com.example.util.CableListItem
import com.example.util.CircuitNet
import com.example.util.ExportUtils
import com.example.util.NetNumberingManager
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromJsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import com.example.ui.timing.CircularSignalBuffer
import com.example.ui.timing.SignalSourceType
import com.example.ui.timing.TrackableSignal
import java.io.File
import java.util.UUID

private const val PREF_THEME = "simyar_settings"
private const val KEY_DARK_THEME = "is_dark_theme"
private const val PREF_ONBOARDING = "simyar_user_prefs"
private const val KEY_ONBOARDING_SEEN = "onboarding_seen"

data class AlignmentGuide(
    val isVertical: Boolean,
    val position: Float,
    val startCoord: Float,
    val endCoord: Float
)

enum class AlignmentType {
    LEFT,
    CENTER_X,
    RIGHT,
    TOP,
    MIDDLE_Y,
    BOTTOM
}

enum class DistributionType {
    HORIZONTAL,
    VERTICAL
}

data class CircuitStateSnapshot(
    val components: List<CircuitComponent>,
    val wires: List<WireConnection>,
    val sheets: List<CircuitSheet> = emptyList(),
    val layers: List<CircuitLayer> = emptyList()
)

class SimYarViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppCircuitDatabase::class.java,
        "simyar_circuits.db"
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()

    private val projectDao = db.circuitProjectDao()
    private val draftDao: ProjectDraftDao = db.projectDraftDao()
    private val circuitEngine = CircuitSimulationEngine()
    private val plcEngine = PlcLadderEngine()

    val billingRepository: BillingRepository = MyketBillingRepository(application)

    companion object {
        const val DRAFT_UNNAMED_KEY = "_unnamed_project_draft_"
        const val AUTOSAVE_DEBOUNCE_MS = 3000L

        /**
         * Only recover a draft belonging to the project currently being opened.
         * Falling back to the newest unrelated draft can put another project's
         * circuit into the current editor and must therefore be avoided.
         */
        internal fun selectDraftForRecovery(
            targetProjectId: String?,
            keyToCheck: String,
            drafts: List<ProjectDraftEntity>
        ): ProjectDraftEntity? {
            val expectedKey = targetProjectId ?: keyToCheck
            return drafts.firstOrNull { it.draftKey == expectedKey }
        }
    }

    private val _isDirty = MutableStateFlow(false)
    val isDirty: StateFlow<Boolean> = _isDirty.asStateFlow()

    private var autosaveJob: Job? = null
    // Serialize autosave and lifecycle-triggered saves so an older snapshot cannot
    // overwrite a newer draft when both paths run close together.
    private val draftSaveMutex = Mutex()

    // دیالوگ بازیابی پیش‌نویس هنگام شروع
    data class DraftRecoveryPrompt(
        val draftKey: String,
        val draftTitle: String,
        val draftTimestamp: Long,
        val components: List<CircuitComponent>,
        val wires: List<WireConnection>,
        val ladderRungs: List<LadderRung>,
        val sheets: List<CircuitSheet> = emptyList(),
        val layers: List<CircuitLayer> = emptyList(),
        val warning: String? = null
    )

    private val _draftRecoveryPrompt = MutableStateFlow<DraftRecoveryPrompt?>(null)
    val draftRecoveryPrompt: StateFlow<DraftRecoveryPrompt?> = _draftRecoveryPrompt.asStateFlow()

    // دیالوگ تایید سه گزینه‌ای تغییرات ذخیره‌نشده
    data class UnsavedChangesAction(
        val title: String = "ذخیره تغییرات",
        val message: String = "تغییراتی در پروژه اعمال شده که هنوز ذخیره نشده است. آیا مایل به ذخیره تغییرات هستید؟",
        val onSave: () -> Unit,
        val onDiscard: () -> Unit,
        val onCancel: () -> Unit = {}
    )

    private val _pendingUnsavedAction = MutableStateFlow<UnsavedChangesAction?>(null)
    val pendingUnsavedAction: StateFlow<UnsavedChangesAction?> = _pendingUnsavedAction.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _currentScreen = MutableStateFlow(AppNavScreen.SPLASH)
    val currentScreen: StateFlow<AppNavScreen> = _currentScreen.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val undoStack = mutableListOf<CircuitStateSnapshot>()
    private val redoStack = mutableListOf<CircuitStateSnapshot>()
    private val maxHistorySteps = 100

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // ۱. برگه‌ها (Multi-Sheet)
    private val _sheets = MutableStateFlow<List<CircuitSheet>>(listOf(CircuitSheet("sheet_1", "برگه ۱", 0)))
    val sheets: StateFlow<List<CircuitSheet>> = _sheets.asStateFlow()

    private val _currentSheetId = MutableStateFlow("sheet_1")
    val currentSheetId: StateFlow<String> = _currentSheetId.asStateFlow()

    // ۲. لایه‌ها (Layers)
    private val _layers = MutableStateFlow<List<CircuitLayer>>(CircuitTypeConverters.defaultLayers())
    val layers: StateFlow<List<CircuitLayer>> = _layers.asStateFlow()

    // ۳. انتخاب چندتایی (Multi-Select)
    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    private val _selectedComponentIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedComponentIds: StateFlow<Set<String>> = _selectedComponentIds.asStateFlow()

    // کلیپ‌بورد برای کپی/پیست چندتایی
    private var clipboardComponents: List<CircuitComponent> = emptyList()
    private var clipboardInternalWires: List<WireConnection> = emptyList()

    // ۴. راهنماهای هوشمند هم‌ترازی (Smart Guides)
    private val _isSmartGuidesEnabled = MutableStateFlow(true)
    val isSmartGuidesEnabled: StateFlow<Boolean> = _isSmartGuidesEnabled.asStateFlow()

    private val _activeGuides = MutableStateFlow<List<AlignmentGuide>>(emptyList())
    val activeGuides: StateFlow<List<AlignmentGuide>> = _activeGuides.asStateFlow()

    // ۵. جستجوی قطعه روی بوم
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CircuitComponent>>(emptyList())
    val searchResults: StateFlow<List<CircuitComponent>> = _searchResults.asStateFlow()

    private val _highlightedComponentId = MutableStateFlow<String?>(null)
    val highlightedComponentId: StateFlow<String?> = _highlightedComponentId.asStateFlow()

    private val _canvasScale = MutableStateFlow(1f)
    val canvasScale: StateFlow<Float> = _canvasScale.asStateFlow()

    private val _canvasPanX = MutableStateFlow(0f)
    val canvasPanX: StateFlow<Float> = _canvasPanX.asStateFlow()

    private val _canvasPanY = MutableStateFlow(0f)
    val canvasPanY: StateFlow<Float> = _canvasPanY.asStateFlow()

    // ابعاد واقعی بوم و تراکم پیکسلی (Density) برای محاسبه بهینه Fit-to-Screen
    private val _viewportWidth = MutableStateFlow(0f)
    val viewportWidth: StateFlow<Float> = _viewportWidth.asStateFlow()

    private val _viewportHeight = MutableStateFlow(0f)
    val viewportHeight: StateFlow<Float> = _viewportHeight.asStateFlow()

    private val _screenDensity = MutableStateFlow(1f)
    val screenDensity: StateFlow<Float> = _screenDensity.asStateFlow()

    private var pendingFit: Boolean = false

    private val _isSnapToGrid = MutableStateFlow(true)
    val isSnapToGrid: StateFlow<Boolean> = _isSnapToGrid.asStateFlow()

    private val _usePersianDigits = MutableStateFlow(true)
    val usePersianDigits: StateFlow<Boolean> = _usePersianDigits.asStateFlow()

    private var clipboardComponent: CircuitComponent? = null

    private val _components = MutableStateFlow<List<CircuitComponent>>(emptyList())
    val components: StateFlow<List<CircuitComponent>> = _components.asStateFlow()

    private val _wires = MutableStateFlow<List<WireConnection>>(emptyList())
    val wires: StateFlow<List<WireConnection>> = _wires.asStateFlow()

    private val _selectedComponentId = MutableStateFlow<String?>(null)
    val selectedComponentId: StateFlow<String?> = _selectedComponentId.asStateFlow()

    private val _selectedWireId = MutableStateFlow<String?>(null)
    val selectedWireId: StateFlow<String?> = _selectedWireId.asStateFlow()

    private val _showWireNumbers = MutableStateFlow(true)
    val showWireNumbers: StateFlow<Boolean> = _showWireNumbers.asStateFlow()

    private var _cachedNets: List<CircuitNet> = emptyList()
    private val _circuitNets = MutableStateFlow<List<CircuitNet>>(emptyList())
    val circuitNets: StateFlow<List<CircuitNet>> = _circuitNets.asStateFlow()

    private val _isCircuitSimRunning = MutableStateFlow(false)
    val isCircuitSimRunning: StateFlow<Boolean> = _isCircuitSimRunning.asStateFlow()

    private val _simulationSpeedMultiplier = MutableStateFlow(1f)
    val simulationSpeedMultiplier: StateFlow<Float> = _simulationSpeedMultiplier.asStateFlow()

    private val _simulationStatusMessage = MutableStateFlow("مدار آماده تست و شبیه‌سازی")
    val simulationStatusMessage: StateFlow<String> = _simulationStatusMessage.asStateFlow()

    // هشدارهای اعتبارسنجی بارگذاری؛ فقط اطلاع‌رسانی است و داده‌های پروژه را تغییر نمی‌دهد.
    private val _projectLoadValidationWarning = MutableStateFlow<String?>(null)
    val projectLoadValidationWarning: StateFlow<String?> = _projectLoadValidationWarning.asStateFlow()
    private val _projectLoadValidationIssues = MutableStateFlow<List<CircuitReferenceValidator.Issue>>(emptyList())
    val projectLoadValidationIssues: StateFlow<List<CircuitReferenceValidator.Issue>> = _projectLoadValidationIssues.asStateFlow()

    fun dismissProjectLoadValidationWarning() {
        _projectLoadValidationWarning.value = null
        _projectLoadValidationIssues.value = emptyList()
    }

    private val _activeCoils = MutableStateFlow<Set<String>>(emptySet())
    val activeCoils: StateFlow<Set<String>> = _activeCoils.asStateFlow()

    private val _ladderRungs = MutableStateFlow<List<LadderRung>>(emptyList())
    val ladderRungs: StateFlow<List<LadderRung>> = _ladderRungs.asStateFlow()

    private val _plcMemory = MutableStateFlow(PlcMemory())
    val plcMemory: StateFlow<PlcMemory> = _plcMemory.asStateFlow()

    private val _isPlcRunning = MutableStateFlow(false)
    val isPlcRunning: StateFlow<Boolean> = _isPlcRunning.asStateFlow()

    private val _plcScanTimeMs = MutableStateFlow(8L)
    val plcScanTimeMs: StateFlow<Long> = _plcScanTimeMs.asStateFlow()

    private val _savedProjects = MutableStateFlow<List<CircuitProjectEntity>>(emptyList())
    val savedProjects: StateFlow<List<CircuitProjectEntity>> = _savedProjects.asStateFlow()

    private val _currentProjectId = MutableStateFlow(UUID.randomUUID().toString())
    val currentProjectId: StateFlow<String> = _currentProjectId.asStateFlow()

    private val _currentProjectTitle = MutableStateFlow("مدار راه‌اندازی مستقیم موتور (DOL)")
    val currentProjectTitle: StateFlow<String> = _currentProjectTitle.asStateFlow()

    private val _currentProjectDescription = MutableStateFlow("")
    val currentProjectDescription: StateFlow<String> = _currentProjectDescription.asStateFlow()

    private val _designerName = MutableStateFlow("مهندس مهدی رضایی (ردلاین البرز)")
    val designerName: StateFlow<String> = _designerName.asStateFlow()

    private var simulationLoopJob: Job? = null

    // نمودار زمانی سیگنال‌ها (P6 Logic Timing Diagram)
    val timingSignalBuffer = CircularSignalBuffer(maxDurationSeconds = 60f, maxSamplesPerSignal = 1200)
    private val _selectedTimingSignals = MutableStateFlow<List<TrackableSignal>>(emptyList())
    val selectedTimingSignals: StateFlow<List<TrackableSignal>> = _selectedTimingSignals.asStateFlow()

    private val _availableTimingSignals = MutableStateFlow<List<TrackableSignal>>(emptyList())
    val availableTimingSignals: StateFlow<List<TrackableSignal>> = _availableTimingSignals.asStateFlow()

    private val _isTimingRecordingPaused = MutableStateFlow(false)
    val isTimingRecordingPaused: StateFlow<Boolean> = _isTimingRecordingPaused.asStateFlow()

    private val _timingCurrentTimeSeconds = MutableStateFlow(0f)
    val timingCurrentTimeSeconds: StateFlow<Float> = _timingCurrentTimeSeconds.asStateFlow()
    private var timingAccumulatedTimeSeconds = 0f

    init {
        val themePrefs = application.getSharedPreferences(PREF_THEME, Context.MODE_PRIVATE)
        _isDarkTheme.value = themePrefs.getBoolean(KEY_DARK_THEME, true)

        billingRepository.checkVipStatusOnAppStart(application)

        loadTemplateDol()
        _ladderRungs.value = DefaultCircuitTemplates.createDefaultLadderProgram()
        _isDirty.value = false
        initDefaultTimingSignals()

        viewModelScope.launch {
            projectDao.getAllProjects().collect {
                _savedProjects.value = it
            }
        }

        viewModelScope.launch {
            delay(600)
            checkForDraftRecovery()
        }
    }

    fun markDirty() {
        _isDirty.value = true
        scheduleAutosave()
    }

    fun clearDirty() {
        _isDirty.value = false
        autosaveJob?.cancel()
    }

    fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DEBOUNCE_MS)
            if (_isDirty.value) {
                saveDraftDirectly()
            }
        }
    }

    suspend fun saveDraftDirectly() {
        draftSaveMutex.withLock {
            if (!_isDirty.value) {
                return
            }
            // Capture and persist one consistent snapshot while holding the mutex.
            // This prevents a debounced autosave and an onStop save from writing
            // overlapping snapshots out of order.
            val sanitizedComps = sanitizeComponentsForDesignState(_components.value)
            val cleanWires = _wires.value.map { it.copy(isEnergized = false) }
            val cleanRungs = sanitizeLadderRungsForDesignState(_ladderRungs.value)

            val compsJson = json.encodeToString(sanitizedComps)
            val wiresJson = json.encodeToString(cleanWires)
            val ladderJson = json.encodeToString(cleanRungs)
            val sheetsJson = json.encodeToString(_sheets.value)
            val layersJson = json.encodeToString(_layers.value)

            val currentId = _currentProjectId.value
            // The projects StateFlow is updated asynchronously. Query Room directly so an
            // autosave immediately after opening a saved project cannot be misfiled as
            // the unnamed draft just because the UI list has not emitted yet.
            val isKnownProject = currentId.isNotBlank() && projectDao.getProjectById(currentId) != null
            val draftKey = if (isKnownProject) currentId else DRAFT_UNNAMED_KEY

            val draft = ProjectDraftEntity(
                draftKey = draftKey,
                originalProjectId = if (isKnownProject) currentId else null,
                title = _currentProjectTitle.value,
                description = _currentProjectDescription.value,
                componentsJson = compsJson,
                wiresJson = wiresJson,
                ladderRungsJson = ladderJson,
                sheetsJson = sheetsJson,
                layersJson = layersJson,
                updatedAt = System.currentTimeMillis()
            )
            draftDao.insertOrUpdateDraft(draft)
        }
    }

    fun saveDraftImmediate() {
        // Cancel a pending debounce so it cannot race with the lifecycle save.
        autosaveJob?.cancel()
        autosaveJob = null
        viewModelScope.launch {
            // onStop is a best-effort persistence point. Keep this coroutine from being
            // cancelled by a sibling autosave cancellation while the Room write is in flight.
            withContext(NonCancellable) {
                saveDraftDirectly()
            }
        }
    }

    fun sanitizeComponentsForDesignState(components: List<CircuitComponent>): List<CircuitComponent> {
        return components.map { comp ->
            val cleanClosed = when (comp.type) {
                ComponentType.CONTACTOR_NO_AUX,
                ComponentType.CONTACTOR_NC_AUX,
                ComponentType.TIMER_NO_CONTACT,
                ComponentType.TIMER_NC_CONTACT,
                ComponentType.MAIN_POWER_CONTACTS_3P -> false
                ComponentType.PUSH_BUTTON_NO,
                ComponentType.PUSH_BUTTON_NC,
                ComponentType.EMERGENCY_STOP -> if (!comp.isMaintained) false else comp.isClosedOrPressed
                else -> comp.isClosedOrPressed
            }
            comp.copy(
                isClosedOrPressed = cleanClosed,
                isEnergized = false,
                currentTimerProgress = 0f,
                motorRotationDirection = com.example.model.MotorDirection.STOPPED
            )
        }
    }

    fun sanitizeLadderRungsForDesignState(rungs: List<LadderRung>): List<LadderRung> {
        return rungs.map { rung ->
            rung.copy(
                isPowerFlowing = false,
                contacts = rung.contacts.map { it.copy(isActive = false, currentValue = 0) },
                branchContacts = rung.branchContacts.map { it.copy(isActive = false, currentValue = 0) },
                coils = rung.coils.map { it.copy(isActive = false, currentValue = 0) }
            )
        }
    }

    fun checkForDraftRecovery(targetProjectId: String? = null) {
        viewModelScope.launch {
            val keyToCheck = targetProjectId ?: if (_savedProjects.value.any { it.id == _currentProjectId.value && _currentProjectId.value.isNotBlank() }) {
                _currentProjectId.value
            } else {
                DRAFT_UNNAMED_KEY
            }

            // Never fall back to an arbitrary draft: it may belong to a different project.
            val draft = selectDraftForRecovery(
                targetProjectId = targetProjectId,
                keyToCheck = keyToCheck,
                drafts = draftDao.getAllDrafts()
            ) ?: return@launch

            // بازیابی سازگار با نسخه‌های قدیمی: فیلدهای پیش‌فرض مدل‌ها حفظ می‌شوند؛
            // اگر یک آیتم JSON خراب باشد، فقط همان آیتم کنار گذاشته می‌شود و پیش‌نویس حذف نمی‌شود.
            val parseWarnings = mutableListOf<String>()
            fun <T> recoverList(raw: String, fieldLabel: String, decode: (JsonElement) -> T): List<T> {
                val root = try {
                    json.parseToJsonElement(raw)
                } catch (_: Exception) {
                    parseWarnings += fieldLabel
                    return emptyList()
                }
                if (root !is JsonArray) {
                    parseWarnings += fieldLabel
                    return emptyList()
                }
                val recovered = mutableListOf<T>()
                var skipped = false
                root.forEach { item ->
                    try {
                        recovered += decode(item)
                    } catch (_: Exception) {
                        skipped = true
                    }
                }
                if (skipped) parseWarnings += fieldLabel
                return recovered
            }

            val parsedComps = recoverList(draft.componentsJson, "قطعات مدار") {
                json.decodeFromJsonElement<com.example.model.CircuitComponent>(it)
            }
            val parsedWires = recoverList(draft.wiresJson, "سیم‌کشی") {
                json.decodeFromJsonElement<WireConnection>(it)
            }
            val parsedRungs = recoverList(draft.ladderRungsJson, "برنامه لدر") {
                json.decodeFromJsonElement<LadderRung>(it)
            }
            val parsedSheets = recoverList(draft.sheetsJson, "برگه‌ها") {
                json.decodeFromJsonElement<CircuitSheet>(it)
            }
            val parsedLayers = recoverList(draft.layersJson, "لایه‌ها") {
                json.decodeFromJsonElement<CircuitLayer>(it)
            }

            // مرحله ۱۷: اعتبارسنجی ارجاع‌ها پس از رمزگشایی؛ داده خام پیش‌نویس دست‌نخورده می‌ماند.
            // سیم‌هایی که به قطعه موجود ارجاع ندارند، فقط از نسخه بازیابی‌شده کنار گذاشته می‌شوند.
            val componentIds = parsedComps.map { it.id }.toSet()
            val validWires = parsedWires.filter { wire ->
                wire.fromComponentId in componentIds && wire.toComponentId in componentIds
            }
            if (validWires.size != parsedWires.size) {
                parseWarnings += "اتصال‌های بدون قطعهٔ معتبر"
            }

            val sheetIds = parsedSheets.map { it.id }.toSet()
            val layerIds = parsedLayers.map { it.id }.toSet()
            val validComponents = parsedComps.map { component ->
                val sheetId = component.sheetId.takeIf { it in sheetIds }
                val layerId = component.layerId.takeIf { it in layerIds }
                if (sheetId == null || layerId == null) {
                    parseWarnings += "ارجاع برگه یا لایه"
                    component.copy(
                        sheetId = sheetId ?: parsedSheets.firstOrNull()?.id ?: "sheet_1",
                        layerId = layerId ?: parsedLayers.firstOrNull()?.id ?: "layer_control"
                    )
                } else component
            }
            val validWiresWithRefs = validWires.map { wire ->
                val sheetId = wire.sheetId.takeIf { it in sheetIds }
                val layerId = wire.layerId.takeIf { it in layerIds }
                if (sheetId == null || layerId == null) {
                    parseWarnings += "ارجاع برگه یا لایه در سیم‌کشی"
                    wire.copy(
                        sheetId = sheetId ?: parsedSheets.firstOrNull()?.id ?: "sheet_1",
                        layerId = layerId ?: parsedLayers.firstOrNull()?.id ?: "layer_control"
                    )
                } else wire
            }

            // اگر هیچ داده قابل بازیابی وجود ندارد، نسخه اصلی پیش‌نویس را نگه می‌داریم؛
            // پاک‌کردن خودکار می‌تواند امکان بررسی یا بازیابی دستی بعدی را از بین ببرد.
            if (parsedComps.isEmpty() && parsedWires.isEmpty() && parsedRungs.isEmpty()) {
                return@launch
            }

            val targetId = draft.originalProjectId ?: draft.draftKey
            val savedProject = projectDao.getProjectById(targetId) ?: _savedProjects.value.find { it.id == targetId }
            if (savedProject != null && draft.updatedAt <= savedProject.updatedAt) {
                draftDao.deleteDraft(draft.draftKey)
                return@launch
            }

            _draftRecoveryPrompt.value = DraftRecoveryPrompt(
                draftKey = draft.draftKey,
                draftTitle = draft.title,
                draftTimestamp = draft.updatedAt,
                components = validComponents,
                wires = validWiresWithRefs,
                ladderRungs = parsedRungs,
                sheets = parsedSheets,
                layers = parsedLayers,
                warning = parseWarnings.takeIf { it.isNotEmpty() }?.distinct()?.joinToString("، ")
                    ?.let { "بخشی از اطلاعات پیش‌نویس قابل خواندن نبود؛ موارد مشکل‌دار نادیده گرفته شدند: $it. نسخه ذخیره‌شده حفظ شده است." }
            )
        }
    }

    fun restoreDraft(prompt: DraftRecoveryPrompt) {
        stopCircuitSimulation()
        pushHistoryState()
        _components.value = prompt.components
        _wires.value = prompt.wires
        _ladderRungs.value = prompt.ladderRungs
        if (prompt.sheets.isNotEmpty()) {
            _sheets.value = prompt.sheets
            _currentSheetId.value = prompt.sheets.firstOrNull()?.id ?: "sheet_1"
        }
        if (prompt.layers.isNotEmpty()) {
            _layers.value = prompt.layers
        }
        _currentProjectTitle.value = prompt.draftTitle
        if (prompt.draftKey != DRAFT_UNNAMED_KEY) {
            _currentProjectId.value = prompt.draftKey
        }
        _isDirty.value = true
        _draftRecoveryPrompt.value = null
        fitToScreen()
    }

    fun discardDraft(draftKey: String) {
        viewModelScope.launch {
            draftDao.deleteDraft(draftKey)
            _draftRecoveryPrompt.value = null
        }
    }

    fun checkUnsavedChangesBeforeAction(onProceed: () -> Unit) {
        if (_isDirty.value) {
            _pendingUnsavedAction.value = UnsavedChangesAction(
                title = "ذخیره تغییرات",
                message = "تغییراتی در مدار یا برنامه لدر اعمال شده که هنوز ذخیره نشده است. آیا مایل به ذخیره تغییرات هستید؟",
                onSave = {
                    val currentId = _currentProjectId.value
                    val currentTitle = _currentProjectTitle.value
                    val currentDesc = _currentProjectDescription.value
                    saveCurrentProject(currentTitle, currentDesc) {
                        clearDirty()
                        viewModelScope.launch {
                            draftDao.deleteDraft(currentId)
                            draftDao.deleteDraft(DRAFT_UNNAMED_KEY)
                        }
                        _pendingUnsavedAction.value = null
                        onProceed()
                    }
                },
                onDiscard = {
                    val currentId = _currentProjectId.value
                    viewModelScope.launch {
                        draftDao.deleteDraft(currentId)
                        draftDao.deleteDraft(DRAFT_UNNAMED_KEY)
                    }
                    clearDirty()
                    _pendingUnsavedAction.value = null
                    onProceed()
                },
                onCancel = {
                    _pendingUnsavedAction.value = null
                }
            )
        } else {
            onProceed()
        }
    }

    fun dismissUnsavedChangesDialog() {
        _pendingUnsavedAction.value = null
    }

    fun setDesignerName(name: String) {
        _designerName.value = name
    }

    fun toggleDarkTheme() {
        val next = !_isDarkTheme.value
        setDarkTheme(next)
    }

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        val prefs = getApplication<Application>().getSharedPreferences(PREF_THEME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_THEME, isDark).apply()
    }

    fun pushHistoryState() {
        if (undoStack.size >= maxHistorySteps) {
            undoStack.removeAt(0)
        }
        undoStack.add(CircuitStateSnapshot(_components.value, _wires.value, _sheets.value, _layers.value))
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
        markDirty()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val currentState = CircuitStateSnapshot(_components.value, _wires.value, _sheets.value, _layers.value)
            redoStack.add(currentState)
            val previous = undoStack.removeAt(undoStack.lastIndex)
            _components.value = previous.components
            _wires.value = previous.wires
            if (previous.sheets.isNotEmpty()) {
                _sheets.value = previous.sheets
                if (_sheets.value.none { it.id == _currentSheetId.value }) {
                    _currentSheetId.value = _sheets.value.firstOrNull()?.id ?: "sheet_1"
                }
            }
            if (previous.layers.isNotEmpty()) {
                _layers.value = previous.layers
            }
            _selectedComponentId.value = null
            _selectedWireId.value = null
            _selectedComponentIds.value = emptySet()
            recomputeNetsAndWireNumbers()
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = true
            markDirty()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val currentState = CircuitStateSnapshot(_components.value, _wires.value, _sheets.value, _layers.value)
            undoStack.add(currentState)
            val next = redoStack.removeAt(redoStack.lastIndex)
            _components.value = next.components
            _wires.value = next.wires
            if (next.sheets.isNotEmpty()) {
                _sheets.value = next.sheets
                if (_sheets.value.none { it.id == _currentSheetId.value }) {
                    _currentSheetId.value = _sheets.value.firstOrNull()?.id ?: "sheet_1"
                }
            }
            if (next.layers.isNotEmpty()) {
                _layers.value = next.layers
            }
            _selectedComponentId.value = null
            _selectedWireId.value = null
            _selectedComponentIds.value = emptySet()
            recomputeNetsAndWireNumbers()
            _canUndo.value = true
            _canRedo.value = redoStack.isNotEmpty()
            markDirty()
        }
    }

    fun updateTransform(scale: Float, panX: Float, panY: Float) {
        _canvasScale.value = scale
        _canvasPanX.value = panX
        _canvasPanY.value = panY
    }

    fun setViewportSize(width: Float, height: Float, density: Float = 1f) {
        val widthChanged = kotlin.math.abs(_viewportWidth.value - width) > 1f
        val heightChanged = kotlin.math.abs(_viewportHeight.value - height) > 1f
        _viewportWidth.value = width
        _viewportHeight.value = height
        if (density > 0.1f) {
            _screenDensity.value = density
        }

        // اگر انطباق در صف بود یا اندازه صفحه برای اولین بار دریافت شد یا چرخش روی داد
        if (pendingFit || (width > 0f && height > 0f && (widthChanged || heightChanged) && _canvasScale.value == 1f && _canvasPanX.value == 0f && _canvasPanY.value == 0f)) {
            pendingFit = false
            fitToScreen()
        }
    }

    fun requestFitOnNextLayout() {
        pendingFit = true
        if (_viewportWidth.value > 0f && _viewportHeight.value > 0f) {
            pendingFit = false
            fitToScreen()
        }
    }

    fun resetCanvasView() {
        val density = _screenDensity.value
        _canvasScale.value = density.coerceIn(1f, 3f)
        _canvasPanX.value = 0f
        _canvasPanY.value = 0f
    }

    fun zoomIn() {
        val current = _canvasScale.value
        val density = _screenDensity.value
        val maxLimit = CanvasFitCalculator.MAX_SCALE * density
        _canvasScale.value = (current * 1.25f).coerceAtMost(maxLimit)
    }

    fun zoomOut() {
        val current = _canvasScale.value
        val density = _screenDensity.value
        val minLimit = CanvasFitCalculator.MIN_SCALE * density
        _canvasScale.value = (current * 0.8f).coerceAtLeast(minLimit)
    }

    fun fitToScreen(viewportWidth: Float = 0f, viewportHeight: Float = 0f) {
        val actualW = if (viewportWidth > 0f) viewportWidth else _viewportWidth.value
        val actualH = if (viewportHeight > 0f) viewportHeight else _viewportHeight.value
        val density = _screenDensity.value

        if (actualW <= 0f || actualH <= 0f) {
            // ابعاد بوم هنوز مشخص نیست، در صف قرار می‌گیرد تا با اولین Layout اجرا شود
            pendingFit = true
            return
        }

        val comps = _components.value
        if (comps.isEmpty()) {
            resetCanvasView()
            return
        }

        val minX = comps.minOf { it.x }
        val maxX = comps.maxOf { it.x + COMPONENT_WIDTH }
        val minY = comps.minOf { it.y }
        val maxY = comps.maxOf { it.y + COMPONENT_HEIGHT }

        val bbox = CircuitBoundingBox(
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY
        )

        val result = CanvasFitCalculator.calculateFit(
            bbox = bbox,
            viewportWidth = actualW,
            viewportHeight = actualH,
            density = density,
            padding = CanvasFitCalculator.DEFAULT_PADDING
        )

        _canvasScale.value = result.scale
        _canvasPanX.value = result.panX
        _canvasPanY.value = result.panY
    }

    fun toggleSnapToGrid() {
        _isSnapToGrid.value = !_isSnapToGrid.value
    }

    fun togglePersianDigits() {
        _usePersianDigits.value = !_usePersianDigits.value
    }

    fun setSimulationSpeed(speed: Float) {
        _simulationSpeedMultiplier.value = speed
    }

    fun finishSplash() {
        val prefs = getApplication<Application>().getSharedPreferences(PREF_ONBOARDING, Context.MODE_PRIVATE)
        val hasSeen = prefs.getBoolean(KEY_ONBOARDING_SEEN, false)
        if (!hasSeen) {
            _currentScreen.value = AppNavScreen.ONBOARDING
        } else {
            _currentScreen.value = AppNavScreen.CIRCUIT_DESIGNER
        }
    }

    fun finishOnboarding() {
        val prefs = getApplication<Application>().getSharedPreferences(PREF_ONBOARDING, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply()
        _currentScreen.value = AppNavScreen.CIRCUIT_DESIGNER
    }

    fun navigateTo(screen: AppNavScreen) {
        _currentScreen.value = screen
    }

    fun loadTemplateDol() {
        stopCircuitSimulation()
        pushHistoryState()
        val template = DefaultCircuitTemplates.createDirectOnlineStarter()
        _components.value = template.components
        _wires.value = template.wires
        _currentProjectTitle.value = template.title
        _currentProjectDescription.value = template.description
        _currentProjectId.value = template.id
        _selectedComponentId.value = null
        _selectedWireId.value = null
        recomputeNetsAndWireNumbers()
        fitToScreen()
    }

    fun loadTemplateStarDelta() {
        stopCircuitSimulation()
        pushHistoryState()
        val template = DefaultCircuitTemplates.createStarDeltaStarter()
        _components.value = template.components
        _wires.value = template.wires
        _currentProjectTitle.value = template.title
        _currentProjectDescription.value = template.description
        _currentProjectId.value = template.id
        _selectedComponentId.value = null
        _selectedWireId.value = null
        recomputeNetsAndWireNumbers()
        fitToScreen()
    }

    fun loadTemplateReverse() {
        stopCircuitSimulation()
        pushHistoryState()
        val template = DefaultCircuitTemplates.createForwardReverseStarter()
        _components.value = template.components
        _wires.value = template.wires
        _currentProjectTitle.value = template.title
        _currentProjectDescription.value = template.description
        _currentProjectId.value = template.id
        _selectedComponentId.value = null
        _selectedWireId.value = null
        recomputeNetsAndWireNumbers()
        fitToScreen()
    }

    fun loadProjectFromLesson(lesson: AcademyLesson) {
        checkUnsavedChangesBeforeAction {
            stopCircuitSimulation()
            stopPlcSimulation()
            undoStack.clear()
            redoStack.clear()
            _canUndo.value = false
            _canRedo.value = false
            _components.value = lesson.initialProject.components
            _wires.value = lesson.initialProject.wires
            _currentProjectTitle.value = lesson.initialProject.title
            _currentProjectDescription.value = lesson.initialProject.description
            _currentProjectId.value = lesson.initialProject.id
            _selectedComponentId.value = null
            _selectedWireId.value = null
            recomputeNetsAndWireNumbers()
            clearDirty()
            _currentScreen.value = AppNavScreen.CIRCUIT_DESIGNER
            fitToScreen()
        }
    }

    fun clearCircuit() {
        checkUnsavedChangesBeforeAction {
            stopCircuitSimulation()
            pushHistoryState()
            _components.value = emptyList()
            _wires.value = emptyList()
            _selectedComponentId.value = null
            _selectedWireId.value = null
            _currentProjectId.value = UUID.randomUUID().toString()
            _currentProjectTitle.value = "مدار جدید"
            _currentProjectDescription.value = ""
            resetCanvasView()
        }
    }

    fun addComponent(type: ComponentType, customSpecs: TechnicalSpecs? = null) {
        pushHistoryState()
        val prefix = type.defaultPrefix
        val count = _components.value.count { it.type.defaultPrefix == prefix } + 1
        val label = "$prefix$count"

        val selectedComp = _components.value.find { it.id == _selectedComponentId.value }
        val selectedCoilTag = selectedComp?.let {
            if (it.type in listOf(ComponentType.CONTACTOR_COIL, ComponentType.TIMER_ON_DELAY_COIL, ComponentType.TIMER_OFF_DELAY_COIL)) it.tagBinding else null
        }

        val tag = when (type) {
            ComponentType.CONTACTOR_COIL -> {
                val existingNumbers = _components.value
                    .filter { it.type == ComponentType.CONTACTOR_COIL && it.tagBinding.startsWith("KM") }
                    .mapNotNull { it.tagBinding.removePrefix("KM").toIntOrNull() }
                val nextNum = if (existingNumbers.isEmpty()) 1 else (existingNumbers.maxOrNull() ?: 0) + 1
                "KM$nextNum"
            }
            ComponentType.TIMER_ON_DELAY_COIL, ComponentType.TIMER_OFF_DELAY_COIL -> {
                val existingNumbers = _components.value
                    .filter { it.type in listOf(ComponentType.TIMER_ON_DELAY_COIL, ComponentType.TIMER_OFF_DELAY_COIL) && it.tagBinding.startsWith("KT") }
                    .mapNotNull { it.tagBinding.removePrefix("KT").toIntOrNull() }
                val nextNum = if (existingNumbers.isEmpty()) 1 else (existingNumbers.maxOrNull() ?: 0) + 1
                "KT$nextNum"
            }
            ComponentType.CONTACTOR_NO_AUX, ComponentType.CONTACTOR_NC_AUX, ComponentType.MAIN_POWER_CONTACTS_3P -> {
                selectedCoilTag ?: _components.value.lastOrNull { it.type == ComponentType.CONTACTOR_COIL }?.tagBinding ?: "KM1"
            }
            ComponentType.TIMER_NO_CONTACT, ComponentType.TIMER_NC_CONTACT -> {
                selectedCoilTag ?: _components.value.lastOrNull { it.type in listOf(ComponentType.TIMER_ON_DELAY_COIL, ComponentType.TIMER_OFF_DELAY_COIL) }?.tagBinding ?: "KT1"
            }
            else -> ""
        }

        val specs = customSpecs ?: AcademyRepository.getDefaultSpecsForType(type)
        val newComp = CircuitComponent(
            id = "c_${UUID.randomUUID().toString().take(6)}",
            type = type,
            label = label,
            x = 120f + ((_components.value.size % 4) * 85f),
            y = 120f + ((_components.value.size / 4) * 95f),
            tagBinding = tag,
            specs = specs,
            sheetId = _currentSheetId.value,
            layerId = "layer_control"
        )
        _components.value = _components.value + newComp
        _selectedComponentId.value = newComp.id
        _selectedWireId.value = null
    }

    fun copySelectedComponent() {
        val sel = _components.value.find { it.id == _selectedComponentId.value }
        clipboardComponent = sel
    }

    fun pasteComponent() {
        val clip = clipboardComponent ?: return
        pushHistoryState()
        val newComp = clip.copy(
            id = "c_${UUID.randomUUID().toString().take(6)}",
            x = clip.x + 35f,
            y = clip.y + 35f
        )
        _components.value = _components.value + newComp
        _selectedComponentId.value = newComp.id
        _selectedWireId.value = null
    }

    fun rotateSelectedComponent() {
        val selId = _selectedComponentId.value ?: return
        pushHistoryState()
        _components.value = _components.value.map {
            if (it.id == selId) {
                val nextRot = (it.rotation + 90f) % 360f
                it.copy(rotation = nextRot)
            } else it
        }
    }

    fun toggleLockSelectedComponent() {
        val selId = _selectedComponentId.value ?: return
        _components.value = _components.value.map {
            if (it.id == selId) it.copy(isLocked = !it.isLocked) else it
        }
        markDirty()
    }

    fun selectComponent(id: String?) {
        _selectedComponentId.value = id
        if (id != null) _selectedWireId.value = null
    }

    fun selectWire(id: String?) {
        _selectedWireId.value = id
        if (id != null) _selectedComponentId.value = null
    }

    fun moveComponent(id: String, newX: Float, newY: Float) {
        _components.value = _components.value.map {
            if (it.id == id && !it.isLocked) it.copy(x = newX, y = newY) else it
        }
    }

    fun recordMoveFinished() {
        rerouteWiresForConnectedComponents()
        pushHistoryState()
    }

    fun rerouteWiresForConnectedComponents() {
        val comps = _components.value
        val compMap = comps.associateBy { it.id }
        val currentWiresList = _wires.value

        _wires.value = currentWiresList.map { wire ->
            val c1 = compMap[wire.fromComponentId]
            val c2 = compMap[wire.toComponentId]
            val t1 = c1?.getTerminals()?.find { it.id == wire.fromTerminalId }
            val t2 = c2?.getTerminals()?.find { it.id == wire.toTerminalId }

            if (c1 != null && c2 != null && t1 != null && t2 != null) {
                val start = Offset(c1.x + t1.relX, c1.y + t1.relY)
                val end = Offset(c2.x + t2.relX, c2.y + t2.relY)
                val newWp = WireRouter.routeWire(
                    start = start,
                    end = end,
                    fromCompId = wire.fromComponentId,
                    toCompId = wire.toComponentId,
                    components = comps,
                    existingWires = currentWiresList,
                    wireToIgnoreId = wire.id
                )
                var dist = 0.0
                for (i in 0 until newWp.size - 1) {
                    val dx = (newWp[i + 1].x - newWp[i].x).toDouble()
                    val dy = (newWp[i + 1].y - newWp[i].y).toDouble()
                    dist += kotlin.math.sqrt(dx * dx + dy * dy)
                }
                wire.copy(waypoints = newWp, wireLengthMm = dist * 2.0)
            } else {
                wire
            }
        }
    }

    fun updateSelectedComponentTag(tag: String) {
        val selectedId = _selectedComponentId.value ?: return
        _components.value = _components.value.map {
            if (it.id == selectedId) it.copy(tagBinding = tag) else it
        }
        markDirty()
    }

    fun updateSelectedComponentPlcTag(plcTag: String) {
        val selectedId = _selectedComponentId.value ?: return
        _components.value = _components.value.map {
            if (it.id == selectedId) it.copy(plcTagBinding = plcTag) else it
        }
        markDirty()
    }

    fun updateSelectedComponentTimerDelay(seconds: Float) {
        val selectedId = _selectedComponentId.value ?: return
        _components.value = _components.value.map {
            if (it.id == selectedId) it.copy(delaySeconds = seconds) else it
        }
        markDirty()
    }

    fun deleteSelectedComponent() {
        val id = _selectedComponentId.value ?: return
        pushHistoryState()
        _components.value = _components.value.filter { it.id != id }
        _wires.value = _wires.value.filter { it.fromComponentId != id && it.toComponentId != id }
        _selectedComponentId.value = null
        recomputeNetsAndWireNumbers()
        markDirty()
    }

    fun deleteWire(wireId: String) {
        pushHistoryState()
        _wires.value = _wires.value.filter { it.id != wireId }
        if (_selectedWireId.value == wireId) {
            _selectedWireId.value = null
        }
        recomputeNetsAndWireNumbers()
        markDirty()
    }

    fun deleteSelectedWire() {
        val wireId = _selectedWireId.value ?: return
        deleteWire(wireId)
    }

    fun addWire(fromComp: String, fromTerm: String, toComp: String, toTerm: String) {
        val exists = _wires.value.any {
            (it.fromComponentId == fromComp && it.fromTerminalId == fromTerm && it.toComponentId == toComp && it.toTerminalId == toTerm) ||
                    (it.fromComponentId == toComp && it.fromTerminalId == toTerm && it.toComponentId == fromComp && it.toTerminalId == fromTerm)
        }
        if (!exists) {
            val comps = _components.value
            val c1 = comps.find { it.id == fromComp }
            val c2 = comps.find { it.id == toComp }

            // سیم نمی‌تواند از برگه عبور کند
            if (c1 != null && c2 != null && c1.sheetId != c2.sheetId) {
                return
            }

            pushHistoryState()

            val t1 = c1?.getTerminals()?.find { it.id == fromTerm }
            val t2 = c2?.getTerminals()?.find { it.id == toTerm }

            val waypoints = if (c1 != null && c2 != null && t1 != null && t2 != null) {
                val start = Offset(c1.x + t1.relX, c1.y + t1.relY)
                val end = Offset(c2.x + t2.relX, c2.y + t2.relY)
                WireRouter.routeWire(
                    start = start,
                    end = end,
                    fromCompId = fromComp,
                    toCompId = toComp,
                    components = comps,
                    existingWires = _wires.value
                )
            } else {
                emptyList()
            }

            var dist = 0.0
            for (i in 0 until waypoints.size - 1) {
                val dx = (waypoints[i + 1].x - waypoints[i].x).toDouble()
                val dy = (waypoints[i + 1].y - waypoints[i].y).toDouble()
                dist += kotlin.math.sqrt(dx * dx + dy * dy)
            }

            val targetSheetId = c1?.sheetId ?: _currentSheetId.value
            val targetLayerId = c1?.layerId ?: "layer_control"

            val wire = WireConnection(
                id = "w_${UUID.randomUUID().toString().take(6)}",
                fromComponentId = fromComp,
                fromTerminalId = fromTerm,
                toComponentId = toComp,
                toTerminalId = toTerm,
                waypoints = waypoints,
                wireLengthMm = dist * 2.0,
                sheetId = targetSheetId,
                layerId = targetLayerId
            )
            _wires.value = _wires.value + wire
            recomputeNetsAndWireNumbers()
            markDirty()
        }
    }

    // ==========================================
    // بخش برگه‌ها (Multi-Sheet Management)
    // ==========================================

    fun selectSheet(id: String) {
        if (_sheets.value.any { it.id == id }) {
            _currentSheetId.value = id
            _selectedComponentId.value = null
            _selectedWireId.value = null
            _selectedComponentIds.value = emptySet()
            _activeGuides.value = emptyList()
        }
    }

    fun addSheet(name: String? = null): CircuitSheet {
        pushHistoryState()
        val nextOrder = _sheets.value.size
        val nextNum = nextOrder + 1
        val sheetName = name?.ifBlank { "برگه $nextNum" } ?: "برگه $nextNum"
        val newSheet = CircuitSheet(
            id = "sheet_${UUID.randomUUID().toString().take(6)}",
            name = sheetName,
            order = nextOrder
        )
        _sheets.value = _sheets.value + newSheet
        _currentSheetId.value = newSheet.id
        _selectedComponentId.value = null
        _selectedWireId.value = null
        _selectedComponentIds.value = emptySet()
        markDirty()
        return newSheet
    }

    fun renameSheet(id: String, newName: String) {
        if (newName.isBlank()) return
        pushHistoryState()
        _sheets.value = _sheets.value.map {
            if (it.id == id) it.copy(name = newName.trim()) else it
        }
        markDirty()
    }

    fun duplicateSheet(id: String) {
        val target = _sheets.value.find { it.id == id } ?: return
        pushHistoryState()
        val newSheetId = "sheet_${UUID.randomUUID().toString().take(6)}"
        val newSheet = CircuitSheet(
            id = newSheetId,
            name = "${target.name} (کپی)",
            order = _sheets.value.size
        )
        val originalComps = _components.value.filter { it.sheetId == id }
        val compIdMap = mutableMapOf<String, String>()
        val clonedComps = originalComps.map { comp ->
            val newId = "c_${UUID.randomUUID().toString().take(6)}"
            compIdMap[comp.id] = newId
            comp.copy(id = newId, sheetId = newSheetId)
        }
        val originalWires = _wires.value.filter { it.sheetId == id }
        val clonedWires = originalWires.mapNotNull { wire ->
            val newFrom = compIdMap[wire.fromComponentId]
            val newTo = compIdMap[wire.toComponentId]
            if (newFrom != null && newTo != null) {
                wire.copy(
                    id = "w_${UUID.randomUUID().toString().take(6)}",
                    sheetId = newSheetId,
                    fromComponentId = newFrom,
                    toComponentId = newTo
                )
            } else null
        }

        _sheets.value = _sheets.value + newSheet
        _components.value = _components.value + clonedComps
        _wires.value = _wires.value + clonedWires
        _currentSheetId.value = newSheetId
        recomputeNetsAndWireNumbers()
        markDirty()
    }

    fun deleteSheet(id: String): Boolean {
        if (_sheets.value.size <= 1) return false
        pushHistoryState()
        _sheets.value = _sheets.value.filter { it.id != id }
        _components.value = _components.value.filter { it.sheetId != id }
        _wires.value = _wires.value.filter { it.sheetId != id }
        if (_currentSheetId.value == id) {
            _currentSheetId.value = _sheets.value.first().id
        }
        _selectedComponentId.value = null
        _selectedWireId.value = null
        _selectedComponentIds.value = emptySet()
        recomputeNetsAndWireNumbers()
        markDirty()
        return true
    }

    // ==========================================
    // بخش لایه‌ها (Layers Management)
    // ==========================================

    fun toggleLayerVisibility(layerId: String) {
        _layers.value = _layers.value.map {
            if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it
        }
        markDirty()
    }

    fun toggleLayerLock(layerId: String) {
        _layers.value = _layers.value.map {
            if (it.id == layerId) it.copy(isLocked = !it.isLocked) else it
        }
        markDirty()
    }

    fun setComponentLayer(componentId: String, layerId: String) {
        _components.value = _components.value.map {
            if (it.id == componentId) it.copy(layerId = layerId) else it
        }
        markDirty()
    }

    // ==========================================
    // بخش انتخاب چندتایی (Multi-Select & Batch Operations)
    // ==========================================

    fun toggleMultiSelectMode() {
        val nextMode = !_isMultiSelectMode.value
        _isMultiSelectMode.value = nextMode
        if (nextMode) {
            val currentSel = _selectedComponentId.value
            if (currentSel != null) {
                _selectedComponentIds.value = setOf(currentSel)
            }
        } else {
            val first = _selectedComponentIds.value.firstOrNull()
            _selectedComponentId.value = first
            _selectedComponentIds.value = emptySet()
        }
    }

    fun setMultiSelectMode(enabled: Boolean) {
        _isMultiSelectMode.value = enabled
        if (!enabled) {
            _selectedComponentIds.value = emptySet()
        }
    }

    fun toggleComponentSelectionInMultiSelect(id: String) {
        val currentSet = _selectedComponentIds.value.toMutableSet()
        if (currentSet.contains(id)) {
            currentSet.remove(id)
        } else {
            currentSet.add(id)
        }
        _selectedComponentIds.value = currentSet
        _selectedComponentId.value = currentSet.firstOrNull()
        _selectedWireId.value = null
    }

    fun setMultiSelection(ids: Set<String>) {
        _selectedComponentIds.value = ids
        _selectedComponentId.value = ids.firstOrNull()
        if (ids.isNotEmpty()) _selectedWireId.value = null
    }

    fun addComponentsToSelection(ids: Set<String>) {
        _selectedComponentIds.value = _selectedComponentIds.value + ids
        _selectedComponentId.value = _selectedComponentIds.value.firstOrNull()
        if (ids.isNotEmpty()) _selectedWireId.value = null
    }

    fun selectAllInCurrentSheet() {
        val activeSheetComps = _components.value.filter { it.sheetId == _currentSheetId.value }
        _selectedComponentIds.value = activeSheetComps.map { it.id }.toSet()
        _selectedComponentId.value = activeSheetComps.firstOrNull()?.id
        _isMultiSelectMode.value = true
    }

    fun clearSelection() {
        _selectedComponentId.value = null
        _selectedWireId.value = null
        _selectedComponentIds.value = emptySet()
        _highlightedComponentId.value = null
    }

    fun moveSelectedComponentsBy(deltaX: Float, deltaY: Float) {
        if (deltaX == 0f && deltaY == 0f) return
        val targetIds = if (_isMultiSelectMode.value && _selectedComponentIds.value.isNotEmpty()) {
            _selectedComponentIds.value
        } else {
            _selectedComponentId.value?.let { setOf(it) } ?: emptySet()
        }
        if (targetIds.isEmpty()) return

        val lockedLayerIds = _layers.value.filter { it.isLocked }.map { it.id }.toSet()

        _components.value = _components.value.map { comp ->
            if (comp.id in targetIds && !comp.isLocked && comp.layerId !in lockedLayerIds) {
                comp.copy(
                    x = (comp.x + deltaX).coerceAtLeast(10f),
                    y = (comp.y + deltaY).coerceAtLeast(10f)
                )
            } else comp
        }
    }

    fun recordBatchMoveFinished() {
        rerouteWiresForConnectedComponents()
        pushHistoryState()
        clearActiveGuides()
    }

    fun copySelectedComponents() {
        val targetIds = if (_isMultiSelectMode.value && _selectedComponentIds.value.isNotEmpty()) {
            _selectedComponentIds.value
        } else {
            _selectedComponentId.value?.let { setOf(it) } ?: emptySet()
        }
        if (targetIds.isEmpty()) return

        val compsToCopy = _components.value.filter { it.id in targetIds }
        clipboardComponents = compsToCopy
        clipboardComponent = compsToCopy.firstOrNull()
        clipboardInternalWires = _wires.value.filter {
            it.fromComponentId in targetIds && it.toComponentId in targetIds
        }
    }

    fun pasteComponents() {
        if (clipboardComponents.isEmpty()) {
            pasteComponent()
            return
        }
        pushHistoryState()
        val compIdMap = mutableMapOf<String, String>()
        val newComps = clipboardComponents.map { orig ->
            val newId = "c_${UUID.randomUUID().toString().take(6)}"
            compIdMap[orig.id] = newId
            orig.copy(
                id = newId,
                sheetId = _currentSheetId.value,
                x = orig.x + 40f,
                y = orig.y + 40f
            )
        }
        val newWires = clipboardInternalWires.mapNotNull { origWire ->
            val newFrom = compIdMap[origWire.fromComponentId]
            val newTo = compIdMap[origWire.toComponentId]
            if (newFrom != null && newTo != null) {
                val newWaypoints = origWire.waypoints.map { wp ->
                    com.example.model.WirePoint(wp.x + 40f, wp.y + 40f)
                }
                origWire.copy(
                    id = "w_${UUID.randomUUID().toString().take(6)}",
                    sheetId = _currentSheetId.value,
                    fromComponentId = newFrom,
                    toComponentId = newTo,
                    waypoints = newWaypoints
                )
            } else null
        }

        _components.value = _components.value + newComps
        _wires.value = _wires.value + newWires
        val newIds = newComps.map { it.id }.toSet()
        _selectedComponentIds.value = newIds
        _selectedComponentId.value = newComps.firstOrNull()?.id
        _selectedWireId.value = null
        recomputeNetsAndWireNumbers()
        markDirty()
    }

    fun deleteSelectedBatch() {
        val targetIds = if (_isMultiSelectMode.value && _selectedComponentIds.value.isNotEmpty()) {
            _selectedComponentIds.value
        } else {
            _selectedComponentId.value?.let { setOf(it) } ?: emptySet()
        }
        if (targetIds.isEmpty()) return

        pushHistoryState()
        _components.value = _components.value.filter { it.id !in targetIds }
        _wires.value = _wires.value.filter { it.fromComponentId !in targetIds && it.toComponentId !in targetIds }
        _selectedComponentIds.value = emptySet()
        _selectedComponentId.value = null
        _selectedWireId.value = null
        recomputeNetsAndWireNumbers()
        markDirty()
    }

    fun rotateSelectedComponents() {
        val targetIds = if (_isMultiSelectMode.value && _selectedComponentIds.value.isNotEmpty()) {
            _selectedComponentIds.value
        } else {
            _selectedComponentId.value?.let { setOf(it) } ?: emptySet()
        }
        if (targetIds.isEmpty()) return

        pushHistoryState()
        _components.value = _components.value.map { comp ->
            if (comp.id in targetIds) {
                val nextRot = (comp.rotation + 90f) % 360f
                comp.copy(rotation = nextRot)
            } else comp
        }
        rerouteWiresForConnectedComponents()
        markDirty()
    }

    fun toggleLockSelectedComponents() {
        val targetIds = if (_isMultiSelectMode.value && _selectedComponentIds.value.isNotEmpty()) {
            _selectedComponentIds.value
        } else {
            _selectedComponentId.value?.let { setOf(it) } ?: emptySet()
        }
        if (targetIds.isEmpty()) return

        pushHistoryState()
        val anyUnlocked = _components.value.any { it.id in targetIds && !it.isLocked }
        val targetLock = anyUnlocked
        _components.value = _components.value.map { comp ->
            if (comp.id in targetIds) comp.copy(isLocked = targetLock) else comp
        }
        markDirty()
    }

    // ==========================================
    // بخش هم‌ترازی و توزیع (Alignment & Distribution)
    // ==========================================

    fun alignSelectedComponents(type: AlignmentType) {
        val targetIds = _selectedComponentIds.value
        val selected = _components.value.filter { it.id in targetIds }
        if (selected.size < 2) return

        pushHistoryState()
        val updatedMap = mutableMapOf<String, Pair<Float, Float>>()

        when (type) {
            AlignmentType.LEFT -> {
                val minX = selected.minOf { it.x }
                selected.forEach { updatedMap[it.id] = Pair(minX, it.y) }
            }
            AlignmentType.CENTER_X -> {
                val minX = selected.minOf { it.x }
                val maxX = selected.maxOf { it.x + COMPONENT_WIDTH }
                val midX = (minX + maxX) / 2f
                selected.forEach { updatedMap[it.id] = Pair(midX - COMPONENT_WIDTH / 2f, it.y) }
            }
            AlignmentType.RIGHT -> {
                val maxRight = selected.maxOf { it.x + COMPONENT_WIDTH }
                selected.forEach { updatedMap[it.id] = Pair(maxRight - COMPONENT_WIDTH, it.y) }
            }
            AlignmentType.TOP -> {
                val minY = selected.minOf { it.y }
                selected.forEach { updatedMap[it.id] = Pair(it.x, minY) }
            }
            AlignmentType.MIDDLE_Y -> {
                val minY = selected.minOf { it.y }
                val maxY = selected.maxOf { it.y + COMPONENT_HEIGHT }
                val midY = (minY + maxY) / 2f
                selected.forEach { updatedMap[it.id] = Pair(it.x, midY - COMPONENT_HEIGHT / 2f) }
            }
            AlignmentType.BOTTOM -> {
                val maxBottom = selected.maxOf { it.y + COMPONENT_HEIGHT }
                selected.forEach { updatedMap[it.id] = Pair(it.x, maxBottom - COMPONENT_HEIGHT) }
            }
        }

        _components.value = _components.value.map { comp ->
            val newCoords = updatedMap[comp.id]
            if (newCoords != null) comp.copy(x = newCoords.first, y = newCoords.second) else comp
        }
        rerouteWiresForConnectedComponents()
        markDirty()
    }

    fun distributeSelectedComponents(type: DistributionType) {
        val targetIds = _selectedComponentIds.value
        val selected = _components.value.filter { it.id in targetIds }
        if (selected.size < 3) return

        pushHistoryState()
        val updatedMap = mutableMapOf<String, Pair<Float, Float>>()

        when (type) {
            DistributionType.HORIZONTAL -> {
                val sorted = selected.sortedBy { it.x }
                val firstX = sorted.first().x
                val lastX = sorted.last().x
                val step = (lastX - firstX) / (sorted.size - 1)
                sorted.forEachIndexed { index, comp ->
                    val newX = firstX + index * step
                    updatedMap[comp.id] = Pair(newX, comp.y)
                }
            }
            DistributionType.VERTICAL -> {
                val sorted = selected.sortedBy { it.y }
                val firstY = sorted.first().y
                val lastY = sorted.last().y
                val step = (lastY - firstY) / (sorted.size - 1)
                sorted.forEachIndexed { index, comp ->
                    val newY = firstY + index * step
                    updatedMap[comp.id] = Pair(comp.x, newY)
                }
            }
        }

        _components.value = _components.value.map { comp ->
            val newCoords = updatedMap[comp.id]
            if (newCoords != null) comp.copy(x = newCoords.first, y = newCoords.second) else comp
        }
        rerouteWiresForConnectedComponents()
        markDirty()
    }

    // ==========================================
    // بخش راهنماهای هوشمند (Smart Guides & Snap)
    // ==========================================

    fun toggleSmartGuides() {
        _isSmartGuidesEnabled.value = !_isSmartGuidesEnabled.value
    }

    fun setSmartGuidesEnabled(enabled: Boolean) {
        _isSmartGuidesEnabled.value = enabled
    }

    fun clearActiveGuides() {
        _activeGuides.value = emptyList()
    }

    fun snapWithSmartGuides(
        draggingCompId: String,
        rawX: Float,
        rawY: Float
    ): Triple<Float, Float, List<AlignmentGuide>> {
        if (!_isSmartGuidesEnabled.value) {
            return Triple(rawX, rawY, emptyList())
        }

        val others = _components.value.filter {
            it.sheetId == _currentSheetId.value && it.id != draggingCompId && it.id !in _selectedComponentIds.value
        }
        if (others.isEmpty()) {
            _activeGuides.value = emptyList()
            return Triple(rawX, rawY, emptyList())
        }

        val snapThreshold = 10f
        var snappedX = rawX
        var snappedY = rawY
        val guides = mutableListOf<AlignmentGuide>()

        val dragLeft = rawX
        val dragCenterX = rawX + COMPONENT_WIDTH / 2f
        val dragRight = rawX + COMPONENT_WIDTH

        val dragTop = rawY
        val dragCenterY = rawY + COMPONENT_HEIGHT / 2f
        val dragBottom = rawY + COMPONENT_HEIGHT

        var minXDiff = snapThreshold + 1f
        var bestSnapX: Float? = null
        var bestGuideX: AlignmentGuide? = null

        for (other in others) {
            val oLeft = other.x
            val oCenter = other.x + COMPONENT_WIDTH / 2f
            val oRight = other.x + COMPONENT_WIDTH

            val minY = kotlin.math.min(rawY, other.y) - 40f
            val maxY = kotlin.math.max(rawY + COMPONENT_HEIGHT, other.y + COMPONENT_HEIGHT) + 40f

            val diffLL = kotlin.math.abs(dragLeft - oLeft)
            if (diffLL < minXDiff) {
                minXDiff = diffLL
                bestSnapX = oLeft
                bestGuideX = AlignmentGuide(true, oLeft, minY, maxY)
            }
            val diffCC = kotlin.math.abs(dragCenterX - oCenter)
            if (diffCC < minXDiff) {
                minXDiff = diffCC
                bestSnapX = oCenter - COMPONENT_WIDTH / 2f
                bestGuideX = AlignmentGuide(true, oCenter, minY, maxY)
            }
            val diffRR = kotlin.math.abs(dragRight - oRight)
            if (diffRR < minXDiff) {
                minXDiff = diffRR
                bestSnapX = oRight - COMPONENT_WIDTH
                bestGuideX = AlignmentGuide(true, oRight, minY, maxY)
            }
        }
        if (bestSnapX != null && bestGuideX != null) {
            snappedX = bestSnapX
            guides.add(bestGuideX)
        }

        var minYDiff = snapThreshold + 1f
        var bestSnapY: Float? = null
        var bestGuideY: AlignmentGuide? = null

        for (other in others) {
            val oTop = other.y
            val oCenter = other.y + COMPONENT_HEIGHT / 2f
            val oBottom = other.y + COMPONENT_HEIGHT

            val minX = kotlin.math.min(rawX, other.x) - 40f
            val maxX = kotlin.math.max(rawX + COMPONENT_WIDTH, other.x + COMPONENT_WIDTH) + 40f

            val diffTT = kotlin.math.abs(dragTop - oTop)
            if (diffTT < minYDiff) {
                minYDiff = diffTT
                bestSnapY = oTop
                bestGuideY = AlignmentGuide(false, oTop, minX, maxX)
            }
            val diffCC = kotlin.math.abs(dragCenterY - oCenter)
            if (diffCC < minYDiff) {
                minYDiff = diffCC
                bestSnapY = oCenter - COMPONENT_HEIGHT / 2f
                bestGuideY = AlignmentGuide(false, oCenter, minX, maxX)
            }
            val diffBB = kotlin.math.abs(dragBottom - oBottom)
            if (diffBB < minYDiff) {
                minYDiff = diffBB
                bestSnapY = oBottom - COMPONENT_HEIGHT
                bestGuideY = AlignmentGuide(false, oBottom, minX, maxX)
            }
        }
        if (bestSnapY != null && bestGuideY != null) {
            snappedY = bestSnapY
            guides.add(bestGuideY)
        }

        _activeGuides.value = guides
        return Triple(snappedX, snappedY, guides)
    }

    // ==========================================
    // بخش جستجوی قطعات روی بوم
    // ==========================================

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        val q = query.trim().lowercase()
        val matches = _components.value.filter { comp ->
            comp.label.lowercase().contains(q) ||
            comp.tagBinding.lowercase().contains(q) ||
            comp.plcTagBinding.lowercase().contains(q) ||
            comp.type.persianName.lowercase().contains(q) ||
            comp.type.name.lowercase().contains(q) ||
            comp.specs.manufacturerModel.lowercase().contains(q)
        }
        _searchResults.value = matches
    }

    fun focusOnComponent(componentId: String) {
        val comp = _components.value.find { it.id == componentId } ?: return
        if (comp.sheetId != _currentSheetId.value) {
            _currentSheetId.value = comp.sheetId
        }
        _selectedComponentId.value = comp.id
        _highlightedComponentId.value = comp.id
        _selectedWireId.value = null

        val compCenterX = comp.x + COMPONENT_WIDTH / 2f
        val compCenterY = comp.y + COMPONENT_HEIGHT / 2f
        val targetScale = 1.2f.coerceIn(0.2f, 4.0f)

        _canvasScale.value = targetScale
        val vpW = if (_viewportWidth.value > 0f) _viewportWidth.value else 800f
        val vpH = if (_viewportHeight.value > 0f) _viewportHeight.value else 1200f
        _canvasPanX.value = (vpW / 2f) - (compCenterX * targetScale)
        _canvasPanY.value = (vpH / 2f) - (compCenterY * targetScale)
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _highlightedComponentId.value = null
    }

    fun recomputeNetsAndWireNumbers() {
        val updatedNets = NetNumberingManager.computeNetsWithStableNumbering(_wires.value, _cachedNets)
        _cachedNets = updatedNets
        _circuitNets.value = updatedNets
        _wires.value = NetNumberingManager.assignNumbersToWires(_wires.value, updatedNets)
    }

    fun toggleShowWireNumbers() {
        _showWireNumbers.value = !_showWireNumbers.value
    }

    fun updateWireNumber(wireId: String, newNumber: String) {
        _wires.value = _wires.value.map {
            if (it.id == wireId) it.copy(wireNumber = newNumber) else it
        }
        markDirty()
    }

    fun updateWireColor(wireId: String, colorHex: String, presetId: String, standardClause: String) {
        _wires.value = _wires.value.map {
            if (it.id == wireId) it.copy(
                wireColorHex = colorHex,
                wireColorPreset = presetId,
                colorStandardRef = standardClause
            ) else it
        }
        markDirty()
    }

    fun updateWireCrossSection(wireId: String, crossSectionMm2: Double) {
        _wires.value = _wires.value.map {
            if (it.id == wireId) it.copy(crossSectionMm2 = crossSectionMm2) else it
        }
        markDirty()
    }

    fun updateWireLength(wireId: String, lengthMm: Double) {
        _wires.value = _wires.value.map {
            if (it.id == wireId) it.copy(wireLengthMm = lengthMm) else it
        }
        markDirty()
    }

    fun rerouteSingleWire(wireId: String) {
        val comps = _components.value
        val compMap = comps.associateBy { it.id }
        _wires.value = _wires.value.map { wire ->
            if (wire.id == wireId) {
                val c1 = compMap[wire.fromComponentId]
                val c2 = compMap[wire.toComponentId]
                val t1 = c1?.getTerminals()?.find { it.id == wire.fromTerminalId }
                val t2 = c2?.getTerminals()?.find { it.id == wire.toTerminalId }
                if (c1 != null && c2 != null && t1 != null && t2 != null) {
                    val start = Offset(c1.x + t1.relX, c1.y + t1.relY)
                    val end = Offset(c2.x + t2.relX, c2.y + t2.relY)
                    val newWp = WireRouter.routeWire(start, end, wire.fromComponentId, wire.toComponentId, comps, _wires.value, wire.id)
                    var dist = 0.0
                    for (i in 0 until newWp.size - 1) {
                        val dx = (newWp[i + 1].x - newWp[i].x).toDouble()
                        val dy = (newWp[i + 1].y - newWp[i].y).toDouble()
                        dist += kotlin.math.sqrt(dx * dx + dy * dy)
                    }
                    wire.copy(waypoints = newWp, wireLengthMm = dist * 2.0)
                } else wire
            } else wire
        }
        markDirty()
    }

    fun getCableList(): List<CableListItem> {
        return NetNumberingManager.buildCableList(_wires.value, _components.value)
    }

    fun exportCableListCsv(): String {
        return NetNumberingManager.exportCableListToCsv(getCableList())
    }

    fun toggleComponentInteractiveState(compId: String) {
        _components.value = _components.value.map { comp ->
            if (comp.id == compId) {
                if (comp.type == ComponentType.BIMETAL_OVERLOAD) {
                    comp.copy(isBimetalTripped = !comp.isBimetalTripped)
                } else {
                    val toggled = !comp.isClosedOrPressed
                    if (comp.plcTagBinding.startsWith("I")) {
                        _plcMemory.value = _plcMemory.value.setInput(comp.plcTagBinding, toggled)
                    }
                    comp.copy(isClosedOrPressed = toggled)
                }
            } else comp
        }
    }

    fun setComponentPressed(compId: String, isPressed: Boolean) {
        _components.value = _components.value.map { comp ->
            if (comp.id == compId) {
                if (comp.plcTagBinding.startsWith("I")) {
                    _plcMemory.value = _plcMemory.value.setInput(comp.plcTagBinding, isPressed)
                }
                comp.copy(isClosedOrPressed = isPressed)
            } else comp
        }
    }

    fun toggleComponentMaintained(compId: String) {
        pushHistoryState()
        _components.value = _components.value.map {
            if (it.id == compId) it.copy(isMaintained = !it.isMaintained) else it
        }
    }

    fun toggleBimetalTrip(compId: String) {
        pushHistoryState()
        _components.value = _components.value.map {
            if (it.id == compId) it.copy(isBimetalTripped = !it.isBimetalTripped) else it
        }
    }

    fun updateSelectedComponentInterlockTag(tag: String) {
        val selectedId = _selectedComponentId.value ?: return
        _components.value = _components.value.map {
            if (it.id == selectedId) it.copy(interlockTag = tag) else it
        }
    }

    fun toggleCircuitSimulation() {
        if (_isCircuitSimRunning.value) {
            stopCircuitSimulation()
        } else {
            startCircuitSimulation()
        }
    }

    fun executeSingleStepSimulation() {
        val result = circuitEngine.stepSimulation(_components.value, _wires.value, deltaSeconds = 0.05f)
        _components.value = result.updatedComponents
        _wires.value = result.updatedWires
        _activeCoils.value = result.energizedCoilTags
        _simulationStatusMessage.value = result.logMessage
    }

    private fun startCircuitSimulation() {
        _isCircuitSimRunning.value = true
        simulationLoopJob?.cancel()
        simulationLoopJob = viewModelScope.launch {
            var lastNanoTime = System.nanoTime()
            while (isActive && (_isCircuitSimRunning.value || _isPlcRunning.value)) {
                val now = System.nanoTime()
                val realDelta = ((now - lastNanoTime) / 1_000_000_000f).coerceIn(0.01f, 0.2f)
                lastNanoTime = now
                val scaledDelta = realDelta * _simulationSpeedMultiplier.value

                // ۱. پردازش لدر PLC
                if (_isPlcRunning.value) {
                    val plcStart = System.currentTimeMillis()
                    val plcResult = plcEngine.executeCycle(_ladderRungs.value, _plcMemory.value, cycleDeltaSeconds = scaledDelta)
                    _ladderRungs.value = plcResult.first
                    _plcMemory.value = plcResult.second
                    _plcScanTimeMs.value = (System.currentTimeMillis() - plcStart).coerceAtLeast(1L)

                    // همگام‌سازی دوطرفه خروجی Q با کویل‌ها و لامپ‌های مدار
                    val qOutputs = plcResult.second.outputs
                    _components.value = _components.value.map { comp ->
                        if (comp.plcTagBinding.startsWith("Q")) {
                            val isQActive = qOutputs[comp.plcTagBinding] ?: false
                            if (comp.type in listOf(ComponentType.CONTACTOR_COIL, ComponentType.PILOT_LAMP_GREEN, ComponentType.PILOT_LAMP_RED, ComponentType.PILOT_LAMP_YELLOW)) {
                                comp.copy(isEnergized = isQActive)
                            } else comp
                        } else comp
                    }
                }

                // ۲. پردازش مدار الکتریکی
                if (_isCircuitSimRunning.value) {
                    val result = circuitEngine.stepSimulation(_components.value, _wires.value, deltaSeconds = scaledDelta)
                    _components.value = result.updatedComponents
                    _wires.value = result.updatedWires
                    _activeCoils.value = result.energizedCoilTags
                    _simulationStatusMessage.value = result.logMessage
                }

                // ۳. ثبت تاریخچه زمانی سیگنال‌ها (Timing Diagram Recording)
                if (!_isTimingRecordingPaused.value && (_isCircuitSimRunning.value || _isPlcRunning.value)) {
                    timingAccumulatedTimeSeconds += scaledDelta
                    _timingCurrentTimeSeconds.value = timingAccumulatedTimeSeconds

                    val currentPlcMem = _plcMemory.value
                    val curComps = _components.value
                    val curActiveCoils = _activeCoils.value

                    val snapshot = mutableMapOf<String, Boolean>()
                    _selectedTimingSignals.value.forEach { sig ->
                        val value = when (sig.type) {
                            SignalSourceType.PLC_INPUT -> currentPlcMem.inputs[sig.addressOrTag] ?: false
                            SignalSourceType.PLC_OUTPUT -> currentPlcMem.outputs[sig.addressOrTag] ?: false
                            SignalSourceType.PLC_MARKER -> currentPlcMem.markers[sig.addressOrTag] ?: false
                            SignalSourceType.PLC_TIMER_DONE -> currentPlcMem.timerDone[sig.addressOrTag] ?: false
                            SignalSourceType.PLC_COUNTER_DONE -> currentPlcMem.counterDone[sig.addressOrTag] ?: false
                            SignalSourceType.CIRCUIT_COIL -> {
                                sig.addressOrTag in curActiveCoils || curComps.find { it.id == sig.id.removePrefix("comp_") }?.isEnergized == true
                            }
                            SignalSourceType.CIRCUIT_COMPONENT -> {
                                curComps.find { it.id == sig.id.removePrefix("comp_") }?.isClosedOrPressed ?: false
                            }
                        }
                        snapshot[sig.id] = value
                    }
                    timingSignalBuffer.recordBatch(timingAccumulatedTimeSeconds, snapshot)
                }

                val sleepDelay = (40 / _simulationSpeedMultiplier.value).toLong().coerceIn(15L, 80L)
                delay(sleepDelay)
            }
        }
    }

    private fun stopCircuitSimulation() {
        _isCircuitSimRunning.value = false
        if (!_isPlcRunning.value) {
            simulationLoopJob?.cancel()
        }
        _simulationStatusMessage.value = "شبیه‌سازی متوقف شد"
        _wires.value = _wires.value.map { it.copy(isEnergized = false) }
        _activeCoils.value = emptySet()
        _components.value = _components.value.map { comp ->
            val resetClosed = when (comp.type) {
                ComponentType.CONTACTOR_NO_AUX,
                ComponentType.CONTACTOR_NC_AUX,
                ComponentType.TIMER_NO_CONTACT,
                ComponentType.TIMER_NC_CONTACT,
                ComponentType.MAIN_POWER_CONTACTS_3P -> false
                ComponentType.PUSH_BUTTON_NO,
                ComponentType.PUSH_BUTTON_NC,
                ComponentType.EMERGENCY_STOP -> if (!comp.isMaintained) false else comp.isClosedOrPressed
                else -> comp.isClosedOrPressed
            }
            comp.copy(
                isClosedOrPressed = resetClosed,
                isEnergized = false,
                currentTimerProgress = 0f,
                motorRotationDirection = com.example.model.MotorDirection.STOPPED
            )
        }
    }

    fun stopPlcSimulation() {
        _isPlcRunning.value = false
        if (!_isCircuitSimRunning.value) {
            simulationLoopJob?.cancel()
        }
    }

    fun togglePlcSimulation() {
        if (_isPlcRunning.value) {
            _isPlcRunning.value = false
            if (!_isCircuitSimRunning.value) {
                simulationLoopJob?.cancel()
            }
        } else {
            _isPlcRunning.value = true
            if (simulationLoopJob == null || simulationLoopJob?.isActive != true) {
                startCircuitSimulation()
            }
        }
    }

    fun togglePlcInput(address: String) {
        _plcMemory.value = _plcMemory.value.toggleInput(address)
    }

    fun addLadderRung() {
        val nextNum = (_ladderRungs.value.maxOfOrNull { it.rungNumber } ?: 0) + 1
        val newRung = LadderRung(
            id = "rung_$nextNum",
            rungNumber = nextNum,
            comment = "خط فرمان جدید شماره $nextNum",
            contacts = listOf(
                LadderElement("c_${UUID.randomUUID().toString().take(5)}", LadderElementType.NO_CONTACT, "I0.0", comment = "کنتاکت باز")
            ),
            coils = listOf(
                LadderElement("q_${UUID.randomUUID().toString().take(5)}", LadderElementType.NORMAL_COIL, "Q0.0", comment = "کویل خروجی")
            )
        )
        _ladderRungs.value = _ladderRungs.value + newRung
        markDirty()
    }

    fun deleteLadderRung(rungId: String) {
        _ladderRungs.value = _ladderRungs.value.filter { it.id != rungId }
        markDirty()
    }

    fun moveLadderRungUp(rungId: String) {
        val index = _ladderRungs.value.indexOfFirst { it.id == rungId }
        if (index > 0) {
            val list = _ladderRungs.value.toMutableList()
            val item = list.removeAt(index)
            list.add(index - 1, item)
            _ladderRungs.value = list.mapIndexed { idx, rung -> rung.copy(rungNumber = idx + 1) }
            markDirty()
        }
    }

    fun moveLadderRungDown(rungId: String) {
        val index = _ladderRungs.value.indexOfFirst { it.id == rungId }
        if (index >= 0 && index < _ladderRungs.value.size - 1) {
            val list = _ladderRungs.value.toMutableList()
            val item = list.removeAt(index)
            list.add(index + 1, item)
            _ladderRungs.value = list.mapIndexed { idx, rung -> rung.copy(rungNumber = idx + 1) }
            markDirty()
        }
    }

    fun addElementToLadderRung(rungId: String, type: LadderElementType, address: String, isContact: Boolean, preset: Int = 5) {
        _ladderRungs.value = _ladderRungs.value.map { rung ->
            if (rung.id == rungId) {
                val element = LadderElement(
                    id = "elem_${UUID.randomUUID().toString().take(5)}",
                    type = type,
                    address = address,
                    comment = type.persianTitle,
                    presetValue = preset
                )
                if (isContact) {
                    rung.copy(contacts = rung.contacts + element)
                } else {
                    rung.copy(coils = rung.coils + element)
                }
            } else rung
        }
        markDirty()
    }

    fun updateLadderElement(rungId: String, elementId: String, newAddress: String, newType: LadderElementType, newPreset: Int) {
        _ladderRungs.value = _ladderRungs.value.map { rung ->
            if (rung.id == rungId) {
                val updatedContacts = rung.contacts.map {
                    if (it.id == elementId) it.copy(address = newAddress, type = newType, presetValue = newPreset) else it
                }
                val updatedBranches = rung.branchContacts.map {
                    if (it.id == elementId) it.copy(address = newAddress, type = newType, presetValue = newPreset) else it
                }
                val updatedCoils = rung.coils.map {
                    if (it.id == elementId) it.copy(address = newAddress, type = newType, presetValue = newPreset) else it
                }
                rung.copy(contacts = updatedContacts, branchContacts = updatedBranches, coils = updatedCoils)
            } else rung
        }
        markDirty()
    }

    fun deleteLadderElement(rungId: String, elementId: String) {
        _ladderRungs.value = _ladderRungs.value.map { rung ->
            if (rung.id == rungId) {
                rung.copy(
                    contacts = rung.contacts.filter { it.id != elementId },
                    branchContacts = rung.branchContacts.filter { it.id != elementId },
                    coils = rung.coils.filter { it.id != elementId }
                )
            } else rung
        }
        markDirty()
    }

    fun addBranchToRung(rungId: String, element: LadderElement) {
        _ladderRungs.value = _ladderRungs.value.map { rung ->
            if (rung.id == rungId) {
                rung.copy(branchContacts = rung.branchContacts + element)
            } else rung
        }
        markDirty()
    }

    fun placeSymbolOnRung(
        rungId: String,
        type: LadderElementType,
        isBranch: Boolean = false,
        suggestedAddress: String? = null,
        preset: Int = 5
    ) {
        val defaultAddress = suggestedAddress ?: when (type) {
            LadderElementType.NO_CONTACT, LadderElementType.NC_CONTACT, LadderElementType.EDGE_POS, LadderElementType.EDGE_NEG -> {
                val existing = _ladderRungs.value.find { it.id == rungId }?.let { it.contacts + it.branchContacts } ?: emptyList()
                val nextIdx = existing.size
                if (nextIdx == 0) "I0.0" else "I0.$nextIdx"
            }
            LadderElementType.NORMAL_COIL, LadderElementType.SET_COIL, LadderElementType.RESET_COIL -> {
                val existing = _ladderRungs.value.find { it.id == rungId }?.coils ?: emptyList()
                val nextIdx = existing.size
                "Q0.$nextIdx"
            }
            LadderElementType.TIMER_TON, LadderElementType.TIMER_TOF -> {
                val existingCount = _ladderRungs.value.flatMap { it.coils }.count { it.type == LadderElementType.TIMER_TON || it.type == LadderElementType.TIMER_TOF }
                "T${existingCount + 1}"
            }
            LadderElementType.COUNTER_CTU, LadderElementType.COUNTER_CTD, LadderElementType.COUNTER_CTUD -> {
                val existingCount = _ladderRungs.value.flatMap { it.coils }.count { it.type in listOf(LadderElementType.COUNTER_CTU, LadderElementType.COUNTER_CTD, LadderElementType.COUNTER_CTUD) }
                "C${existingCount + 1}"
            }
            else -> "MW0"
        }
        val isContact = type == LadderElementType.NO_CONTACT || type == LadderElementType.NC_CONTACT || type == LadderElementType.EDGE_POS || type == LadderElementType.EDGE_NEG
        if (isBranch) {
            val element = LadderElement(
                id = "elem_${UUID.randomUUID().toString().take(5)}",
                type = type,
                address = defaultAddress,
                comment = type.persianTitle,
                presetValue = preset
            )
            addBranchToRung(rungId, element)
        } else {
            addElementToLadderRung(rungId, type, defaultAddress, isContact, preset)
        }
    }

    fun saveCurrentProject(title: String, desc: String, onFinished: (() -> Unit)? = null) {
        viewModelScope.launch {
            val sanitizedComps = _components.value.map { comp ->
                val cleanClosed = when (comp.type) {
                    ComponentType.CONTACTOR_NO_AUX,
                    ComponentType.CONTACTOR_NC_AUX,
                    ComponentType.TIMER_NO_CONTACT,
                    ComponentType.TIMER_NC_CONTACT,
                    ComponentType.MAIN_POWER_CONTACTS_3P -> false
                    ComponentType.PUSH_BUTTON_NO,
                    ComponentType.PUSH_BUTTON_NC,
                    ComponentType.EMERGENCY_STOP -> if (!comp.isMaintained) false else comp.isClosedOrPressed
                    else -> comp.isClosedOrPressed
                }
                comp.copy(
                    isClosedOrPressed = cleanClosed,
                    isEnergized = false,
                    currentTimerProgress = 0f,
                    motorRotationDirection = com.example.model.MotorDirection.STOPPED
                )
            }
            val compsJson = json.encodeToString(sanitizedComps)
            val cleanWires = _wires.value.map { it.copy(isEnergized = false) }
            val wiresJson = json.encodeToString(cleanWires)
            val ladderJson = json.encodeToString(_ladderRungs.value)
            val sheetsJson = json.encodeToString(_sheets.value)
            val layersJson = json.encodeToString(_layers.value)
            val finalId = _currentProjectId.value.ifBlank { UUID.randomUUID().toString() }
            val finalTitle = title.ifBlank { _currentProjectTitle.value }

            val entity = CircuitProjectEntity(
                id = finalId,
                title = finalTitle,
                description = desc,
                componentsJson = compsJson,
                wiresJson = wiresJson,
                ladderRungsJson = ladderJson,
                sheetsJson = sheetsJson,
                layersJson = layersJson,
                updatedAt = System.currentTimeMillis()
            )
            projectDao.insertOrUpdateProject(entity)
            _currentProjectId.value = finalId
            _currentProjectTitle.value = finalTitle
            _currentProjectDescription.value = desc
            clearDirty()
            draftDao.deleteDraft(finalId)
            draftDao.deleteDraft(DRAFT_UNNAMED_KEY)
            onFinished?.invoke()
        }
    }

    fun newProject() {
        checkUnsavedChangesBeforeAction {
            stopCircuitSimulation()
            stopPlcSimulation()
            undoStack.clear()
            redoStack.clear()
            _canUndo.value = false
            _canRedo.value = false
            _components.value = emptyList()
            _wires.value = emptyList()
            _sheets.value = listOf(CircuitSheet("sheet_1", "برگه ۱", 0))
            _currentSheetId.value = "sheet_1"
            _layers.value = CircuitTypeConverters.defaultLayers()
            _selectedComponentIds.value = emptySet()
            _isMultiSelectMode.value = false
            _ladderRungs.value = DefaultCircuitTemplates.createDefaultLadderProgram()
            _currentProjectId.value = ""
            _currentProjectTitle.value = "پروژه جدید"
            _currentProjectDescription.value = ""
            _selectedComponentId.value = null
            _selectedWireId.value = null
            clearDirty()
            viewModelScope.launch {
                draftDao.deleteDraft(DRAFT_UNNAMED_KEY)
            }
            _currentScreen.value = AppNavScreen.CIRCUIT_DESIGNER
            fitToScreen()
        }
    }

    fun duplicateProject(entity: CircuitProjectEntity) {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val dup = entity.copy(
                id = newId,
                title = "${entity.title} (کپی)",
                updatedAt = System.currentTimeMillis()
            )
            projectDao.insertOrUpdateProject(dup)
        }
    }

    fun loadProject(entity: CircuitProjectEntity) {
        stopCircuitSimulation()
        stopPlcSimulation()
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        // Use the same tolerant item-by-item readers as Room converters. A single
        // malformed legacy item must not make every valid item in the project disappear.
        val converters = CircuitTypeConverters()
        val comps: List<CircuitComponent> = converters.toComponents(entity.componentsJson)
        val wiresList: List<WireConnection> = converters.toWires(entity.wiresJson)
        val rungsList: List<LadderRung> = converters.toLadderRungs(entity.ladderRungsJson)
        val sheetsList: List<CircuitSheet> = converters.toSheets(entity.sheetsJson)
        val layersList: List<CircuitLayer> = converters.toLayers(entity.layersJson)

        // اعتبارسنجی فقط گزارش می‌دهد؛ هیچ سیم یا قطعه‌ای خودکار حذف/اصلاح نمی‌شود.
        val validationIssues = CircuitReferenceValidator.validate(comps, wiresList)
        _projectLoadValidationIssues.value = validationIssues
        _projectLoadValidationWarning.value = validationIssues.takeIf { it.isNotEmpty() }?.let { issues ->
            val count = issues.size
            "پروژه بارگذاری شد، اما $count مورد نیازمند بررسی اتصال است. اطلاعات اصلی تغییر نکرده؛ اتصالات را بررسی کنید."
        }

        _components.value = comps
        _wires.value = wiresList
        _sheets.value = if (sheetsList.isEmpty()) listOf(CircuitSheet("sheet_1", "برگه ۱", 0)) else sheetsList
        _currentSheetId.value = _sheets.value.firstOrNull()?.id ?: "sheet_1"
        _layers.value = if (layersList.isEmpty()) CircuitTypeConverters.defaultLayers() else layersList
        _selectedComponentIds.value = emptySet()
        _isMultiSelectMode.value = false
        recomputeNetsAndWireNumbers()
        if (rungsList.isNotEmpty()) {
            _ladderRungs.value = rungsList
        }
        _currentProjectId.value = entity.id
        _currentProjectTitle.value = entity.title
        _currentProjectDescription.value = entity.description
        _selectedComponentId.value = null
        _selectedWireId.value = null
        clearDirty()
        _currentScreen.value = AppNavScreen.CIRCUIT_DESIGNER
        fitToScreen()
        checkForDraftRecovery(entity.id)
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            projectDao.deleteProject(id)
        }
    }

    fun exportProjectAsJson(): String {
        val project = CircuitProject(
            id = _currentProjectId.value,
            title = _currentProjectTitle.value,
            description = _currentProjectDescription.value,
            components = _components.value,
            wires = _wires.value,
            ladderRungs = _ladderRungs.value,
            designerName = _designerName.value
        )
        return json.encodeToString(project)
    }

    fun importProjectFromJson(jsonStr: String): Boolean {
        return try {
            val proj = json.decodeFromString<CircuitProject>(jsonStr)
            stopCircuitSimulation()
            pushHistoryState()
            _components.value = proj.components
            _wires.value = proj.wires
            if (proj.ladderRungs.isNotEmpty()) {
                _ladderRungs.value = proj.ladderRungs
            }
            if (proj.designerName.isNotBlank()) {
                _designerName.value = proj.designerName
            }
            _currentProjectId.value = proj.id
            _currentProjectTitle.value = proj.title
            _currentProjectDescription.value = proj.description
            _selectedComponentId.value = null
            _selectedWireId.value = null
            fitToScreen()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun exportToPdf(context: Context): File {
        return ExportUtils.exportToPdf(context, _components.value, _wires.value, _currentProjectTitle.value, _designerName.value)
    }

    suspend fun exportToPng(context: Context): File {
        return ExportUtils.exportToPng(context, _components.value, _wires.value, _currentProjectTitle.value, _designerName.value)
    }

    fun shareExport(context: Context, isPdf: Boolean) {
        viewModelScope.launch {
            val file = if (isPdf) exportToPdf(context) else exportToPng(context)
            val mime = if (isPdf) "application/pdf" else "image/png"
            ExportUtils.shareExportedFile(context, file, mime)
        }
    }

    // متدهای مدیریت سیگنال‌های نمودار زمانی (P6 Logic Timing Diagram)
    fun initDefaultTimingSignals() {
        val defaults = listOf(
            TrackableSignal("plc_i00", "I0.0", "I0.0 (شستی استارت)", SignalSourceType.PLC_INPUT),
            TrackableSignal("plc_q00", "Q0.0", "Q0.0 (خروجی موتور)", SignalSourceType.PLC_OUTPUT),
            TrackableSignal("coil_KM1", "KM1", "KM1 (کویل کنتاکتور)", SignalSourceType.CIRCUIT_COIL),
            TrackableSignal("plc_t1_dn", "T1", "T1.DN (تایمر تاخیر)", SignalSourceType.PLC_TIMER_DONE)
        )
        _selectedTimingSignals.value = defaults
        refreshAvailableTimingSignals()
    }

    fun refreshAvailableTimingSignals() {
        val list = mutableListOf<TrackableSignal>()

        // ورودی‌های PLC
        for (i in 0..7) {
            val addr = "I0.$i"
            list.add(TrackableSignal("plc_$addr", addr, "$addr (ورودی PLC)", SignalSourceType.PLC_INPUT))
        }
        // خروجی‌های PLC
        for (q in 0..7) {
            val addr = "Q0.$q"
            list.add(TrackableSignal("plc_$addr", addr, "$addr (خروجی PLC)", SignalSourceType.PLC_OUTPUT))
        }
        // حافظه‌های میانی PLC (Markers)
        for (m in 0..7) {
            val addr = "M0.$m"
            list.add(TrackableSignal("plc_$addr", addr, "$addr (حافظه میانی M)", SignalSourceType.PLC_MARKER))
        }
        // تایمرها و شمارنده‌های موجود در لدر
        _ladderRungs.value.forEach { rung ->
            val allLadderElements = rung.contacts + rung.coils + rung.branchContacts + rung.parallelBranches.flatMap { it.contacts }
            allLadderElements.forEach { elem ->
                if (elem.type in listOf(LadderElementType.TIMER_TON, LadderElementType.TIMER_TOF)) {
                    val tag = elem.address
                    if (list.none { it.addressOrTag == tag && it.type == SignalSourceType.PLC_TIMER_DONE }) {
                        list.add(TrackableSignal("timer_$tag", tag, "$tag.DN (${elem.type.persianTitle})", SignalSourceType.PLC_TIMER_DONE))
                    }
                }
                if (elem.type in listOf(LadderElementType.COUNTER_CTU, LadderElementType.COUNTER_CTD)) {
                    val tag = elem.address
                    if (list.none { it.addressOrTag == tag && it.type == SignalSourceType.PLC_COUNTER_DONE }) {
                        list.add(TrackableSignal("counter_$tag", tag, "$tag.DN (${elem.type.persianTitle})", SignalSourceType.PLC_COUNTER_DONE))
                    }
                }
            }
        }
        // بوبین‌ها، لامپ‌ها و قطعات مدار
        _components.value.forEach { comp ->
            if (comp.type in listOf(ComponentType.CONTACTOR_COIL, ComponentType.PILOT_LAMP_GREEN, ComponentType.PILOT_LAMP_RED, ComponentType.PILOT_LAMP_YELLOW, ComponentType.MOTOR_3PHASE, ComponentType.CONTROL_RELAY_1CO, ComponentType.CONTROL_RELAY_2CO)) {
                val tag = comp.tagBinding.ifBlank { comp.label }
                if (tag.isNotBlank() && list.none { it.id == "comp_${comp.id}" }) {
                    list.add(TrackableSignal("comp_${comp.id}", tag, "${comp.label} (${comp.type.persianName})", SignalSourceType.CIRCUIT_COIL))
                }
            } else if (comp.type in listOf(ComponentType.PUSH_BUTTON_NO, ComponentType.PUSH_BUTTON_NC, ComponentType.EMERGENCY_STOP, ComponentType.CIRCUIT_BREAKER_MCB, ComponentType.CIRCUIT_BREAKER_MCB_3P)) {
                if (list.none { it.id == "comp_${comp.id}" }) {
                    list.add(TrackableSignal("comp_${comp.id}", comp.label, "${comp.label} (${comp.type.persianName})", SignalSourceType.CIRCUIT_COMPONENT))
                }
            }
        }

        _availableTimingSignals.value = list
    }

    fun addTimingSignal(signal: TrackableSignal) {
        if (_selectedTimingSignals.value.size >= 12) return
        if (_selectedTimingSignals.value.none { it.id == signal.id }) {
            _selectedTimingSignals.value = _selectedTimingSignals.value + signal
        }
    }

    fun removeTimingSignal(signalId: String) {
        _selectedTimingSignals.value = _selectedTimingSignals.value.filterNot { it.id == signalId }
    }

    fun toggleTimingRecordingPause() {
        _isTimingRecordingPaused.value = !_isTimingRecordingPaused.value
    }

    fun clearTimingBuffer() {
        timingSignalBuffer.clear()
        timingAccumulatedTimeSeconds = 0f
        _timingCurrentTimeSeconds.value = 0f
    }
}
