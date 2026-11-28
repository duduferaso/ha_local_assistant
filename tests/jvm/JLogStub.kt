package io.github.duduferaso.halocalassistant.domain
object JLog { fun i(m: String) = println("JLog I $m"); fun w(m: String, t: Throwable? = null) = println("JLog W $m $t"); fun e(m: String, t: Throwable? = null) = println("JLog E $m $t") }
