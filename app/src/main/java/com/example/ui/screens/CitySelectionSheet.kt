package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CityEntity
import com.example.ui.theme.LocalThemePalette
import com.example.util.rememberDropletPlayers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySelectionSheet(
    sheetState: SheetState,
    savedCities: List<CityEntity>,
    selectedCity: CityEntity,
    searchQuery: String,
    searchResults: List<CityEntity>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSelectCity: (CityEntity) -> Unit,
    onSaveCity: (CityEntity) -> Unit,
    onDeleteCity: (CityEntity) -> Unit,
    onDetectLocation: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalThemePalette.current
    val feedback = rememberDropletPlayers()
    val playFeedback = feedback.plink

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.background,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOCATIONS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    color = palette.primaryText
                )

                IconButton(
                    onClick = {
                        feedback.whooshDown()
                        onDismiss()
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.chromeBg)
                        .testTag("close_city_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = palette.chromeFg
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("city_search_input"),
                placeholder = {
                    Text(
                        text = "Search city worldwide...",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = palette.secondaryText
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = palette.primaryText
                    )
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = palette.primaryText,
                            strokeWidth = 2.dp
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.primaryText,
                    unfocusedBorderColor = palette.fieldBorder,
                    focusedContainerColor = palette.fieldBg,
                    unfocusedContainerColor = palette.fieldBg,
                    focusedTextColor = palette.primaryText,
                    unfocusedTextColor = palette.primaryText,
                    cursorColor = palette.accent
                )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (searchQuery.isNotBlank()) {
                    item {
                        Text(
                            text = "SEARCH RESULTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = palette.secondaryText,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(searchResults) { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.surface)
                                .clickable {
                                    feedback.chime()
                                    onSaveCity(city)
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = city.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryText
                                )
                                Text(
                                    text = city.country,
                                    fontSize = 12.sp,
                                    color = palette.secondaryText
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = palette.primaryText
                            )
                        }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.chipBg)
                                .clickable {
                                    feedback.chime()
                                    onDetectLocation()
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("use_current_location_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Detect Location",
                                tint = palette.primaryText,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "USE CURRENT LOCATION",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = palette.primaryText
                            )
                        }
                    }

                    item {
                        Text(
                            text = "SAVED LOCATIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = palette.secondaryText,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(savedCities) { city ->
                        val isSelected = city.id == selectedCity.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) palette.chipSelectedBg else palette.surface
                                )
                                .clickable {
                                    feedback.snap()
                                    onSelectCity(city)
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (isSelected) {
                                        palette.chipSelectedFg
                                    } else {
                                        palette.primaryText
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = city.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) {
                                            palette.chipSelectedFg
                                        } else {
                                            palette.primaryText
                                        }
                                    )
                                    Text(
                                        text = city.country,
                                        fontSize = 12.sp,
                                        color = if (isSelected) {
                                            palette.chipSelectedFg.copy(alpha = 0.7f)
                                        } else {
                                            palette.secondaryText
                                        }
                                    )
                                }
                            }

                            if (savedCities.size > 1 && !city.isDefault) {
                                IconButton(
                                    onClick = { onDeleteCity(city) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = if (isSelected) {
                                            palette.chipSelectedFg.copy(alpha = 0.65f)
                                        } else {
                                            palette.secondaryText
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
