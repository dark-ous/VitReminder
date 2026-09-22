package com.vitreminder.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitreminder.app.data.model.DivisionItem
import com.vitreminder.app.data.model.Timetable
import com.vitreminder.app.ui.components.DirectoryPicker
import com.vitreminder.app.ui.theme.*

@Composable
fun OnboardingScreen(
    isLoading: Boolean,
    pendingTimetable: Timetable?,
    errorMessage: String? = null,
    successMessage: String? = null,
    onPickPdf: (Uri) -> Unit,
    onFetchUrl: (String) -> Unit,
    onQuickLoad: () -> Unit,
    onConfirmBatch: (String) -> Unit,
    onSelectDirectoryDivision: (DivisionItem, batch: String) -> Unit = { _, _ -> }
) {
    var urlInput by remember {
        mutableStateOf("https://www.vit.edu/DESH/wp-content/uploads/2026/09/CS-H.pdf")
    }
    var selectedBatch by remember { mutableStateOf("B3") }
    var showFallbackOptions by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onPickPdf(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // App Icon Badge
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.15f))
                    .border(2.dp, AccentBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "VIT Class Reminder",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Automated reminders, room notifications & home widgets for Vishwakarma Institute of Technology students",
                fontSize = 12.5.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )

            // Visible Error Card
            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AccentRed)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = errorMessage, color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }

            // Visible Success Card
            if (!successMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = successMessage, color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (pendingTimetable == null) {
                // Primary 1-Tap Campus & Division Selector
                DirectoryPicker(
                    isLoading = isLoading,
                    onSelectDivision = onSelectDirectoryDivision
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Fallback Accordion (Device PDF & Custom URL)
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFallbackOptions = !showFallbackOptions },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Custom PDF Upload or URL (Fallback)",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = if (showFallbackOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }

                        AnimatedVisibility(visible = showFallbackOptions) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Instant quick sample button
                                Button(
                                    onClick = onQuickLoad,
                                    colors = ButtonDefaults.buttonColors(containerColor = CardDarkHover),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    enabled = !isLoading
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentGreen)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("⚡ Instant Sample (FY CS-H)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Device PDF Button
                                Button(
                                    onClick = { filePicker.launch("application/pdf") },
                                    colors = ButtonDefaults.buttonColors(containerColor = CardDarkHover),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    enabled = !isLoading
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = AccentBlue)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Choose PDF from Phone Storage", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = urlInput,
                                    onValueChange = { urlInput = it },
                                    label = { Text("Timetable Direct PDF Link") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentBlue,
                                        unfocusedBorderColor = CardDarkHover,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextSecondary
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = { onFetchUrl(urlInput.trim()) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    enabled = !isLoading && urlInput.isNotBlank()
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = SurfaceDark)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Fetch & Parse Custom URL", color = SurfaceDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // Batch Selection Card (if parsed via fallback)
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Timetable Verified! 🎉",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = AccentGreen
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${pendingTimetable.division} • Semester ${pendingTimetable.semester} (${pendingTimetable.academicYear})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )

                        Text(
                            text = "Found ${pendingTimetable.allSessions.size} class slots",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Select Your Batch:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val batches = pendingTimetable.availableBatches.ifEmpty { listOf("B1", "B2", "B3") }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            batches.forEach { batch ->
                                val isSelected = selectedBatch == batch
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) AccentBlue else CardDarkHover)
                                        .clickable { selectedBatch = batch }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = batch,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (isSelected) SurfaceDark else TextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { onConfirmBatch(selectedBatch) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            enabled = !isLoading
                        ) {
                            Text(
                                text = "Confirm & Start Reminders",
                                color = SurfaceDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
