/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package de.akquinet.timref.registrationservice.rawdata.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class RawData(
    @get:JsonProperty("Inst-ID")
    var `Inst-ID`: String? = "TIM-registration-service",
    @get:JsonProperty("UA-PTV")
    var `UA-PTV`: String?,
    @get:JsonProperty("UA-PV")
    var `UA-PV`: String?,
    @get:JsonProperty("UA-A")
    var `UA-A`: String?,
    @get:JsonProperty("UA-P")
    var `UA-P`: String?,
    @get:JsonProperty("UA-OS")
    var `UA-OS`: String?,
    @get:JsonProperty("UA-OS-VERSION")
    var `UA-OS-VERSION`: String?,
    @get:JsonProperty("UA-cid")
    var `UA-cid`: String?,
    @get:JsonProperty("M-Dom")
    var `M-Dom`: String?,
    @get:JsonProperty("sizeIn")
    var sizeIn: Int?,
    @get:JsonProperty("sizeOut")
    var sizeOut: Int?,
    @get:JsonProperty("tID")
    var tID: String,
    @get:JsonProperty("profOID")
    var profOID: String,
    @get:JsonProperty("Res")
    var Res: String
)
data class RawDataMetaData(
    val start: Instant,
    val durationInMs: Int,
    val operation: Operation,
    val status: String,
    val message: RawData
)

enum class Operation(private val value: String) {
    RS_LOGIN("TIM.UC_100 60_01"),
    RS_CREATE_MESSENGER_SERVICE("TIM.UC_100 60_02"),
    RS_ADD_MESSENGER_SERVICE_TO_FEDERATION("TIM.UC_100 60_03");

    override fun toString() = value
}
