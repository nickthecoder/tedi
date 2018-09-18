package uk.co.nickthecoder.tedi.demo

import javafx.application.Application
import javafx.stage.Stage

class Demo : Application() {

    override fun start(stage: Stage) {
        DemoWindow(stage)
    }

}

fun main(args: Array<String>) {

    Application.launch(Demo::class.java)

}

