package io.bazel.kotlin.builder.utils

import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ArgMapTest {
  @Test
  fun optionalSingleIfExistsDespiteCondition() {
    val key = object : Flag {
      override val flag = "mirror mirror"
    }
    val value = listOf("on the wall")
    val args = ArgMap(mapOf(Pair(key.flag, value)))
    Truth.assertThat(args.optionalSingleIf(key) { false }).isEqualTo(value[0])
    Truth.assertThat(args.optionalSingleIf(key) { true }).isEqualTo(value[0])
  }

  @Test
  fun optionalSingleIfMandatoryOnConditionFalse() {
    val key = object : Flag {
      override val flag = "mirror mirror"
    }
    val args = ArgMap(mapOf())
    Assert.assertThrows("Option is mandatory when condition is false",
        IllegalArgumentException::class.java) {
      args.optionalSingleIf(key) { false }
    }
    Truth.assertThat(args.optionalSingleIf(key) { true }).isNull();
  }

  @Test
  fun hasAll() {
    val empty = object : Flag {
      override val flag = "pessimist"
    }
    val full = object : Flag {
      override val flag = "optimist"
    }
    val args = ArgMap(mapOf(
        Pair(empty.flag, listOf()),
        Pair(full.flag, listOf("half"))
    ))
    Truth.assertThat(args.hasAll(full)).isTrue()
    Truth.assertThat(args.hasAll(empty, full)).isFalse()
    Truth.assertThat(args.hasAll(object : Flag {
      override val flag = "immaterial"
    })).isFalse()
  }
}
