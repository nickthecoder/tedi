# Tedi (pronounced Teddy).

A simple text editor component suitable for embedding inside a JavaFX application.

The main usage is for writing simple text files, as well as short pieces of code.
(I want to use it as a simple code editor in my
[Tickle Game Engine](https://github.com/nickthecoder/tickle)
)

I've tried looking for a suitable alternative, and the best I found is
[RichTextFX](https://github.com/FXMisc/RichTextFX).

## Progress

So far, I have replicated the functionality of TextArea.
This isn't as easy as it sounds!
I started off by copy/pasting the TextArea, TextAreaSkin and TextAreaBehaviour, changing names appropriately.
However, then the code ends up using shed loads of "private" APIs
(i.e. those in com.sun.javafx.xxx, rather than javafx.xxx).

I then made exact duplicates of about a dozen "simple" classes in the com.sun.javafx package,
such as ExpressionHelper, KeyBinding, BehaviourBase, OptionalBoolean.

However, there are some classes that cannot be copy/pasted, due to interwoven dependencies.
For example HitInfo which is implicitly bound with the (public) "Text" class.
Getting rid of those dependencies will be much harder.

So my code currently depends on the following "private" classes :

    com.sun.javafx.scene.control.skin.TextInputControlSkin
    com.sun.javafx.scene.control.skin.Utils
    com.sun.javafx.scene.input.ExtendedInputMethodRequests
    com.sun.javafx.scene.text.HitInfo
    com.sun.javafx.scene.text.TextLayout
    com.sun.javafx.tk.FontMetrics
    com.sun.javafx.tk.Toolkit

While I think JavaFX is good, the fact that writing a simple TextArea is extremely challenging
shows that the API is really lacking.

I think I'll be able to "bodge" my way round most of these, but I'm worried that some may be
near impossible to get rid of.

## Compiler Warnings

There are still lots of warnings during compilation. I do plan on removing them, but it isn't as easy as you
might expect. (Some are, and I will get to those soon!)

Many of these are due to use of "deprecated" apis. I use quotes, because they aren't deprecated.
They are in use within the regular TextArea. IMHO, you should NOT add @deprecated to an API
if the same code base is still using it!

I think these are due to java's lack of Kotlin's "internal" keyword (Kotlin really if better than Java!),
and they are abusing @deprecated to achieve a similar, but inferior result!

## Differences between TediArea and TextArea

Given that TediArea started as a copy of TextArea, they should be fairly similar.

### Missing Features

- Context menu (for copy, paste, etc)
- Option to wrap text

I chose to exclude the context menu, because it is likely that any application that embeds TediArea will
add their own context menu, with more features. Also, getting I18N (translations) of the text seems to
require more "private" APIs, and even hard-coded strings containing "com.sun.xxx" package names. Yuck!

Wrapping text is just evil, and won't work well when I implement line numbers.
Tedi is primarily designed to be for code.

### Additional Features

- Exposes a lineCount property
- Exposes a paragraphsProperty (should this be called linesProperty??)

## Styling TediArea

TediArea has the style classes of "text-area" and "tedi-area".

As with TextArea, you can style ".tedi-area", ".tedi-area .content" and ".tedi-area .scroll-pane"

TediArea has similar styleable properties as TextArea, with the addition of :
- -fx-display-line-numbers

I have included a style sheet as a resource in package uk.co.nickthecoder.tedi called "tedi.css".
Currently, this applies a monospaced font to .tedi-area, and will later be used to style the gutters
containing the line numbers.

## Paragraphs

The use of the word "paragraph" in the code took me by surprise. It turns out that a "paragraph" is actually
a line of text ending in a new-line character.
I would have called it "line" rather than "paragraph".
However, I suppose using "line" may be confusing when line-wrapping is enabled.

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
