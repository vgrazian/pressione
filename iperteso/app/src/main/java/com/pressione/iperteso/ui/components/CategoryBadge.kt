package com.pressione.iperteso.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pressione.iperteso.domain.model.Category
import com.pressione.iperteso.services.LocaleManager
import com.pressione.iperteso.ui.theme.CategoryCrisis
import com.pressione.iperteso.ui.theme.CategoryCrisisDark
import com.pressione.iperteso.ui.theme.CategoryGrade1
import com.pressione.iperteso.ui.theme.CategoryGrade1Dark
import com.pressione.iperteso.ui.theme.CategoryGrade2
import com.pressione.iperteso.ui.theme.CategoryGrade2Dark
import com.pressione.iperteso.ui.theme.CategoryGrade3
import com.pressione.iperteso.ui.theme.CategoryGrade3Dark
import com.pressione.iperteso.ui.theme.CategoryHighNormal
import com.pressione.iperteso.ui.theme.CategoryHighNormalDark
import com.pressione.iperteso.ui.theme.CategoryNormal
import com.pressione.iperteso.ui.theme.CategoryNormalDark
import com.pressione.iperteso.ui.theme.CategoryOptimal
import com.pressione.iperteso.ui.theme.CategoryOptimalDark

@Composable
fun CategoryBadge(category: Category, modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val (bgColor, textColor) = categoryColors(category, dark)
    val label = if (LocaleManager.getLanguage(LocalContext.current) == LocaleManager.LANG_ENGLISH)
        category.labelEn else category.label

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

fun categoryColors(category: Category, dark: Boolean = false): Pair<Color, Color> {
    val base = when (category) {
        Category.OPTIMAL -> if (dark) CategoryOptimalDark else CategoryOptimal
        Category.NORMAL -> if (dark) CategoryNormalDark else CategoryNormal
        Category.HIGH_NORMAL -> if (dark) CategoryHighNormalDark else CategoryHighNormal
        Category.GRADE_1 -> if (dark) CategoryGrade1Dark else CategoryGrade1
        Category.GRADE_2 -> if (dark) CategoryGrade2Dark else CategoryGrade2
        Category.GRADE_3 -> if (dark) CategoryGrade3Dark else CategoryGrade3
        Category.CRISIS -> if (dark) CategoryCrisisDark else CategoryCrisis
    }
    return base.copy(alpha = 0.15f) to base
}
