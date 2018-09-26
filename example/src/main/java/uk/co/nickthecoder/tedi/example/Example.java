package uk.co.nickthecoder.tedi.example;

import javafx.application.Application;
import javafx.application.Platform;
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
import uk.co.nickthecoder.tedi.HighlightRange;
import uk.co.nickthecoder.tedi.TediArea;
import uk.co.nickthecoder.tedi.TediUtilKt;
import uk.co.nickthecoder.tedi.syntax.JavaSyntaxKt;
import uk.co.nickthecoder.tedi.ui.*;

import java.util.List;
import java.util.prefs.Preferences;

public class Example extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        HistoryComboBox.loadHistory(FindBar.getFindHistory(), Preferences.userRoot().node(Example.class.getName() + "/find"));
        HistoryComboBox.loadHistory(ReplaceBar.getReplacementHistory(), Preferences.userRoot().node(Example.class.getName() + "/replace"));

        new ExampleWindow(primaryStage);
    }

    @Override
    public void stop() {
        HistoryComboBox.saveHistory(FindBar.getFindHistory(), Preferences.userRoot().node(Example.class.getName() + "/find"), 30);
        HistoryComboBox.saveHistory(ReplaceBar.getReplacementHistory(), Preferences.userRoot().node(Example.class.getName() + "/replace"), 30);
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
         * The root of the scene.
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

        ExampleWindow(Stage stage) {
            /*
             * Automatically removes children when they are made invisible.
             * Then replaces them if they are made visible again.
             * Without this, the findAndReplaceToolBars VBox would take up space even when its children were hidden.
             */
            new RemoveHiddenChildren(findAndReplaceToolBars.getChildren());

            // Applies tedi.css found in tedi-core's jar file.
            TediArea.style(scene);

            borderPane.getStyleClass().add("example");
            borderPane.setCenter(tediArea);
            borderPane.setTop(toolBar);
            borderPane.setBottom(findAndReplaceToolBars);

            TediUtilKt.loadGraphic(undo, Example.class, "undo.png");
            undo.setOnAction((event) -> tediArea.getUndoRedo().undo());

            TediUtilKt.loadGraphic(redo, Example.class, "redo.png");
            redo.setOnAction((event) -> tediArea.getUndoRedo().redo());

            TediUtilKt.loadGraphic(toggleLineNumbers, FindBar.class, "line-numbers.png");

            findBar.getToolBar().getStyleClass().add("bottom");
            replaceBar.getToolBar().getStyleClass().add("bottom");

            toolBar.getItems().addAll(undo, redo, toggleLineNumbers, toggleFind, toggleFindAndReplace, gotoButton);

            findAndReplaceToolBars.getChildren().addAll(findBar.getToolBar(), replaceBar.getToolBar());

            // Hides the find and replace toolbars.
            matcher.setInUse(false);

            stage.setScene(scene);

            // Handle keyboard shortcuts
            borderPane.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);

            tediArea.textProperty().addListener((observable, oldValue, newValue) -> applySyntax());

            tediArea.setText("public class Example {\n\n" +
                    "    boolean test = false;\n\n" +
                    "    public void hello( String name ) {\n" +
                    "        System.out.println( \"Hello \" + name );\n" +
                    "    }\n" +
                    "}\n"
            );

            tediArea.setUndoRedo(new BetterUndoRedo(tediArea));
            toggleLineNumbers.selectedProperty().bindBidirectional(tediArea.displayLineNumbersProperty());
            undo.disableProperty().bind(tediArea.getUndoRedo().getUndoableProperty().not());
            redo.disableProperty().bind(tediArea.getUndoRedo().getRedoableProperty().not());

            stage.setTitle("Tedi Example Application");
            stage.show();

        }

        /**
         * Note, this is bad, as it is doing a potentially LONG task on the JavaFX thread.
         * Alas, I couldn't work out how to call my Kotlin function propertyChangeDelayedThread
         * from Java. Sorry.
         */
        void applySyntax() {
            List<HighlightRange> ranges = JavaSyntaxKt.javaSyntax(tediArea.getText());
            Platform.runLater(() -> {
                tediArea.highlightRanges().clear();
                tediArea.highlightRanges().addAll(ranges);
            });
        }

        void onKeyPressed(KeyEvent event) {
            boolean consume = true;
            KeyCode code = event.getCode();

            if (event.isShortcutDown()) {

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
                    boolean wasInUse = matcher.getInUse();
                    replaceBar.getToolBar().setVisible(true);
                    if (wasInUse) {
                        replaceBar.requestFocus();
                    }

                } else {
                    consume = false;
                }
            } else {
                // Not control down
                if (code == KeyCode.ESCAPE) {
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
