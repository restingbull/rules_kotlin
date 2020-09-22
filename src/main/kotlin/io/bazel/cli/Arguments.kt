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

package io.bazel.cli

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Arguments derived from an array of strings.
 *
 * Usage:
 *   class Command(a:Arguments) {
 *     val aString by a.flag(name="string", description = "an interesting string") {
 *       first()
 *     }
 *   }
 *
 *
 *   Arguments(*args)
 *     .parseInto(::Command)
 *     .onError { exitCode ->
 *       print(help())
 *     }
 *     .execute()
 *
 */
class Arguments(
  private val arguments: List<String>,
  private val allowUnused: Boolean = false,
  private val fs: FileSystem = FileSystems.getDefault()
) {
  constructor(vararg arguments: String) : this(arguments.toList())

  private val options = mutableMapOf<String, FlagReadOnlyProperty<*>>()

  private val tasks = mutableMapOf<String, TasksReadOnlyProperty<*>>()

  /** Custom provides a fluid interface for declaring multiple value flag */
  interface Custom {
    fun <T : Any?> flag(
      name: String,
      description: String,
      default: T,
      required: Boolean = false,
      convert: ListIterator<String>.(T) -> T
    ): ReadOnlyProperty<Any?, T>
  }

  /**
   * Result of parsing Arguments.
   */
  inner class ParseResult<T>(
    val errs: Collection<String>,
    val cmd: T,
    val remaining: List<String>
  ) {
    private infix fun String.ln(n: Any): String {
      return this + (if (isNotEmpty()) "\n" else "") + n.toString()
    }

    private fun <K : Any, V : Any> Map<K, V>.ifNotEmpty(block: Map<K, V>.() -> String): String {
      if (isNotEmpty()) {
        return block()
      }
      return ""
    }

    /**
     * help provides a test representation of the valid arguments.
     */
    fun help(): String {
      return tasks
        .ifNotEmpty {
          "Tasks" ln
            values.joinToString("\n\t") { it.help() }
        } ln
        "Flags:" ln
        options.entries.joinToString("\n") { (n, o) -> "  --$n: ${o.description}" }
    }

    fun ifError(handle: ParseResult<T>.() -> Unit) {
      if (errs.isNotEmpty()) {
        handle()
      }
    }

    fun then(enact: T.(ParseResult<T>) -> Unit): T? =
      cmd.enact(this).run { if (errs.isEmpty()) cmd else null }
  }

  /** FlagReadOnlyProperty converts and stores parsed flag value(s) */
  private class FlagReadOnlyProperty<T>(
    private val convert: ListIterator<String>.(T) -> T,
    var value: T,
    val description: String,
    required: Boolean
  ) :
    ReadOnlyProperty<Any?, T> {

    private var mustParse: Boolean = !required

    fun parse(args: ListIterator<String>) {
      value = args.convert(value)
      mustParse = true
    }

    val isSatisfied: Boolean get() = mustParse

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
      return value
    }
  }

  /** TasksReadOnlyProperty parses and holds a Task if created. Only one Task may be created. */
  private class TasksReadOnlyProperty<T : Any>(val tasks: MutableMap<String, Task<T>>) :
    ReadOnlyProperty<Any?, T?> {
    private var value: T? = null

    fun create(name: String, arguments: Arguments) {
      tasks[name]?.run {
        require(value == null) {
          "cannot create $name, Task already created: $value"
        }
        value = new(arguments)
      }
    }

    fun help(): String {
      return tasks.entries.joinToString("\n") { (n, o) -> "  $n: ${o.description}" }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
      return value
    }
  }

  private data class Task<T : Any?>(val new: (Arguments) -> T, val description: String)

  /**
   * custom indicates that argument conversion will consume any number of values.
   *
   * Usage:
   *
   *  class Complicated(a:Arguments) {
   *    val tokenList by a.custom.flag("a", "list of tokens", emptyList<String>()) {
   *
   *    }
   *  }
   */
  val custom: Custom
    get() = object : Custom {
      override fun <T : Any?> flag(
        name: String,
        description: String,
        default: T,
        required: Boolean,
        convert: ListIterator<String>.(T) -> T
      ): ReadOnlyProperty<Any?, T> {
        return FlagReadOnlyProperty(
          convert = convert,
          value = default,
          description = description,
          required = required).also { options[name] = it }
      }
    }

  /**
   * flag defines a command line attribute of "--$name" populated using convert.
   *
   * @param name is used to derive the command line flag
   * @param description of the flag usage
   * @param default value for the flag
   * @param required flag must be set
   * @param convert a List<String> into expected value. Any exception is treated as a failed conversion.
   *
   * @return ReadOnlyProperty property for the value.
   */
  fun <T : Any?> flag(
    name: String,
    description: String,
    default: T,
    required: Boolean = false,
    convert: String.(T) -> T
  ): ReadOnlyProperty<Any?, T> {
    return FlagReadOnlyProperty(
      convert = { last -> if (hasNext()) next().convert(last) else error("expected argument") },
      value = default,
      description = description,
      required = required).also { options[name] = it }
  }

  /**
   * flag defines a command line attribute of "--$name" populated as a string.
   *
   * Convenience function for string flags, equivalent to flag(...) { this }
   *
   * @param name is used to derive the command line flag
   * @param description of the flag usage
   * @param default value for the flag
   * @param required flag must be set
   *
   * @return ReadOnlyProperty property for the string value.
   */
  fun flag(
    name: String,
    description: String,
    default: String,
    required: Boolean = false
  ): ReadOnlyProperty<Any?, String> {
    return FlagReadOnlyProperty(
      convert = { if (hasNext()) next() else error("expected argument") },
      value = default,
      description = description,
      required = required).also { options[name] = it }
  }

  /** Tasks defines context configuring a task (positional argument followed by flags.) */
  inner class Tasks<T : Any> {
    private val grouped = mutableMapOf<String, Task<T>>()

    /** of declares a possible task to be created on the command line. */
    fun of(
      name: String,
      description: String,
      new: (Arguments) -> T
    ) {
      grouped[name] = Task(new, description)
    }

    internal fun exportReadOnlyProperty(): ReadOnlyProperty<Any?, T?> {
      val td = TasksReadOnlyProperty(grouped)
      grouped.keys.forEach { k ->
        tasks[k] = td
      }
      return td
    }
  }

  /**
   * task provides context to define one or more related tasks.
   */
  fun <T : Any> task(define: Tasks<T>.() -> Unit): ReadOnlyProperty<Any?, T?> {
    return Tasks<T>().apply(define).exportReadOnlyProperty()
  }

  /**
   * Argument derived from a string token.
   */
  private data class Argument(
    val key: String,
    val values: MutableList<String> = mutableListOf(),
    val flag: String? = if (key.startsWith("--")) key.substring(2) else null,
    val end: Boolean = key == "--",
    val file: Boolean = key.startsWith("@")
  )

  private fun convertToFlag(arg: Argument, args: ListIterator<String>): String? {
    return options[arg.flag]
             ?.run {
               try {
                 parse(args)
                 return null
               } catch (e: Throwable) {
                 return "Failed to parse ${arg.flag}: $e"
               }
             }
           ?: if (allowUnused) null else "unexpected argument: ${arg.flag}"
  }

  /**
   * parseInto a newCommand and return a Result the parse.
   */
  fun <COMMAND> parseInto(newCommand: (Arguments) -> COMMAND): ParseResult<COMMAND> {
    val cmd = newCommand(this)
    val tokens = arguments.asSequence().flatMap { t ->
      if (t.startsWith("@")) {
        Files.readAllLines(fs.getPath(t.drop(1))).asSequence()
      } else {
        sequenceOf(t)
      }
    }.toList().listIterator()
    val (remainder, errs) = parse(tokens)
    return ParseResult(errs, cmd, remainder)
  }

  private fun parse(tokens: ListIterator<String>): Pair<List<String>, Set<String>> {
    val errs = mutableSetOf<String>()
    while (tokens.hasNext()) {
      val arg = Argument(tokens.next())
      if (arg.end) {
        break
      }
      when {
        arg.end -> return tokens.asSequence().toList() to errs
        arg.flag != null -> {
          convertToFlag(arg, ConditionalListIterator(tokens) {
            it.startsWith("--")
          })?.let(errs::add)
        }
        arg.key in tasks -> {
          runCatching {
            tasks[arg.key]?.create(arg.key, this)
          }.onFailure {
            errs.add("Unable to create Task ${arg.key}: ${it.message}")
          }
        }
        else -> {
          errs.add("unexpected argument $arg")
        }
      }
    }
    options
      .filterNot { (_, flag) -> flag.isSatisfied }
      .forEach { (name, _) ->
        errs.add("--$name is required")
      }
    return emptyList<String>() to errs
  }

  /** ConditionalListIterator reads arguments until the condition is satisfied. */
  private class ConditionalListIterator(
    private val args: ListIterator<String>,
    private val done: (String) -> Boolean
  ) : ListIterator<String> by args {
    override fun hasNext(): Boolean {
      val hasNext = args.hasNext()
      if (hasNext && done(args.next().also { args.previous() })) {
        return false
      }
      return hasNext
    }
  }
}
