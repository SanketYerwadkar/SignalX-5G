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

    enum class Mode { FIVE_G, FOUR_G }

    enum class Step {
        HANDLE_PHONE_INDEX,
        FIND_NETWORK_SPINNER,
        CHOOSE_NR_ONLY,
        SCROLL_TO_SMSC,
        REFRESH_SMSC,
        UPDATE_SMSC,
        SCROLL_TO_TOP,
        DONE
    }

    enum class Step4G {
        CLICK_SIM,
        FIND_PREFERRED_NETWORK,
        SELECT_4G_OPTION,
        DONE
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isAutomating) return

        // Safety timeout: 30 seconds
        if (System.currentTimeMillis() - autoStartTime > 30_000) {
            isAutomating = false
            return
        }

        val root = rootInActiveWindow ?: return
        val allRoots = getAllCandidateRoots(root)

        // 1. If the "Phone 0 / Phone 1" popup is open anywhere on screen, dismiss it by tapping Phone 0
        if (currentMode == Mode.FIVE_G && handlePhoneIndexPopupIfOpen(allRoots)) {
            return
        }

        if (currentMode == Mode.FIVE_G) {
            when (currentStep) {
                Step.HANDLE_PHONE_INDEX -> handlePhoneIndex(allRoots, root)
                Step.FIND_NETWORK_SPINNER -> handleFindNetworkSpinner(allRoots, root)
                Step.CHOOSE_NR_ONLY -> handleSelectNrOnly(allRoots)
                Step.SCROLL_TO_SMSC -> handleScrollToSmsc(root)
                Step.REFRESH_SMSC -> handleRefreshSmsc(root)
                Step.UPDATE_SMSC -> handleUpdateSmsc(root)
                Step.SCROLL_TO_TOP -> {}
                Step.DONE -> {}
            }
        } else {
            when (currentStep4G) {
                Step4G.CLICK_SIM -> handleClickSim(allRoots)
                Step4G.FIND_PREFERRED_NETWORK -> handleFindPreferredNetwork(allRoots)
                Step4G.SELECT_4G_OPTION -> handleSelect4GOption(allRoots)
                Step4G.DONE -> {}
            }
        }
    }

    private fun getAllCandidateRoots(fallback: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val list = mutableListOf<AccessibilityNodeInfo>()
        try {
            windows?.forEach { w ->
                w.root?.let { if (!list.contains(it)) list.add(it) }
            }
        } catch (_: Exception) {}
        if (!list.contains(fallback)) {
            list.add(fallback)
        }
        return list
    }

    /**
     * Checks if the phone index popup is open (e.g. ListView / CheckedTextView with "Phone 0").
     * If so, taps "Phone 0" to select it and advances to FIND_NETWORK_SPINNER.
     */
    private fun handlePhoneIndexPopupIfOpen(roots: List<AccessibilityNodeInfo>): Boolean {
        for (activeRoot in roots) {
            val phone0Nodes = activeRoot.findAccessibilityNodeInfosByText("Phone 0")
            for (node in phone0Nodes) {
                val isPopupItem = node.className?.contains("CheckedTextView", ignoreCase = true) == true ||
                    node.parent?.className?.contains("ListView", ignoreCase = true) == true ||
                    node.parent?.parent?.className?.contains("ListView", ignoreCase = true) == true

                if (isPopupItem) {
                    clickNode(node)
                    currentStep = Step.FIND_NETWORK_SPINNER
                    mainHandler.postDelayed({
                        rootInActiveWindow?.let { handleFindNetworkSpinner(getAllCandidateRoots(it), it) }
                    }, 400)
                    return true
                }
            }
        }
        return false
    }

    /**
     * Finds the Phone Index spinner.
     * If already "Phone 0", immediately advances to FIND_NETWORK_SPINNER.
     * If it says "Phone 1" or something else, clicks it to open the popup.
     * If no phone index spinner is present (single SIM device), advances to FIND_NETWORK_SPINNER.
     */
    private fun handlePhoneIndex(roots: List<AccessibilityNodeInfo>, fallbackRoot: AccessibilityNodeInfo) {
        var phoneSpinner: AccessibilityNodeInfo? = null

        // 1. By view ID (e.g. com.android.phone:id/phoneIndex)
        for (activeRoot in roots) {
            val idMatches = activeRoot.findAccessibilityNodeInfosByViewId("com.android.phone:id/phoneIndex")
                .ifEmpty { activeRoot.findAccessibilityNodeInfosByViewId("com.android.settings:id/phoneIndex") }
            if (idMatches.isNotEmpty()) {
                phoneSpinner = idMatches[0]
                break
            }
        }

        // 2. By searching Spinners that contain "Phone"
        if (phoneSpinner == null) {
            val spinners = mutableListOf<AccessibilityNodeInfo>()
            findAllNodesByClassName(fallbackRoot, "Spinner", spinners)
            phoneSpinner = spinners.firstOrNull { spinner ->
                val text = getAllText(spinner)
                text.contains("Phone", ignoreCase = true)
            }
        }

        if (phoneSpinner == null) {
            // Single SIM device or no phone index spinner found -> advance to network spinner
            currentStep = Step.FIND_NETWORK_SPINNER
            handleFindNetworkSpinner(roots, fallbackRoot)
            return
        }

        val spinnerText = getAllText(phoneSpinner)
        if (spinnerText.contains("Phone 0", ignoreCase = true) || spinnerText.startsWith("0")) {
            // Already Phone 0! Advance to network selection
            currentStep = Step.FIND_NETWORK_SPINNER
            handleFindNetworkSpinner(roots, fallbackRoot)
        } else {
            // Need to switch to Phone 0. Click the phone index spinner to open the popup
            clickNode(phoneSpinner)
            mainHandler.postDelayed({
                val updated = rootInActiveWindow ?: return@postDelayed
                handlePhoneIndexPopupIfOpen(getAllCandidateRoots(updated))
            }, 500)
        }
    }

    /**
     * Locates the Preferred Network Type spinner (explicitly skipping the Phone Index spinner).
     * If it is already "NR only", skips straight to SMSC.
     * Otherwise, clicks it to open the network options dropdown.
     */
    private fun handleFindNetworkSpinner(roots: List<AccessibilityNodeInfo>, fallbackRoot: AccessibilityNodeInfo) {
        var networkSpinner: AccessibilityNodeInfo? = null

        // 1. By ID
        for (activeRoot in roots) {
            val idMatches = activeRoot.findAccessibilityNodeInfosByViewId("com.android.phone:id/preferredNetworkType")
                .ifEmpty { activeRoot.findAccessibilityNodeInfosByViewId("com.android.settings:id/preferredNetworkType") }
            if (idMatches.isNotEmpty()) {
                networkSpinner = idMatches[0]
                break
            }
        }

        // 2. By Spinners
        if (networkSpinner == null) {
            val spinners = mutableListOf<AccessibilityNodeInfo>()
            findAllNodesByClassName(fallbackRoot, "Spinner", spinners)
            networkSpinner = spinners.firstOrNull { spinner ->
                val text = getAllText(spinner)
                !text.contains("Phone", ignoreCase = true) && !text.contains("Ping", ignoreCase = true)
            }
        }

        if (networkSpinner == null) {
            // Scroll down to bring the network spinner into view
            scrollDown(fallbackRoot)
            return
        }

        val spinnerText = getAllText(networkSpinner)
        if (spinnerText.equals("NR only", ignoreCase = true) || spinnerText.equals("NR Only", ignoreCase = true)) {
            // Already set to NR only! Proceed directly to SMSC
            currentStep = Step.SCROLL_TO_SMSC
            smscScrollAttempts = 0
            mainHandler.postDelayed({
                rootInActiveWindow?.let { handleScrollToSmsc(it) } ?: finishAutomation()
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
    private fun handleSelectNrOnly(roots: List<AccessibilityNodeInfo>) {
        var nrNode: AccessibilityNodeInfo? = null

        for (activeRoot in roots) {
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
                val t = getAllText(fallback)
                if (t.contains("only", ignoreCase = true)) {
                    nrNode = fallback
                    break
                }
            }
        }

        if (nrNode != null) {
            currentStep = Step.SCROLL_TO_SMSC
            smscScrollAttempts = 0
            clickNode(nrNode)

            mainHandler.postDelayed({
                val updated = rootInActiveWindow
                if (updated != null) {
                    handleScrollToSmsc(updated)
                } else {
                    startScrollToTop()
                }
            }, 700)
        } else {
            // Scroll down in the opened dropdown list
            for (activeRoot in roots) {
                val scroll = findScrollableNode(activeRoot)
                if (scroll != null) {
                    scroll.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    break
                }
            }
        }
    }

    /**
     * Scrolls down smoothly to find the SMSC section (Refresh and Update buttons).
     */
    private fun handleScrollToSmsc(root: AccessibilityNodeInfo) {
        val (refreshBtn, updateBtn) = findSmscButtons(root)

        if (refreshBtn != null) {
            currentStep = Step.REFRESH_SMSC
            handleRefreshSmsc(root)
        } else if (updateBtn != null) {
            currentStep = Step.UPDATE_SMSC
            handleUpdateSmsc(root)
        } else {
            if (smscScrollAttempts < 8) {
                smscScrollAttempts++
                scrollDown(root)
                mainHandler.postDelayed({
                    val updated = rootInActiveWindow
                    if (updated != null) {
                        handleScrollToSmsc(updated)
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
     * Finds SMSC Refresh and Update buttons by resource IDs or text.
     */
    private fun findSmscButtons(root: AccessibilityNodeInfo): Pair<AccessibilityNodeInfo?, AccessibilityNodeInfo?> {
        var refreshBtn: AccessibilityNodeInfo? = null
        var updateBtn: AccessibilityNodeInfo? = null

        // 1. By ID
        val refreshIds = root.findAccessibilityNodeInfosByViewId("com.android.phone:id/refresh_smsc")
            .ifEmpty { root.findAccessibilityNodeInfosByViewId("com.android.settings:id/refresh_smsc") }
        if (refreshIds.isNotEmpty()) refreshBtn = refreshIds[0]

        val updateIds = root.findAccessibilityNodeInfosByViewId("com.android.phone:id/update_smsc")
            .ifEmpty { root.findAccessibilityNodeInfosByViewId("com.android.settings:id/update_smsc") }
        if (updateIds.isNotEmpty()) updateBtn = updateIds[0]

        // 2. By text
        if (refreshBtn == null) {
            val rMatches = root.findAccessibilityNodeInfosByText("REFRESH")
                .ifEmpty { root.findAccessibilityNodeInfosByText("Refresh") }
            if (rMatches.isNotEmpty()) refreshBtn = rMatches[0]
        }

        if (updateBtn == null) {
            val uMatches = root.findAccessibilityNodeInfosByText("UPDATE")
                .ifEmpty { root.findAccessibilityNodeInfosByText("Update") }
            if (uMatches.isNotEmpty()) updateBtn = uMatches[0]
        }

        return Pair(refreshBtn, updateBtn)
    }

    /**
     * Taps the SMSC REFRESH button, waits for SMSC number to fetch, then proceeds to UPDATE.
     */
    private fun handleRefreshSmsc(root: AccessibilityNodeInfo) {
        val (refreshBtn, _) = findSmscButtons(root)
        if (refreshBtn != null) {
            clickNode(refreshBtn)
        }

        // Wait 700ms for network to return SMSC number into the box, then click Update
        mainHandler.postDelayed({
            currentStep = Step.UPDATE_SMSC
            val updated = rootInActiveWindow
            if (updated != null) {
                handleUpdateSmsc(updated)
            } else {
                startScrollToTop()
            }
        }, 700)
    }

    /**
     * Taps the SMSC UPDATE button to commit the SMSC, then scrolls back to top.
     */
    private fun handleUpdateSmsc(root: AccessibilityNodeInfo) {
        val (_, updateBtn) = findSmscButtons(root)
        if (updateBtn != null) {
            clickNode(updateBtn)
        }

        mainHandler.postDelayed({
            startScrollToTop()
        }, 500)
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
            val successMsg = "✓ Network set to 5G (NR only) on Phone 0 & SMSC updated!"
            try {
                Toast.makeText(applicationContext, successMsg, Toast.LENGTH_LONG).show()
            } catch (_: Exception) {}

            mainHandler.postDelayed({
                finishAutomation()
            }, 1000)
            return
        }

        if (root != null) {
            findScrollView(root)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
        }
        scrollUp()

        mainHandler.postDelayed({
            doScrollToTopStep(attempt + 1)
        }, 300)
    }

    private fun isScreenAtTop(root: AccessibilityNodeInfo): Boolean {
        val phoneIndexNodes = root.findAccessibilityNodeInfosByViewId("com.android.phone:id/phoneIndex")
            .ifEmpty { root.findAccessibilityNodeInfosByViewId("com.android.settings:id/phoneIndex") }
        if (phoneIndexNodes.isNotEmpty()) {
            val rect = Rect()
            phoneIndexNodes[0].getBoundsInScreen(rect)
            if (rect.top in 1..800) return true
        }

        val networkTypeNodes = root.findAccessibilityNodeInfosByViewId("com.android.phone:id/preferredNetworkType")
            .ifEmpty { root.findAccessibilityNodeInfosByViewId("com.android.settings:id/preferredNetworkType") }
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

    private fun scrollDown(root: AccessibilityNodeInfo) {
        findScrollView(root)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        val metrics = resources.displayMetrics
        val cx = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.70f
        val endY = metrics.heightPixels * 0.25f
        swipe(cx, startY, cx, endY, 250L)
    }

    private fun scrollUp() {
        val metrics = resources.displayMetrics
        val cx = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.25f
        val endY = metrics.heightPixels * 0.75f
        swipe(cx, startY, cx, endY, 250L)
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

        val msg = "✓ Network set to 5G (NR only) on Phone 0 & SMSC updated!"
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

    private fun handleClickSim(roots: List<AccessibilityNodeInfo>) {
        // If Preferred network type is already visible, skip this step
        for (activeRoot in roots) {
            val matches = activeRoot.findAccessibilityNodeInfosByText("Preferred network type")
                .ifEmpty { activeRoot.findAccessibilityNodeInfosByText("Network mode") }
                .ifEmpty { activeRoot.findAccessibilityNodeInfosByText("Preferred network") }
            if (matches.isNotEmpty()) {
                currentStep4G = Step4G.FIND_PREFERRED_NETWORK
                handleFindPreferredNetwork(roots)
                return
            }
        }

        // Otherwise look for SIM 1 and click it
        for (activeRoot in roots) {
            val sim1Nodes = activeRoot.findAccessibilityNodeInfosByText("SIM1")
                .ifEmpty { activeRoot.findAccessibilityNodeInfosByText("SIM 1") }
                
            for (node in sim1Nodes) {
                // Find the closest clickable parent to ensure the whole card is clicked
                var clickableNode: AccessibilityNodeInfo? = node
                while (clickableNode != null && !clickableNode.isClickable) {
                    clickableNode = clickableNode.parent
                }
                
                if (clickableNode != null) {
                    clickNode(clickableNode)
                } else {
                    clickNode(node)
                }
                
                currentStep4G = Step4G.FIND_PREFERRED_NETWORK
                return
            }
        }
    }

    private fun handleFindPreferredNetwork(roots: List<AccessibilityNodeInfo>) {
        // Look for the "Preferred network type" option in settings
        for (activeRoot in roots) {
            val matches = activeRoot.findAccessibilityNodeInfosByText("Preferred network type")
                .ifEmpty { activeRoot.findAccessibilityNodeInfosByText("Network mode") }
            
            for (node in matches) {
                clickNode(node)
                currentStep4G = Step4G.SELECT_4G_OPTION
                return
            }
        }
        
        // Also check if we are ALREADY in the popup (by looking for 4G/LTE text)
        if (is4GPopupOpen(roots)) {
            currentStep4G = Step4G.SELECT_4G_OPTION
            handleSelect4GOption(roots)
        }
    }

    private fun is4GPopupOpen(roots: List<AccessibilityNodeInfo>): Boolean {
        for (activeRoot in roots) {
            val allNodes = mutableListOf<AccessibilityNodeInfo>()
            findAllNodesWithText(activeRoot, allNodes)
            for (node in allNodes) {
                val text = getAllText(node)
                // Look for strings typical of the network options list to prevent false positives (like a SIM named "Jio 4G")
                if (text.contains("4G/3G", ignoreCase = true) || 
                    text.contains("5G/4G", ignoreCase = true) || 
                    text.contains("LTE/WCDMA", ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

    private fun handleSelect4GOption(roots: List<AccessibilityNodeInfo>) {
        for (activeRoot in roots) {
            val allNodes = mutableListOf<AccessibilityNodeInfo>()
            findAllNodesWithText(activeRoot, allNodes)
            
            for (node in allNodes) {
                val text = getAllText(node)
                // We want an option with 4G/LTE, but definitely NOT 5G or NR
                if ((text.contains("4G", ignoreCase = true) || text.contains("LTE", ignoreCase = true)) &&
                    !text.contains("5G", ignoreCase = true) && !text.contains("NR", ignoreCase = true)) {
                    
                    val isNetworkOption = node.isCheckable || 
                        node.className?.contains("RadioButton", ignoreCase = true) == true ||
                        node.className?.contains("CheckedTextView", ignoreCase = true) == true ||
                        text.contains("3G", ignoreCase = true) || 
                        text.contains("2G", ignoreCase = true) ||
                        text.contains("auto", ignoreCase = true)

                    if (isNetworkOption) {
                        clickNode(node)
                        finishAutomation4G()
                        return
                    }
                }
            }
        }
    }

    private fun findAllNodesWithText(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>) {
        if (!node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank()) {
            if (node.isClickable || node.parent?.isClickable == true || node.className?.contains("CheckedTextView", ignoreCase = true) == true) {
                 result.add(node)
            }
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findAllNodesWithText(it, result) }
        }
    }

    private fun finishAutomation4G() {
        if (currentStep4G == Step4G.DONE) return
        currentStep4G = Step4G.DONE
        isAutomating = false
        
        performGlobalAction(GLOBAL_ACTION_BACK)
        
        val msg = "✓ Network set to 4G!"
        AutomationEvents.notifySuccess(msg)
        mainHandler.postDelayed({
            try { Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show() } catch (_: Exception) {}
        }, 500)
    }

    /**
     * Clicks an accessibility node using both node actions and a direct touch gesture
     * at the center of the node's screen coordinates.
     */
    private fun clickNode(node: AccessibilityNodeInfo) {
        node.performAction(AccessibilityNodeInfo.ACTION_SELECT)
        val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        if (!clicked) {
            node.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

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
        var currentMode: Mode = Mode.FIVE_G
        var currentStep: Step = Step.HANDLE_PHONE_INDEX
        var currentStep4G: Step4G = Step4G.FIND_PREFERRED_NETWORK
        private var autoStartTime: Long = 0
        private var isScrollingToTop: Boolean = false
        private var smscScrollAttempts: Int = 0
        private val mainHandler = Handler(Looper.getMainLooper())

        fun startAutoConfigure() {
            isAutomating = true
            currentMode = Mode.FIVE_G
            currentStep = Step.HANDLE_PHONE_INDEX
            autoStartTime = System.currentTimeMillis()
            isScrollingToTop = false
            smscScrollAttempts = 0
        }

        fun startAutoConfigure4G() {
            isAutomating = true
            currentMode = Mode.FOUR_G
            currentStep4G = Step4G.CLICK_SIM
            autoStartTime = System.currentTimeMillis()
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
