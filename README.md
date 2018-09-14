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
