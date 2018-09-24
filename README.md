# Tedi

A simple text editor control suitable for embedding inside a JavaFX application.

Pronounced Tedi (and is short for **T**ext **edi**tor)

The main usage is for writing simple text files, and source code.

I've tried looking for a suitable alternative, and the best I found is
[RichTextFX](https://github.com/FXMisc/RichTextFX).
However, I don't like it this for a number of reasons :

- It does not inherit from TextIntputControl, and
  therefore cannot be dropped into places that currently use a TextArea.
  (It actually extends from Region, which IMHO is bad, it should be a Control).
- Isn't integrated with JavaFX in other ways - e.g. it doesn't use the same css styles for selected text
  as every other text based control.
- Doesn't seem to support multiple views of the same document (though I may be wrong).
- Doesn't support indent/un-indent (Tab and Shift+Tab), and IMHO, is useless as a code editor (my main use case).
- Doesn't support tabs as spaces (I love the tab key, and hate the tab character!)
- It is large (has 4 external dependencies)
- The code is weird.

Tedi is written in Kotlin, but should be fine for you poor folks still
working with Java ;-)

Code snippets in this document are Kotlin code, so add a few extra semi-colons,
and the odd **new** keywords as appropriate ;-)

## Progress

TediArea already does most of what I want
(with one notable exception : highlighting multiple search results).

It started out as a simple copy/paste of JavaFX's TextArea
(isn't Free software great!)

However, it spews out 5 compiler warnings due to use of "deprecated" APIs.

I've been whittling down the list, and am quietly confident I will get
rid of the rest of them!

I'm already using Tedi within a couple of my projects.

- As a script editor in my
  [Tickle Game Engine](https://github.com/nickthecoder/tickle).
- As a simple text editor in
  [ParaTask](https://github.com/nickthecoder/paratask).

I'm yet to optimise it, and therefore isn't suitable for 5,000+ line
documents. (I'm working on it!). See the "Performance" section below.

## Differences between TediArea and TextArea

Given that TediArea started as a copy of TextArea, they should be fairly
similar.

### Missing Features

- prefRowCount and prefColumnCount
- Option to wrap text
- Context menu (for copy, paste, etc)
- right-to-left text flow
- Input methods
- Accessibility

I don't think prefRowCount and prefColumnCount are useful for a
text editor (as opposed to a text area within a form).
It is much more likely that TediArea will be the center of a BorderPane,
where its size is governed by the size of the scene.

I may add word-wrapping back at some point.

The other are missing primarily due to TextArea using non-standard
(com.sun.xxx) or deprecated APIs.
Rather than spend time re-implementing features that I don't need, with
 standard APIs, I removed them.

### Additional Features

- Option to display line numbers.
- Better word breaks for coding. See SourceCodeWordIterator class.
- Indent/un-indent block of text/
- Tab key can insert spaces or tabs/
- Bundled with GUI component for find/replace and goto line if you want them.
- Easier navigation through a document, by line & column,
  character position, or pixel coordinate.
- Exposes an observable list of "Paragraphs", which can be more useful
  than only listening to chages to the "text" property.

## Styling TediArea

TediArea has the style classes of **text-input**, **text-area**
and **tedi-area**.

As with TextArea, you can style **.tedi-area**, **.tedi-area .content** and
**.tedi-area .scroll-pane**

TediArea has similar styleable properties as TextArea, with the addition of :
**-fx-display-line-numbers** (boolean)

You can also style **.tedi-area .gutter** (a Region),
which is where the line numbers appear.
Note that the top padding and bottom padding of .gutter are ignored
(it uses the padding of .content, to ensure that the line numbers
align with the content!)

e.g. Use

    .tedi-area .gutter { -fx-text-fill: xxx }

to change the color of the line numbers.
You cannot change the line number's font, as it must be the same as
the content's font.

I have included a style sheet as a resource in package
**uk.co.nickthecoder.tedi** called **tedi.css**.

You can add this to you scene using :

    TediArea.style( yourScene )

## Known Bugs/Issues

### Undo/Redo is weird.

The undo/redo feature inherited from TextInputControl isn't very good,
but because it uses private and final methods, I cannot fix it as I'd like.

Therefore I've had to "bodge" things.

TediArea has a field called undoRedo of type UndoRedo ;-)
The default value is a StandardUndoRedo, which just calls TextInputControl's
methods.

If you want better undo redo, then initialise it like so :

    myTediArea.undoRedo = BetterUndoRedo( myTediArea )

Then instead of calling methods such as :

    myTediArea.undo()

replace it with :

    myTediArea.undoRedo.undo()

Never mix and match the two calls, otherwise you may end up using two
completely different undo/redo lists!

I have added code inside BetterUndoRedo which disables the standard
undo/redo list. (undoable and redoable are always false).
However I cannot guarantee that it will disable it in future
versions of JavaFX. So just DON'T mix and match!

## Planned additional features

- Highlight all search matches
- Get rid of remaining deprecation warning messages.
- Optimise for large documents. TextArea (on which this is based), is not efficient at all. See below.
- Expose properties for current line number and column.
- Syntax highlighting

## Performance

TLDR; Don't use Tedi for large documents until I get round to optimising it.
A 10,000 line document is sluggish, but bearable. RichTextFX is noticeably quicker than Tedi.

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

I've also added to the inefficiency : CodeWordBreakIterator is horribly inefficient at the moment. Sorry!
