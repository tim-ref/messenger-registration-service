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

package de.akquinet.timref.registrationservice.security.signature.jose4extension

import org.jose4j.jwa.AlgorithmFactoryFactory
import org.jose4j.keys.EllipticCurves


object Brainpool {
    private var installed = false
    fun installExtension() {
        if (installed) {
            return
        }
        installed = true
        EllipticCurves.addCurve(BP256R1Algorithm.CURVE, BP256R1Algorithm.EC_PARAMETER_SPEC)
        AlgorithmFactoryFactory.getInstance().jwsAlgorithmFactory
            .registerAlgorithm(BP256R1Algorithm)
    }
}