package io.kusius.letterbox.platform.gestures.converters

import androidx.compose.ui.geometry.Offset
import io.github.aakira.napier.Napier
import io.kusius.letterbox.ui.common.swipeablecard.ScrollGesture
import io.kusius.letterbox.ui.common.swipeablecard.ScrollGesture.Changed
import io.kusius.letterbox.ui.common.swipeablecard.ScrollGesture.Ended
import io.kusius.letterbox.ui.common.swipeablecard.ScrollGesture.None
import io.kusius.letterbox.ui.common.swipeablecard.ScrollGesture.Started

class MacOSGestureConverter : GestureConverter {
    override fun invoke(vararg args: Any): ScrollGesture {
        val phase = args[0] as? Int ?: return None
        val scrollX = args[1] as? Double
        val scrollY = args[2] as? Double

        return when (phase) {
            1 -> {
                Started
            }

            4 -> {
                if (scrollX != null && scrollY != null) {
                    Changed(Offset(scrollX.toFloat(), scrollY.toFloat()))
                } else {
                    None
                }
            }

            8 -> {
                Ended
            }

            else -> {
                None
            }
        }
    }
}
