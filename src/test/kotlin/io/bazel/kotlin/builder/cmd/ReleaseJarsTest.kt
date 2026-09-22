/*
 * Copyright 2026 The Bazel Authors. All rights reserved.
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

package io.bazel.kotlin.builder.cmd

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReleaseJarsTest {
  @Test
  fun sha256HexMatchesKnownDigest() {
    // SHA-256 of ASCII "abc".
    assertThat(ReleaseJars.sha256Hex("abc".toByteArray(Charsets.UTF_8)))
      .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
  }

  @Test
  fun stableJarNameJoinsNameAndVersion() {
    assertThat(ReleaseJars.stableJarName("kotlin-stdlib", "2.4.20"))
      .isEqualTo("kotlin-stdlib-2.4.20.jar")
  }

  @Test
  fun parseVersionReadsKeyedLine() {
    assertThat(ReleaseJars.parseVersion("STABLE_X 1\nVERSION 2.4.20\n", "VERSION"))
      .isEqualTo("2.4.20")
  }

  @Test
  fun parseVersionThrowsWhenKeyAbsent() {
    try {
      ReleaseJars.parseVersion("STABLE_X 1\n", "VERSION")
      throw AssertionError("expected parseVersion to throw for missing key")
    } catch (e: IllegalArgumentException) {
      assertThat(e).hasMessageThat().contains("VERSION")
    }
  }

  @Test
  fun renderJarVersionBzlIsDeterministicAndSorted() {
    val rendered =
      ReleaseJars.renderJarVersionBzl(
        "2.4.20",
        mapOf(
          "b" to ReleaseJars.JarEntry("u2", "s2"),
          "a" to ReleaseJars.JarEntry("u1", "s1"),
        ),
      )
    assertThat(rendered).contains("VERSION = \"2.4.20\"")
    // Keys must be emitted in sorted order for reproducible builds.
    assertThat(rendered.indexOf("\"a\"")).isLessThan(rendered.indexOf("\"b\""))
    assertThat(rendered).contains("url = \"u1\"")
    assertThat(rendered).contains("sha256 = \"s1\"")
  }
}
