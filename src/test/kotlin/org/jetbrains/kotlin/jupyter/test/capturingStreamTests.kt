package org.jetbrains.kotlin.jupyter.test

import org.jetbrains.kotlin.jupyter.CapturingOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.OutputStream
import java.io.PrintStream

class CapturingStreamTests {
    private val nullOStream = object: OutputStream() {
        override fun write(b: Int) {
        }
    }

    private fun getStream(stdout: OutputStream = nullOStream,
                          maxOutputSize: Int = 1000,
                          captureOutput: Boolean = true,
                          maxBufferSize: Int = 1000,
                          maxBufferLifeTimeMs: Int = 1000,
                          onCaptured: (String) -> Unit = {}): CapturingOutputStream {

        val printStream = PrintStream(stdout, false, "UTF-8")
        return CapturingOutputStream(printStream, maxOutputSize, captureOutput,
                maxBufferSize, maxBufferLifeTimeMs, onCaptured)
    }

    @Test
    fun testMaxOutputSizeOk() {
        val s = getStream(maxOutputSize = 6)
        s.write("kotlin".toByteArray())
    }

    @Test(expected = CapturingOutputStream.OutputLimitExceededException::class)
    fun testMaxOutputSizeError() {
        val s = getStream(maxOutputSize = 3)
        s.write("java".toByteArray())
    }

    @Test
    fun testOutputCapturingFlag() {
        val contents = "abc".toByteArray()

        val s1 = getStream(captureOutput = false)
        s1.write(contents)
        assertEquals(0, s1.capturedOutput.size())

        val s2 = getStream(captureOutput = true)
        s2.write(contents)
        assertArrayEquals(contents, s2.capturedOutput.toByteArray())
    }

    @Test
    fun testMaxBufferSize() {
        val contents = "0123456789\nfortran".toByteArray()
        val expected = arrayOf("012", "345", "678", "9\n", "for", "tra", "n")

        var i = 0
        val s = getStream(maxBufferSize = 3) {
            assertEquals(expected[i], it)
            ++i
        }

        s.write(contents)
        s.flush()

        assertEquals(expected.size, i)
    }
    
    @Test
    fun testMaxBufferLifeTime() {
        val strings = arrayOf("c ", "a", "ada ", "b", "scala ", "c")
        val expected = arrayOf("c a", "ada b", "scala c")

        var i = 0
        val s = getStream(maxBufferLifeTimeMs = 1000) {
            assertEquals(expected[i], it)
            ++i
        }

        strings.forEach {
            Thread.sleep(600)
            s.write(it.toByteArray())
        }

        s.flush()

        assertEquals(expected.size, i)
    }
}