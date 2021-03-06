# Tedi

A simple text editor control suitable for embedding inside a JavaFX application.

Pronounced Teddy (and is short for **T**ext **edi**tor)

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

Tedi is written in Kotlin, but will work fine with Java or any other
JVM based language.

There are a few Java classes too (simple utilities),
which are copy/pasted from JavaFX's com.sun.xxx packages.
I had to copy/paste them, so that Tedi doesn't rely on non-standard APIs
that are likely to change in future versions of JavaFX.

Code snippets in this document are Kotlin code, so add a few extra semi-colons,
and the odd **new** keywords as appropriate ;-)

![demo](screenshots/demo-kotlin.png)

There are more [screenshots](screenshots/screenshots.md) to have a look at.

Take a look at [Example.java](example/src/main/java/uk/co/nickthecoder/tedi/example/Example.java)
or the much more complicated [Demo.kt](demo/src/main/kotlin/uk/co/nickthecoder/tedi/demo/Demo.kt).
application (written in Kotlin).

## Status

TediArea does everything I want it to, and more!

I've just finished optimising TediArea for very large documents.

I've already embedded TediArea within a couple of my other projects.

- As a script editor in my
  [Tickle Game Engine](https://github.com/nickthecoder/tickle).
- As a simple text editor in
  [ParaTask](https://github.com/nickthecoder/paratask).

## Design Philosophy

TediArea should fit in with existing JavaFX controls, with few
surprises.

It uses Properties, and listeners in the same way as other JavaFX controls.

It shares the same basic design as other controls (including a Skin, and
a Behaviour).

TediArea is a type of TextInputControl (the base class for JavaFX's
TextArea and TextField), and can therefore be used wherever a
TextInputControl is expected.

TediArea should be as simple as possible. It has no additional
GUI components, not even a context menu.

For example, TediArea know's nothing of find & replace, nor
syntax highlighting.

These features can be added in a mix and match way,
allowing your application to chose which helper classes to use.

In summary, TediArea is a simple JavaFX control. The Tedi project
includes TediArea as well as additional classes that you may find
useful to use in conjunction with TediArea.

## Syntax Highlighting

Tedi comes with basic syntax highlighting for Java, Kotlin, and Groovy.
These are included only for demonstration purposes, and I do not intend
to add support for other languages.
(and if I did, I'd create a new project for them).

These syntax highlighters are based on regular expressions,
and therefore do not understand the grammar of the languages.

If you are serious about syntax highlighting, don't use regexs, use
[ANTLR](http://www.antlr.org/).
But that is beyond the scope of this project for now.

## Known Bugs/Issues

- Line wrapping is not supported. My use case doesn't require line wrapping,
  but I may it later if persuaded ;-)
- Does not handle **very** long lines efficiently.
  A huge document without line breaks will be very laggy.
  I don't plan on fixing this, so if this is an issue, I suggest you
  check for very long lines while loading, and then prompt the user to either :
  1. abort
  2. continue (and expect a bad experience)
  3. add additional line breaks.
- HighlightRanges are not included in the undo/redo list.
  This is deliberate, because highlighting is merely cosmetic,
  not an integral part of the document.
  Note, it is possible for you to add this behaviour on top of TediArea.
  - Listen for changes to the highlight ranges.
  - Filter those highlights that you care about (e.g. isinstanceof an UndoableHighlightRange interface).
  - Create your own UndoRedo.Change objects
  - Pass them to BetterUndoRedo.add().
- There is a minor "flash", when text turns from normal to highlighted.
  The highlighted text sometimes moves left/right by a pixel,
  which can be distracting. This is due to "Kerning", and the lack thereof
  between portions of text with different highlights.
  This will be more pronounced with large, fancy fonts (especially heavily slanted fonts).

### Undo/Redo is weird.

The undo/redo feature inherited from TextInputControl isn't very good,
but because it uses private and final methods, I cannot fix it as I'd like.

Therefore I've had to "bodge" things.

TediArea has a field called undoRedo of type UndoRedo ;-)
The default value is a StandardUndoRedo, which makes TediArea's undo/redo
the same as TextArea's.

If you want better undo/redo, then initialise it like so :

    myTediArea.undoRedo = BetterUndoRedo( myTediArea )

Then instead of calling methods such as :

    myTediArea.undo()

replace it with :

    myTediArea.undoRedo.undo()

Never mix and match the two, otherwise you will end up using two
completely different undo/redo lists!

I have added code inside BetterUndoRedo which disables the standard
undo/redo list. (undoable and redoable always return false).
However I cannot guarantee that it will remain disabled it in future
versions of JavaFX. So just DON'T mix and match!

I am aware that this breaks one of my goals (being a consistent sub-class
of TextInputControl), but it's the best that I can do.

## Performance

TediArea has now been optimised for large documents.
Alas there was a price to pay.
TediArea extends TextInputControl, and that class is ridiculously
inefficient. It converts the whole document to a String for the simplest
of operations (such as moving the caret forwards/backwards, deleting a
character etc).

I've replaced all of these with efficient versions, however, I had to
access private field using reflection to fix the following :

- The "length" property
- The "selection" property

I really like the idea of extending TextInputControl, but maybe the
advantages aren't enough to outweigh the disadvantages. Hmm.

## Styling TediArea

TediArea has the style classes of **text-input**, **text-area**
and **tedi-area**.

I chose to add **text-area** even though it isn't actually a TextArea,
so that a TediArea will look the same as a TextArea.

If you don't want that then :

    myTediArea.getStyleClass().remove( "text-area" )

As with TextArea, you can style :
- .tedi-area
- .tedi-area .content

TediArea has similar styleable properties as TextArea, with the addition of :
**-fx-display-line-numbers** (boolean)

You can also style **.tedi-area .gutter** (a Region),
which is where the line numbers appear.

I have included a style sheet as a resource in package
**uk.co.nickthecoder.tedi** called **tedi.css**.

You can add this to your scene using :

    TediArea.style( yourScene )

**tedi.css** also includes styles for syntax highlighting,
and the GUI components (Find & Replace toolbars, and the Go To Line dialog).

## Alternatives

Before writing Tedi, I looked for pre-existing projects, but didn't
find anything suitable.
The only JavaFX text editor I found was RichTextFX (see below).

### TextArea

The most obvious alternative is the humble TextArea.
I'm sure many projects use a TextArea as a text editor, adding bodges to
get around its shortcomings. I did, before writing Tedi!

Given that TediArea started as a copy of TextArea, they should be fairly
similar. Here are the main differences :

#### Additional Features of TediArea

- Can display line numbers.
- Highlighting text (e.g. syntax highlighting / find & replace highlights)
- Better word breaks for coding. See SourceCodeWordIterator class.
- Indent/un-indent block of text/
- Tab key can insert spaces or tabs/
- Easier navigation through a document, by line & column,
  character position, or pixel coordinate.
- Exposes an observable list of "Paragraphs", which can be more useful
  than only listening to changes to the "text" property.
- Better undo/redo.


#### Features Missing from TediArea

- Option to wrap text
- prefRowCount and prefColumnCount
- Context menu (for copy, paste, etc)
- right-to-left text flow
- Input methods
- Accessibility

I don't think prefRowCount and prefColumnCount are useful for a
text editor.

I may add word-wrapping back at some point.

The other features are missing primarily due to TextArea using non-standard
(com.sun.xxx) or deprecated APIs. Therefore these features were difficult
to copy, and I have no need for them.

### RichTextFX

If you've ruled out a simple TextArea, then the only other option I've found is
[RichTextFX](https://github.com/FXMisc/RichTextFX).

However, I don't like it for a number of reasons :

- It does not inherit from TextIntputControl, and
  therefore cannot be dropped into places that currently use a TextArea.
  (It actually extends from Region, which IMHO is bad, it should be a Control).
- Isn't integrated with JavaFX in other ways - e.g. it doesn't use the same
  css styles as other text based controls.
  It doesn't even have a **text property**.
- Doesn't seem to support multiple views of the same document
  (though I may be wrong).
- Doesn't support indent/un-indent (Tab and Shift+Tab),
  and IMHO, is useless as a source code editor.
- Doesn't support tabs as spaces
  (I love the tab key, and hate the tab character!)
- It is large (requiring 4 external dependencies)
- The code is weird. e.g. the main class has many different sub-classes.

If you want a rich text editor
(as opposed to a plain text editor with highlights),
then RichTextFX may be a better choice than Tedi.

Also, if you need line wrapping, TediArea doesn't currently support that,
so RichTextFX may be a better fit.

### Efxclipse/SmartCode

This is the source code editor used by the Eclipse IDE.

https://wiki.eclipse.org/Efxclipse/SmartCode#Sample

I'm sure it's really good, but I haven't tried it.
The instructions linked above make no sense to me, and the demo application
doesn't help either :

https://github.com/BestSolution-at/dartedit

The demo application contains over **500 files**.
I don't think I've ever seen bloat that bad before.
OMG, this is supposed to be a **demo** application.

    git clone https://github.com/BestSolution-at/dartedit.git
    cd dartedit
    find . | grep -v .git | wc

    565     570   51804

If that weren't bad enough, there isn't even any instructions on how to
compile or run the demo. There is no README, nothing but directories
till you dig at least 2 levels deep into the hierarchy.

I have a hard and fast rule : If I can't compile an application at the
command line, I don't want it. I have a hunch that it would takes days
trying to build the demo application without using the Eclipse IDE.
No, just **NO**. Don't force a particular IDE onto developers.

It's a shame, because if you could strip away all the OSGi bloat,
I'm sure it would be a perfect solution.

## License

TediArea started out as a fork of JavaFX's TextArea,
and therefore the license is the same :
GPL version 2 (with no option to move to a later version).
