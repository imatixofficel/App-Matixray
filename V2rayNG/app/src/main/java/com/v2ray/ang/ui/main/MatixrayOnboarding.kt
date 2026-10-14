package com.v2ray.ang.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.XfPurple

@Composable
fun MatixrayOnboarding(
    onFinished: () -> Unit,
) {
    var page by remember { mutableIntStateOf(0) }
    val texts = listOf(
        stringResource(R.string.mx_onboarding_1),
        stringResource(R.string.mx_onboarding_2),
        stringResource(R.string.mx_onboarding_3),
        stringResource(R.string.mx_onboarding_4),
    )

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.46f))
        ) {
            // A simple spotlight frame follows the control being explained.
            val highlight = when (page) {
                0 -> Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 8.dp, start = 8.dp)
                    .size(58.dp)
                1 -> Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 52.dp)
                    .size(48.dp)
                2 -> Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .height(90.dp)
                else -> Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp, start = 12.dp, end = 12.dp)
                    .fillMaxWidth()
                    .height(64.dp)
            }

            Box(
                modifier = highlight.border(
                    width = 2.dp,
                    color = XfPurple,
                    shape = RoundedCornerShape(14.dp),
                )
            )

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.mx_onboarding_title),
                        color = Color(0xFF19151F),
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = texts[page],
                        color = Color(0xFF6D6575),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(texts.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == page) 18.dp else 7.dp, 7.dp)
                                    .background(XfPurple, RoundedCornerShape(20.dp))
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (page > 0) {
                            Button(
                                onClick = { page-- },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0EAF7), contentColor = Color(0xFF5F536B)),
                            ) { Text(stringResource(R.string.mx_onboarding_prev)) }
                        }
                        Button(
                            onClick = {
                                if (page == texts.lastIndex) onFinished() else page++
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = XfPurple),
                        ) {
                            Text(stringResource(if (page == texts.lastIndex) R.string.mx_onboarding_done else R.string.mx_onboarding_next))
                        }
                    }
                }
            }
        }
    }
}
