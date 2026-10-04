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
package io.bazel.kotlin.builder.tasks

import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.model.PluginPhase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * The full plugins-payload parsing behavior (unknown-key/phase handling, malformed rejection,
 * field defaults) is pinned by the model's PluginsPayloadTest. This only pins that the builder-side
 * consumer routes a payload through that parser and hands back the mapped model plugins.
 */
@RunWith(JUnit4::class)
class PluginsPayloadParserTest {
  @Test
  fun `routes a payload through the model parser`() {
    val plugins =
      PluginsPayloadParser.parse(
        """
        {
          "plugins": [
            {
              "id": "plugin.test",
              "classpath": ["a.jar", "b.jar"],
              "options": [{"key": "k1", "value": "v1"}],
              "phases": ["PLUGIN_PHASE_COMPILE", "PLUGIN_PHASE_STUBS"]
            }
          ]
        }
        """.trimIndent(),
      )

    val plugin = plugins.single()
    assertThat(plugin.id).isEqualTo("plugin.test")
    assertThat(plugin.classpathList).containsExactly("a.jar", "b.jar").inOrder()
    assertThat(plugin.optionsList.map { "${it.key}=${it.value}" }).containsExactly("k1=v1")
    assertThat(plugin.phasesList)
      .containsExactly(PluginPhase.PLUGIN_PHASE_COMPILE, PluginPhase.PLUGIN_PHASE_STUBS)
      .inOrder()
  }
}
