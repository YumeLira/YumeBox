package com.github.yumelira.yumebox.presentation.icon

import androidx.compose.ui.graphics.vector.ImageVector
import com.github.yumelira.yumebox.presentation.icon.yume.Atom
import com.github.yumelira.yumebox.presentation.icon.yume.Bolt
import com.github.yumelira.yumebox.presentation.icon.yume.Chromium
import com.github.yumelira.yumebox.presentation.icon.yume.Github
import com.github.yumelira.yumebox.presentation.icon.yume.House
import com.github.yumelira.yumebox.presentation.icon.yume.Link
import com.github.yumelira.yumebox.presentation.icon.yume.Meta
import com.github.yumelira.yumebox.presentation.icon.yume.Play
import com.github.yumelira.yumebox.presentation.icon.yume.Rocket
import com.github.yumelira.yumebox.presentation.icon.yume.Sparkles
import com.github.yumelira.yumebox.presentation.icon.yume.Square
import com.github.yumelira.yumebox.presentation.icon.yume.Substore
import com.github.yumelira.yumebox.presentation.icon.yume.Zap
import com.github.yumelira.yumebox.presentation.icon.yume.Zashboard
import com.github.yumelira.yumebox.presentation.icon.yume.`Arrow-down-up`
import com.github.yumelira.yumebox.presentation.icon.yume.`Chart-column`
import com.github.yumelira.yumebox.presentation.icon.yume.`Git-merge`
import com.github.yumelira.yumebox.presentation.icon.yume.`List-chevrons-up-down`
import com.github.yumelira.yumebox.presentation.icon.yume.`Package-check`
import com.github.yumelira.yumebox.presentation.icon.yume.`Redo-dot`
import com.github.yumelira.yumebox.presentation.icon.yume.`Scan-eye`
import com.github.yumelira.yumebox.presentation.icon.yume.`Scroll-text`
import com.github.yumelira.yumebox.presentation.icon.yume.`Settings-2`
import com.github.yumelira.yumebox.presentation.icon.yume.`Squares-exclude`
import com.github.yumelira.yumebox.presentation.icon.yume.`Wifi-cog`
import kotlin.String
import kotlin.collections.List as ____KtList
import kotlin.collections.Map as ____KtMap

public object Yume

private var __AllIcons: ____KtList<ImageVector>? = null

public val Yume.AllIcons: ____KtList<ImageVector>
  get() {
    if (__AllIcons != null) {
      return __AllIcons!!
    }
    __AllIcons= listOf(`Arrow-down-up`, Atom, Bolt, `Chart-column`, Chromium, `Git-merge`, Github,
        House, Link, `List-chevrons-up-down`, Meta, `Package-check`, Play, `Redo-dot`, Rocket,
        `Scan-eye`, `Scroll-text`, `Settings-2`, Sparkles, Square, `Squares-exclude`, Substore,
        `Wifi-cog`, Zap, Zashboard)
    return __AllIcons!!
  }

private var __AllIconsNamed: ____KtMap<String, ImageVector>? = null

public val Yume.AllIconsNamed: ____KtMap<String, ImageVector>
  get() {
    if (__AllIconsNamed != null) {
      return __AllIconsNamed!!
    }
    __AllIconsNamed= mapOf("arrow-down-up" to `Arrow-down-up`, "atom" to Atom, "bolt" to Bolt,
        "chart-column" to `Chart-column`, "chromium" to Chromium, "git-merge" to `Git-merge`,
        "github" to Github, "house" to House, "link" to Link, "list-chevrons-up-down" to
        `List-chevrons-up-down`, "meta" to Meta, "package-check" to `Package-check`, "play" to Play,
        "redo-dot" to `Redo-dot`, "rocket" to Rocket, "scan-eye" to `Scan-eye`, "scroll-text" to
        `Scroll-text`, "settings-2" to `Settings-2`, "sparkles" to Sparkles, "square" to Square,
        "squares-exclude" to `Squares-exclude`, "substore" to Substore, "wifi-cog" to `Wifi-cog`,
        "zap" to Zap, "zashboard" to Zashboard)
    return __AllIconsNamed!!
  }
