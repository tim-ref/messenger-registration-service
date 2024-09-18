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

package de.akquinet.tim.registrationservice.security.signature.jose4extension

import org.jose4j.jws.EcdsaUsingShaAlgorithm
import org.jose4j.jws.JsonWebSignatureAlgorithm
import java.math.BigInteger
import java.security.spec.ECFieldFp
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.EllipticCurve

object BP256R1Algorithm : EcdsaUsingShaAlgorithm("BP256R1", "SHA256withECDSA", "BP-256", 64), JsonWebSignatureAlgorithm {
    val EC_PARAMETER_SPEC = ECParameterSpec(
        EllipticCurve(
            ECFieldFp(
                BigInteger("76884956397045344220809746629001649093037950200943055203735601445031516197751")
            ),
            BigInteger("56698187605326110043627228396178346077120614539475214109386828188763884139993"),
            BigInteger("17577232497321838841075697789794520262950426058923084567046852300633325438902")
        ),
        ECPoint(
            BigInteger("63243729749562333355292243550312970334778175571054726587095381623627144114786"),
            BigInteger("38218615093753523893122277964030810387585405539772602581557831887485717997975")
        ),
        BigInteger("76884956397045344220809746629001649092737531784414529538755519063063536359079"),
        1
    )
    const val CURVE = "BP-256"
    const val ALGORITHM = "BP256R1"
}