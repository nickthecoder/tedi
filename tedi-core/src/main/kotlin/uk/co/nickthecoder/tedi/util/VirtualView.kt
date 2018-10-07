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

/**
 * A scrollable view, where the contents of the view are "virtual",
 * i.e. the Nodes are created only when needed.
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

    protected val vScroll = VirtualScrollBar()

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

        vScroll.valueProperty().addListener { _, _, _ ->
            adjustScroll()
        }

        clippedView.isManaged = false
        visibleNodes.isManaged = false
        standardScrolling = false
    }

    protected fun clear() {
        visibleNodes.children
        nodes.clear()
        visibleNodes.children.clear()
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
            needsRebuild = false
            clear()
            addFirstNode()
            addLeadingNodes()
            addTrailingNodes()
        }

        vScroll.max = (nodeCount - nodes.size + 1).toDouble()
        vScroll.blockIncrement = (nodes.size - 1).toDouble()
        vScroll.visibleAmount = nodes.size.toDouble()

        if (!vScroll.isVisible) {
            clippedView.layoutY = 0.0
        }

        clippedView.resizeRelocate(0.0, 0.0, viewportWidth, viewportHeight)
    }

    protected fun adjustScroll() {

        if (nodes.isEmpty()) {
            visibleNodes.layoutY = 0.0
            return
        }

        val nodeIndex = vScroll.value.toInt()

        // TODO This isn't efficient.
        // Scrolling from the start to the end (or vise versa) will build ALL nodes.
        val node = getNode(nodeIndex)
        val extra = nodeHeight(node) * (vScroll.value - nodeIndex)
        visibleNodes.layoutY = -nodePosition(node) - extra

        addTrailingNodes()
        cull()
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


    fun getNode(requiredIndex: Int): Node {

        val firstVisibleNode = nodes.first()
        var offset = nodePosition(firstVisibleNode) - nodeHeight(firstVisibleNode)

        for (index in topNodeIndex - 1 downTo requiredIndex) {
            val node = createNode(index, offset)
            nodes.add(0, node)
            offset -= nodeHeight(node)
        }

        val lastVisibleNode = nodes.last()
        offset = nodePosition(lastVisibleNode) + nodeHeight(lastVisibleNode)
        for (index in topNodeIndex + nodes.size..requiredIndex) {
            val node = createNode(index, offset)
            nodes.add(node)
            offset += nodeHeight(node)
        }

        return nodes[requiredIndex - topNodeIndex]
    }


    protected fun createNode(index: Int, offset: Double): Node {
        val node = nodeFactory(index)
        // TODO This should probably be replaced by topNodeIndex -- when called in "addLeading" and "getNode"
        if (index < topNodeIndex) {
            topNodeIndex = index
        }
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

        nodes.add(0, createNode(0, 0.0))
    }

    protected fun disposeOfNode(node: Node?) {
        node ?: return
        visibleNodes.children.remove(node)
    }

    protected fun addLeadingNodes() {
        if (nodes.isEmpty()) return

        val firstVisibleNode = nodes.first()
        var index = topNodeIndex - 1
        var offset = nodePosition(firstVisibleNode) - nodeHeight(firstVisibleNode)

        while (index >= 0 && offset > 0) {
            val node = createNode(index, offset)
            nodes.add(0, node)
            offset -= nodeHeight(node)
            index--
        }

        // Sometimes, with variable height nodes, the first node can end
        // up not at 0.0, in which case, we need to adjust all visible nodes.
        val firstNode = nodes.first()
        if (topNodeIndex == 0 && nodePosition(firstNode) != 0.0) {
            val diff = nodePosition(firstNode)
            for (node in nodes) {
                node.layoutY -= diff
            }
        }
    }

    protected fun addTrailingNodes() {
        // If nodes is empty then addLeadingNodes bailed for some reason and
        // we're hosed, so just punt
        if (nodes.isEmpty()) return

        val startNode = nodes.last()
        var offset = nodePosition(startNode) + nodeHeight(startNode)
        var index = topNodeIndex + nodes.size

        val bottom = viewportHeight - visibleNodes.layoutY

        while (offset < bottom && index < nodeCount) {
            val node = createNode(index, offset)
            nodes.add(node)
            offset += nodeHeight(node)
            index++
        }
    }

    protected fun cull() {

        val top = -visibleNodes.layoutY
        for (i in nodes.indices) {
            val node = nodes[i]
            if (nodePosition(node) + nodeHeight(node) >= top) {
                // We've found the first visible node
                repeat(i) {
                    disposeOfNode(nodes.removeFirst())
                    topNodeIndex++
                }
                break
            }
        }

        val bottom = top + viewportHeight
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

    class VirtualViewAppWindow(val stage: Stage) {

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
