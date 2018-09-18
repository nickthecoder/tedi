# Tedi (pronounced Teddy).

A simple text editor component suitable for embedding inside a JavaFX application.

The main usage is for writing simple text files, as well as short pieces of code.
(I want to use it as a simple code editor in my
[Tickle Game Engine](https://github.com/nickthecoder/tickle)
)

I've tried looking for a suitable alternative, and the best I found is
[RichTextFX](https://github.com/FXMisc/RichTextFX).

## Progress

TediArea already does most of what I want (highlighted search matches being the most notable exception).

However, it currently depends on the following non-standard classes :

    com.sun.javafx.tk.Toolkit
    com.sun.javafx.scene.text.TextLayout

It also spews out numerous compiler warnings due to use of "deprecated" APIs.

I've been whittling down the list, and am quietly confident of getting rid of the rest of them!

## Differences between TediArea and TextArea

Given that TediArea started as a copy of TextArea, they should be fairly similar.

### Missing Features

- Context menu (for copy, paste, etc)
- Option to wrap text
- prefRowCount and prefColumnCount.
- mixed left-to-right and right-to-left text flow.

I chose to exclude the context menu, because it is likely that any application that embeds TediArea will
add their own context menu, with more features. Also, getting I18N (translations) of the text seems to
require more "private" APIs, and even needs hard-coded strings containing "com.sun.xxx" package names. Yuck!

Tedi is primarily designed to be for coding, where line-wrapping is just not done!
Also, line-wrapping doesn't work with my implementation of line numbers.

I don't think prefRowCount and prefColumnCount are useful for a text editor (as opposed to a text area within a form).
It is much more likely that TediArea will be the center of a BorderPane, where its size is governed by the size of the scene.

### Additional Features

- Exposes a lineCount property
- Exposes a paragraphsProperty (should this be called linesProperty??)
- Can use a better word breaks for coding : (myTediArea.wordIterator = CodeWordBreakIterator())

## Styling TediArea

TediArea has the style classes of "text-input", "text-area" and "tedi-area".

As with TextArea, you can style ".tedi-area", ".tedi-area .content" and ".tedi-area .scroll-pane"

TediArea has similar styleable properties as TextArea, with the addition of :
- -fx-display-line-numbers (boolean)

You can also style "".tedi-area .gutter" (a Region), which is where the line numbers appear.
Note that the top padding and bottom padding of .gutter are ignored (it uses the padding of .content,
to ensure that the line numbers align with the content!)

Use .tedi-area .gutter { -fx-text-fill: xxx } to change the color of the line numbers.
You cannot change the line number's font, as it must be the same as the content's font.

I have included a style sheet as a resource in package uk.co.nickthecoder.tedi called "tedi.css".
This applies a monospaced font to .tedi-area, and styles the gutter containing the line numbers.

## Planned additional features

Possibly in the order that I'll develop them :

- Goto (go to a line number / column position)
- Search and replace, with matches highlighted.
- Create a full-featured example application, i.e. a simple text editor.
- Remove dependencies on com.sun.xxx
- Optimise for large documents. TextArea (on which this is based), is not efficient at all. See below.

## Performance

While digging through the TextArea code, I soon realised that it isn't very efficient.
The TextArea class breaks a large document into a list of StringBuffers (one per line of the document).
That's good!

However, TextAreaSkin does not do a similar trick, and instead, it has just one Text object,
which contains the whole document.
It is clear from the code that some work has been done to "fix" this, but it isn't fully
written, and therefore isn't enabled.
So it seems that for every key you type, a new String object is created containing the whole document. Eek!

Also as TextAreaSkin is implemented with a ScrollPane containing one Text object, there is no attempt
made to optimise the rendering, so that only the portion of the document that has changed is re-rendered.
It's code like this that burns through the CPU power of today's PCs without giving any extra performance
over "well written" code on crusty old hardware of yesteryear. Now get off my lawn ;-)

So it seems there's a lot of work to be done to convert TextArea into an efficient general purpose editor.
Alas, there's a good chance that I won't put in that amount of work, so Tedi will probably remain
as poorly written as TextArea. Sorry.

CodeWordBreakIterator is horribly inefficient. Sorry!

## Right-to-Left and Accessibility

I've keep code which handles right-to-left support, and accessibility support.
However, I don't understand them, nor how to use them, so I may have broken them without knowing.
Help will be appreciated; even just a confirmation on whether they do work!
