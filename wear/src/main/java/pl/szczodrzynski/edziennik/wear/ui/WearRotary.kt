/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 *
 * Bridges rotary crown input (delivered as ACTION_SCROLL / AXIS_SCROLL)
 * to the currently visible scrollable list.
 */

package pl.szczodrzynski.edziennik.wear.ui

object WearRotary {
    /**
     * Pixels to scroll per unit of AXIS_SCROLL.
     */
    const val STEP_PIXELS = 90f

    @Volatile
    private var handler: ((Float) -> Unit)? = null

    fun register(handler: (Float) -> Unit) {
        this.handler = handler
    }

    fun clear() {
        this.handler = null
    }

    fun onRotate(axisValue: Float) {
        handler?.invoke(axisValue * STEP_PIXELS)
    }
}
