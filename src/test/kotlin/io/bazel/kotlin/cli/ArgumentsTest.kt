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

package io.bazel.kotlin.cli

import com.google.common.truth.Truth.assertThat
import io.bazel.cli.Arguments
import org.junit.Test
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class ArgumentsTest {

  private val tmp by lazy {
    Files.createTempDirectory(javaClass.canonicalName)
  }

  @Test
  fun flags() {
    class Forest(a: Arguments) {
      val little by a.flag("little", "frolicking animal", "rabbit")

      val surname by a.flag("surname", "surname", emptyList<String>()) {
        split(",")
      }

      val bops by a.flag("bop", "head bop count", 0) { last ->
        RuntimeException().printStackTrace()
        toInt().plus(last)
      }

      val fairy by a.flag<Any?>("fairy", "parole officer", null) {
        object {}
      }
    }

    Arguments(
      "--little", "bunny",
      "--surname", "foo,foo,foo,foo",
      "--bop", "1",
      "--bop", "1",
      "--bop", "1",
      "--bop", "1"
    ).parseInto(::Forest).then { status ->
      status.ifError {
        assertThat(errs).isEmpty()
      }
      apply {
        assertThat(little).isEqualTo("bunny")
        assertThat(surname).containsExactly("foo", "foo", "foo", "foo")
        assertThat(bops).isEqualTo(4)
        assertThat(fairy).isNull()
      }
    }
  }

  @Test
  fun customFlag() {
    class Forest(a: Arguments) {
      val locomotion by a.custom.flag(
        "loco",
        "moving through the forest",
        "wiggle"
      ) {
        asSequence().joinToString(",")
      }

      val mammal by a.flag(
        "mammal",
        "",
        "worm"
      )
    }

    Arguments("--loco", "hop", "hop", "--mammal", "bunny").parseInto(::Forest).then { status ->
      status.ifError {
        assertThat(errs).isEmpty()
      }
      apply {
        assertThat(locomotion).isEqualTo("hop,hop")
        assertThat(mammal).isEqualTo("bunny")
      }
    }
  }

  @Test
  fun required() {
    class Forest(a: Arguments) {
      val fairy by a.flag<Any?>("fairy", "mice loving (required)", "no fairy", true) {
        object {}
      }
    }

    assertThat(Arguments().parseInto(::Forest).then { status ->
      status.ifError {
        assertThat(errs).containsExactly("--fairy is required")
        assertThat(help()).isEqualTo("""
                      Flags:
                        --fairy: mice loving (required)
                      """.trimIndent()
        )
      }
    }).isNull()
  }

  @Test
  fun tasks() {
    class Bopping(a: Arguments) {
      val mouseTarget by a.flag("mouse", "poor mouse appendage", "tail")
    }

    class Forest(a: Arguments) {
      val action by a.task<Bopping> {
        of("bopping", "action", ::Bopping)
      }

      val little by a.flag("little", "suspect mammal", "fox")
    }
    Arguments(
      "--little", "bunny",
      "bopping",
      "--mouse", "head"
    ).parseInto(::Forest).then { status ->
      status.ifError {
        assertThat(errs).isEmpty()
      }
      assertThat(little).isEqualTo("bunny")
      assertThat(action).isNotNull()
      action?.apply {
        assertThat(mouseTarget).isEqualTo("head")
      }
    }
  }

  @Test
  fun expand() {
    class Forest(a: Arguments) {
      val fairy by a.flag("fairy", "peacekeeper", "anarchy")

      val bopper by a.flag("bopper", "miscreant", "big")
    }

    val params = Files.write(tmp.resolve("forest.params"),
                             listOf("--bopper", "foo foo"),
                             StandardOpenOption.CREATE_NEW)
    Arguments(
      "--fairy", "authoritarian", "@$params"
    ).parseInto(::Forest).then { status ->
      status.ifError {
        assertThat(errs).isEmpty()
      }
      apply {
        assertThat(fairy).isEqualTo("authoritarian")
        assertThat(bopper).isEqualTo("foo foo")
      }
    }
  }
}
