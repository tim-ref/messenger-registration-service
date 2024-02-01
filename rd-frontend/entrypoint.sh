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
#

dl=$(echo -en '\x1e')

# param 1: file name
# param 2: string to replace / name of env variable
function checkAndReplaceEnvVar() {
  mainFile=$(find "./" -name "$1")
  unsetOccurences=$(grep -c "NOT_SET_$2" "$mainFile")
  if [[ "$unsetOccurences" == 0 ]]; then
    # parameter was set already -> keep it
    echo "Use default value for parameter $2 in main"
  else
    # parameter is not set yet
    if [[ "${!2}" ]]; then
      # Environment variable set -> set parameter
      sed -i -e "s${dl}NOT_SET_$2${dl}${!2}${dl}g" "$mainFile"
      echo "Set parameter from environment variable $2 in main: ${!2}"
    else
      # Environment variable not set -> stop
      echo "Error. Environment variable missing ($2 expected)"
      exit 1
    fi
  fi
}

cd /usr/share/nginx/html/ || exit
checkAndReplaceEnvVar main*.js API_URL
checkAndReplaceEnvVar main*.js KEYCLOAK_URL
checkAndReplaceEnvVar main*.js REDIRECT_URI
checkAndReplaceEnvVar main*.js ORG_ADMIN_URI
checkAndReplaceEnvVar main*.js FACHDIENST_META_URL

exec nginx -g "daemon off;"
