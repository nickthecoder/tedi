package uk.co.nickthecoder.tedi.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import uk.co.nickthecoder.tedi.BetterUndoRedo;
import uk.co.nickthecoder.tedi.TediArea;
import uk.co.nickthecoder.tedi.TediUtilKt;
import uk.co.nickthecoder.tedi.syntax.JavaSyntax;
import uk.co.nickthecoder.tedi.ui.*;

/**
 * An example application with a TediArea.
 * <p>
 * Features include :
 * <p>
 * find/replace
 * goto line dialog box
 * java syntax highlighting
 * use of "BetterUndoRedo"
 * <p>
 * You may also like to look at the Kotlin Demo application, which also demonstrates the following :
 * <p>
 * Re-use of a single find & replace matcher for multiple TediAreas (in a TabPane).
 * Load/Save of find/replace via Preferences
 * Threading of the syntax highlighter
 * Example of how to get the character position from a mouse event's x, y coordinate
 */
public class Example extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        new ExampleWindow(primaryStage);
    }

    public static void main(String[] args) {
        Application.launch(Example.class);
    }

    public class ExampleWindow {

        /**
         * The text editor
         */
        TediArea tediArea = new TediArea();

        /**
         * The root of the scene top= toolBar, center = tediArea, bottom = findAndReplaceToolBars.
         */
        BorderPane borderPane = new BorderPane();

        /**
         * The tool bar at the top of the scene
         */
        ToolBar toolBar = new ToolBar();

        /**
         * The non-gui part of find and replace.
         */
        TextInputControlMatcher matcher = new TextInputControlMatcher(tediArea);

        /**
         * A tool bar, which appears below the tabPane (inside findAndReplaceToolBars)
         */
        FindBar findBar = new FindBar(matcher);

        /**
         * A tool bar, which appears below the findBar (inside findAndReplaceToolBars)
         */
        ReplaceBar replaceBar = new ReplaceBar(matcher);

        /**
         * At the bottom of the scene. Contains findBar and replaceBar.
         */
        VBox findAndReplaceToolBars = new VBox();

        // Buttons within the toolBar
        Button undo = new Button();
        Button redo = new Button();
        ToggleButton toggleLineNumbers = new ToggleButton();
        ToggleButton toggleFind = findBar.createToggleButton();
        ToggleButton toggleFindAndReplace = replaceBar.createToggleButton();
        Button gotoButton = GotoDialog.createGotoButton(tediArea);

        Scene scene = new Scene(borderPane, 700.0, 500.0);

        /**
         * Constructor called from Example.start
         */
        ExampleWindow(Stage stage) {
            /*
             * Automatically removes children when they are made invisible.
             * Then replaces them if they are made visible again.
             * Without this, the findAndReplaceToolBars VBox would take up space even when its children were hidden.
             */
            new RemoveHiddenChildren(findAndReplaceToolBars.getChildren());

            // Applies tedi.css and syntax.css found in tedi-core's jar file.
            TediArea.style(scene);
            TediArea.syntaxStyle(scene);

            tediArea.getStyleClass().add("code"); // Fixed with font + line numbers

            borderPane.setCenter(tediArea);
            borderPane.setTop(toolBar);
            borderPane.setBottom(findAndReplaceToolBars);

            // Note, the graphics for the find and replace buttons are automatically added,
            // and are stored within the tedi-core jar file.
            // You can of course change them to match your applications other buttons.
            // The undo and redo button graphics are in this Example application's jar file.
            TediUtilKt.loadGraphic(undo, Example.class, "undo.png");
            TediUtilKt.loadGraphic(redo, Example.class, "redo.png");
            // This button graphic is also in the tedi-core jar file.
            TediUtilKt.loadGraphic(toggleLineNumbers, FindBar.class, "line-numbers.png");

            // Without this, the tool bars look slightly wrong.
            findBar.getToolBar().getStyleClass().add("bottom");
            replaceBar.getToolBar().getStyleClass().add("bottom");

            toolBar.getItems().addAll(undo, redo, toggleLineNumbers, toggleFind, toggleFindAndReplace, gotoButton);

            findAndReplaceToolBars.getChildren().addAll(findBar.getToolBar(), replaceBar.getToolBar());

            // Hides the find and replace toolbars.
            matcher.setInUse(false);

            stage.setScene(scene);

            // Handle keyboard shortcuts
            borderPane.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);

            // Whenever the document is changed, re-apply the java syntax highlighting. The 500ms wait time
            // prevents the highlighting being performed for EVERY keystroke, and instead only does it
            // after an idle period.
            JavaSyntax.getInstance().attach(tediArea, 500);

            // Add some example text.
            tediArea.setText("public class Example {\n\n" +
                    "    boolean test = false;\n\n" +
                    "    public void hello( String name ) {\n" +
                    "        System.out.println( \"Hello \" + name );\n" +
                    "    }\n" +
                    "}\n"
            );

            // Bind the line number toggle button to tediArea's property.
            // That's all we have to do to hide/show line numbers.
            toggleLineNumbers.selectedProperty().bindBidirectional(tediArea.displayLineNumbersProperty());

            // Undo redo button states and actions
            tediArea.setUndoRedo(new BetterUndoRedo(tediArea));
            undo.disableProperty().bind(tediArea.getUndoRedo().undoableProperty().not());
            redo.disableProperty().bind(tediArea.getUndoRedo().redoableProperty().not());
            redo.setOnAction((event) -> tediArea.getUndoRedo().redo());
            undo.setOnAction((event) -> tediArea.getUndoRedo().undo());

            // Or, we could use TextInputControl's undo/redo (which is a little naff)
            // undo.disableProperty().bind(tediArea.undoableProperty().not());
            // redo.disableProperty().bind(tediArea.redoableProperty().not());
            // redo.setOnAction((event) -> tediArea.redo());
            // undo.setOnAction((event) -> tediArea.undo());

            stage.setTitle("Tedi Example Application");
            stage.show();
        }

        /**
         * Handles keyboard shortcuts. You may like to add tooltips to the buttons, indicating the keys to use.
         * Nothing interesting to see here!
         */
        void onKeyPressed(KeyEvent event) {

            // We'll assume the event is to be consumed, unless we fall into an "else" clause.
            boolean consume = true;
            KeyCode code = event.getCode();

            if (event.isShortcutDown()) { // The same as isControlDown unless you are using a mac

                if (code == KeyCode.F) {
                    matcher.setInUse(true);
                    findBar.requestFocus();

                } else if (code == KeyCode.Z) {
                    tediArea.getUndoRedo().undo();

                } else if (code == KeyCode.Y) {
                    tediArea.getUndoRedo().redo();

                } else if (code == KeyCode.G) {
                    new GotoDialog(tediArea).show();

                } else if (code == KeyCode.L) {
                    toggleLineNumbers.setSelected(!toggleLineNumbers.isSelected());

                } else if (code == KeyCode.R) {
                    // Focus on either the "find", or the "replace", depending on if "find" is already visible.
                    boolean wasInUse = matcher.getInUse();
                    replaceBar.getToolBar().setVisible(true);
                    if (wasInUse) {
                        replaceBar.requestFocus();
                    }
                    // else, the findBar will automatically focus itself.

                } else {
                    consume = false;
                }
            } else {
                // Not shortcut down
                if (code == KeyCode.ESCAPE) {
                    // Hide the find/replace toolbars if they are visible.
                    // This works because the toolbars' visibility is bound to matcher's inUse property.
                    matcher.setInUse(false);
                } else {
                    consume = false;
                }
            }

            if (consume) {
                event.consume();
            }
        }

    }
}
