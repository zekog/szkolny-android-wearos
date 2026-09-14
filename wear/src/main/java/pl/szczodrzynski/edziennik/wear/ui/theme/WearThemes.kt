/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 */

package pl.szczodrzynski.edziennik.wear.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors

/**
 * Available watch color themes.
 */
enum class WearTheme(val key: String, val label: String) {
    BLUE("blue", "Niebieski"),
    DARK("dark", "Ciemny"),
    GREEN("green", "Zielony"),
    PURPLE("purple", "Fioletowy"),
    LATTE("latte", "Catppuccin Latte"),
    FRAPPE("frappe", "Catppuccin Frappé"),
    MACCHIATO("macchiato", "Catppuccin Macchiato"),
    MOCHA("mocha", "Catppuccin Mocha"),
    ;

    companion object {
        fun fromKey(key: String?): WearTheme =
                entries.firstOrNull { it.key == key } ?: BLUE
    }
}

/**
 * Returns a copy of the current Wear Material [Colors] adjusted to [theme].
 */
fun Colors.applyTheme(theme: WearTheme): Colors = when (theme) {
    WearTheme.BLUE -> this

    WearTheme.DARK -> copy(
            primary = Color(0xFFB0BEC5),
            primaryVariant = Color(0xFF78909C),
            secondary = Color(0xFF80CBC4),
            secondaryVariant = Color(0xFF4DB6AC),
            background = Color(0xFF000000),
            surface = Color(0xFF121212),
            onPrimary = Color(0xFF000000),
            onSecondary = Color(0xFF000000),
            onBackground = Color(0xFFECEFF1),
            onSurface = Color(0xFFECEFF1),
            onSurfaceVariant = Color(0xFFB0BEC5),
            error = Color(0xFFCF6679),
            onError = Color(0xFF000000),
    )

    WearTheme.GREEN -> copy(
            primary = Color(0xFF81C784),
            primaryVariant = Color(0xFF4CAF50),
            secondary = Color(0xFFA5D6A7),
            secondaryVariant = Color(0xFF66BB6A),
            background = Color(0xFF001B0A),
            surface = Color(0xFF07280F),
            onPrimary = Color(0xFF00210F),
            onSecondary = Color(0xFF00210F),
            onBackground = Color(0xFFDCEDC8),
            onSurface = Color(0xFFDCEDC8),
            onSurfaceVariant = Color(0xFFA5D6A7),
            error = Color(0xFFCF6679),
            onError = Color(0xFF000000),
    )

    WearTheme.PURPLE -> copy(
            primary = Color(0xFFB39DDB),
            primaryVariant = Color(0xFF9575CD),
            secondary = Color(0xFFCE93D8),
            secondaryVariant = Color(0xFFAB47BC),
            background = Color(0xFF120722),
            surface = Color(0xFF1E1033),
            onPrimary = Color(0xFF160A2B),
            onSecondary = Color(0xFF160A2B),
            onBackground = Color(0xFFEDE7F6),
            onSurface = Color(0xFFEDE7F6),
            onSurfaceVariant = Color(0xFFD1C4E9),
            error = Color(0xFFCF6679),
            onError = Color(0xFF000000),
    )

    // Catppuccin: https://github.com/catppuccin/catppuccin
    WearTheme.LATTE -> catppuccin(
            base = 0xFFEFF1F5, text = 0xFF4C4F69, surface = 0xFFE6E9EF,
            accent = 0xFF8839EF, accent2 = 0xFF1E66F5, red = 0xFFD20F39,
    )

    WearTheme.FRAPPE -> catppuccin(
            base = 0xFF303446, text = 0xFFC6D0F5, surface = 0xFF414559,
            accent = 0xFFCA9EE6, accent2 = 0xFF8CAAEE, red = 0xFFE78284,
    )

    WearTheme.MACCHIATO -> catppuccin(
            base = 0xFF24273A, text = 0xFFCAD3F5, surface = 0xFF363A4F,
            accent = 0xFFC6A0F6, accent2 = 0xFF8AADF4, red = 0xFFED8796,
    )

    WearTheme.MOCHA -> catppuccin(
            base = 0xFF1E1E2E, text = 0xFFCDD6F4, surface = 0xFF313244,
            accent = 0xFFCBA6F7, accent2 = 0xFF89B4FA, red = 0xFFF38BA8,
    )
}

private fun Colors.catppuccin(
        base: Long,
        text: Long,
        surface: Long,
        accent: Long,
        accent2: Long,
        red: Long,
): Colors = copy(
        primary = Color(accent),
        primaryVariant = Color(accent2),
        secondary = Color(accent2),
        secondaryVariant = Color(accent),
        background = Color(base),
        surface = Color(surface),
        onPrimary = Color(base),
        onSecondary = Color(base),
        onBackground = Color(text),
        onSurface = Color(text),
        onSurfaceVariant = Color(text),
        error = Color(red),
        onError = Color(base),
)
