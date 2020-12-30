package acr.browser.lightning.html.history

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.R
import acr.browser.lightning.constant.FILE
import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.html.HtmlPageFactory
import acr.browser.lightning.html.ListPageReader
import acr.browser.lightning.html.jsoup.*
import acr.browser.lightning.utils.ThemeUtils
import acr.browser.lightning.utils.htmlColor
import android.app.Application
import dagger.Reusable
import io.reactivex.Completable
import io.reactivex.Single
import java.io.File
import java.io.FileWriter
import javax.inject.Inject

/**
 * Factory for the history page.
 */
@Reusable
class HistoryPageFactory @Inject constructor(
    private val listPageReader: ListPageReader,
    private val application: Application,
    private val historyRepository: HistoryRepository
) : HtmlPageFactory {

    private val title = application.getString(R.string.action_history)

    override fun buildPage(): Single<String> = historyRepository
        .lastHundredVisitedHistoryEntries()
        .map { list ->
            parse(listPageReader.provideHtml()
                    // Show localized page title
                    .replace("\${pageTitle}", application.getString(R.string.action_history))
                    // Theme our page first
                    .replace("\${backgroundColor}", htmlColor(ThemeUtils.getPrimaryColor(BrowserApp.currentContext())))
                    .replace("\${textColor}", htmlColor(ThemeUtils.getTextColor(BrowserApp.currentContext())))
                    .replace("\${secondaryTextColor}", htmlColor(ThemeUtils.getColor(BrowserApp.currentContext(),R.attr.colorAccent)))
                    .replace("\${dividerColor}", htmlColor(ThemeUtils.getColor(BrowserApp.currentContext(),R.attr.dividerColor)))
            ) andBuild {
                title { title }
                body {
                    val repeatedElement = id("repeated").removeElement()
                    id("content") {
                        list.forEach {
                            appendChild(repeatedElement.clone {
                                tag("a") { attr("href", it.url) }
                                id("title") { text(it.title) }
                                id("url") { text(it.url) }
                            })
                        }
                    }
                }
            }
        }
        .map { content -> Pair(createHistoryPage(), content) }
        .doOnSuccess { (page, content) ->
            FileWriter(page, false).use { it.write(content) }
        }
        .map { (page, _) -> "$FILE$page" }

    /**
     * Use this observable to immediately delete the history page. This will clear the cached
     * history page that was stored on file.
     *
     * @return a completable that deletes the history page when subscribed to.
     */
    fun deleteHistoryPage(): Completable = Completable.fromAction {
        with(createHistoryPage()) {
            if (exists()) {
                delete()
            }
        }
    }

    private fun createHistoryPage() = File(application.filesDir, FILENAME)

    companion object {
        const val FILENAME = "history.html"
    }

}
