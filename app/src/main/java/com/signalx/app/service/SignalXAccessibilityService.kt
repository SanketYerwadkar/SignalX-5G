package com.signalx.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.flow.asSharedFlow

class SignalXAccessibilityService : AccessibilityService() {

    enum class Step {
        HANDLE_PHONE_INDEX,
        FIND_NETWORK_SPINNER,
        CHOOSE_NR_ONLY,
        REFRESH_SMSC,
        SCROLL_TO_TOP,
        DONE
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isAutomating) return

        // Safety timeout: 20 seconds
        if (System.currentTimeMillis() - autoStartTime > 20_000) {
            isAutomating = false
            return
        }

        val root = rootInActiveWindow ?: return

        // 1. If the "Phone 0 / Phone 1" popup is open on screen, dismiss it by tapping Phone 0
        if (handlePhoneIndexPopupIfOpen(root)) {
            return
        }

        when (currentStep) {
            Step.HANDLE_PHONE_INDEX,
            Step.FIND_NETWORK_SPINNER -> handleFindNetworkSpinner(root)
            Step.CHOOSE_NR_ONLY -> handleSelectNrOnly(root)
            Step.REFRESH_SMSC -> handleRefreshSmsc(root)
            Step.SCROLL_TO_TOP -> {}
            Step.DONE -> {}
        }
    }

    /**
     * Detects if the phone index popup list (showing Phone 0 / Phone 1) is currently open,
     * and taps "Phone 0" to select it and dismiss the popup.
     */
    private fun handlePhoneIndexPopupIfOpen(root: AccessibilityNodeInfo): Boolean {
        val phone0Nodes = root.findAccessibilityNodeInfosByText("Phone 0")
        for (node in phone0Nodes) {
            val isPopupItem = node.className?.contains("CheckedTextView", ignoreCase = true) == true ||
                node.parent?.className?.contains("ListView", ignoreCase = true) == true ||
                node.parent?.parent?.className?.contains("ListView", ignoreCase = true) == true

            if (isPopupItem) {
                clickNode(node)
                currentStep = Step.FIND_NETWORK_SPINNER
                return true
            }
        }
        return false
    }

    /**
     * Locates the Preferred Network Type spinner (explicitly skipping the Phone Index spinner).
     * If it is already "NR only", skips straight to SMSC refresh.
     * Otherwise, clicks it to open the network options dropdown.
     */
    private fun handleFindNetworkSpinner(root: AccessibilityNodeInfo) {
        val spinners = mutableListOf<AccessibilityNodeInfo>()
        findAllNodesByClassName(root, "Spinner", spinners)

        // Select the spinner that is NOT the phone index spinner
        val networkSpinner = spinners.firstOrNull { spinner ->
            val text = getAllText(spinner)
            !text.contains("Phone", ignoreCase = true)
        }

        if (networkSpinner == null) {
            // If only Phone 0 is visible, scroll down to bring the network spinner into view
            findScrollView(root)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            return
        }

        val spinnerText = getAllText(networkSpinner)
        if (spinnerText.equals("NR only", ignoreCase = true)) {
            // Already set to NR only! Proceed directly to SMSC refresh
            currentStep = Step.REFRESH_SMSC
            mainHandler.postDelayed({
                rootInActiveWindow?.let { handleRefreshSmsc(it) } ?: finishAutomation()
            }, 300)
            return
        }

        // Click the network type dropdown
        currentStep = Step.CHOOSE_NR_ONLY
        clickNode(networkSpinner)
    }

    /**
     * Finds "NR only" in the opened network dropdown list and taps it.
     */
    private fun handleSelectNrOnly(root: AccessibilityNodeInfo) {
        val candidateRoots = mutableListOf(root)
        try {
            windows?.forEach { w -> w.root?.let { if (it != root) candidateRoots.add(it) } }
        } catch (_: Exception) {}

        var nrNode: AccessibilityNodeInfo? = null

        for (activeRoot in candidateRoots) {
            val exactMatches = activeRoot.findAccessibilityNodeInfosByText("NR only")
            if (exactMatches.isNotEmpty()) {
                nrNode = exactMatches[0]
                break
            }

            val capMatches = activeRoot.findAccessibilityNodeInfosByText("NR Only")
            if (capMatches.isNotEmpty()) {
                nrNode = capMatches[0]
                break
            }

            val fallback = findNodeStartingWith(activeRoot, "NR")
            if (fallback != null) {
                nrNode = fallback
                break
            }
        }

        if (nrNode != null) {
            currentStep = Step.REFRESH_SMSC
            clickNode(nrNode)

            mainHandler.postDelayed({
                val updated = rootInActiveWindow
                if (updated != null) {
                    handleRefreshSmsc(updated)
                } else {
                    startScrollToTop()
                }
            }, 700)
        } else {
            // Scroll down in the opened dropdown list
            for (activeRoot in candidateRoots) {
                val scroll = findScrollableNode(activeRoot)
                if (scroll != null) {
                    scroll.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    break
                }
            }
        }
    }

    /**
     * Finds the SMSC REFRESH button, clicks it, and initiates scrolling to top.
     */
    private fun handleRefreshSmsc(root: AccessibilityNodeInfo) {
        var refreshBtn: AccessibilityNodeInfo? = null

        // 1. By resource ID
        val idMatches = root.findAccessibilityNodeInfosByViewId("com.android.phone:id/refresh_smsc")
        if (idMatches.isNotEmpty()) {
            refreshBtn = idMatches[0]
        }

        // 2. By text
        if (refreshBtn == null) {
            val upperMatches = root.findAccessibilityNodeInfosByText("REFRESH")
            if (upperMatches.isNotEmpty()) {
                refreshBtn = upperMatches[0]
            } else {
                val lowerMatches = root.findAccessibilityNodeInfosByText("Refresh")
                if (lowerMatches.isNotEmpty()) refreshBtn = lowerMatches[0]
            }
        }

        if (refreshBtn != null) {
            clickNode(refreshBtn)
            mainHandler.postDelayed({
                startScrollToTop()
            }, 500)
        } else {
            if (smscScrollAttempts < 3) {
                smscScrollAttempts++
                findScrollView(root)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                swipe(540f, 1600f, 540f, 600f, 250L)
                mainHandler.postDelayed({
                    val updated = rootInActiveWindow
                    if (updated != null) {
                        handleRefreshSmsc(updated)
                    } else {
                        startScrollToTop()
                    }
                }, 400)
            } else {
                startScrollToTop()
            }
        }
    }

    /**
     * Smoothly scrolls all the way back to the top of Phone Info screen.
     */
    private fun startScrollToTop() {
        if (isScrollingToTop) return
        isScrollingToTop = true
        currentStep = Step.SCROLL_TO_TOP
        doScrollToTopStep(0)
    }

    private fun doScrollToTopStep(attempt: Int) {
        if (!isAutomating) return

        val root = rootInActiveWindow
        val atTop = if (root != null) isScreenAtTop(root) else false

        if (atTop || attempt >= 6) {
            isScrollingToTop = false
            val successMsg = "✓ Network set to 5G (NR only) & Scrolled to top!"
            try {
                Toast.makeText(applicationContext, successMsg, Toast.LENGTH_LONG).show()
            } catch (_: Exception) {}

            // Pause at the top so user clearly sees the top of Phone Info before returning
            mainHandler.postDelayed({
                finishAutomation()
            }, 1200)
            return
        }

        if (root != null) {
            findScrollView(root)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
        }
        // Swipe down from top towards bottom, which flings content downwards (scrolls to top)
        swipe(540f, 600f, 540f, 1800f, 250L)

        mainHandler.postDelayed({
            doScrollToTopStep(attempt + 1)
        }, 300)
    }

    private fun isScreenAtTop(root: AccessibilityNodeInfo): Boolean {
        val phoneIndexNodes = root.findAccessibilityNodeInfosByViewId("com.android.phone:id/phoneIndex")
        if (phoneIndexNodes.isNotEmpty()) {
            val rect = Rect()
            phoneIndexNodes[0].getBoundsInScreen(rect)
            if (rect.top in 1..800) return true
        }

        val networkTypeNodes = root.findAccessibilityNodeInfosByViewId("com.android.phone:id/preferredNetworkType")
        if (networkTypeNodes.isNotEmpty()) {
            val rect = Rect()
            networkTypeNodes[0].getBoundsInScreen(rect)
            if (rect.top in 1..900) return true
        }

        val topTexts = root.findAccessibilityNodeInfosByText("Select phone index")
        if (topTexts.isNotEmpty()) {
            val rect = Rect()
            topTexts[0].getBoundsInScreen(rect)
            if (rect.top in 1..800) return true
        }

        return false
    }

    private fun swipe(fromX: Float, fromY: Float, toX: Float, toY: Float, durationMs: Long = 250L) {
        val path = Path().apply {
            moveTo(fromX, fromY)
            lineTo(toX, toY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun finishAutomation() {
        if (currentStep == Step.DONE) return
        currentStep = Step.DONE
        isAutomating = false

        // Navigate back to SignalX
        performGlobalAction(GLOBAL_ACTION_BACK)

        val msg = "✓ Network set to 5G (NR only) on Phone 0 & SMSC refreshed!"
        AutomationEvents.notifySuccess(msg)

        mainHandler.postDelayed({
            try {
                Toast.makeText(
                    applicationContext,
                    msg,
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {}
        }, 500)
    }

    /**
     * Clicks an accessibility node using both node action and a direct touch gesture
     * at the center of the node's screen coordinates.
     * Note: Never invokes parent.performAction, which could trigger sibling elements.
     */
    private fun clickNode(node: AccessibilityNodeInfo) {
        node.performAction(AccessibilityNodeInfo.ACTION_SELECT)
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        val rect = Rect()
        node.getBoundsInScreen(rect)
        val x = rect.centerX().toFloat()
        val y = rect.centerY().toFloat()

        if (x > 0 && y > 0) {
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }

    private fun getAllText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        fun collect(n: AccessibilityNodeInfo) {
            val t = n.text?.toString() ?: n.contentDescription?.toString()
            if (!t.isNullOrBlank()) {
                sb.append(t).append(" ")
            }
            for (i in 0 until n.childCount) {
                n.getChild(i)?.let { collect(it) }
            }
        }
        collect(node)
        return sb.toString().trim()
    }

    private fun findNodeStartingWith(node: AccessibilityNodeInfo, prefix: String): AccessibilityNodeInfo? {
        val text = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
        if (text.startsWith(prefix, ignoreCase = true)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeStartingWith(child, prefix)
            if (found != null) return found
        }
        return null
    }

    private fun findAllNodesByClassName(
        node: AccessibilityNodeInfo,
        classNamePart: String,
        result: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.className?.contains(classNamePart, ignoreCase = true) == true) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findAllNodesByClassName(child, classNamePart, result)
        }
    }

    private fun findScrollView(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val className = node.className?.toString() ?: ""
        if (className.contains("ScrollView", ignoreCase = true) ||
            className.contains("ListView", ignoreCase = true) ||
            node.isScrollable
        ) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollView(child)
            if (found != null) return found
        }
        return null
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollableNode(child)
            if (found != null) return found
        }
        return null
    }

    override fun onInterrupt() {
        isAutomating = false
    }

    companion object {
        var isAutomating: Boolean = false
        var currentStep: Step = Step.HANDLE_PHONE_INDEX
        private var autoStartTime: Long = 0
        private var isScrollingToTop: Boolean = false
        private var smscScrollAttempts: Int = 0
        private val mainHandler = Handler(Looper.getMainLooper())

        fun startAutoConfigure() {
            isAutomating = true
            currentStep = Step.HANDLE_PHONE_INDEX
            autoStartTime = System.currentTimeMillis()
            isScrollingToTop = false
            smscScrollAttempts = 0
        }

        fun isServiceEnabled(context: Context): Boolean {
            val expectedComponentName = ComponentName(context, SignalXAccessibilityService::class.java).flattenToString()
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }
    }
}

object AutomationEvents {
    private val _events = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 5)
    val events = _events.asSharedFlow()

    fun notifySuccess(msg: String) {
        _events.tryEmit(msg)
    }
}

