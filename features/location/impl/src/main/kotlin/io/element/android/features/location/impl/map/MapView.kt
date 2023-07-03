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

package io.element.android.features.location.impl.map

import android.Manifest
import android.annotation.SuppressLint
import android.view.Gravity
import androidx.annotation.RequiresPermission
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener
import com.mapbox.mapboxsdk.location.engine.LocationEngineRequest
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.Property.ICON_ANCHOR_BOTTOM
import io.element.android.features.location.api.Location
import io.element.android.features.location.api.internal.buildTileServerUrl
import io.element.android.libraries.designsystem.preview.DayNightPreviews
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.theme.ElementTheme
import io.element.android.libraries.theme.compound.generated.SemanticColors
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import io.element.android.libraries.designsystem.R as DesignSystemR

/**
 * Composable wrapper around MapLibre's [MapView].
 */
@SuppressLint("MissingPermission")
@Composable
fun MapView(
    modifier: Modifier = Modifier,
    mapState: MapState = rememberMapState(),
    darkMode: Boolean = !ElementTheme.isLightTheme,
) {
    // When in preview, early return a Box with the received modifier preserving layout
    if (LocalInspectionMode.current) {
        @Suppress("ModifierReused") // False positive, the modifier is not reused due to the early return.
        Box(
            modifier = modifier.background(Color.DarkGray)
        ) {
            Text("[MapView]", modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val context = LocalContext.current
    val mapView = remember {
        Mapbox.getInstance(context)
        MapView(context)
    }

    // Build map
    val semanticColors = ElementTheme.colors
    LaunchedEffect(darkMode, mapState.isLocationEnabled) {
        mapState.mapRefs = mapView.buildMap(
            mapState = mapState,
            darkMode = darkMode,
            semanticColors = semanticColors,
        )
    }

    key(mapState.mapRefs) {
        // Enable location
        LaunchedEffect(mapState.isLocationEnabled) {
            mapState.mapRefs?.map?.apply {
                setCameraZoom(15.0)
                enableLocationComponent(mapState.isLocationEnabled)
            }
        }

        // Draw markers
        LaunchedEffect(mapState.markers) {
            mapState.mapRefs?.also { (_, style, symbolManager) ->
                symbolManager.deleteAll()
                mapState.markers.forEachIndexed { index, marker ->
                    AppCompatResources.getDrawable(context, marker.drawable)?.let { style.addImage("marker_$index", it) }
                    symbolManager.create(
                        SymbolOptions().apply {
                            withLatLng(LatLng(marker.lat, marker.lon))
                            withIconImage("marker_$index")
                            if (marker.anchorBottom) withIconAnchor(ICON_ANCHOR_BOTTOM)
                        }
                    )
                    Timber.d("Showing marker: $marker")
                }
            }
        }
    }

    @Suppress("ModifierReused")
    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

@DayNightPreviews
@Composable
fun MapViewPreview() = ElementPreview {
    MapView(
        modifier = Modifier.size(400.dp),
        mapState = rememberMapState(
            position = MapState.CameraPosition(
                lat = 0.0,
                lon = 0.0,
                zoom = 0.0,
            ),
            location = Location(
                lat = 0.0,
                lon = 0.0,
                accuracy = 0.0f,
            ),
            markers = listOf(
                MapState.Marker(
                    drawable = DesignSystemR.drawable.pin,
                    lat = 0.0,
                    lon = 0.0,
                )
            ).toImmutableList()
        ),
    )
}

private suspend inline fun MapView.buildMap(
    mapState: MapState,
    darkMode: Boolean,
    semanticColors: SemanticColors,
): MapState.MapRefs {
    val map = awaitMap()
    val style = map.awaitStyle(buildTileServerUrl(darkMode = darkMode))
    val symbolManager = SymbolManager(this, map, style).apply {
        iconAllowOverlap = true
    }
    map.uiSettings.apply {
        attributionGravity = Gravity.TOP
        setAttributionTintColor(semanticColors.iconPrimary.toArgb())
        logoGravity = Gravity.TOP
        isCompassEnabled = false
        isRotateGesturesEnabled = false
    }
    map.locationComponent.activateLocationComponent(
        LocationComponentActivationOptions.Builder(context, style)
            .locationComponentOptions(
                LocationComponentOptions.builder(context)
                    .pulseEnabled(true)
                    .build()
            )
            .locationEngineRequest(
                LocationEngineRequest.Builder(750)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                    .setFastestInterval(750)
                    .build()
            )
            .build()
    )

    map.setCameraPosition(mapState.position)
    map.addOnCameraIdleListener {
        val cameraPosition = map.cameraPosition.target?.let {
            MapState.CameraPosition(
                lat = it.latitude,
                lon = it.longitude,
                zoom = map.cameraPosition.zoom
            )
        }
        val location = map.locationComponent.lastKnownLocation?.let {
            Location(
                lat = it.latitude,
                lon = it.longitude,
                accuracy = it.accuracy
            )
        }
        cameraPosition?.let { mapState.rawPosition = it }
        location?.let { mapState.location = it }
        Timber.d("Camera now idle at position: $cameraPosition - location: $location")
    }

    map.enableTrackingCameraMode(mapState.isTrackingLocation)
    map.locationComponent.addOnCameraTrackingChangedListener(object : OnCameraTrackingChangedListener {
        override fun onCameraTrackingDismissed() {}

        override fun onCameraTrackingChanged(currentMode: Int) {
            mapState.rawIsTrackingLocation = when (currentMode) {
                CameraMode.NONE -> false
                CameraMode.TRACKING -> true
                else -> error("Illegal camera mode: $currentMode")
            }
            Timber.d("onCameraTrackingChanged: $currentMode")
        }
    })

    return MapState.MapRefs(
        map = map,
        style = style,
        symbolManager = symbolManager,
    )
}

private suspend inline fun MapView.awaitMap(): MapboxMap = suspendCoroutine {
    getMapAsync { map ->
        it.resume(map)
    }
}

private suspend inline fun MapboxMap.awaitStyle(url: String): Style = suspendCoroutine {
    setStyle(url) { style ->
        it.resume(style)
    }
}

internal fun MapboxMap.setCameraPosition(position: MapState.CameraPosition) {
    cameraPosition = CameraPosition.Builder()
        .target(LatLng(position.lat, position.lon))
        .zoom(position.zoom)
        .build()
    Timber.d("Camera position set to: $position")
}

private fun MapboxMap.setCameraZoom(zoom: Double) {
    cameraPosition = CameraPosition.Builder()
        .zoom(zoom)
        .build()
    Timber.d("Camera zoom set to: $zoom")
}

@RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
private fun MapboxMap.enableLocationComponent(enabled: Boolean) {
    locationComponent.apply {
        isLocationComponentEnabled = enabled
        Timber.d("Location component enabled: $enabled")
    }
}

internal fun MapboxMap.enableTrackingCameraMode(enabled: Boolean) {
    locationComponent.apply {
        cameraMode = if (enabled) CameraMode.TRACKING else CameraMode.NONE
        Timber.d("Tracking location enabled: $enabled")
    }
}

