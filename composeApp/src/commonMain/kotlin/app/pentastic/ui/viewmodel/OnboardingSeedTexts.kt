package app.pentastic.ui.viewmodel

import app.pentastic.data.AppLanguage

/**
 * The sample pages and notes written to a fresh install, in the app's saved
 * language (Farsi by default). They are stored content, not UI labels, so
 * they are captured once at first launch and are not affected by later
 * language changes.
 */
internal class OnboardingSeedTexts(isFarsi: Boolean) {
    val pageTodo = if (isFarsi) "کارها" else "Todo"
    val todoInstall = if (isFarsi) "پنتاستیک رو نصب کن!" else "Install Pentastic!"
    val todoDoubleTap = if (isFarsi) "دو ضربه بزن تا کار انجام‌شده علامت بخوره. ✔" else "Double tap to mark a task as done. ✔"
    val todoSingleTap = if (isFarsi) "برای باز کردن منو یه ضربه بزن؛ برای جابه‌جایی نگهش دار." else "Single tap for menu; long press to reorder."
    val todoSwipe = if (isFarsi) "و از راست بکش... چون ما دوتایی به هم میایم. 😎" else "And swipe right... because we're a perfect match. 😎"

    val pageReminders = if (isFarsi) "یادآورها" else "Reminders"
    val remindersText =
        if (isFarsi) "برای کارهای تکی یا تکرارشونده می‌تونی یادآور بذاری. روی کار ضربه بزن تا منوش باز بشه."
        else "You can set reminders for your one-off or repeating tasks. Tap on the task to open the menu."

    val pageThings = if (isFarsi) "گوناگون" else "Stuff"
    val pageThingsTodo = if (isFarsi) "انجام بدم" else "To do"
    val pageThingsRead = if (isFarsi) "بخونم" else "Read"
    val pageThingsWatch = if (isFarsi) "ببینم" else "Watch"
    val thingsTextForget = if (isFarsi) "که یادت نره آخرش واقعاً انجامش بدی! 😄" else "so that you don't forget to actually do it! 😄"
    val thingsTextIntro =
        if (isFarsi) "جایی برای یادداشت همه‌ی کارهای باحالی که می‌خوای انجام بدی، بخونی یا ببینی..."
        else "A place to note down all the fun stuff you want to do, read, or watch..."

    val pageNotes = if (isFarsi) "یادداشت‌ها" else "Notes"
    val notesTextTimestamps = if (isFarsi) "این یک صفحه‌ی یادداشته؛ زیر هر یادداشت زمانش ثبت میشه. 📝" else "This is a Notes page with timestamps for each note. 📝"
    val notesTextType = if (isFarsi) "نوع هر صفحه رو می‌تونی از فهرست عوض کنی." else "You can change the type of a page from the Index page."

    val pageIdeas = if (isFarsi) "ایده‌ها" else "Ideas"
    val ideasTextScribble =
        if (isFarsi) "هر فکر و ایده‌ای که به ذهنت می‌رسه همین‌جا بنویس تا بعداً گم نشه."
        else "Scribble your random thoughts and ideas here to revisit later."
    val ideasTextHappy = if (isFarsi) "خوش بنویسی! :)" else " Happy scribbling! :)"

    val pageDontDo = if (isFarsi) "کار نکن" else "Don't do"
    val dontDoTextBack = if (isFarsi) "با دکمه‌ی بازگشت به فهرست برمی‌گردی." else "Press BACK to go to the Index page."
    val dontDoTextPriority =
        if (isFarsi) "کارهای اولویت‌دار قرمز دیده می‌شن. روی هر کار ضربه بزن تا اولویتش رو عوض کنی. 👆"
        else "Priority tasks will appear in red. Tap on any task to change priority. 👆"
    val dontDoTextOkay = if (isFarsi) "اشکالی نداره... همه لازمش داریم. :D" else "It's okay... we all need this list. :D"

    companion object {
        /** (English name, Farsi name) of every seeded page, keyed by nothing — for renaming untouched seed pages on a language switch. */
        val pageNamePairs: List<Pair<String, String>> = listOf(
            "Todo" to "کارها",
            "Reminders" to "یادآورها",
            "Stuff" to "گوناگون",
            "To do" to "انجام بدم",
            "Read" to "بخونم",
            "Watch" to "ببینم",
            "Notes" to "یادداشت‌ها",
            "Ideas" to "ایده‌ها",
            "Don't do" to "کار نکن",
            "Timeline" to "خط زمانی",
        )

        /** The seed name in [language] for a page currently named [name]; null when [name] is not an untouched seed name. */
        fun translatedName(name: String, from: AppLanguage, to: AppLanguage): String? {
            if (from == to) return null
            return when (from) {
                AppLanguage.ENGLISH -> pageNamePairs.firstOrNull { it.first == name }?.second
                AppLanguage.FARSI -> pageNamePairs.firstOrNull { it.second == name }?.first
            }
        }
    }
}
