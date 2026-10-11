# mihon-integration

Mihon module map: app (Voyager tabs: LibraryTab, UpdatesTab, HistoryTab, BrowseTab, MoreTab),
source-api/source-local (Local source, formats), reader (Activity + pager/webtoon viewers,
ChapterLoader → ArchivePageLoader/EpubPageLoader/DirectoryPageLoader, reader settings screens),
i18n (strings.xml per locale). Permitted changes ONLY: rebrand (name/icon/applicationId
`app.auplayer`/About), remove Browse/extensions/trackers + their permissions, bottom nav
Library·Videos·History·Web·More, "Add local folder" via Local-source use-cases, PDF page loader,
EnhancingPageLoader wrapper, new ar/fr/en strings, "Enhance pages" reader section, player/enhancement/
Diagnostics/Licenses settings sections. Everything else stays upstream. Align JDK/Gradle/AGP/NDK
to Mihon's own CI workflow; never modify Mihon code to work around build issues.
