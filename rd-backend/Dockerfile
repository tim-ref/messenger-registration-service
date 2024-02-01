#
# Copyright (C) 2023 akquinet GmbH
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
FROM eclipse-temurin:17-jre

LABEL maintainer="TIMREF Maintainers"

WORKDIR /app

ARG JAR_FILE

HEALTHCHECK --interval=30s --timeout=5s --retries=5 CMD wget --quiet --tries=1 --spider http://localhost:8081/actuator/health/liveness

COPY java.security /app
COPY ${JAR_FILE} app.jar

ENTRYPOINT [ "java", \
    "-Djava.security.properties==/app/java.security", \
    "-Djdk.tls.namedGroups=brainpoolP384r1tls13,brainpoolP256r1tls13,brainpoolP384r1,brainpoolP256r1,secp384r1,secp256r1", \
    "-Dsun.security.ssl.allowLegacyHelloMessages=false", \
    "-Dlog4j2.formatMsgNoLookups=true", \
    "-Xms1024m", \
    "-Xmx3072m", \
    "-jar", \
    "app.jar" \
]
