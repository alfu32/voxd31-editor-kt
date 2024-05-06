package com.voxd31.gdxui

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.graphics.GL20
import org.junit.runner.notification.RunNotifier
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.FrameworkMethod
import org.mockito.Mockito.mock


class GdxTestRunner(klass: Class<*>?) : BlockJUnit4ClassRunner(klass), ApplicationListener {
    private val invokeInRender: MutableMap<FrameworkMethod, RunNotifier> = HashMap<FrameworkMethod, RunNotifier>()

    init {
        val conf: HeadlessApplicationConfiguration = HeadlessApplicationConfiguration()

        HeadlessApplication(this, conf)
        Gdx.gl = mock(GL20::class.java)
    }

    override fun create() {
    }

    override fun resume() {
    }

    override fun render() {
        synchronized(invokeInRender) {
            for ((key, value) in invokeInRender) {
                super.runChild(key, value)
            }
            invokeInRender.clear()
        }
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }

    override fun dispose() {
    }

    override fun runChild(method: FrameworkMethod, notifier: RunNotifier) {
        synchronized(invokeInRender) {
            // add for invoking in render phase, where gl context is available
            invokeInRender.put(method, notifier)
        }
        // wait until that test was invoked
        waitUntilInvokedInRenderMethod()
    }

    /**
     *
     */
    private fun waitUntilInvokedInRenderMethod() {
        try {
            var cont=true
            while (cont) {
                Thread.sleep(10)
                synchronized(invokeInRender) {
                    if (invokeInRender.isEmpty()) cont=false
                }
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}