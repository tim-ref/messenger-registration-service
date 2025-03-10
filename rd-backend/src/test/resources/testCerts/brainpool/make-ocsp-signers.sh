#!/usr/bin/env bash

#
# Copyright (C) 2025 akquinet GmbH
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

set -e
set -u
set -o pipefail

command -v openssl &>/dev/null || {
	printf "openssl is required\n"
	exit 1
}

issue_ocsp_signer() {
	local name=$1
	local signer_crt=$2
	local signer_key=$3

	openssl pkcs8 \
		-topk8 \
		-nocrypt \
		-inform PEM -in <(openssl ecparam -name brainpoolP256r1 -genkey) \
		-out priv-key."$name".pkcs8.pem

	openssl x509 \
		-new \
		-CA "$signer_crt" -CAkey "$signer_key" \
		-set_subject /CN="$name" \
		-force_pubkey priv-key."$name".pkcs8.pem \
		-out "$name".crt \
		-extensions ocsp_signer_extensions -extfile ca.conf
}

issue_ocsp_signer ocsp-signer-ca ca.crt priv-key.ca.pkcs8.pem
issue_ocsp_signer ocsp-signer-intermediate intermediate.crt priv-key.intermediate.pkcs8.pem
