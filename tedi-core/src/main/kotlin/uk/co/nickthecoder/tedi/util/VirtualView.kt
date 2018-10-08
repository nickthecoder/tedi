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
import uk.co.nickthecoder.tedi.TediArea

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
 * Nodes are removed from the scene graph when they move outside of the viewport.
 * Therefore very long lists render quickly (as only the visible nodes need be considered).
 * Without this class TediArea would be sluggish with 5,000+ line documents, and unbearable
 * with much larger ones.
 *
 * This makes it possible to display very long lists efficiently.
 *
 * Only the Y direction is virtual.
 *
 * This is NOT a general purpose, reusable class. It is specially designed to work
 * with TediArea. It has many quirks not found in a general-purpose virtual view.
 *
 * However, VirtualView know nothing of TediArea's internals, nor its data structures.
 */
class VirtualView<P>(
        val list: ObservableList<P>,
        val factory: VirtualFactory) : Region() {


    private val hScroll = ScrollBar()

    private val vScroll = VirtualScrollBar(this)

    /**
     * The "empty" area in the bottom right when both scroll bars are visible.
     */
    private var corner = StackPane().apply { styleClass.setAll("corner") }

    /**
     * The main content. As the scrollbar changes value, [contentGroup]'s layoutY is adjusted accordingly
     * (with a value of 0 or less).
     *
     * Its children are the same nodes as those in [contentList], which means any nodes which go out of the
     * viewport are removed, and only the visible ones remain.
     */
    private val contentGroup = Group()

    private val contentList = contentGroup.children

    var gutter: VirtualGutter? = null
        set(v) {
            // The existing gutter must have "free" called for existing gutterNodes.
            clear()
            field = v
            gutterRegion.isVisible = v != null
            needsRebuild = true
            requestLayout()
        }

    private val gutterRegion = GutterRegion()

    private val gutterList = gutterRegion.children

    private val clippedView = ClippedView(contentGroup)

    private var viewportHeight: Double = 0.0
    private var viewportWidth: Double = 0.0

    private var needsRebuild = true

    /**
     * The index of the first visible node.
     */
    private var topNodeIndex = 0

    private val bottomNodeIndex
        get() = topNodeIndex + contentList.size - 1

    /**
     * The maximum preferred width of the nodes.
     * This is a guess to calculate the h scroll bar.
     * However, it isn't correct (there may be wider nodes that haven't been created).
     * Also, when nodes are destroyed, we tend to KEEP this value, even though it may now be too big.
     */
    private var maxPrefWidth = 0.0

    private var maxGutterPrefWidth = 0.0

    private var gutterWidth = 0.0

    var standardScrolling: Boolean
        get() = vScroll.standardScrolling
        set(v) {
            vScroll.standardScrolling = v
        }

    private var ignoreVScrollChanges = false

    init {
        styleClass.add("virtual-view")
        contentGroup.styleClass.add("content")
        gutterRegion.styleClass.add("gutter")

        children.addAll(vScroll, hScroll, corner, gutterRegion, clippedView)

        clippedView.isManaged = false
        contentGroup.isManaged = false
        gutterRegion.isManaged = false
        gutterRegion.isVisible = false

        vScroll.valueProperty().addListener { _, oldValue, newValue -> vScrollChanged(oldValue.toDouble(), newValue.toDouble()) }
        hScroll.valueProperty().addListener { _, _, _ -> clippedView.clipX = hScroll.value }

        list.addListener { change: ListChangeListener.Change<out P> -> listChanged(change) }

        clippedView.addEventHandler(ScrollEvent.SCROLL) { event -> onScrollEvent(event) }
    }

    /**
     * The node for a given index of [list].
     * @return null if the required node is not visible (and therefore doesn't exist)
     */
    fun getContentNode(index: Int): Node? {
        if (index < topNodeIndex || index > bottomNodeIndex) return null
        return contentList[index - topNodeIndex]
    }

    /**
     * The gutter node for a given index of [list].
     * @return null if the required node is not visible (and therefore doesn't exist)
     */
    fun getGutterNode(index: Int): Node? {
        if (gutterList.isEmpty()) return null
        if (index < topNodeIndex || index > bottomNodeIndex) return null
        return gutterList[index - topNodeIndex]
    }

    /**
     * Removes all nodes from the contentList and from gutterList.
     */
    private fun clear() {
        contentList.clear()
        clearGutterNodes(gutterList, topNodeIndex)
    }

    /**
     * Removes the content nodes from the list, calling "free" on each
     */
    private fun clearContentNodes(list: MutableList<Node>, startIndex: Int) {
        list.forEachIndexed { index, node ->
            factory.free(index + startIndex, node)
        }
        list.clear()
    }

    /**
     * Removes a single content node, and calling "free"
     */
    private fun removeContentNode(visibleIndex: Int) {
        val removed = contentList.removeAt(visibleIndex)
        factory.free(visibleIndex + topNodeIndex, removed)
    }

    /**
     * Removes the gutter nodes from the list, calling "free" on each
     */
    private fun clearGutterNodes(list: MutableList<Node>, startIndex: Int) {
        gutter?.let { gutter ->
            list.forEachIndexed { index, node ->
                gutter.free(index + startIndex, node)
            }
        }
        list.clear()
    }

    /**
     * Removes a single gutter node, and calling "free"
     */
    private fun removeGutterNode(visibleIndex: Int) {
        val removed = gutterList.removeAt(visibleIndex)
        gutter?.free(visibleIndex + topNodeIndex, removed)
    }

    private fun listChanged(change: ListChangeListener.Change<out P>) {

        // Set if a rebuild is needed due to these changes
        var rebuild = false

        var documentChanged = false

        // Set to the index into "contentList" of the first node that needs re-jigging
        var adjustFrom = Int.MAX_VALUE

        val initialOffset = contentList.firstOrNull()?.layoutY ?: 0.0

        fun addedItems(from: Int, to: Int) {
            //println("Add $from .. $to")
            if (contentList.isEmpty() || (to - from > 1)) {
                rebuild = true
            } else {
                documentChanged = true
                if (from >= topNodeIndex && to < bottomNodeIndex) {
                    // We don't care about the offset at this stage, it will be corrected later.
                    val node = createNode(from, 0.0)
                    val index = from - topNodeIndex
                    contentList.add(index, node)
                    adjustFrom = index
                    if (gutter != null) {
                        createGutterNode(from, node, index)
                    }
                }
            }
        }

        fun removedItems(from: Int, amount: Int) {
            //println("Remove $from  ($amount) topNodeIndex=$topNodeIndex")

            documentChanged = true

            if (from < topNodeIndex || from > bottomNodeIndex) return

            val visibleFrom = Math.max(0, from - topNodeIndex)
            val visibleTo = Math.min(contentList.size - 1, from - topNodeIndex + amount)

            adjustFrom = Math.min(adjustFrom, visibleFrom)
            contentList.subList(visibleFrom, visibleTo).clear()
            if (gutter != null) {
                clearGutterNodes(gutterList.subList(visibleFrom, visibleTo), visibleFrom + topNodeIndex)
            }
        }

        fun updatedItems(from: Int, to: Int) {
            //println("Update $from .. $to")
            if (from < topNodeIndex || to > bottomNodeIndex) return

            for (i in from..to - 1) {
                if (i >= topNodeIndex && i <= bottomNodeIndex) {
                    getContentNode(i)?.let { factory.itemChanged(i, it) }
                    gutter?.let { gutter ->
                        getGutterNode(i)?.let { gutter.itemChanged(i, it) }
                    }
                    // Note, this may have changed its height, but we'll deal with that later
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
            needsRebuild = true
            requestLayout()

        } else {

            if (adjustFrom != Int.MAX_VALUE) {
                var offset = if (adjustFrom == 0) initialOffset else nodeBottom(contentList[adjustFrom - 1])

                for (i in adjustFrom..contentList.size - 1) {
                    val node = contentList[i]
                    node.layoutY = offset
                    if (gutter != null) {
                        gutterList[i].layoutY = offset
                    }
                    offset += nodeHeight(node)
                }

            }

            addTrailingNodes()
            updateScrollMaxAndVisible()

            if (documentChanged) {
                gutter?.let { gutter ->
                    gutterList.forEachIndexed { i, gutterNode ->
                        val index = topNodeIndex + i
                        gutter.documentChanged(index, gutterNode)
                    }
                }
            }
        }
    }

    private fun onScrollEvent(event: ScrollEvent) {
        if (contentList.isEmpty()) return

        if (event.deltaY != 0.0) {
            val fromPixels = event.deltaY / nodeHeight(contentList.first())
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
        if (Math.abs(diff) > contentList.size - 1) {
            // Clear and start from scratch
            rebuild()
        } else {
            adjustScroll(diff)
        }
    }

    private fun adjustScroll(delta: Double) {
        if (contentList.isEmpty()) return

        val pixels = delta * if (delta < 0) nodeHeight(contentList.first()) else nodeHeight(contentList.last())

        for (node in contentList) {
            node.layoutY -= pixels
        }
        if (gutter != null) {
            for (node in gutterList) {
                node.layoutY -= pixels
            }
        }

        addLeadingNodes()
        addTrailingNodes()
        cull()
    }

    internal fun pageUp() {
        if (contentList.isEmpty()) return

        // TODO This could be better, because it assumes that the PREVIOUS page is the
        // same height as the CURRENT page.
        vScroll.setSafeValue(vScroll.value - contentList.size)
    }

    internal fun pageDown() {
        if (contentList.isEmpty()) return

        vScroll.setSafeValue(vScroll.value + contentList.size - 1)
    }

    private fun updateScrollMaxAndVisible() {
        if (contentList.isEmpty()) {
            // Some default values, just so that nothing goes weird.
            vScroll.max = 10.0
            vScroll.visibleAmount = 1.0
            setVScrollValue(0.0)
        } else {
            vScroll.max = (list.size - contentList.size + 1).toDouble()
            if (vScroll.value > vScroll.max) {
                setVScrollValue(vScroll.max)
            }
            vScroll.visibleAmount = Math.min(1.0, contentList.size.toDouble())
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

        // If the hScroll visibility changes, we may need to add/remove some nodes
        addTrailingNodes()
        cull()

        if (needsRebuild) {
            rebuild()
        } else {
            updateScrollMaxAndVisible()
        }

        gutterWidth = if (gutterRegion.isVisible) {
            maxGutterPrefWidth + gutterRegion.snappedLeftInset() + gutterRegion.snappedRightInset()
        } else {
            0.0
        }

        layoutScrollBars()

        clippedView.resizeRelocate(gutterWidth, 0.0, viewportWidth - gutterWidth, viewportHeight)

        for (child in gutterRegion.children) {
            child.resize(maxGutterPrefWidth, nodeHeight(child))
        }
    }

    private fun layoutScrollBars() {

        val lastNode = contentList.lastOrNull()
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
                    || list.size > contentList.size
                    || list.size == contentList.size && lastNodePosition + lastNodeHeight > newViewportHeight
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
            val available = if (gutter == null) viewportWidth else viewportWidth - maxGutterPrefWidth
            hScroll.max = maxPrefWidth - available
            hScroll.visibleAmount = (available / maxPrefWidth) * hScroll.max

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
     * It is left to the caller to add it to the [contentList] list, and
     * to update [topNodeIndex] if necessary.
     */
    private fun createNode(index: Int, offset: Double): Node {
        val node = factory.createNode(index)
        val prefWidth = node.prefWidth(-1.0)
        val prefHeight = node.prefHeight(-1.0)
        node.resize(prefWidth, prefHeight)
        node.layoutY = offset

        maxPrefWidth = Math.max(maxPrefWidth, node.layoutBounds.width)
        return node
    }

    /**
     * Creates a node for a line of the gutter, and adds it to [gutterList].
     * @param index The position of the item within [list]
     * @param contentNode The corresponding node, previously createde from [createNode]
     * @param visibleIndex The index into [gutterList] (or null to add it to the end of the list)
     */
    private fun createGutterNode(index: Int, contentNode: Node, visibleIndex: Int?): Node {
        gutter?.let { gutter ->
            val node = gutter.createNode(index)
            if (visibleIndex == null) {
                gutterList.add(node)
            } else {
                gutterList.add(visibleIndex, node)
            }
            //println("Create Gutter node ${node.prefWidth(-1.0)} vs $maxGutterPrefWidth")
            val prefWidth = node.prefWidth(-1.0)
            if (prefWidth > maxGutterPrefWidth) {
                maxGutterPrefWidth = prefWidth
                println("Requesting layout")
                //layoutChildren() // TODO This is VERY bad
                requestLayout()
            }
            node.isManaged = false

            val height = nodeHeight(contentNode)
            node.resize(maxGutterPrefWidth, height)
            node.layoutY = contentNode.layoutY
            return node
        }
        throw IllegalStateException("Gutter is null")
    }

    private fun addFirstNode() {
        if (list.isEmpty()) return
        if (contentList.isNotEmpty()) throw IllegalStateException("Content List is not empty")
        if (gutterList.isNotEmpty()) throw IllegalStateException("Gutter List is not empty")

        topNodeIndex = vScroll.value.toInt()
        if (topNodeIndex >= list.size) {
            topNodeIndex = 0
        }

        val node = createNode(topNodeIndex, 0.0)
        // Adjust by the fractional part of vScroll.value
        node.layoutY = (vScroll.value - topNodeIndex) * nodeHeight(node)
        contentList.add(node)

        if (gutter != null) {
            createGutterNode(topNodeIndex, node, null)
        }
    }

    private fun addLeadingNodes() {
        if (contentList.isEmpty()) return

        val firstVisibleNode = contentList.first()
        var index = topNodeIndex - 1
        var offset = nodePosition(firstVisibleNode)
        var nextPosition = offset - nodeHeight(firstVisibleNode)

        while (index >= 0 && offset > 0) {
            val node = createNode(index, nextPosition)
            //topNodeIndex--
            contentList.add(0, node)

            if (gutter != null) {
                createGutterNode(index, node, 0)
            }

            val nodeHeight = nodeHeight(node)
            offset -= nodeHeight
            nextPosition -= nodeHeight
            index--
        }
        topNodeIndex = index + 1

        // Check if we've scrolled too far down
        val firstNode = contentList.first()
        if (topNodeIndex == 0 && (nodePosition(firstNode) > 0.0 || vScroll.value <= 0)) {
            val diff = nodePosition(firstNode)
            for (node in contentList) {
                node.layoutY -= diff
            }
            if (gutter != null) {
                for (node in gutterList) {
                    node.layoutY -= diff
                }
            }
            setVScrollValue(0.0)
        }
    }

    private fun addTrailingNodes() {
        // If contentList is empty then addLeadingNodes bailed for some reason and
        // we're hosed, so just punt
        if (contentList.isEmpty()) return

        val startNode = contentList.last()
        var offset = nodeBottom(startNode)
        var index = topNodeIndex + contentList.size

        val bottom = viewportHeight

        val isMax = contentList.size > 1 && vScroll.value >= vScroll.max

        while ((offset < bottom && index < list.size) || (isMax && topNodeIndex + contentList.size < list.size)) {
            val node = createNode(index, offset)
            contentList.add(node)

            if (gutter != null) {
                createGutterNode(index, node, null)
            }

            offset += nodeHeight(node)
            index++
        }

        // Check if we've scrolled too far up
        val lastNode = contentList.last()
        val diff = nodeBottom(lastNode) - viewportHeight
        if (topNodeIndex + contentList.size == list.size && (diff < 0.0 || vScroll.value >= vScroll.max) && !needsRebuild) {
            for (node in contentList) {
                node.layoutY -= diff
            }
            if (gutter != null) {
                for (node in gutterList) {
                    node.layoutY -= diff
                }
            }
            setVScrollValue(vScroll.max)
        }

    }

    private fun cull() {

        for (i in contentList.indices) {
            val node = contentList[i]
            if (nodeBottom(node) >= 0.0) {
                // We've found the first visible node
                repeat(i) {
                    removeContentNode(0)
                    if (gutter != null) {
                        removeGutterNode(0)
                    }
                    topNodeIndex++
                }
                break
            }
        }

        val bottom = viewportHeight
        for (i in contentList.indices.reversed()) {
            val node = contentList[i]
            if (nodeBottom(node) <= bottom) {
                // We've found the last visible node
                repeat(contentList.size - i - 2) {
                    removeContentNode(contentList.size - 1)
                    if (gutter != null) {
                        removeGutterNode(gutterList.size - 1)
                    }
                }
                break
            }
        }
    }

    fun nodePosition(node: Node?) = node?.layoutY ?: 0.0
    fun nodeHeight(node: Node?) = node?.layoutBounds?.height ?: 0.0
    fun nodeBottom(node: Node?) = if (node == null) 0.0 else node.layoutY + node.layoutBounds.height


    private inner class GutterRegion : Region() {

        // Make it public.
        public override fun getChildren() = super.getChildren()

        override fun computePrefWidth(height: Double): Double {
            return snappedLeftInset() + snappedRightInset() + maxGutterPrefWidth
        }

        // Children are being manually laid out, so do nothing here.
        override fun layoutChildren() {
            return
        }
    }
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

        val virtualFlow = VirtualView(tediArea.paragraphs, ParagraphNodeFactory())

        val scene = Scene(virtualFlow, 600.0, 400.0)

        val gutter = LineNumberGutter()

        init {
            TediArea.style(scene)
            stage.scene = scene
            with(stage) {
                title = "VirtualScroll Demo Application"
                show()
            }
            virtualFlow.gutter = gutter
            tediArea.text = "A really, really, really, really, really, really, really, long line.\n123\n456\n789\nabcde\nfghj\n" + ("extra lines\n".repeat(100))
        }

        inner class ParagraphNodeFactory : VirtualFactory {
            override fun createNode(index: Int): Node {
                val paragraph = tediArea.paragraphs[index]

                //println("Creating node # $index")
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

            override fun itemChanged(index: Int, node: Node) {
                println("itemChanged")
                val text = (node as TextFlow).children[4] as Text
                text.text = tediArea.paragraphs[index].text
            }
        }
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(VirtualViewApp::class.java, * args)
        }

    }

}
