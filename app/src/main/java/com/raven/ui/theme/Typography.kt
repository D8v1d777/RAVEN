package com.raven.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object RavenTypography {
    // Font Family - Using system default for now, can be customized later
    val FontFamilyDefault = FontFamily.Default

    // Text Styles
    val DisplayLarge = TextStyle(
        fontFamily = FontFamilyDefault,
        fontSize = 57.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    )

    val DisplayMedium = TextStyle(
        fontFamily = FontFamilyDefault,
        fontSize = 45.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    )

    val DisplaySmall = TextStyle(
        fontFamily = FontFamilyDefault,
        fontSize = 36.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    )

    val HeadlineLarge = TextStyle(
        fontFamily = FontFamilyDefault,
        fontSize = 32.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    )

    val HeadlineMedium = TextStyle(
        fontFamily = FontFamilyDefault,
        fontSize = 28.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    )

    val HeadlineSmall = TextStyle(
        fontFamily = FontFamilyDefault,
        fontSize = 24.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    )

    val TitleLarge = TextStyle(
        fontFamily = FontFamilyDefault,
        fontSize = 22.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    )

    val TitleMedium = TextStyle(
        fontFamily = FontFamilyDefault,
        fontSize = 16.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    )

    val TitleSmall = TextStyle(
        fontFamily = FontFamilyDefault,
        fontSize = 14.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    )

    val BodyLarge = TextStyle(
        fontFamily = FontFamilyDefault,
        fontSize = 16.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    )

    val BodyMedium = TextStyle(
        fontFamily = FontFamilyDefault,
        fontSize = 14.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    )

    val BodySmall = TextStyle(
        fontFamily = FontFamilyDefault,
        fontSize = 12.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    )

    val LabelLarge = TextStyle(
        fontFamily = FontFamilyDefault,
        fontSize = 14.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    )

    val LabelMedium = TextStyle(
        fontFamily = FontFamilyDefault,
        fontSize = 12.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    )

    val LabelSmall = TextStyle(
        fontFamily = FontFamilyDefault,
        fontSize = 11.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    )
}