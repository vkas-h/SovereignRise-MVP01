package com.sovereign_rise.app.ui.theme

import androidx.compose.ui.graphics.Color

// Modern Primary Colors - Deep Blue & Emerald Theme
val Primary = Color(0xFF10B981)        // Emerald green - primary actions
val PrimaryVariant = Color(0xFF059669)  // Darker emerald
val Secondary = Color(0xFF3B82F6)      // Bright blue - secondary actions
val SecondaryVariant = Color(0xFF2563EB) // Darker blue
val Tertiary = Color(0xFF8B5CF6)       // Purple - tertiary accents

// Background Colors - Deep Navy Theme
val BackgroundPrimary = Color(0xFF0A1929)   // Deep navy - main background
val BackgroundSecondary = Color(0xFF0F172A) // Slightly lighter navy
val Surface = Color(0xFF1E293B)            // Card/surface background
val SurfaceVariant = Color(0xFF334155)     // Elevated surfaces

// Status Colors
val Success = Color(0xFF10B981)        // Emerald green
val Warning = Color(0xFFF59E0B)        // Amber
val Error = Color(0xFFEF4444)          // Red
val Info = Color(0xFF3B82F6)           // Blue

// Text Colors - High Contrast for Readability
val TextPrimary = Color(0xFFF8FAFC)    // Almost white - primary text
val TextSecondary = Color(0xFFCBD5E1)  // Light gray - secondary text
val TextTertiary = Color(0xFF94A3B8)   // Medium gray - tertiary text
val TextDisabled = Color(0xFF64748B)   // Darker gray - disabled text

// Accent Colors for Visual Interest
val AccentAmber = Color(0xFFF59E0B)    // Warm amber for highlights
val AccentRose = Color(0xFFF43F5E)     // Rose for important alerts
val AccentTeal = Color(0xFF14B8A6)     // Teal for info
val AccentViolet = Color(0xFF8B5CF6)   // Violet for premium features

// Gradient Colors
val GradientStart = Color(0xFF10B981)  // Emerald
val GradientEnd = Color(0xFF3B82F6)    // Blue

// Overlay Colors
val OverlayLight = Color(0x1AFFFFFF)   // 10% white overlay
val OverlayDark = Color(0x33000000)    // 20% black overlay

// Border Colors
val BorderSubtle = Color(0xFF334155)   // Subtle borders
val BorderMedium = Color(0xFF475569)   // Medium borders
val BorderStrong = Color(0xFF64748B)   // Strong borders

// Additional Surface Colors
val SurfaceDark = Color(0xFF1E293B)       // Same as Surface for now
val SuccessLight = Color(0x3310B981)      // 20% opacity Success
val WarningLight = Color(0x33F59E0B)      // 20% opacity Warning  
val ErrorLight = Color(0x33EF4444)        // 20% opacity Error

// Deprecated - for backward compatibility during migration
@Deprecated("Use Error instead")
val Danger = Error
@Deprecated("Use BackgroundPrimary instead")
val BackgroundDark = BackgroundPrimary
@Deprecated("Use BackgroundSecondary instead")
val BackgroundLight = BackgroundSecondary
@Deprecated("Use TextTertiary instead")
val TextMuted = TextTertiary
@Deprecated("Use Secondary instead")
val Accent = Secondary
