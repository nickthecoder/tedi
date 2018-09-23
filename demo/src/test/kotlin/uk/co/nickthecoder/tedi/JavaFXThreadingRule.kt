package uk.co.nickthecoder.tedi

import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.CountDownLatch
import javax.swing.SwingUtilities

/**
 * A JUnit {@link Rule} for running tests on the JavaFX thread and performing
 * JavaFX initialisation.  To include in your test case, add the following code:
 *
 *     @JvmField @Rule
 *     val jfxRule = JavaFXThreadingRule()
 *
 * @author Andy Till
 *
 * http://andrewtill.blogspot.com/2012/10/junit-rule-for-javafx-controller-testing.html
 */
class JavaFXThreadingRule : TestRule {

    override fun apply(statement: Statement, description: Description): Statement {
        return OnJFXThreadStatement(statement)
    }

    private class OnJFXThreadStatement(private val statement: Statement) : Statement() {

        private var rethrownException: Throwable? = null

        override fun evaluate() {

            if (!jfxIsSetup) {
                setupJavaFX()
                jfxIsSetup = true
            }

            val countDownLatch = CountDownLatch(1)

            Platform.runLater {
                try {
                    statement.evaluate()
                } catch (e: Throwable) {
                    rethrownException = e
                }

                countDownLatch.countDown()
            }

            countDownLatch.await()

            // if an exception was thrown by the statement during evaluation,
            // then re-throw it to fail the test
            rethrownException?.let { throw it }
        }

        private fun setupJavaFX() {

            val latch = CountDownLatch(1)

            SwingUtilities.invokeLater {
                // initializes JavaFX environment
                JFXPanel()
                latch.countDown()
            }

            latch.await()
        }

    }

    companion object {
        private var jfxIsSetup: Boolean = false
    }
}
