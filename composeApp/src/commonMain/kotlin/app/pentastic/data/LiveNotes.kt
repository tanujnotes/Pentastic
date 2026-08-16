package app.pentastic.data

/**
 * Pages whose notes are live: root pages + their sub-pages + the Timeline page.
 *
 * [rootPages] comes from `getRootPages()`, which already excludes deleted, archived
 * and Timeline-type pages; [timelinePage] comes from `getTimelinePage()`, which
 * filters deleted but *not* archived. Deliberately not expressed as a SQL join on
 * `page.deletedAt/archivedAt` — that would silently change the archived-Timeline
 * case, and this set is read by the index counts, the Timeline and the widgets,
 * which must agree exactly.
 */
fun livePageIds(
    rootPages: List<Page>,
    subPagesByParent: Map<Long, List<Page>>,
    timelinePage: Page?,
): Set<Long> = buildSet {
    rootPages.forEach { add(it.id) }
    subPagesByParent.values.forEach { subs -> subs.forEach { add(it.id) } }
    timelinePage?.let { add(it.id) }
}

/** The notes belonging to [livePageIds], flattened out of a per-page grouping. */
fun liveNotes(
    notesByPage: Map<Long, List<Note>>,
    livePageIds: Set<Long>,
): List<Note> = notesByPage.filterKeys { it in livePageIds }.values.flatten()
