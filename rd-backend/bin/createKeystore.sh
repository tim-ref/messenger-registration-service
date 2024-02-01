#!/bin/bash
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

env=$1

if [[ $env == ru ]]
then
  downloadURL="download-ref.tsl.ti-dienste.de"
elif [[ $env == tu ]]
then
  downloadURL="download-test.tsl.ti-dienste.de"
else
  echo "Please set ru or tu"
  exit 1
fi

keystoreSecret=$(az keyvault secret show --vault-name TIMRef-$env -n komp-truststore-password --query value -o tsv)

for cert in $(curl https://$downloadURL/ECC/SUB-CA/ | grep -o "GEM.KOMP.*der" | cut -d '"' -f1 | grep -v sha256 | grep -v txt)
do
  wget "https://$downloadURL/ECC/SUB-CA/$cert" -O /tmp/$cert
  keytool -importcert -file /tmp/$cert -alias $cert -keystore komp-truststore.p12 -storetype pkcs12 -noprompt -storepass $keystoreSecret
done
