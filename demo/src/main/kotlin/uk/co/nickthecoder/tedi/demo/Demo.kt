package uk.co.nickthecoder.tedi.demo

import javafx.application.Application
import javafx.stage.Stage
import uk.co.nickthecoder.tedi.ui.HistoryComboBox
import uk.co.nickthecoder.tedi.ui.ReplaceBar
import uk.co.nickthecoder.tedi.ui.SearchBar
import java.util.prefs.Preferences

class Demo : Application() {

    override fun start(stage: Stage) {
        // Load search and replace history
        HistoryComboBox.loadHistory(SearchBar.searchHistory, Preferences.userRoot().node(this::class.java.name + "/search"))
        HistoryComboBox.loadHistory(ReplaceBar.replacementHistory, Preferences.userRoot().node(this::class.java.name + "/replace"))

        DemoWindow(stage)
    }

    override fun stop() {
        // Save search and replace history, so that the next time the application is started, the SearchBar and ReplaceBar
        // will have remembered you searches from this session.
        HistoryComboBox.saveHistory(SearchBar.searchHistory, Preferences.userRoot().node(this::class.java.name + "/search"), 20)
        HistoryComboBox.saveHistory(ReplaceBar.replacementHistory, Preferences.userRoot().node(this::class.java.name + "/replace"), 20)

        super.stop()
    }

}

fun main(args: Array<String>) {

    Application.launch(Demo::class.java)

}

