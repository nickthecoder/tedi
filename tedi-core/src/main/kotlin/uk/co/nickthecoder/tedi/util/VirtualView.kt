package uk.co.nickthecoder.tedi.util

import javafx.application.Application
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.ScrollBar
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.Stage
import uk.co.nickthecoder.tedi.ParagraphList
import uk.co.nickthecoder.tedi.TediArea
import uk.co.nickthecoder.tedi.javafx.ListListenerHelper

/*
 * Before writing this class, I looked for alternatives, and found :
 * - JavaFX has VirtualFlow
 * - Flowless
 *
 * VirtualFlow isn't what I wanted. It requires a specific type of node (a Cell),
 * and fills the viewport with dummy nodes. Also, it didn't seem particularly
 * efficient. Scrolling up/down by one causes all visible nodes to be recreated.
 *
 * Flowless doesn't share these problems, but I just disliked the code.
 * It uses a weird (and unnecessary) external library (of his own creation).
 * Generics a-plenty, and is overly complex in other ways.
 *
 * I hate re-inventing the wheel, sigh, but it does let me create something specifically
 * tailored to my needs.
 */

/**
 * A scrollable view, where the contents of the view are "virtual",
 * i.e. the Nodes are created only when needed (i.e. when they are
 * visible within the viewport).
 *
 * This makes it possible to display very long lists efficiently.
 *
 * Only the Y direction is virtual.
 *
 * This is NOT a general purpose class, but specially designed to work
 * with TediArea. However, VirtualView know nothing of TediArea's
 * internals, nor its data structures.
 */
class VirtualView<P>(
        val list: ObservableList<P>,
        val nodeFactory: (Int, P) -> Node) : Region() {


    private val hScroll = ScrollBar()

    private val vScroll = VirtualScrollBar(this)

    /**
     * The "empty" area in the bottom right when both scroll bars are visible.
     */
    private var corner = StackPane().apply { styleClass.setAll("corner") }

    /**
     * The main content. As the scrollbar changes value, [visibleNodes]'s layoutY is adjusted accordingly
     * (with a value of 0 or less).
     *
     * Its children are the same nodes as those in [nodes], which means any nodes which go out of the
     * viewport are removed, and only the visible ones remain.
     */
    private val visibleNodes = Group()

    private val nodes = visibleNodes.children

    private val clippedView = ClippedView(visibleNodes)

    private var viewportHeight: Double = 0.0
    private var viewportWidth: Double = 0.0

    private var needsRebuild = true

    /**
     * The index of the first visible node.
     */
    private var topNodeIndex = 0

    private val bottomNodeIndex
        get() = topNodeIndex + nodes.size - 1

    /**
     * The maximum preferred width of the nodes.
     * This is a guess to calculate the h scroll bar.
     * However, it isn't correct (there may be wider nodes that haven't been created).
     * Also, when nodes are destroyed, we tend to KEEP this value, even though it may now be too big.
     */
    private var maxPrefWidth = 0.0

    var standardScrolling: Boolean
        get() = vScroll.standardScrolling
        set(v) {
            vScroll.standardScrolling = v
        }

    private var ignoreVScrollChanges = false

    init {
        styleClass.add("virtual-view")
        children.addAll(vScroll, hScroll, corner, clippedView)

        clippedView.isManaged = false
        visibleNodes.isManaged = false

        vScroll.valueProperty().addListener { _, oldValue, newValue -> vScrollChanged(oldValue.toDouble(), newValue.toDouble()) }
        hScroll.valueProperty().addListener { _, _, _ -> clippedView.clipX = hScroll.value }

        list.addListener { change: ListChangeListener.Change<out P> -> listChanged(change) }

        clippedView.addEventHandler(ScrollEvent.SCROLL) { event -> onScrollEvent(event) }
    }

    private fun clear() {
        nodes.clear()
    }

    private fun listChanged(change: ListChangeListener.Change<out P>) {

        // Set if a rebuild is needed due to these changes
        var rebuild = false

        // Set to the index into "nodes" of the first node that needs re-jigging
        var adjustFrom = Int.MAX_VALUE

        val initialOffset = nodes.firstOrNull()?.layoutY ?: 0.0

        fun addedItems(from: Int, to: Int) {
            println("Add $from .. $to")
            if (nodes.isEmpty() || (to - from > 1)) {
                rebuild = true
            } else {
                if (from >= topNodeIndex && to < bottomNodeIndex) {
                    // We don't care about the offset at this stage, it will be corrected later.
                    val node = createNode(from, 0.0)
                    val index = from - topNodeIndex
                    nodes.add(index, node)
                    adjustFrom = index
                }
            }
        }

        fun removedItems(from: Int, amount: Int) {
            println("Remove $from  ($amount) topNodeIndex=$topNodeIndex")

            if (from < topNodeIndex || from > bottomNodeIndex) return

            val visibleFrom = Math.max(0, from - topNodeIndex)
            val visibleTo = Math.min(nodes.size - 1, from - topNodeIndex + amount)

            adjustFrom = Math.min(adjustFrom, visibleFrom)
            nodes.subList(visibleFrom, visibleTo).clear()
        }

        fun updatedItems(from: Int, to: Int) {
            println("Update $from .. $to")
            if (from < topNodeIndex || to > bottomNodeIndex) return

            for (i in from..to - 1) {
                if (i >= topNodeIndex && i <= bottomNodeIndex) {
                    val index = i - topNodeIndex
                    // We don't care about the offset at this stage, it will be corrected later.
                    val node = createNode(i, 0.0)
                    nodes[index] = node
                }
                adjustFrom = Math.min(adjustFrom, from - topNodeIndex)
            }
        }

        while (change.next()) {
            if (change.wasRemoved()) {
                removedItems(change.from, change.removedSize)
            }
            if (change.wasAdded()) {
                addedItems(change.from, change.to)
            }
            if (change.wasUpdated()) {
                updatedItems(change.from, change.to)
            }
        }

        if (rebuild) {
            rebuild()

        } else {

            if (adjustFrom != Int.MAX_VALUE) {
                var offset = if (adjustFrom == 0) initialOffset else nodePosition(nodes[adjustFrom - 1]) + nodeHeight(nodes[adjustFrom - 1])

                for (i in adjustFrom..nodes.size - 1) {
                    val node = nodes[i]
                    node.layoutY = offset
                    offset += nodeHeight(node)
                }

                addTrailingNodes()
            }

            updateScrollMaxAndVisible()
        }
    }

    private fun onScrollEvent(event: ScrollEvent) {
        if (nodes.isEmpty()) return

        if (event.deltaY != 0.0) {
            val fromPixels = event.deltaY / nodeHeight(nodes.first())
            vScroll.setSafeValue(vScroll.value - fromPixels)
        }
        // I don't have a horizontal scroll wheel. Is this working ok?
        if (event.deltaX != 0.0) {
            hScroll.value = clamp(0.0, hScroll.value - event.deltaX, hScroll.max)
        }
        event.consume()
    }

    private fun setVScrollValue(newValue: Double) {
        ignoreVScrollChanges = true
        vScroll.value = newValue
        ignoreVScrollChanges = false
    }

    private fun vScrollChanged(oldValue: Double, newValue: Double) {
        if (ignoreVScrollChanges) return

        val diff = newValue - oldValue
        if (Math.abs(diff) > nodes.size - 1) {
            // Clear and start from scratch
            rebuild()
        } else {
            adjustScroll(diff)
        }
    }

    private fun adjustScroll(delta: Double) {
        if (nodes.isEmpty()) return

        val pixels = delta * if (delta < 0) nodeHeight(nodes.first()) else nodeHeight(nodes.last())
        for (node in nodes) {
            node.layoutY -= pixels
        }
        addLeadingNodes()
        addTrailingNodes()
        cull()
    }

    internal fun pageUp() {
        if (nodes.isEmpty()) return

        // TODO This could be better, because it assumes that the PREVIOUS page is the
        // same height as the CURRENT page.
        vScroll.setSafeValue(vScroll.value - nodes.size)
    }

    internal fun pageDown() {
        if (nodes.isEmpty()) return

        vScroll.setSafeValue(vScroll.value + nodes.size - 1)
    }

    private fun updateScrollMaxAndVisible() {
        if (nodes.isEmpty()) {
            // Some default values, just so that nothing goes weird.
            vScroll.max = 10.0
            vScroll.visibleAmount = 1.0
            setVScrollValue(0.0)
        } else {
            vScroll.max = (list.size - nodes.size + 1).toDouble()
            if (vScroll.value > vScroll.max) {
                setVScrollValue(vScroll.max)
            }
            vScroll.visibleAmount = Math.min(1.0, nodes.size.toDouble())
        }
    }

    private fun rebuild() {
        clear()
        addFirstNode()
        addLeadingNodes()
        addTrailingNodes()

        updateScrollMaxAndVisible()
        needsRebuild = false
    }

    override fun layoutChildren() {
        println("Laying out")

        val width = width
        val height = height

        if (width <= 0 || height <= 0) {
            hScroll.isVisible = false
            vScroll.isVisible = false
            corner.isVisible = false
            clear()
            return
        }

        layoutScrollBars()

        // If the hScroll visibility changes, we may need to add/remove some nodes
        addTrailingNodes()
        cull()

        if (needsRebuild) {
            rebuild()
        } else {
            updateScrollMaxAndVisible()
        }

        clippedView.resizeRelocate(0.0, 0.0, viewportWidth, viewportHeight)
    }

    private fun layoutScrollBars() {

        val lastNode = nodes.lastOrNull()
        val lastNodePosition = nodePosition(lastNode)
        val lastNodeHeight = nodeHeight(lastNode)
        var needVBar = false
        var needHBar = false

        val width = width
        val height = height

        var newViewportWidth = width
        var newViewportHeight = height

        val hBarHeight = hScroll.prefHeight(newViewportWidth)
        val vBarWidth = vScroll.prefWidth(newViewportHeight)

        for (i in 0..1) {
            needVBar = topNodeIndex > 0
                    || list.size > nodes.size
                    || list.size == nodes.size && lastNodePosition + lastNodeHeight > newViewportHeight
            needHBar = maxPrefWidth > newViewportWidth

            if (needHBar) {
                newViewportHeight = height - hBarHeight
            }
            if (needVBar) {
                newViewportWidth = width - vBarWidth
            }
        }

        viewportWidth = newViewportWidth
        viewportHeight = newViewportHeight

        hScroll.isVisible = needHBar
        vScroll.isVisible = needVBar

        if (needHBar) {
            hScroll.resizeRelocate(0.0, viewportHeight, viewportWidth, hBarHeight)
            // For example, maxPrefWidth=100, viewportWidth=80, then max = 20
            hScroll.max = maxPrefWidth - viewportWidth
            hScroll.visibleAmount = (viewportWidth / maxPrefWidth) * hScroll.max
        } else {
            clippedView.clipX = 0.0
        }
        if (needVBar) {
            vScroll.resizeRelocate(width - vBarWidth, 0.0, vBarWidth, viewportHeight)
        }

        corner.isVisible = needHBar && needVBar
        if (corner.isVisible) {
            corner.resize(vBarWidth, hBarHeight)
            corner.relocate(hScroll.layoutX + hScroll.width, vScroll.layoutY + vScroll.height)
        }
    }

    /**
     * Creates a Node.
     * It is left to the caller to add it to the [nodes] list, and
     * to update [topNodeIndex] if necessary.
     */
    private fun createNode(index: Int, offset: Double): Node {
        val node = nodeFactory(index, list[index])
        val prefWidth = node.prefWidth(-1.0)
        val prefHeight = node.prefHeight(-1.0)
        node.resizeRelocate(0.0, offset, prefWidth, prefHeight)

        maxPrefWidth = Math.max(maxPrefWidth, node.layoutBounds.width)
        return node
    }

    private fun addFirstNode() {
        if (list.isEmpty()) return
        if (nodes.isNotEmpty()) throw IllegalStateException("Nodes already exist")

        topNodeIndex = vScroll.value.toInt()
        if (topNodeIndex >= list.size) {
            topNodeIndex = 0
        }

        println("Creating topNodeIndex $topNodeIndex from scroll value ${vScroll.value}")
        val node = createNode(topNodeIndex, 0.0)
        // Adjust by the fractional part of vScroll.value
        node.layoutY = (vScroll.value - topNodeIndex) * nodeHeight(node)
        nodes.add(0, node)
    }

    private fun addLeadingNodes() {
        if (nodes.isEmpty()) return

        val firstVisibleNode = nodes.first()
        var index = topNodeIndex - 1
        var offset = nodePosition(firstVisibleNode)
        var nextPosition = offset - nodeHeight(firstVisibleNode)

        while (index >= 0 && offset > 0) {
            val node = createNode(index, nextPosition)
            //topNodeIndex--
            nodes.add(0, node)
            val nodeHeight = nodeHeight(node)
            offset -= nodeHeight
            nextPosition -= nodeHeight
            index--
        }
        topNodeIndex = index + 1

        // Check if we've scrolled too far down
        val firstNode = nodes.first()
        if (topNodeIndex == 0 && (nodePosition(firstNode) > 0.0 || vScroll.value <= 0)) {
            val diff = nodePosition(firstNode)
            for (node in nodes) {
                node.layoutY -= diff
            }
            setVScrollValue(0.0)
        }
    }

    private fun addTrailingNodes() {
        // If nodes is empty then addLeadingNodes bailed for some reason and
        // we're hosed, so just punt
        if (nodes.isEmpty()) return

        val startNode = nodes.last()
        var offset = nodePosition(startNode) + nodeHeight(startNode)
        var index = topNodeIndex + nodes.size

        val bottom = viewportHeight

        val isMax = nodes.size > 1 && vScroll.value >= vScroll.max

        while ((offset < bottom && index < list.size) || (isMax && topNodeIndex + nodes.size < list.size)) {
            val node = createNode(index, offset)
            nodes.add(node)
            offset += nodeHeight(node)
            index++
        }

        // Check if we've scrolled too far up
        val lastNode = nodes.last()
        val diff = nodePosition(lastNode) + nodeHeight(lastNode) - viewportHeight
        if (topNodeIndex + nodes.size == list.size && (diff < 0.0 || vScroll.value >= vScroll.max) && !needsRebuild) {
            for (node in nodes) {
                node.layoutY -= diff
            }
            setVScrollValue(vScroll.max)
        }

    }

    private fun cull() {

        for (i in nodes.indices) {
            val node = nodes[i]
            if (nodePosition(node) + nodeHeight(node) >= 0.0) {
                // We've found the first visible node
                repeat(i) {
                    nodes.removeFirst()
                    topNodeIndex++
                }
                break
            }
        }

        val bottom = viewportHeight
        for (i in nodes.indices.reversed()) {
            val node = nodes[i]
            if (nodePosition(node) + nodeHeight(node) <= bottom) {
                // We've found the last visible node
                repeat(nodes.size - i - 2) {
                    nodes.removeLast()
                }
                break
            }
        }
    }

    fun nodePosition(node: Node?) = node?.layoutY ?: 0.0
    fun nodeHeight(node: Node?) = node?.layoutBounds?.height ?: 0.0

}

private fun <T> MutableList<T>.removeLast() = removeAt(size - 1)
private fun <T> MutableList<T>.removeFirst() = removeAt(0)

/**
 * A demo application using a [VirtualView].
 */
class VirtualViewApp : Application() {

    override fun start(primaryStage: Stage) {
        VirtualViewAppWindow(primaryStage)
    }

    class VirtualViewAppWindow(stage: Stage) {

        val tediArea = TediArea()

        val virtualFlow = VirtualView(tediArea.paragraphs) { index, paragraph -> createNode(index, paragraph) }

        val scene = Scene(virtualFlow, 600.0, 400.0)

        var listenerHelper: ListListenerHelper<StringBuffer>? = null

        init {
            TediArea.style(scene)
            stage.scene = scene
            with(stage) {
                title = "VirtualScroll Demo Application"
                show()
            }

            tediArea.text = "A really, really, really, really, really, really, really, long line.\n123\n456\n789\nabcde\nfghj\n" + ("extra lines\n".repeat(50))
        }

        fun createNode(index: Int, paragraph: ParagraphList.Paragraph): Node {
            println("Creating node # $index")
            val add = Text(" + ")
            val sub = Text(" - ")
            val delete = Text(" X ")
            val insert = Text(" ! ")
            val text = Text(paragraph.text)

            add.onMouseClicked = EventHandler {
                val myIndex = tediArea.paragraphs.indexOf(paragraph)
                val pos = tediArea.positionOfLine(myIndex)
                tediArea.insertText(pos, ".")
            }
            sub.onMouseClicked = EventHandler {
                val myIndex = tediArea.paragraphs.indexOf(paragraph)
                val pos = tediArea.positionOfLine(myIndex)
                tediArea.replaceText(pos, pos + 1, "")
            }
            delete.onMouseClicked = EventHandler {
                val myIndex = tediArea.paragraphs.indexOf(paragraph)
                if (myIndex != 0) {
                    val pos = tediArea.positionOfLine(myIndex) - 1
                    val end = tediArea.positionOfLine(myIndex + 1) - 1
                    tediArea.replaceText(pos, end, "")
                }
            }
            insert.onMouseClicked = EventHandler {
                val myIndex = tediArea.paragraphs.indexOf(paragraph)
                tediArea.insertText(tediArea.positionOfLine(myIndex), "This is a new line\n")
            }

            return TextFlow(delete, insert, add, sub, text)
        }
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(VirtualViewApp::class.java, * args)
        }

    }
}
