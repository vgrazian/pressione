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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pressione.iperteso.domain.model.Category
import com.pressione.iperteso.services.LocaleManager
import com.pressione.iperteso.ui.theme.CategoryCrisis
import com.pressione.iperteso.ui.theme.CategoryGrade1
import com.pressione.iperteso.ui.theme.CategoryGrade2
import com.pressione.iperteso.ui.theme.CategoryGrade3
import com.pressione.iperteso.ui.theme.CategoryHighNormal
import com.pressione.iperteso.ui.theme.CategoryNormal
import com.pressione.iperteso.ui.theme.CategoryOptimal

@Composable
fun CategoryBadge(category: Category, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = categoryColors(category)
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

fun categoryColors(category: Category): Pair<Color, Color> {
    return when (category) {
        Category.OPTIMAL -> CategoryOptimal.copy(alpha = 0.15f) to CategoryOptimal
        Category.NORMAL -> CategoryNormal.copy(alpha = 0.15f) to CategoryNormal
        Category.HIGH_NORMAL -> CategoryHighNormal.copy(alpha = 0.15f) to CategoryHighNormal
        Category.GRADE_1 -> CategoryGrade1.copy(alpha = 0.15f) to CategoryGrade1
        Category.GRADE_2 -> CategoryGrade2.copy(alpha = 0.15f) to CategoryGrade2
        Category.GRADE_3 -> CategoryGrade3.copy(alpha = 0.15f) to CategoryGrade3
        Category.CRISIS -> CategoryCrisis.copy(alpha = 0.15f) to CategoryCrisis
    }
}
