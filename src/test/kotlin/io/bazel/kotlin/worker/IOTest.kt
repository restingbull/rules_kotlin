/*
 * Copyright 2020 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.bazel.kotlin.worker

import com.google.common.truth.Truth
import io.bazel.worker.IO
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

class IOTest {

  fun ByteArrayOutputStream.written() = String(toByteArray(), StandardCharsets.UTF_8)

  val stdErr = System.err
  val stdIn = BufferedInputStream(System.`in`)
  val stdOut = System.out
  val inputBuffer = ByteArrayInputStream(ByteArray(0))
  val captured = ByteArrayOutputStream()
  val outputBuffer = PrintStream(captured)

  @Before
  fun captureSystem() {
    // delegate the system defaults to capture execution information
    System.setErr(outputBuffer)
    System.setOut(outputBuffer)
    System.setIn(inputBuffer)
  }

  @After
  fun restoreSystem() {
    System.setErr(stdErr)
    System.setIn(stdIn)
    System.setOut(stdOut)
  }

  @Test
  fun capture() {
    Truth.assertThat(captured.written()).isEmpty()
    IO.capture { io ->
      println("foo foo is on the loose")
      Truth.assertThat(io.captured.written()).isEqualTo("foo foo is on the loose")
    }
    Truth.assertThat(captured.written()).isEmpty()
  }
}
