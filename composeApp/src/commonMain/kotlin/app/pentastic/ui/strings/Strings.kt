package app.pentastic.ui.strings

import androidx.compose.runtime.staticCompositionLocalOf
import app.pentastic.data.AppLanguage
import app.pentastic.data.DueDateOption
import app.pentastic.data.RepeatFrequency
import app.pentastic.data.TimelineSection
import app.pentastic.data.AppCalendar
import app.pentastic.data.ThemeMode
import app.pentastic.data.WeekStart
import app.pentastic.utils.withFarsiDigits

interface AppStrings {
    // Common
    val cancel: String
    val save: String

    // Settings
    val settings: String
    val theme: String
    val language: String
    val trash: String
    val follow: String
    val share: String
    val rate: String
    val shareText: String

    // Index page
    val index: String
    val reorder: String
    val done: String
    val options: String
    val showSubPages: String
    val hideSubPages: String
    val timeline: String
    val enable: String
    val disable: String
    val addSubPage: String
    fun subPageTarget(name: String): String
    val switchToNotes: String
    val switchToTasks: String
    val rename: String
    val archive: String
    val unarchive: String
    val delete: String
    val defaultPageName: String
    val defaultSubPageName: String
    val notesPage: String
    val tasksPage: String
    val editPageName: String
    val moveToTrash: String
    val subPageName: String
    val add: String
    val limitReached: String
    val subPageLimitMessage: String
    val ok: String

    // Home, Archive, Trash
    val addTask: String
    val addNote: String
    val addPage: String
    val addWithPriority: String
    val archiveEmpty: String
    val empty: String
    val trashEmpty: String
    val pages: String
    val tasks: String
    val emptyTrash: String
    val emptyTrashMessage: String
    val deleteAll: String
    val deletePermanently: String
    val deleteTaskMessage: String
    val subPage: String
    val restore: String
    val deleteForever: String

    // Note page
    val repeatingTask: String
    val upcomingReminder: String
    val dueDate: String
    val moreOptions: String
    val deleteCompleted: String
    val markDone: String
    val markTodo: String
    val priority: String
    val copy: String
    val edit: String
    val repeatLabel: String
    val moveTo: String
    val reminder: String
    val current: String
    val repeatTask: String
    val startFrom: String
    val remindMe: String
    val clear: String
    val selectStartDate: String

    // Timeline, Reminder, Due date
    val timelineRepeating: String
    val timelineUnscheduled: String
    val timelineCompleted: String
    val timelineEmpty: String
    val setReminder: String
    val changeDateTime: String
    val tapToEditRepeat: String
    val enabled: String
    val remove: String
    val selectDate: String
    val back: String
    val permissionRequired: String
    val permissionMessage: String
    val continueLabel: String

    // Calendar
    val calendar: String

    // Week
    val weekStart: String

    // Dates
    val someday: String

    // Functions
    fun themeLabel(mode: ThemeMode): String
    fun archiveCount(count: Int): String
    fun deletePageMessage(pageName: String): String
    fun movePageToTrashMessage(pageName: String): String
    fun completedTasks(count: Int): String
    fun moveCompletedMessage(count: Int): String
    fun calendarLabel(calendar: AppCalendar): String
    fun weekStartName(start: WeekStart): String
    fun dueOption(option: DueDateOption): String
    fun timelineSection(section: TimelineSection): String
    fun repeatFrequency(frequency: RepeatFrequency): String
    fun repeatingTaskInfo(frequency: RepeatFrequency): String
}

object EnglishStrings : AppStrings {
    // Common
    override val cancel = "Cancel"
    override val save = "Save"

    // Settings
    override val settings = "Settings"
    override val theme = "Theme"
    override val language = "Language"
    override val trash = "Trash"
    override val follow = "Follow"
    override val share = "Share"
    override val rate = "Rate"
    override val shareText =
        "Get things done with Pentastic!\nhttps://play.google.com/store/apps/details?id=app.pentastic"

    // Index page
    override val index = "Index"
    override val reorder = "Reorder"
    override val done = "Done"
    override val options = "Options"
    override val showSubPages = "Show sub-pages"
    override val hideSubPages = "Hide sub-pages"
    override val timeline = "Timeline"
    override val enable = "Enable"
    override val disable = "Disable"
    override val addSubPage = "Add sub-page"
    override fun subPageTarget(name: String) = "to $name"
    override val switchToNotes = "Switch to Notes"
    override val switchToTasks = "Switch to Tasks"
    override val rename = "Rename"
    override val archive = "Archive"
    override val unarchive = "Unarchive"
    override val delete = "Delete"
    override val defaultPageName = "Page"
    override val defaultSubPageName = "Sub-page"
    override val notesPage = "Notes page"
    override val tasksPage = "Tasks page"
    override val editPageName = "Edit page name"
    override val moveToTrash = "Move to trash"
    override val subPageName = "Sub-page name"
    override val add = "Add"
    override val limitReached = "Limit reached"
    override val subPageLimitMessage = "You can only add 10 sub-pages per page."
    override val ok = "OK"

    // Home, Archive, Trash
    override val addTask = "Add a task..."
    override val addNote = "Add a note..."
    override val addPage = "Add a new page..."
    override val addWithPriority = "Add with priority"
    override val archiveEmpty = "Archive is empty"
    override val empty = "Empty"
    override val trashEmpty = "Trash is empty"
    override val pages = "Pages"
    override val tasks = "Tasks"
    override val emptyTrash = "Empty trash"
    override val emptyTrashMessage = "Permanently delete all items in trash? This cannot be undone."
    override val deleteAll = "Delete all"
    override val deletePermanently = "Delete permanently"
    override val deleteTaskMessage = "Permanently delete this task? This cannot be undone."
    override val subPage = "Sub-page"
    override val restore = "Restore"
    override val deleteForever = "Delete forever"

    // Note page
    override val repeatingTask = "Repeating task"
    override val upcomingReminder = "Upcoming reminder"
    override val dueDate = "Due date"
    override val moreOptions = "More options"
    override val deleteCompleted = "Delete completed"
    override val markDone = "Done"
    override val markTodo = "Todo"
    override val priority = "Priority"
    override val copy = "Copy"
    override val edit = "Edit"
    override val repeatLabel = "Repeat"
    override val moveTo = "Move to"
    override val reminder = "Reminder"
    override val current = "Current"
    override val repeatTask = "Repeat task"
    override val startFrom = "Start from"
    override val remindMe = "Remind me"
    override val clear = "Clear"
    override val selectStartDate = "Select start date"

    // Timeline, Reminder, Due date
    override val timelineRepeating = "Repeating"
    override val timelineUnscheduled = "Unscheduled"
    override val timelineCompleted = "Completed"
    override val timelineEmpty =
        "Nothing scheduled yet.\n\nAdd a task below, or open any task's menu and set a due date — it will show up here."
    override val setReminder = "Set Reminder"
    override val changeDateTime = "Change date & time"
    override val tapToEditRepeat = "Tap to edit repeat settings"
    override val enabled = "Enabled"
    override val remove = "Remove"
    override val selectDate = "Select date"
    override val back = "Back"
    override val permissionRequired = "Permission required"
    override val permissionMessage =
        "To set reminders, we need permission to send notifications and schedule alarms."
    override val continueLabel = "Continue"

    // Calendar
    override val calendar = "Calendar"

    // Week
    override val weekStart = "Start of week"

    // Dates
    override val someday = "Someday"

    // Functions
    override fun themeLabel(mode: ThemeMode) = when (mode) {
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
        ThemeMode.SYSTEM -> "System"
        ThemeMode.DAY_NIGHT -> "Day/Night"
    }

    override fun archiveCount(count: Int) = "Archive ($count)"

    override fun deletePageMessage(pageName: String) =
        "Permanently delete page '$pageName' and all its notes? This cannot be undone."

    override fun movePageToTrashMessage(pageName: String) =
        "Move page '$pageName' and all its notes to trash?"

    override fun completedTasks(count: Int) = "Completed tasks ($count)"

    override fun moveCompletedMessage(count: Int) = "Move all $count completed tasks to trash?"

    override fun calendarLabel(calendar: AppCalendar) = when (calendar) {
        AppCalendar.GREGORIAN -> "Gregorian"
        AppCalendar.JALALI -> "Jalali (Persian)"
    }

    override fun weekStartName(start: WeekStart) = when (start) {
        WeekStart.SATURDAY -> "Saturday"
        WeekStart.SUNDAY -> "Sunday"
        WeekStart.MONDAY -> "Monday"
    }

    override fun dueOption(option: DueDateOption) = when (option) {
        DueDateOption.TODAY -> "Today"
        DueDateOption.TOMORROW -> "Tomorrow"
        DueDateOption.THIS_WEEK -> "This week"
        DueDateOption.NEXT_WEEK -> "Next week"
        DueDateOption.THIS_MONTH -> "This month"
        DueDateOption.NEXT_MONTH -> "Next month"
        DueDateOption.THIS_YEAR -> "This year"
        DueDateOption.NEXT_YEAR -> "Next year"
        DueDateOption.SOMEDAY -> "Someday"
        DueDateOption.CUSTOM -> "Custom"
    }

    override fun timelineSection(section: TimelineSection) = when (section) {
        TimelineSection.OVERDUE -> "Overdue"
        TimelineSection.TODAY -> "Today"
        TimelineSection.TOMORROW -> "Tomorrow"
        TimelineSection.THIS_WEEK -> "This week"
        TimelineSection.THIS_WEEKEND -> "This weekend"
        TimelineSection.NEXT_WEEK -> "Next week"
        TimelineSection.THIS_MONTH -> "This month"
        TimelineSection.NEXT_MONTH -> "Next month"
        TimelineSection.SOMEDAY -> "Someday"
    }

    override fun repeatFrequency(frequency: RepeatFrequency) = when (frequency) {
        RepeatFrequency.NONE -> "None"
        RepeatFrequency.DAILY -> "Daily"
        RepeatFrequency.WEEKLY -> "Weekly"
        RepeatFrequency.MONTHLY -> "Monthly"
        RepeatFrequency.QUARTERLY -> "Quarterly"
        RepeatFrequency.YEARLY -> "Yearly"
    }

    override fun repeatingTaskInfo(frequency: RepeatFrequency) =
        "This is a ${repeatFrequency(frequency).lowercase()} repeating task"
}

object FarsiStrings : AppStrings {
    // Common
    override val cancel = "لغو"
    override val save = "ذخیره"

    // Settings
    override val settings = "تنظیمات"
    override val theme = "پوسته"
    override val language = "زبان"
    override val trash = "سطل زباله"
    override val follow = "دنبال کردن"
    override val share = "اشتراک‌گذاری"
    override val rate = "امتیاز دادن"
    override val shareText =
        "با Pentastic کارهات رو انجام بده!\nhttps://play.google.com/store/apps/details?id=app.pentastic"

    // Index page
    override val index = "فهرست"
    override val reorder = "مرتب‌سازی"
    override val done = "تمام"
    override val options = "گزینه‌ها"
    override val showSubPages = "نمایش زیرصفحه‌ها"
    override val hideSubPages = "پنهان کردن زیرصفحه‌ها"
    override val timeline = "خط زمانی"
    override val enable = "فعال کردن"
    override val disable = "غیرفعال کردن"
    override val addSubPage = "افزودن زیرصفحه"
    override fun subPageTarget(name: String) = "به $name"
    override val switchToNotes = "تبدیل به یادداشت‌ها"
    override val switchToTasks = "تبدیل به کارها"
    override val rename = "تغییر نام"
    override val archive = "بایگانی"
    override val unarchive = "خروج از بایگانی"
    override val delete = "حذف"
    override val defaultPageName = "صفحه"
    override val defaultSubPageName = "زیرصفحه"
    override val notesPage = "صفحه‌ی یادداشت"
    override val tasksPage = "صفحه کارها"
    override val editPageName = "ویرایش نام صفحه"
    override val moveToTrash = "انتقال به سطل زباله"
    override val subPageName = "نام زیرصفحه"
    override val add = "افزودن"
    override val limitReached = "به حد مجاز رسیدید"
    override val subPageLimitMessage = "برای هر صفحه حداکثر ۱۰ زیرصفحه می‌توانید اضافه کنید."
    override val ok = "تأیید"

    // Home, Archive, Trash
    override val addTask = "یک کار اضافه کن..."
    override val addNote = "یک یادداشت اضافه کن..."
    override val addPage = "یک صفحه‌ی جدید اضافه کن..."
    override val addWithPriority = "افزودن با اولویت"
    override val archiveEmpty = "بایگانی خالی است"
    override val empty = "خالی کردن"
    override val trashEmpty = "سطل زباله خالی است"
    override val pages = "صفحه‌ها"
    override val tasks = "کارها"
    override val emptyTrash = "خالی کردن سطل زباله"
    override val emptyTrashMessage = "همه‌ی موارد سطل زباله برای همیشه حذف شوند؟ این عمل قابل بازگشت نیست."
    override val deleteAll = "حذف همه"
    override val deletePermanently = "حذف دائمی"
    override val deleteTaskMessage = "این مورد برای همیشه حذف شود؟ این عمل قابل بازگشت نیست."
    override val subPage = "زیرصفحه"
    override val restore = "بازیابی"
    override val deleteForever = "حذف برای همیشه"

    // Note page
    override val repeatingTask = "کار تکرارشونده"
    override val upcomingReminder = "یادآور پیش‌رو"
    override val dueDate = "تاریخ سررسید"
    override val moreOptions = "گزینه‌های بیشتر"
    override val deleteCompleted = "حذف انجام‌شده‌ها"
    override val markDone = "انجام شد"
    override val markTodo = "انجام نشده"
    override val priority = "اولویت"
    override val copy = "کپی"
    override val edit = "ویرایش"
    override val repeatLabel = "تکرار"
    override val moveTo = "انتقال به"
    override val reminder = "یادآور"
    override val current = "فعلی"
    override val repeatTask = "تکرار کار"
    override val startFrom = "شروع از"
    override val remindMe = "یادآوری کن"
    override val clear = "پاک کردن"
    override val selectStartDate = "انتخاب تاریخ شروع"

    // Timeline, Reminder, Due date
    override val timelineRepeating = "تکرارشونده"
    override val timelineUnscheduled = "بدون سررسید"
    override val timelineCompleted = "انجام‌شده"
    override val timelineEmpty =
        "هنوز چیزی برنامه‌ریزی نشده.\n\nپایین یک کار اضافه کن، یا منوی هر کار را باز کن و تاریخ سررسید بگذار؛ اینجا نمایش داده می‌شود."
    override val setReminder = "تنظیم یادآور"
    override val changeDateTime = "تغییر تاریخ و زمان"
    override val tapToEditRepeat = "برای تغییر تنظیمات تکرار ضربه بزن"
    override val enabled = "فعال"
    override val remove = "برداشتن"
    override val selectDate = "انتخاب تاریخ"
    override val back = "بازگشت"
    override val permissionRequired = "نیاز به دسترسی"
    override val permissionMessage =
        "برای تنظیم یادآور، به اجازه‌ی ارسال اعلان و زمان‌بندی هشدار نیاز داریم."
    override val continueLabel = "ادامه"
        
    // Calendar
    override val calendar = "تقویم"

    // Week
    override val weekStart = "روز شروع هفته"

    // Dates
    override val someday = "بعداً"

    // Functions
    override fun themeLabel(mode: ThemeMode) = when (mode) {
        ThemeMode.LIGHT -> "روشن"
        ThemeMode.DARK -> "تیره"
        ThemeMode.SYSTEM -> "سیستم"
        ThemeMode.DAY_NIGHT -> "روز/شب"
    }

    override fun archiveCount(count: Int) = "بایگانی (${count.withFarsiDigits()})"

    override fun deletePageMessage(pageName: String) =
        "صفحه‌ی «$pageName» و همه‌ی یادداشت‌های آن برای همیشه حذف شود؟ این عمل قابل بازگشت نیست."

    override fun movePageToTrashMessage(pageName: String) =
        "صفحه‌ی «$pageName» و همه‌ی یادداشت‌های آن به سطل زباله منتقل شود؟"

    override fun completedTasks(count: Int) = "کارهای انجام‌شده (${count.withFarsiDigits()})"

    override fun moveCompletedMessage(count: Int) = "همه‌ی ${count.withFarsiDigits()} کار انجام‌شده به سطل زباله منتقل شوند؟"

    override fun calendarLabel(calendar: AppCalendar) = when (calendar) {
        AppCalendar.GREGORIAN -> "میلادی"
        AppCalendar.JALALI -> "شمسی"
    }

    override fun weekStartName(start: WeekStart) = when (start) {
        WeekStart.SATURDAY -> "شنبه"
        WeekStart.SUNDAY -> "یکشنبه"
        WeekStart.MONDAY -> "دوشنبه"
    }

    override fun dueOption(option: DueDateOption) = when (option) {
        DueDateOption.TODAY -> "امروز"
        DueDateOption.TOMORROW -> "فردا"
        DueDateOption.THIS_WEEK -> "این هفته"
        DueDateOption.NEXT_WEEK -> "هفته بعد"
        DueDateOption.THIS_MONTH -> "این ماه"
        DueDateOption.NEXT_MONTH -> "ماه بعد"
        DueDateOption.THIS_YEAR -> "امسال"
        DueDateOption.NEXT_YEAR -> "سال بعد"
        DueDateOption.SOMEDAY -> "بعداً"
        DueDateOption.CUSTOM -> "دلخواه"
    }

    override fun timelineSection(section: TimelineSection) = when (section) {
        TimelineSection.OVERDUE -> "از موعد گذشته"
        TimelineSection.TODAY -> "امروز"
        TimelineSection.TOMORROW -> "فردا"
        TimelineSection.THIS_WEEK -> "این هفته"
        TimelineSection.THIS_WEEKEND -> "آخر هفته"
        TimelineSection.NEXT_WEEK -> "هفته بعد"
        TimelineSection.THIS_MONTH -> "این ماه"
        TimelineSection.NEXT_MONTH -> "ماه بعد"
        TimelineSection.SOMEDAY -> "بعداً"
    }

    override fun repeatFrequency(frequency: RepeatFrequency) = when (frequency) {
        RepeatFrequency.NONE -> "هیچ"
        RepeatFrequency.DAILY -> "روزانه"
        RepeatFrequency.WEEKLY -> "هفتگی"
        RepeatFrequency.MONTHLY -> "ماهانه"
        RepeatFrequency.QUARTERLY -> "سه‌ماهه"
        RepeatFrequency.YEARLY -> "سالانه"
    }

    override fun repeatingTaskInfo(frequency: RepeatFrequency) =
        "این یک کار ${repeatFrequency(frequency)} است"
}

fun stringsFor(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.ENGLISH -> EnglishStrings
    AppLanguage.FARSI -> FarsiStrings
}

val LocalStrings = staticCompositionLocalOf<AppStrings> { EnglishStrings }