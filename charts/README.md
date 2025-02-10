# Registration Service


## Create Namespace
```shell
kubectl create namespace registration-service
```
## Create temporary Alias
```shell
alias createPassPhrase='echo $(pwgen -sncv -1)-$(pwgen -sncv -1)-$(pwgen -sncv -1)-$(pwgen -sncv -1)'
```

## Create Postgres Password
Wird nur gebraucht, wenn man eine PostgreSQL aus dem Bitnami Chart bezieht, nicht bei externer Datenbank oder dem Operator!
```shell
kubectl create secret generic registration-service-postgresql --from-literal="postgres-password=$(createPassPhrase)" --from-literal="password=$(createPassPhrase)" --from-literal="replication-password=$(createPassPhrase)" -n registration-service
```

## Create Keycloak Admin User
```shell
kubectl create secret generic keycloak-admin-user --from-literal="username=$(createPassPhrase)" --from-literal="password=$(createPassPhrase)" -n registration-service
```

## Create Secret for Certs and Privkey
Hier werden die Zertifikate und Privatekeys deployd, die wir von der gematik erhalten habe, um mit dem VZD zu sprechen.
### Dieser Prozess funktioniert noch nicht, hier müssen wir uns nochmal was überlegen.
```shell
kubectl create secret generic registration-service-openid --from-literal="privkey=$(az keyvault secret show --vault-name TIMRef-shared -n XU-privkey-ecc-pkcs8-timref-regdienst --query value -o tsv |base64 --decode)" --from-literal="cert=$(az keyvault secret show --vault-name TIMRef-shared -n XU-cert-ecc-timref-regdienst --query value -o tsv | base64 --decode)" --from-literal="cacert=$(az keyvault secret show --vault-name TIMRef-shared -n XU-ca-cert-ecc-timref-regdienst --query value -o tsv | base64 --decode)" -n registration-service
```

## Create Secret for VZD
```shell
kubectl create secret generic registration-service-vzd-client-secret --from-literal="secret=$(az keyvault secret show --vault-name TIMRef-shared -n XU-registrierungs-dienst-vzd-secret --query value -o tsv)" -n registration-service
```

## Create Secrets for Operator API
```shell
kubectl create secret generic operator-auth-user --from-literal="username=$(createPassPhrase)" --from-literal="password=$(createPassPhrase)" -n registration-service
```

## Create smtp password secret
```shell
kubectl create secret generic registration-service-smtp-password  --from-literal="password=$(az keyvault secret show --vault-name TIMRef-infra -n mail-svc-tim --query value -o tsv)" -n registration-service
```

## Create keycloak client secret
```shell
kubectl create secret generic keycloak-client-registration-service  --from-literal="secret=$(createPassPhrase)" -n registration-service
```

## Create keycloak client secret for KEYCLOAK CLIENT REGISTRATION SERVICE LIFETIME CHECK
```shell
kubectl create secret generic keycloak-client-registration-service-lifetime-check  --from-literal="secret=$(createPassPhrase)" -n registration-service
```

## Create Keycloak AzureID oidc client Secrets
```shell
kubectl create secret generic keycloak-azuread-oidc --from-literal="clientid=$(az keyvault secret show --vault-name TIMRef-infra -n timref-keycloak-oidc-clientid --query value -o tsv)" --from-literal="clientsecret=$(az keyvault secret show --vault-name TIMRef-infra -n timref-keycloak-oidc-clientsecret --query value -o tsv)"
```

## Create test user & password secret
```shell
kubectl create secret generic registration-service-user-password  --from-literal="username=$(createPassPhrase)" --from-literal="password=$(createPassPhrase)" -n registration-service
```
After we created the Test username & password, we need to push the into the azure Keyvault, so that developer without Kubernetes access can test.
```shell
az keyvault secret set -n $ENV-registrierungs-dienst-keycloak-user-user --vault-name TIMRef-shared --value $(k get secrets -n registration-service registration-service-user-password -ojsonpath='{.data.username}' |base64 --decode)
az keyvault secret set -n $ENV-registrierungs-dienst-keycloak-user-password --vault-name TIMRef-shared --value $(k get secrets -n registration-service registration-service-user-password -ojsonpath='{.data.password}' |base64 --decode)
```

## Create Secret for trust store password
```shell
kubectl create secret generic komp-truststore-password --from-literal="password=$(az keyvault secret show --vault-name TIMRef-shared -n XU-komp-truststore-password --query value -o tsv)" -n registration-service
```
## Create Truststore as Secret
```shell
backend/bin/createKeystore.sh $env
```
```shell
kubectl create secret generic komp-truststore --from-file komp-truststore.p12
```

```shell
rm -rf komp-truststore.p12
```

## Deploy Registration Service
```shell
helm upgrade --install registration-service . --namespace registration-service  --create-namespace -f values.yaml
```
