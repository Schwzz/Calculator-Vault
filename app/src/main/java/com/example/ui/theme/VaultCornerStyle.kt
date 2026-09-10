package com.example.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Vault UI Corner & Shape Customization options:
 * - ROUNDED: Pill / fully rounded corners
 * - SOFT: 16.dp rounded corners (Material default)
 * - SHARP: 4.dp crisp slightly-rounded corners
 * - CUT_CORNER: 12.dp modern chamfered / cut corners
 */
enum class VaultCornerStyle(
    val id: String,
    val title: String,
    val subtitle: String,
    val cardRadiusDp: Int,
    val buttonRadiusDp: Int
) {
    ROUNDED("rounded", "Rounded", "Pill & fully rounded", 24, 24),
    SOFT("soft", "Soft (Default)", "16dp smooth corners", 16, 12),
    SHARP("sharp", "Sharp", "4dp crisp corners", 4, 4),
    CUT_CORNER("cut_corner", "Cut Corner", "12dp angled chamfer", 12, 10);

    val cardShape: Shape
        get() = when (this) {
            ROUNDED -> RoundedCornerShape(cardRadiusDp.dp)
            SOFT -> RoundedCornerShape(cardRadiusDp.dp)
            SHARP -> RoundedCornerShape(cardRadiusDp.dp)
            CUT_CORNER -> CutCornerShape(cardRadiusDp.dp)
        }

    val buttonShape: Shape
        get() = when (this) {
            ROUNDED -> RoundedCornerShape(buttonRadiusDp.dp)
            SOFT -> RoundedCornerShape(buttonRadiusDp.dp)
            SHARP -> RoundedCornerShape(buttonRadiusDp.dp)
            CUT_CORNER -> CutCornerShape(buttonRadiusDp.dp)
        }

    val dialogShape: Shape
        get() = when (this) {
            ROUNDED -> RoundedCornerShape(24.dp)
            SOFT -> RoundedCornerShape(16.dp)
            SHARP -> RoundedCornerShape(4.dp)
            CUT_CORNER -> CutCornerShape(14.dp)
        }

    val fabShape: Shape
        get() = when (this) {
            ROUNDED -> RoundedCornerShape(28.dp)
            SOFT -> RoundedCornerShape(16.dp)
            SHARP -> RoundedCornerShape(4.dp)
            CUT_CORNER -> CutCornerShape(14.dp)
        }

    fun toMaterialShapes(): Shapes {
        return when (this) {
            ROUNDED -> Shapes(
                extraSmall = RoundedCornerShape(12.dp),
                small = RoundedCornerShape(16.dp),
                medium = RoundedCornerShape(20.dp),
                large = RoundedCornerShape(24.dp),
                extraLarge = RoundedCornerShape(28.dp)
            )
            SOFT -> Shapes(
                extraSmall = RoundedCornerShape(4.dp),
                small = RoundedCornerShape(8.dp),
                medium = RoundedCornerShape(12.dp),
                large = RoundedCornerShape(16.dp),
                extraLarge = RoundedCornerShape(28.dp)
            )
            SHARP -> Shapes(
                extraSmall = RoundedCornerShape(2.dp),
                small = RoundedCornerShape(4.dp),
                medium = RoundedCornerShape(4.dp),
                large = RoundedCornerShape(4.dp),
                extraLarge = RoundedCornerShape(6.dp)
            )
            CUT_CORNER -> Shapes(
                extraSmall = CutCornerShape(4.dp),
                small = CutCornerShape(8.dp),
                medium = CutCornerShape(12.dp),
                large = CutCornerShape(14.dp),
                extraLarge = CutCornerShape(16.dp)
            )
        }
    }

    companion object {
        fun fromId(id: String?): VaultCornerStyle {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SOFT
        }
    }
}
