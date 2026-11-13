package io.cybersnippets.shodan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class GuideStep(
    val icon: ImageVector,
    val title: String,
    val body: String,
)

private val GUIDE_STEPS = listOf(
    GuideStep(
        Icons.Filled.Terminal,
        "Bienvenue sur VTLOsh0d_",
        "Votre client Shodan de poche. Lancez tous les types de requêtes du " +
            "cheat-sheet et explorez les résultats, directement depuis votre téléphone.",
    ),
    GuideStep(
        Icons.Filled.Key,
        "1 · Votre clé API",
        "Touchez l'icône clé en haut à droite, collez votre clé API Shodan, " +
            "puis enregistrez. Elle reste stockée sur votre appareil.",
    ),
    GuideStep(
        Icons.Filled.Search,
        "2 · Lancer une recherche",
        "Touchez un dork dans la liste, ou tapez votre propre requête, " +
            "puis appuyez sur « Run ». Les résultats s'affichent avec pagination.",
    ),
    GuideStep(
        Icons.Filled.FilterList,
        "3 · Filtrer les résultats",
        "Affinez par pays, ville, organisation, port, produit, OS ou type " +
            "d'appareil. Les filtres s'ajoutent automatiquement à chaque recherche.",
    ),
    GuideStep(
        Icons.Filled.OpenInBrowser,
        "4 · Ouvrir un résultat",
        "Touchez « Open » sur un résultat pour l'afficher dans le navigateur " +
            "intégré, avec le user-agent de votre choix.",
    ),
)

/**
 * Animated French first-launch guide shown over the main screen. Steps slide
 * in with a pulsing icon; [onFinish] is called on skip or on the last step.
 */
@Composable
fun OnboardingOverlay(onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val current = GUIDE_STEPS[step]
    val isLast = step == GUIDE_STEPS.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(16.dp),
                )
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PulsingIcon(current.icon)

            Spacer(Modifier.height(20.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState > initialState
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally { dir * it } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally { -dir * it } + fadeOut(tween(200)))
                },
                label = "guide-step",
            ) { index ->
                val s = GUIDE_STEPS[index]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        s.title,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        s.body,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            StepDots(count = GUIDE_STEPS.size, active = step)
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onFinish) {
                    Text(if (isLast) "Fermer" else "Passer")
                }
                Button(onClick = { if (isLast) onFinish() else step++ }) {
                    Text(if (isLast) "Commencer" else "Suivant")
                }
            }
        }
    }
}

@Composable
private fun PulsingIcon(icon: ImageVector) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse-scale",
    )

    Box(
        modifier = Modifier
            .size(84.dp)
            .scale(scale)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(44.dp),
        )
    }
}

@Composable
private fun StepDots(count: Int, active: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { i ->
            val isActive = i == active
            val width by animateDpAsState(
                targetValue = if (isActive) 22.dp else 8.dp,
                animationSpec = tween(300, easing = LinearEasing),
                label = "dot-width",
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        CircleShape,
                    ),
            )
        }
    }
}
