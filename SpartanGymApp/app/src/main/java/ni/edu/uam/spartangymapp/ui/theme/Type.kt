package ni.edu.uam.spartangymapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ni.edu.uam.spartangymapp.R

/** Titulares y cifras grandes. El texto corrido sigue en la sans del sistema. */
val FuenteTitulo = FontFamily(Font(R.font.barlow_condensed_bold, FontWeight.Bold))

val Typography = Typography(
    displaySmall = TextStyle(fontFamily = FuenteTitulo, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontFamily = FuenteTitulo, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = FuenteTitulo, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = FuenteTitulo, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
)
