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

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import io.element.android.features.location.api.Location
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun rememberMapState(
    position: MapState.CameraPosition = MapState.CameraPosition(0.0, 0.0, 0.0),
    location: Location? = null,
    markers: ImmutableList<MapState.Marker> = emptyList<MapState.Marker>().toImmutableList(),
    isLocationEnabled: Boolean = false,
    isTrackingLocation: Boolean = false,
): MapState = remember {
    MapState(
        position = position,
        location = location,
        markers = markers,
        isLocationEnabled = isLocationEnabled,
        isTrackingLocation = isTrackingLocation,
    )
} // TODO(Use remember saveable with Parcelable custom saver)

/**
 * State of the map.
 *
 * @param position The position of the camera.
 * @param location The location of the user, if any.
 * @param markers A list of markers to be drawn on the map.
 * @param isLocationEnabled Whether to enable the map's location subsystem (needs permissions).
 * @param isTrackingLocation Whether the camera is tracking the user's location.
 */
@Stable
class MapState(
    position: CameraPosition,
    location: Location?,
    markers: ImmutableList<Marker>,
    isLocationEnabled: Boolean,
    isTrackingLocation: Boolean,
) {
    internal var mapRefs: MapRefs? by mutableStateOf(null)

    var rawPosition: CameraPosition by mutableStateOf(position)
    var position: CameraPosition
        get() = rawPosition
        set(value) {
            mapRefs?.map?.setCameraPosition(value)
        }

    var location: Location? by mutableStateOf(location)

    var markers: ImmutableList<Marker> by mutableStateOf(markers)

    var isLocationEnabled: Boolean by mutableStateOf(isLocationEnabled)

    internal var rawIsTrackingLocation: Boolean by mutableStateOf(isTrackingLocation)
    var isTrackingLocation: Boolean
        get() = rawIsTrackingLocation
        set(value) {
            mapRefs?.map?.enableTrackingCameraMode(value)
        }

    @Stable
    data class CameraPosition(
        val lat: Double,
        val lon: Double,
        val zoom: Double,
    )

    @Stable
    data class Marker(
        @DrawableRes val drawable: Int,
        val lat: Double,
        val lon: Double,
        val anchorBottom: Boolean = false,
    )

    internal data class MapRefs(
        val map: MapboxMap,
        val style: Style,
        val symbolManager: SymbolManager,
    )
}
