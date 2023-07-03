/*
 * Copyright (c) 2023 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.element.android.features.location.impl.send

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import io.element.android.features.location.impl.map.MapState
import io.element.android.features.location.impl.map.MapView
import io.element.android.features.location.impl.map.rememberMapState
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.preview.DayNightPreviews
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.theme.components.BottomSheetScaffold
import io.element.android.libraries.designsystem.theme.components.CenterAlignedTopAppBar
import io.element.android.libraries.designsystem.theme.components.FloatingActionButton
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.libraries.designsystem.R as DesignSystemR

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalPermissionsApi::class,
)
@Composable
fun SendLocationView(
    state: SendLocationState,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
) {
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )
    val anyPermissionsGranted: Boolean by remember {
        derivedStateOf { permissionState.permissions.any { it.status.isGranted } }
    }
    val mapState = if (anyPermissionsGranted) {
        rememberMapState(
            isLocationEnabled = true,
            isTrackingLocation = true,
        )
    } else {
        rememberMapState(
            position = MapState.CameraPosition(49.843, 9.902056, 2.7),
            isLocationEnabled = false,
            isTrackingLocation = false,
        )
    }

    LaunchedEffect(Unit) {
        permissionState.launchMultiplePermissionRequest()
    }

    BottomSheetScaffold(
        sheetContent = {
            if (!anyPermissionsGranted) {
                Spacer(modifier = Modifier.height(16.dp))
                if (permissionState.shouldShowRationale) {
                    PermissionRationaleBanner { permissionState.launchMultiplePermissionRequest() }
                } else {
                    PermissionDeniedBanner { /* TODO Go to settings */ }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ListItem(
                headlineContent = {
                    Text(
                        stringResource(
                            when (mapState.isTrackingLocation) {
                                true -> CommonStrings.screen_share_my_location_action
                                false -> CommonStrings.screen_share_this_location_action
                            }
                        )
                    )
                },
                modifier = Modifier.clickable {
                    state.eventSink(
                        SendLocationEvents.ShareLocation(
                            lat = mapState.position.lat,
                            lng = mapState.position.lon
                        )
                    )
                    onBackPressed()
                },
                leadingContent = {
                    Icon(Icons.Default.LocationOn, null)
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
        },
        modifier = modifier,
        scaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.Expanded),
        ),
        sheetDragHandle = {},
        sheetSwipeEnabled = false,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(CommonStrings.screen_share_location_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    BackButton(onClick = onBackPressed)
                },
            )
        },
    ) {
        Box(
            modifier = Modifier
                .padding(it)
                .consumeWindowInsets(it),
            contentAlignment = Alignment.Center
        ) {
            MapView(
                modifier = Modifier.fillMaxSize(),
                mapState = mapState,
            )
            Icon(
                resourceId = DesignSystemR.drawable.pin,
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.align { size, space, _ ->
                    IntOffset(
                        x = (space.width - size.width) / 2,
                        y = (space.height / 2) - size.height,
                    )
                }
            )
            FloatingActionButton(
                onClick = { mapState.isTrackingLocation = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 64.dp),
            ) {
                when (mapState.isTrackingLocation) {
                    false -> Icon(imageVector = Icons.Default.LocationSearching, contentDescription = null)
                    true -> Icon(imageVector = Icons.Default.MyLocation, contentDescription = null)
                }
            }
        }
    }
}

@DayNightPreviews
@Composable
fun SendLocationViewPreview(
    @PreviewParameter(SendLocationStateProvider::class) state: SendLocationState
) = ElementPreview {
    SendLocationView(
        state = state,
        onBackPressed = {},
    )
}

@Composable
private fun PermissionRationaleBanner(
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text("No location permission, click to request.")
        },
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(Icons.Default.LocationOff, null)
        },
    )
}

@Composable
private fun PermissionDeniedBanner(
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text("No location permission, click to go to settings.")
        },
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(Icons.Default.LocationOff, null)
        },
    )
}
