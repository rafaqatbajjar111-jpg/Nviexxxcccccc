package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Reusable premium golden pill button with black bold text.
 */
@Composable
fun InvexxButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    testTag: String = ""
) {
    Button(
        onClick = { if (!isLoading) onClick() },
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryGold,
            contentColor = DarkCharcoal,
            disabledContainerColor = PrimaryGold.copy(alpha = 0.5f),
            disabledContentColor = DarkCharcoal.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(50.dp), // Fully rounded pill
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .testTag(testTag)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = DarkCharcoal,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingIcon()
                }
            }
        }
    }
}

/**
 * Custom text field with a premium white card design, soft neumorphic border,
 * and a golden square leading icon box.
 */
@Composable
fun InvexxTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hintText: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingText: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    testTag: String = ""
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .drawBehind {
                // Soft Neumorphic light-shadow simulation
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.04f),
                    topLeft = Offset(0f, 4f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                )
            },
        shape = RoundedCornerShape(16.dp),
        color = PureWhite,
        border = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading Icon inside a golden square box
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryGold),
                contentAlignment = Alignment.Center
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = "Field Icon",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (leadingText != null) {
                    Text(
                        text = leadingText,
                        color = PureWhite,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Input field
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = hintText,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MediumGray)
                    )
                },
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = DarkCharcoal,
                    unfocusedTextColor = DarkCharcoal
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier
                    .weight(1f)
                    .testTag(testTag)
            )

            if (trailingIcon != null) {
                trailingIcon()
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

/**
 * Reusable card utilizing premium soft Neumorphic shadow styling with 20dp border radius.
 */
@Composable
fun InvexxCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = PureWhite,
    borderRadius: Dp = 20.dp,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // Neumorphic Soft Shadow Layer
                drawRoundRect(
                    color = ShadowColor,
                    topLeft = Offset(0f, 6f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(borderRadius.toPx())
                )
            },
        shape = RoundedCornerShape(borderRadius),
        color = backgroundColor,
        border = null
    ) {
        Column(
            modifier = Modifier.padding(padding)
        ) {
            content()
        }
    }
}

/**
 * Standard horizontal stat metrics row inside a white card.
 */
@Composable
fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(GoldenLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = PrimaryGold,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = MediumGray)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = DarkCharcoal
            )
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = PrimaryGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

/**
 * Premium menu row for profile page, structured with a leading icon, title, and right arrow.
 */
@Composable
fun MenuItemRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    color = ShadowColor,
                    topLeft = Offset(0f, 3f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                )
            },
        shape = RoundedCornerShape(12.dp),
        color = PureWhite
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GoldenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = PrimaryGold,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = DarkCharcoal
                ),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Go",
                tint = MediumGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Section title with elegant link layout.
 */
@Composable
fun SectionTitle(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = PrimaryGold,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.clickable(onClick = onActionClick)
            )
        }
    }
}

/**
 * Reusable, smooth Golden horizontal progress bar.
 */
@Composable
fun GoldenProgressBar(
    progress: Float, // 0f to 1f
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(LightGrayBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(PrimaryGold, SecondaryGold)
                    )
                )
        )
    }
}

/**
 * Custom Canvas-drawn premium INVEXX logo.
 * Renders a glossy, golden 3D-style "X" representing a person with extended arms forming an X, and a circular head.
 */
@Composable
fun InvexxLogo(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Colors
        val goldBrush = Brush.linearGradient(
            colors = listOf(PrimaryGold, SecondaryGold, Color(0xFFFFF099)),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )

        // Head position (top-middle)
        val headRadius = w * 0.12f
        val headCenter = Offset(w / 2f, h * 0.22f)
        drawCircle(
            brush = goldBrush,
            radius = headRadius,
            center = headCenter
        )

        // X body/arms paths
        val strokeWidth = w * 0.14f

        // Draw left-top to right-bottom arm
        // We use curves/paths or straight thick lines with round caps to represent 3D depth
        drawLine(
            brush = goldBrush,
            start = Offset(w * 0.25f, h * 0.40f),
            end = Offset(w * 0.75f, h * 0.85f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Draw right-top to left-bottom arm (overlapping with shadow to look 3D)
        drawLine(
            brush = goldBrush,
            start = Offset(w * 0.75f, h * 0.40f),
            end = Offset(w * 0.25f, h * 0.85f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    borderRadius: Dp = 8.dp
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(borderRadius))
            .background(Color.Gray.copy(alpha = alpha))
    )
}

@Composable
fun PlanSkeletonList() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(3) {
            InvexxCard(borderRadius = 20.dp, backgroundColor = PureWhite) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBox(modifier = Modifier.size(60.dp), borderRadius = 12.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f).height(18.dp))
                        SkeletonBox(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    SkeletonBox(modifier = Modifier.size(75.dp, 36.dp), borderRadius = 18.dp)
                }
            }
        }
    }
}

@Composable
fun TransactionSkeletonList() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(4) {
            InvexxCard(borderRadius = 16.dp, padding = 12.dp) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBox(modifier = Modifier.size(40.dp), borderRadius = 20.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SkeletonBox(modifier = Modifier.fillMaxWidth(0.5f).height(16.dp))
                        SkeletonBox(modifier = Modifier.fillMaxWidth(0.3f).height(12.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    SkeletonBox(modifier = Modifier.size(50.dp, 16.dp))
                }
            }
        }
    }
}
