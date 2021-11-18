package io.github.lee0701.converter.candidates

import android.view.accessibility.AccessibilityEvent

sealed interface CandidatesWindowHider {
    fun isHideEvent(event: AccessibilityEvent): Boolean

    object Gboard: CandidatesWindowHider {
        const val PACKAGE_NAME = "com.google.android.inputmethod.latin"
        val MATCH_DESCRIPTIONS = listOf(
            "keyboard hidden",
            "키보드 숨김",
        )
        override fun isHideEvent(event: AccessibilityEvent): Boolean {
            return event.packageName == PACKAGE_NAME &&
                    MATCH_DESCRIPTIONS.any { event.contentDescription?.contains(it) == true }
        }
    }

    companion object {
        fun of(packageName: String): CandidatesWindowHider? {
            return mapOf(
                Gboard.PACKAGE_NAME to Gboard,
            )[packageName]
        }
    }
}