# Tedi

A simple text editor control suitable for embedding inside a JavaFX application.

Pronounced Tedi (and is short for **T**ext **edi**tor)

The main usage is for composing plain text files, especially source code.

TediArea is a JavaFX Control, very similar to a TextArea,
(but with extra features),
so hopefully, you'll get up and running with Tedi very quickly.
For simple usage, TediArea is the only class you'll need.

Tedi comes bundled with GUI components for find/replace and a
"go-to line" dialog.
The main TediArea control does not reference them,
so you'll need to write the glue to join them together.

I've included a demo application (compiled into a separate jar)
which demonstrates how to do this.

Tedi is written in Kotlin, but should be fine for you poor folks still
working with Java ;-)

There are a few Java classes too, which are copy/pasted from JavaFX's
com.sun.xxx packages.
I had to copy/paste them, so that Tedi doesn't rely on non-standard APIs
that are likely to change in future versions of JavaFX.

Code snippets in this document are Kotlin code, so add a few extra semi-colons,
and the odd **new** keywords as appropriate ;-)

## License

As much of Tedi is based off of JavaFX source code, the license is the same :
GPL version 2 (with no option to move to a later version).

## Progress

TediArea already does most of what I want, with the notable exception of
syntax highlighting, or highlighting multiple search matches.

It's still in active development (as of Sept 2018).
There is no stable release yet.

I'm currently optimising things, and I may well introduce new bugs!
Check the **todo.txt** file for details!
It's currently only as fast as TextArea, which means that 5000+ line documents
are laggy!
Run the demo, and copy/paste the License file a few times.

I'm already embedded Tedi within a couple of my other projects.

- As a script editor in my
  [Tickle Game Engine](https://github.com/nickthecoder/tickle).
- As a simple text editor in
  [ParaTask](https://github.com/nickthecoder/paratask).


## Alternatives

### TextArea

The most obvious alternative is the humble TextArea.
I'm sure many projects use a TextArea as a text editor, adding bodges to
get around its shortcomings. I did, before writing Tedi!

Given that TediArea started as a copy of TextArea, they should be fairly
similar. Here are the main differences :

#### Additional Features in Tedi

- Option to display line numbers.
- Better word breaks for coding. See SourceCodeWordIterator class.
- Indent/un-indent block of text/
- Tab key can insert spaces or tabs/
- Easier navigation through a document, by line & column,
  character position, or pixel coordinate.
- Exposes an observable list of "Paragraphs", which can be more useful
  than only listening to chages to the "text" property.


#### Features Missing from Tedi

- Option to wrap text
- prefRowCount and prefColumnCount
- Context menu (for copy, paste, etc)
- right-to-left text flow
- Input methods
- Accessibility

I don't think prefRowCount and prefColumnCount are useful for a
text editor (as opposed to a text area within a form).
It is much more likely that TediArea will be the center of a BorderPane,
where its size is governed by the size of the scene.

I may add word-wrapping back at some point.

The other features are missing primarily due to TextArea using non-standard
(com.sun.xxx) or deprecated APIs. Therefore these features were difficult
to copy.

### RichTextFX

If you've ruled out a simple TextArea, then the only other option I've found is
[RichTextFX](https://github.com/FXMisc/RichTextFX).

However, I don't like it this for a number of reasons :

- It does not inherit from TextIntputControl, and
  therefore cannot be dropped into places that currently use a TextArea.
  (It actually extends from Region, which IMHO is bad, it should be a Control).
- Isn't integrated with JavaFX in other ways - e.g. it doesn't use the same css styles for selected text
  as every other text based control.
  It even have a **text property**, like other text based controls.
- Doesn't seem to support multiple views of the same document
  (though I may be wrong).
- Doesn't support indent/un-indent (Tab and Shift+Tab),
  and IMHO, is useless as a source code editor.
- Doesn't support tabs as spaces
  (I love the tab key, and hate the tab character!)
- It is large (requiring 4 external dependencies)
- The code is weird. e.g. the main class has many different sub-classes.

If you want multiple text styles within your document, RichTextFX is
probably for you.

As of Sept 2018, RichTextFX is significantly faster than Tedi, and
handles 10,000+ line documents with ease.

## Styling TediArea

TediArea has the style classes of **text-input**, **text-area**
and **tedi-area**.

I chose to ass **text-area** even though it isn't actually a TextArea,
so that a TediArea will look the same as a TextArea.

If you don't want that then :

    myTediArea.getStyleClass().remove( "text-area" )

As with TextArea, you can style **.tedi-area**, **.tedi-area .content** and
**.tedi-area .scroll-pane**

TediArea has similar styleable properties as TextArea, with the addition of :
**-fx-display-line-numbers** (boolean)

You can also style **.tedi-area .gutter** (a Region),
which is where the line numbers appear.

e.g. To change the color of the line numbers :

    .tedi-area .gutter { -fx-text-fill: xxx }

Note, for correct alignment, the top padding of .gutter
must be the same as .content

I have included a style sheet as a resource in package
**uk.co.nickthecoder.tedi** called **tedi.css**.

You can add this to your scene using :

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

- Add support for syntax highlighting
- Highlight all search matches
- Optimise for large documents. TextArea (on which this is based),
  is not efficient at all. See below.

## Performance

Don't use Tedi for large documents until I finish optimising it.
A 10,000 line document is sluggish, but bearable. RichTextFX is noticeably
quicker than Tedi.
