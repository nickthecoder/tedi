package uk.co.nickthecoder.tedi.util

import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.ScrollBar
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane

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


    internal val hScroll = ScrollBar()

    internal val vScroll = VirtualScrollBar(this)

    /**
     * The "empty" area in the bottom right when both scroll bars are visible.
     */
    private var corner = StackPane()

    /**
     * The main content. As the scrollbar changes value, [contentRegion]'s layoutY is adjusted accordingly
     * (with a value of 0 or less).
     *
     * Its children are the same nodes as those in [contentList], which means any nodes which go out of the
     * viewport are removed, and only the visible ones remain.
     */
    private val contentRegion = ContentRegion()

    private val contentList = contentRegion.children

    var gutter: VirtualGutter? = null
        set(v) {
            if (field === v) return

            // The existing gutter must have "free" called for existing gutterNodes.
            clear()
            field = v
            gutterRegion = GutterRegion(v)
            gutterList = gutterRegion.children

            clippedGutter.node = gutterRegion
            clippedGutter.isVisible = v != null
            reset()
        }

    internal var gutterRegion = GutterRegion(gutter)

    private var gutterList: MutableList<Node> = gutterRegion.children

    private val clippedContent = ClippedView(contentRegion)
    private val clippedGutter = ClippedView(gutterRegion)

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
        clippedContent.styleClass.add("content")
        corner.styleClass.setAll("corner")
        clippedGutter.styleClass.add("gutter-area")

        children.addAll(vScroll, hScroll, corner, clippedGutter, clippedContent)
        clippedGutter.isVisible = gutter != null

        clippedContent.isManaged = false
        contentRegion.isManaged = false
        clippedGutter.isManaged = false
        gutterRegion.isManaged = false

        vScroll.valueProperty().addListener { _, oldValue, newValue -> vScrollChanged(oldValue.toDouble(), newValue.toDouble()) }
        hScroll.valueProperty().addListener { _, _, _ -> clippedContent.clipX = hScroll.value }

        list.addListener { change: ListChangeListener.Change<out P> -> listChanged(change) }

        clippedContent.addEventHandler(ScrollEvent.SCROLL) { event -> onScrollEvent(event) }
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
     * Given a Y coordinate, returns the index within [list] that it refers to.
     * @return Ranges from -1 (above the top) to [list].size (below the bottom)
     */
    fun getListIndexAtY(y: Double): Int {
        if (contentList.isEmpty()) return -1
        if (y < nodePosition(contentList.first())) {
            return -1
        }
        contentList.forEachIndexed { index, node ->
            if (y < nodeBottom(node)) return index + topNodeIndex
        }
        return contentList.size
    }

    /**
     * Given an X coordinate relative to the whole view, returns the X coordinate relative to
     * the content nodes (including the contentRegion's left inset).
     *
     * This is the opposite of [fromContentX].
     */
    fun toContentX(x: Double) = x - gutterWidth - clippedContent.snappedLeftInset() + hScroll.value

    /**
     * Given a coordinate relative to a content node, returns the value relative to the whole view.
     *
     * This is the opposite of [toContentX].
     */
    fun fromContentX(x: Double) = x + gutterWidth + clippedContent.snappedLeftInset() - hScroll.value

    fun ensureItemVisible(index: Int) {
        if (contentList.isEmpty()) return

        if (index <= topNodeIndex) {
            scrollToItem(index)
            // TODO Ensure it is aligned with the top
        } else if (index >= bottomNodeIndex) {
            // TODO Scroll so that the bottom item is index
            // TODO Ensure it is aligned with the bottom
        }
    }

    /**
     * Rebuilds the nodes from scratch. Also resets the cached "max" values for the gutter and content.
     */
    fun reset() {
        maxPrefWidth = 0.0
        maxGutterPrefWidth = 0.0
        clear()
        fillViewport()
        requestLayout()
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
            if (contentList.isEmpty() || (to - from > 1)) {
                rebuild = true
            } else {
                documentChanged = true
                if (from >= topNodeIndex && to < bottomNodeIndex) {
                    val index = from - topNodeIndex
                    // We don't care about the offset at this stage, it will be corrected later.
                    val node = createNode(from, 0.0, index)
                    adjustFrom = index
                    if (gutter != null) {
                        createGutterNode(from, node, index)
                    }
                }
            }
        }

        fun removedItems(from: Int, amount: Int) {

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

    private fun scrollToItem(index: Int) {
        vScroll.value = index.toDouble()
    }

    private fun setVScrollValue(newValue: Double) {
        ignoreVScrollChanges = true
        vScroll.value = newValue
        ignoreVScrollChanges = false
    }

    private fun vScrollChanged(oldValue: Double, newValue: Double) {
        if (ignoreVScrollChanges || !vScroll.isVisible) return

        val diff = newValue - oldValue
        if (Math.abs(diff) > contentList.size - 1) {
            // Clear and start from scratch
            clear()
            fillViewport()
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

        fillViewport()
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

    /**
     * Update vScroll.max and vScroll.visibleAmount
     *
     * vScroll is measured in "items", so a value of 10 means the 10th item should be at the top of the viewport.
     * However, insets really complicate things, because they are measured in pixels.
     * A close-enough solution is to use the first & last visible nodes' heights to convert from pixels to "items".
     *
     * The insets cause another problem.
     *
     * Total required = list.size + topInset (in items) + bottomInset (in items)
     * Total visible = contentList.size - hiddenTop - hiddenBottom
     * Where hiddenTop is the amount the 1st visible item is away from the top edge (not the inset) (can be +ve or -ve),
     * and hiddenBottom is the amount the last visible item is below the bottom edge (but not -ve)
     */
    private fun updateScrollMaxAndVisible() {
        if (contentList.isEmpty()) {
            // Some default values, just so that nothing goes weird.
            vScroll.max = 1.0
            vScroll.visibleAmount = 1.0
            setVScrollValue(0.0)
        } else {

            val firstNode = contentList.first()
            val lastNode = contentList.last()

            // The insets measure in "items"
            val topInsetItems = clippedContent.snappedTopInset() / nodeHeight(firstNode)
            val bottomInsetItems = clippedContent.snappedBottomInset() / nodeHeight(lastNode)

            val visible = contentList.size +
                    nodePosition(firstNode) / nodeHeight(firstNode) -
                    Math.max(-bottomInsetItems, (nodeBottom(lastNode) - viewportHeight) / nodeHeight(lastNode))

            val total = list.size + topInsetItems + bottomInsetItems

            vScroll.max = total - visible

            if (vScroll.max < 0.0) {
                vScroll.max = 1.0
                vScroll.visibleAmount = 1.0
            } else {
                vScroll.visibleAmount = vScroll.max * visible / total
            }
            if (vScroll.value > vScroll.max) {
                setVScrollValue(vScroll.max)
            }

        }
    }

    private fun fillViewport() {
        if (contentList.isEmpty()) {
            addFirstNode()
        }
        addLeadingNodes()
        addTrailingNodes()
        cull()

        // Is the first node visible, and LOWER than it should be?
        val firstNode = contentList.first()
        val topDiff = nodePosition(firstNode) - clippedContent.snappedTopInset()
        if (topNodeIndex == 0 && topDiff > 0) {
            for (node in contentList) {
                node.layoutY -= topDiff
            }
            for (node in gutterList) {
                node.layoutY -= topDiff
            }
            setVScrollValue(0.0)
        }

        // Is the last node visible, and too high up? But don't do this when we are at the top of the list
        val lastNode = contentList.last()
        val bottomDiff = viewportHeight - clippedContent.snappedBottomInset() - nodeBottom(lastNode)
        if (bottomNodeIndex == list.size - 1 && bottomDiff > 0 && vScroll.value != 0.0) {
            for (node in contentList) {
                node.layoutY += bottomDiff
            }
            for (node in gutterList) {
                node.layoutY += bottomDiff
            }
            setVScrollValue(vScroll.max)
        }

        updateScrollMaxAndVisible()
        // TODO For debugging. This shouldn't happen any more!
        if (vScroll.value < 0 || vScroll.value > vScroll.max) Thread.dumpStack()
    }

    override fun layoutChildren() {
        // println("VirtualView.layoutChildren")
        val width = width
        val height = height

        if (width <= 0 || height <= 0) {
            hScroll.isVisible = false
            vScroll.isVisible = false
            corner.isVisible = false
            clear()
            return
        }

        if (needsRebuild) {
            clear()
            needsRebuild = false
        }
        // We need to create the gutter nodes, to find the gutterWidth. Only then can we decide which scroll bars
        // must be visible. But then we need to fill the viewport AGAIN, because we may need extra items
        // when the hScroll is removed.
        viewportHeight = height // (Assume hScroll is NOT visible for now)
        fillViewport()

        gutterWidth = if (clippedGutter.isVisible) {
            maxGutterPrefWidth + gutterRegion.snappedLeftInset() + gutterRegion.snappedRightInset()
        } else {
            0.0
        }

        layoutScrollBars()

        // Fill again (see comment above for why this is done twice)
        fillViewport()

        clippedContent.resizeRelocate(gutterWidth, 0.0, viewportWidth - gutterWidth, viewportHeight)
        contentRegion.resizeRelocate(0.0, 0.0, viewportWidth - gutterWidth, viewportHeight)

        if (clippedGutter.isVisible) {
            clippedGutter.resizeRelocate(0.0, 0.0, gutterWidth, viewportHeight)
            gutterRegion.resizeRelocate(0.0, 0.0, gutterWidth, viewportHeight)
            // Make all gutter nodes the correct width
            for (child in gutterRegion.children) {
                child.resize(maxGutterPrefWidth, nodeHeight(child))
            }
        }

    }

    private fun layoutScrollBars() {

        val firstNode = contentList.firstOrNull()
        val lastNode = contentList.lastOrNull()

        val width = width
        val height = height

        var needVBar = false
        var needHBar = false

        var newViewportWidth = width
        var newViewportHeight = height

        val hBarHeight = hScroll.prefHeight(newViewportWidth)
        val vBarWidth = vScroll.prefWidth(newViewportHeight)

        // We need to loop twice, because adding a scrollbar can make the other one needed as well,
        // when it previously wasn't needed.
        repeat(2) {
            // If not all nodes are visible, or ALL nodes are visible, but don't QUITE fit.
            needVBar = list.size > contentList.size || (
                    list.size == contentList.size && (
                            nodeBottom(lastNode) > newViewportHeight - clippedContent.snappedBottomInset() ||
                                    nodePosition(firstNode) < clippedContent.snappedTopInset()
                            )
                    )

            needHBar = maxPrefWidth > newViewportWidth - gutterWidth - clippedContent.snappedLeftInset() - clippedContent.snappedRightInset()

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
            val available = viewportWidth - gutterWidth - clippedContent.snappedLeftInset() - clippedContent.snappedRightInset()
            hScroll.max = maxPrefWidth - available
            hScroll.visibleAmount = (available / maxPrefWidth) * hScroll.max

        } else {
            clippedContent.clipX = 0.0
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
     * It is left to the caller to update [topNodeIndex] if necessary.
     */
    private fun createNode(index: Int, offset: Double, visibleIndex: Int?): Node {
        val node = factory.createNode(index)
        if (visibleIndex == null) {
            contentList.add(node)
        } else {
            contentList.add(visibleIndex, node)
        }

        val prefHeight = node.prefHeight(-1.0)
        node.resize(prefWidth, prefHeight)
        node.layoutY = offset
        node.layoutX = clippedContent.snappedLeftInset()

        maxPrefWidth = Math.max(maxPrefWidth, node.layoutBounds.width)
        return node
    }


    private fun positionGutterNode(node: Node, x: Double, y: Double, width: Double, height: Double) {
        node.resizeRelocate(x + gutterRegion.snappedLeftInset(), y, width, height)
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
            node.applyCss()
            if (node is Parent) node.layout()

            val prefWidth = node.prefWidth(-1.0)
            if (prefWidth > maxGutterPrefWidth) {
                maxGutterPrefWidth = prefWidth
                requestLayout()
            }
            node.isManaged = false

            val height = nodeHeight(contentNode)
            positionGutterNode(node, 0.0, contentNode.layoutY, maxGutterPrefWidth, height)
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
        val node = createNode(topNodeIndex, clippedContent.snappedTopInset(), null)
        // Adjust by the fractional part of vScroll.value
        node.layoutY += (vScroll.value - topNodeIndex) * nodeHeight(node)

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
            val node = createNode(index, nextPosition, 0)
            //topNodeIndex--

            if (gutter != null) {
                createGutterNode(index, node, 0)
            }

            val nodeHeight = nodeHeight(node)
            offset -= nodeHeight
            nextPosition -= nodeHeight
            index--
        }
        topNodeIndex = index + 1

    }

    private fun addTrailingNodes() {
        if (contentList.isEmpty()) return

        val startNode = contentList.last()
        var offset = nodeBottom(startNode)
        var index = topNodeIndex + contentList.size

        while (index < list.size && offset < viewportHeight) {
            val node = createNode(index, offset, null)

            if (gutter != null) {
                createGutterNode(index, node, null)
            }

            offset += nodeHeight(node)
            index++
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

    private fun nodePosition(node: Node?) = node?.layoutY ?: 0.0
    private fun nodeHeight(node: Node?) = node?.prefHeight(-1.0) ?: 0.0//node?.layoutBounds?.height ?: 0.0
    private fun nodeBottom(node: Node?) = if (node == null) 0.0 else node.layoutY + node.prefHeight(-1.0)//node.layoutY + node.layoutBounds.height


    private inner class ContentRegion : Region() {

        // Make it public.
        public override fun getChildren() = super.getChildren()

        // Children are being manually laid out, so do nothing here.
        override fun layoutChildren() {
            return
        }

    }

    private fun <T> MutableList<T>.removeLast() = removeAt(size - 1)
    private fun <T> MutableList<T>.removeFirst() = removeAt(0)

}
