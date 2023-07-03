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

package io.element.android.features.location.impl.permissions

import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import io.element.android.libraries.architecture.Presenter

interface PermissionsPresenter : Presenter<PermissionsState>

sealed interface PermissionsEvents {
    object CheckPermissions : PermissionsEvents
}

@OptIn(ExperimentalPermissionsApi::class)
data class PermissionsState(
    val permissions: List<PermissionState>,
    val revokedPermissions: List<PermissionState>,
    val allPermissionsGranted: Boolean,
    val shouldShowRationale: Boolean,
    val eventSink: (PermissionsEvents) -> Unit,
)

class PermissionsPresenterImpl(
    private val permissions: List<String>
) : PermissionsPresenter {
    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    override fun present(): PermissionsState {
        val state = rememberMultiplePermissionsState(permissions = permissions)

        fun handleEvents(event: PermissionsEvents) {
            when (event) {
                PermissionsEvents.CheckPermissions -> state.launchMultiplePermissionRequest()
            }
        }

        return PermissionsState(
            permissions = state.permissions,
            revokedPermissions = state.revokedPermissions,
            allPermissionsGranted = state.allPermissionsGranted,
            shouldShowRationale = state.shouldShowRationale,
            eventSink = ::handleEvents,
        )
    }
}
