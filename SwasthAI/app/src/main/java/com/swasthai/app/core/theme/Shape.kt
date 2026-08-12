package com.swasthai.app.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * SwasthAI Shape System
 *
 * Uses generous rounding for a friendly, approachable healthcare UI.
 * Large touch targets with rounded corners make the app feel premium
 * and are easier for rural users to interact with.
 */
val SwasthAIShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
