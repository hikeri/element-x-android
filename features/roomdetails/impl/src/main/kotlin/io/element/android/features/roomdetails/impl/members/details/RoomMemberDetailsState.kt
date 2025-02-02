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

package io.element.android.features.roomdetails.impl.members.details

import io.element.android.libraries.architecture.Async

data class RoomMemberDetailsState(
    val userId: String,
    val userName: String?,
    val avatarUrl: String?,
    val isBlocked: Async<Boolean>,
    val displayConfirmationDialog: ConfirmationDialog? = null,
    val isCurrentUser: Boolean,
    val eventSink: (RoomMemberDetailsEvents) -> Unit
) {
    enum class ConfirmationDialog {
        Block, Unblock
    }
}
