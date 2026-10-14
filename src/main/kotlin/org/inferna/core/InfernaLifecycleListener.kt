package org.inferna.core

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.diagnostic.Logger

class InfernaLifecycleListener : AppLifecycleListener {
    private val logger = Logger.getInstance(InfernaLifecycleListener::class.java)

    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        logger.info("=========================================")
        logger.info(" Inferna IDE Engine Initializing...     ")
        logger.info(" Runtime: JVM 21 | Target: Biopunk OS   ")
        logger.info("=========================================")
    }

    override fun appStarted() {
        logger.info("Inferna Platform active. All subsystems dispatched.")
    }
}
