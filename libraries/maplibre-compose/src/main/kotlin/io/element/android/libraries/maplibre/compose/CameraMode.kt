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

package io.element.android.libraries.maplibre.compose

import com.mapbox.mapboxsdk.location.modes.CameraMode

public enum class CameraMode {
    NONE,
    NONE_COMPASS,
    NONE_GPS,
    TRACKING,
    TRACKING_COMPASS,
    TRACKING_GPS,
    TRACKING_GPS_NORTH;

    @CameraMode.Mode
    internal fun toInternal():  Int = when (this) {
        NONE -> CameraMode.NONE
        NONE_COMPASS -> CameraMode.NONE_COMPASS
        NONE_GPS -> CameraMode.NONE_GPS
        TRACKING -> CameraMode.TRACKING
        TRACKING_COMPASS -> CameraMode.TRACKING_COMPASS
        TRACKING_GPS -> CameraMode.TRACKING_GPS
        TRACKING_GPS_NORTH -> CameraMode.TRACKING_GPS_NORTH
    }
}
