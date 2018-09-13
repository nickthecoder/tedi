package uk.co.nickthecoder.tedi.example

import javafx.application.Application
import javafx.stage.Stage

class Example : Application() {

    override fun start(stage: Stage) {
        println("Application started")
        ExampleWindow(stage)
    }

}

fun main(args: Array<String>) {

    Application.launch(Example::class.java)

}

