package com.vitreminder.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitreminder.app.data.local.DirectoryRepository
import com.vitreminder.app.data.model.Branch
import com.vitreminder.app.data.model.Campus
import com.vitreminder.app.data.model.DivisionItem
import com.vitreminder.app.ui.theme.*

@Composable
fun DirectoryPicker(
    isLoading: Boolean,
    onSelectDivision: (DivisionItem, batch: String) -> Unit
) {
    val directoryRepo = remember { DirectoryRepository() }

    var selectedCampus by remember { mutableStateOf(Campus.BIBWEWADI) }

    val branches = remember(selectedCampus) {
        directoryRepo.getBranchesForCampus(selectedCampus)
    }

    var selectedBranchCode by remember(selectedCampus) {
        mutableStateOf(branches.firstOrNull()?.code ?: "CS")
    }

    val divisions = remember(selectedCampus, selectedBranchCode) {
        directoryRepo.getDivisionsForBranch(selectedCampus, selectedBranchCode)
    }

    var selectedSectionLetter by remember(divisions) {
        // Default to 'H' for CS if present, otherwise first available
        val defaultSection = if (selectedBranchCode == "CS" && divisions.any { it.sectionLetter == "H" }) {
            "H"
        } else {
            divisions.firstOrNull()?.sectionLetter ?: "A"
        }
        mutableStateOf(defaultSection)
    }

    var selectedBatch by remember { mutableStateOf("B3") }
    var selectedSemester by remember { mutableStateOf("1") }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select VIT Timetable",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Campus Selection (Bibwewadi vs Kondhwa)
            Text(
                text = "1. Campus",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Campus.values().forEach { campus ->
                    val isSelected = selectedCampus == campus
                    val shortName = if (campus == Campus.BIBWEWADI) "Bibwewadi" else "Kondhwa"
                    val subtitle = if (campus == Campus.BIBWEWADI) "Main Campus" else "Kondhwa Budruk"

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentBlue.copy(alpha = 0.2f) else CardDarkHover)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AccentBlue else CardDarkHover,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedCampus = campus
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = shortName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isSelected) AccentBlue else TextPrimary
                            )
                            Text(
                                text = subtitle,
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Branch Selection
            Text(
                text = "2. Branch",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(branches) { branch ->
                    val isSelected = selectedBranchCode.equals(branch.code, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AccentPurple else CardDarkHover)
                            .clickable { selectedBranchCode = branch.code }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "${branch.code} (${branch.displayName.take(16)})",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) SurfaceDark else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Section Selection
            Text(
                text = "3. Section / Division",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(divisions) { div ->
                    val isSelected = selectedSectionLetter == div.sectionLetter
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AccentGreen else CardDarkHover)
                            .clickable { selectedSectionLetter = div.sectionLetter },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = div.sectionLetter,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) SurfaceDark else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Batch & Semester Selection (Side-by-side)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Batch
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "4. Batch",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("B1", "B2", "B3").forEach { batch ->
                            val isSelected = selectedBatch == batch
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AccentBlue else CardDarkHover)
                                    .clickable { selectedBatch = batch },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = batch,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) SurfaceDark else TextPrimary
                                )
                            }
                        }
                    }
                }

                // Semester
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "5. Semester",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Sem 1", "Sem 2").forEach { sem ->
                            val semNum = if (sem.contains("1")) "1" else "2"
                            val isSelected = selectedSemester == semNum
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AccentPurple else CardDarkHover)
                                    .clickable { selectedSemester = semNum },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sem,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) SurfaceDark else TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Chosen Target Preview Pill
            val targetDivision = divisions.find { it.sectionLetter == selectedSectionLetter }
                ?: divisions.firstOrNull()

            if (targetDivision != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentBlue.copy(alpha = 0.1f))
                        .border(1.dp, AccentBlue.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Selected: ${targetDivision.divisionCode} • Batch $selectedBatch • Sem $selectedSemester",
                        fontSize = 12.sp,
                        color = AccentBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    if (targetDivision != null) {
                        onSelectDivision(targetDivision, selectedBatch)
                    }
                },
                enabled = !isLoading && targetDivision != null,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = SurfaceDark,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = SurfaceDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Load Schedule (${targetDivision?.divisionCode ?: ""})",
                        color = SurfaceDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
