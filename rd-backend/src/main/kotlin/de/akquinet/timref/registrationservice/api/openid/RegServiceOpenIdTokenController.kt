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

package de.akquinet.timref.registrationservice.api.openid

import de.akquinet.timref.registrationservice.service.openid.ErrorResult
import de.akquinet.timref.registrationservice.service.openid.MatrixTokenToRegServiceOpenIdTokenConverterService
import de.akquinet.timref.registrationservice.service.openid.SuccessResult
import de.akquinet.timref.registrationservice.service.openid.TokenConverterResultErrorType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

interface TokenResponse

data class TokenResponseSuccess(
    val access_token: String,
    val expires_in: Long,
) : TokenResponse {
    val token_type: String = "Bearer"
}

data class TokenResponseError(
    val error: String
) : TokenResponse


private const val RESULT_EXAMPLE: String = "{\n" +
        "  \"access_token\": \"eyJ4NWMiOlsiTUlJRE1EQ0NBaGdDRkNRN3dmMFFSZ0VkL1pld2I4OE1EdDl6dGdhU01BMEdDU3FHU0liM0RRRUJDd1VBTUZNeEN6QUpCZ05WQkFZVEFrUkZNUk13RVFZRFZRUUlEQXBUYjIxbExWTjBZWFJsTVJVd0V3WURWUVFLREF4R1lXdGxJRWRsYldGMGFXc3hHREFXQmdOVkJBTU1EMFpoYTJVZ1IyVnRZWFJwYXlCRFFUQWdGdzB5TXpBeU1ETXhNek0xTWpkYUdBOHlNRFV4TURJeE5qRXpNelV5TjFvd1ZERUxNQWtHQTFVRUJoTUNSRVV4RXpBUkJnTlZCQWdNQ2xOdmJXVXRVM1JoZEdVeEVUQVBCZ05WQkFvTUNHRnJjWFZwYm1WME1SMHdHd1lEVlFRRERCUnlaV2RwYzNSeWFXVnlkVzVuYzJScFpXNXpkRENDQVNJd0RRWUpLb1pJaHZjTkFRRUJCUUFEZ2dFUEFEQ0NBUW9DZ2dFQkFNV2xiMEMxaTBua0ljQkt1cVZVL0Y1NVdLRC9DeXluSVVUdFhDN1ZjaFVFWjZob1VMakh3MnhwOEtTTitzTDZEWnBQL3IxaHlSeWV2V3dkem1mM2NBdk15RUFuM0FGSVBTVTdsWTR0SklLZ1hOb2FyTWpxY05XQzhjZDRQWmhLb3JZSEVjR1ptRGxmaGVObzlVVVB3UDBhQk5rM04rejJxZDY2MVVLeVhPNHJwSTVEUmZuOVk5Z3haREc5UXZTK1JYOERzME00ME5ycU1jZE8zREdwYVVJZithbWtkOUxWcWR5bmlEbTlUd2pudklxWnEyNUNlTmlCZWFJc3dtbjVSRTdweWJDR0ZyWG1JaHoySVNwZlZYNmR0U3pDSDA2K1ZCbExUTzRIbjZWUUROTXRYV294cU55cTR0U2kxOWVaZFZDclh4NDQzYWFpOVloQU8wY09rdmNDQXdFQUFUQU5CZ2txaGtpRzl3MEJBUXNGQUFPQ0FRRUFJeTNRQ3VMTTFjMm9zQVFjSWJDTGpBYVQxaEt3NDhvN3U4K254Vm1ZQ2FDWndPR3ZGNTlaeEp0RzErWktYdmlTalNTYXZscElLNVdWYkYyUDZEVE1UMlR1MzRLYnlwM3Z5eUJ3YTRUVFJ2alY2cVFHeVBxL1B4OHNlN01tSVgyT1YxYWZPV0phcm5oZWVKZkdFcUcxdDZxMGwwdytCSDBucHp0dE9ZU3Q0U2c2MHBkaW1HMnJITzkxSWhja1VBQ3FJbk5uczJUYVN6UlIvM0I1clhWR1BCcWMrYWFIUUl0LzRwVmN3VS9JMTY5VWh1QVpZRVN1QlVqRXE2ZlZlcml2NUlSWGxhYy9lSHZ6Q1RGUXhZWGRva3UzRW11UUJwbEJBTGlWMzdoOWkxTE94S2tVNEpyLzJldkdLQmxtbHhaOXd5SHF6SGV1OTliZEc5YW5pME1sc3c9PSIsIk1JSURpVENDQW5HZ0F3SUJBZ0lVUit1aHpZK1MvTHl6TkJnNDBENGFyTGcvb1RZd0RRWUpLb1pJaHZjTkFRRUxCUUF3VXpFTE1Ba0dBMVVFQmhNQ1JFVXhFekFSQmdOVkJBZ01DbE52YldVdFUzUmhkR1V4RlRBVEJnTlZCQW9NREVaaGEyVWdSMlZ0WVhScGF6RVlNQllHQTFVRUF3d1BSbUZyWlNCSFpXMWhkR2xySUVOQk1DQVhEVEl6TURJd016RXpNekUwTkZvWUR6SXdOVEV3TWpFMk1UTXpNVFEwV2pCVE1Rc3dDUVlEVlFRR0V3SkVSVEVUTUJFR0ExVUVDQXdLVTI5dFpTMVRkR0YwWlRFVk1CTUdBMVVFQ2d3TVJtRnJaU0JIWlcxaGRHbHJNUmd3RmdZRFZRUUREQTlHWVd0bElFZGxiV0YwYVdzZ1EwRXdnZ0VpTUEwR0NTcUdTSWIzRFFFQkFRVUFBNElCRHdBd2dnRUtBb0lCQVFDWWdHK25PMzVLZXhCS21jellJaW1RMVlwK1lmWkhJT2N6ZmtFczlCTGo2RytYMC9KYitpQXhkbzNvNkpaNlNwcHRTYkdxd2ZtUjlzbEJ5QndZazl6bTJ3TDZKTFVpTTV1R05GRjhITmt2QnJKa2FPbU9VQXVjR1BXdGQrcHRBTkVCeDNyaExaVDRZWFNLcjBQcnM2ZzZuWDh4QmduUnc1ZzBtK3ZUd2NWYkpLSUZqZ0pFb0I5TnJIMHpKWlp0K1IyQjAxWGhzRjB2MVd5L1p4Q2NKZWxrblpLSkpQNldyTTE3eVAvMmFGRkMzY1RKaHcydFgxeGtlYmFWWVJ2M0l4a0NOczRScW5sU0I1TVhTVXlWd3hKbnVYN1RIbUJITGdDTURTeFVGaUtTN01wWnJjYm9kZVMwU1U3eGw2TlZzT1pWc0VqMTl5SndVZllnUVBib0hQWHBBZ01CQUFHalV6QlJNQjBHQTFVZERnUVdCQlI3NFJuaXpkM3hCRVV3QmJSSm1EMTQxa05BYWpBZkJnTlZIU01FR0RBV2dCUjc0Um5pemQzeEJFVXdCYlJKbUQxNDFrTkFhakFQQmdOVkhSTUJBZjhFQlRBREFRSC9NQTBHQ1NxR1NJYjNEUUVCQ3dVQUE0SUJBUUNPMkNKYzNGZzk2Y2t1RUliUnlwZGNPVG4yNTBlMXFWVmp6NDVNdk0wVWtBR01HaUxBSFI4S0M0UEhIUDAveDEzMHlpVXFIU0hITDNGbk9xWVU3RlRaTjg4MW5YQmRoNnpOc1RzN0tUMXVnbXhrZzJja21qRU1yeFN6alk3SWZEYTlrRjVPWHdKblBxdGhLL05ObTVNby81c2MzRHBpNzJuSUVSTUlkV216cVVnUko4SWRublF0WXU0aVFQQ215T0lZUXgrVFhVM2xpUmUrRmZ5dHMyd1JMTURNOEh3MnQ0UndYUlZWOXJoam42RVoxem9udlJlRDJvLzJDTVpoTjhLdndUdWVLdGlVcGxMdlpDMlYyWW1wUzMva0lUVHJQM2pKc25VMGVHZkR3QnoxeHRMMDFJcGdJbEFZWXJCbkV6OVdqcEZ3TStFREZLWS9odG5yc3dpZSJdLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJAT3JnQWRtaW46dGltLmFrcXVpbmV0LmRlIiwiYXVkIjpbImF1ZDEiLCJhdWQyIl0sInByb2Zlc3Npb25PSUQiOiIxLjIuMy40IiwiaWROdW1tZXIiOiJhcWstMTIzIiwiaXNzIjoiaXNzdWVyIiwiZXhwIjoxNjc1Njk1NDM0LCJpYXQiOjE2NzU2OTE4MzR9.w86bOsh9vzjAwOtjUFsOAO4nnXTJt5P3t_4FtF7yxcwLj-5nbPdZCbxCaMRnH-pA4swsO7hRk8hqjhAt4X2PtsNIs1T5rhjQz6DySa9zQZ8KS2d0SLGNCElXrrA7M02jVd2MaxmSvEV0BZTSVICKYVrnrbYe8H-QQ56uknFQ3UeWu0ttEjIynp8Gz6mVI3tD_kjkktqKWICCKZJ6ZzDiGxy5o-RrKenl_FQ3upRdPRdav-JXv8QG8eYHUF7cuPH5EIxhEjFt4pC-0Fz3-QbVV5wsMjBk3vK26IrlhvcATef0hkV99EZZnL8X3rnIInlsf7fvPMf3rRSQCSoqDmdPnA\",\n" +
        "  \"expires_in\": 3600,\n" +
        "  \"token_type\": \"Bearer\"\n" +
        "}"

@RestController
@Tag(
    name = "Regservice-OpenId-Token",
    description = "API for the org admin to obtain owner access tokens to access the VZD."
)
@ApiResponses(
    value = [ApiResponse(
        responseCode = "500", description = "Internal Server Error", content = [Content()]
    )]
)
class RegServiceOpenIdTokenController(
    private val tokenConverter: MatrixTokenToRegServiceOpenIdTokenConverterService
) {

    @PostMapping(value = ["/regservice/openid/user/{userId}/requesttoken"])
    @Operation(summary = "Obtain Regservice-OpenId-Token.")
    @ApiResponses(
        value = [
            ApiResponse(
                description = "Successfully obtained a token", responseCode = "200", content = [Content(
                    examples = [
                        ExampleObject(RESULT_EXAMPLE)
                    ]
                )]
            ),
            ApiResponse(
                description = "Either the provided access_token or the userId was bad.",
                responseCode = "400",
                content = [Content()]
            )
        ]
    )

    //request_token from matrix OpenIDToken  https://spec.matrix.org/v1.6/client-server-api/#post_matrixclientv3useruseridopenidrequest_token
    fun exchangeSynapseAccessCodeForRegServiceOpenIdToken(
        @PathVariable userId: String,
        @RequestParam("request_token") request_token: String
    ): ResponseEntity<TokenResponse> =
        when (val tokenConvertResult = tokenConverter.convertTokenForUser(userId, request_token)) {
            is SuccessResult -> ResponseEntity.ok(
                TokenResponseSuccess(tokenConvertResult.accessToken, tokenConvertResult.expiresIn)
            )

            is ErrorResult -> when (tokenConvertResult.errorType) {
                TokenConverterResultErrorType.UNAUTHORIZED -> {
                    // as per openid spec https://openid.net/specs/openid-connect-core-1_0.html#TokenErrorResponse
                    ResponseEntity
                        .status(400)
                        .body(TokenResponseError("unauthorized"))
                }

                TokenConverterResultErrorType.USER_INPUT -> {
                    ResponseEntity
                        .badRequest()
                        .body(TokenResponseError(tokenConvertResult.message ?: "user input error"))
                }
            }
        }
}