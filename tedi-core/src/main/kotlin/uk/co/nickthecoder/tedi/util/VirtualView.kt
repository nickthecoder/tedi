package uk.co.nickthecoder.tedi.util

import javafx.application.Application
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.ScrollBar
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.Stage

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
open class VirtualView(val nodeFactory: (Int) -> Node) : Region() {

    /**
     * A list of [Node]s. Any of the nodes which go out of the viewport are removed,
     * and only the visible ones remain.
     */
    internal val nodes = mutableListOf<Node>()

    protected val hScroll = ScrollBar()

    protected val vScroll = VirtualScrollBar(this)

    /**
     * The "empty" area in the bottom right when both scroll bars are visible.
     */
    protected var corner = StackPane().apply { styleClass.setAll("corner") }

    /**
     * The main content. As the scrollbar changes value, [visibleNodes]'s layoutY is adjusted accordingly
     * (with a value of 0 or less).
     *
     * Its children are the same nodes as those in [nodes], which means any nodes which go out of the
     * viewport are removed, and only the visible ones remain.
     */
    protected val visibleNodes = Group()

    protected val clippedView = ClippedView(visibleNodes)

    protected var viewportHeight: Double = 0.0
    protected var viewportWidth: Double = 0.0

    protected var needsRebuild = true

    /**
     * The index of the first visible node.
     */
    protected var topNodeIndex = 0

    var nodeCount: Int = 0
        set(v) {
            val countChanged = field != v
            field = v

            if (countChanged) {
                requestLayout()
            }
        }

    /**
     * The maximum preferred width of the nodes.
     * This is a guess to calculate the h scroll bar.
     * However, it isn't correct (there may be wider nodes that haven't been created).
     * Also, when nodes are destroyed, we tend to KEEP this value, even though it may now be too big.
     */
    var maxPrefWidth = 0.0

    var standardScrolling: Boolean
        get() = vScroll.standardScrolling
        set(v) {
            vScroll.standardScrolling = v
        }

    init {
        children.addAll(vScroll, hScroll, corner, clippedView)

        hScroll.valueProperty().addListener { _, _, _ ->
            clippedView.clipX = hScroll.value
        }

        clippedView.isManaged = false
        visibleNodes.isManaged = false
        standardScrolling = false

        vScroll.valueProperty().addListener { _, oldValue, newValue -> vScrollChanged(oldValue.toDouble(), newValue.toDouble()) }
    }

    protected fun clear() {
        visibleNodes.children
        nodes.clear()
        visibleNodes.children.clear()
    }

    private var ignoreVScrollChanges = false

    fun setVScrollValue(newValue: Double) {
        ignoreVScrollChanges = true
        vScroll.value = newValue
        ignoreVScrollChanges = false
    }

    fun vScrollChanged(oldValue: Double, newValue: Double) {
        if (ignoreVScrollChanges) return

        val diff = newValue - oldValue
        if (Math.abs(diff) > nodes.size - 1) {
            // Clear and start from scratch
            rebuild()
        } else {
            adjustScroll(diff)
        }
    }

    /**
     */
    fun adjustScroll(delta: Double) {
        if (nodes.isEmpty()) return

        val pixels = delta * if (delta < 0) nodeHeight(nodes.first()) else nodeHeight(nodes.last())
        for (node in nodes) {
            node.layoutY -= pixels
        }
        addLeadingNodes()
        addTrailingNodes()
        cull()
    }

    fun pageUp() {
        if (nodes.isEmpty()) return

        // TODO This could be better, because it assumes that the PREVIOUS page is the
        // same height as the CURRENT page.
        vScroll.setSaveValue(vScroll.value - nodes.size)
    }

    fun pageDown() {
        if (nodes.isEmpty()) return

        vScroll.setSaveValue(vScroll.value + nodes.size - 1)
    }

    fun updateScrollMaxAndVisible() {
        vScroll.max = (nodeCount - nodes.size + 1).toDouble()
        vScroll.visibleAmount = nodes.size.toDouble()
    }

    protected fun rebuild() {
        needsRebuild = false
        clear()
        addFirstNode()
        addLeadingNodes()
        addTrailingNodes()
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

        if (needsRebuild) {
            rebuild()
        }
        updateScrollMaxAndVisible()

        clippedView.resizeRelocate(0.0, 0.0, viewportWidth, viewportHeight)
    }

    protected fun layoutScrollBars() {

        val nodesSize = nodes.size
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
                    || nodeCount > nodesSize
                    || nodeCount == nodesSize && lastNodePosition + lastNodeHeight > newViewportHeight
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
            hScroll.resizeRelocate(0.0, viewportHeight - hBarHeight, viewportWidth, hBarHeight)
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
     * Creates a Node, adding it to the visibleNodes.
     * It is left to the caller to add it to the [nodes] list, and
     * to update [topNodeIndex] if necessary.
     */
    protected fun createNode(index: Int, offset: Double): Node {
        val node = nodeFactory(index)
        visibleNodes.children.add(node)
        val prefWidth = node.prefWidth(-1.0)
        val prefHeight = node.prefHeight(-1.0)
        node.resizeRelocate(0.0, offset, prefWidth, prefHeight)

        maxPrefWidth = Math.max(maxPrefWidth, node.layoutBounds.width)
        return node
    }

    protected fun addFirstNode() {
        if (nodeCount == 0) return
        if (nodes.isNotEmpty()) throw IllegalStateException("Nodes already exist")

        topNodeIndex = vScroll.value.toInt()

        val node = createNode(topNodeIndex, 0.0)
        // Adjust by the fractional part of vScroll.value
        node.layoutY = (vScroll.value - topNodeIndex) * nodeHeight(node)
        nodes.add(0, node)
    }

    protected fun disposeOfNode(node: Node?) {
        node ?: return
        visibleNodes.children.remove(node)
    }

    protected fun addLeadingNodes() {
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

    protected fun addTrailingNodes() {
        // If nodes is empty then addLeadingNodes bailed for some reason and
        // we're hosed, so just punt
        if (nodes.isEmpty()) return

        val startNode = nodes.last()
        var offset = nodePosition(startNode) + nodeHeight(startNode)
        var index = topNodeIndex + nodes.size

        val bottom = viewportHeight

        val isMax = vScroll.value >= vScroll.max

        while ((offset < bottom && index < nodeCount) || (isMax && topNodeIndex + nodes.size < nodeCount)) {
            val node = createNode(index, offset)
            nodes.add(node)
            offset += nodeHeight(node)
            index++
        }

        // Check if we've scrolled too far up
        val lastNode = nodes.last()
        val diff = nodePosition(lastNode) + nodeHeight(lastNode) - viewportHeight
        if (topNodeIndex + nodes.size == nodeCount && (diff < 0.0 || vScroll.value >= vScroll.max)) {
            for (node in nodes) {
                node.layoutY -= diff
            }
            setVScrollValue(vScroll.max)
        }

    }

    protected fun cull() {

        for (i in nodes.indices) {
            val node = nodes[i]
            if (nodePosition(node) + nodeHeight(node) >= 0.0) {
                // We've found the first visible node
                repeat(i) {
                    disposeOfNode(nodes.removeFirst())
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
                    disposeOfNode(nodes.removeLast())
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

        val virtualFlow = VirtualView { index -> createNode(index) }

        val scene = Scene(virtualFlow, 600.0, 400.0)

        init {
            virtualFlow.nodeCount = 250

            stage.scene = scene
            with(stage) {
                title = "VirtualScroll Demo Application"
                show()
            }
        }

        fun createNode(index: Int): Node {
            println("Creating node # $index")
            val str = "This was node #$index abcdefg 123456789"
            val text = Text(str)
            return TextFlow(text)
        }
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(VirtualViewApp::class.java, * args)
        }

    }
}
