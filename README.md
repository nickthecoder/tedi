# Tedi (pronounced Teddy).

A simple text editor component suitable for embedding inside a Java/Kotlin JavaFX application.

My main usage for writing simple text files, as well as short pieces of code.
(I want to use it as a simple code editor in my
[Tickle Game Engine](https://github.com/nickthecoder/tickle)
)

I've tried looking for a suitable alternative, and the best I found is
[RichTextFX](https://github.com/FXMisc/RichTextFX).

Improvements over a simple TextArea :

- Line numbers
- Indent/un-indent multiple lines of text
- Sensible cursor movement while holding down "ctrl" (eg "foo.barThingy.bazDohicky" should be treated as three words, and jump accordingly)
- Multiple views of the same document

Improvements over RichTextFX :

- Compatible with TextArea and TextField (because TediView implement TextInputControl)
- Multiple view of the same document

Note, I don't plan on introducing syntax highlighting yet, but I may add this later.

## Progress

Early stages so far, so I recommend you look to RichTextFX for now, as it is still better than Tedi!
