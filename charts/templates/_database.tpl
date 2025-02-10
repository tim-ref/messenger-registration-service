{{- define "registration-service.backend.postgresql.fullname" -}}
{{- printf "%s-%s" "postgresql" .Release.Name  | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "registration-service.backend.postgresql.labels" -}}
{{ include "registration-service.labels" . }}
app.kubernetes.io/component: database
app.kubernetes.io/part-of: backend
{{- end -}}

{{- define "registration-service.backend.postgresql.host" -}}
{{- if .Values.postgresOperator.enabled -}}
acid-registrationservice
{{- else if .Values.cloudnativePGkeycloak.enabled -}}
{{- printf "%s-%s" (include "registration-service.backend.postgresql.fullname" .) "rw" | trimSuffix "-" -}}
{{- else -}}
{{- fail "You need to enable .Values.cloudnativePGkeycloak.enabled or .Values.postgresOperator.enabled" }}
{{- end -}}
{{- end -}}

{{/*
Set postgres secret
*/}}
{{- define "registration-service.backend.postgresql.secret" -}}
{{- if .Values.postgresOperator.enabled -}}
backend.acid-registrationservice.credentials.postgresql.acid.zalan.do
{{- else if .Values.cloudnativePGkeycloak.enabled -}}
{{- printf "%s-%s" (include "registration-service.backend.postgresql.fullname" .) "app" | trimSuffix "-" -}}
{{- else -}}
{{- fail "You need to enable .Values.cloudnativePGkeycloak.enabled or .Values.postgresOperator.enabled" -}}
{{- end -}}
{{- end -}}


{{- define "registration-service.keycloak.postgresql.fullname" -}}
{{- printf "%s-%s-%s" "kc" "postgresql" .Release.Name  | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "registration-service.keycloak.postgresql.labels" -}}
{{ include "registration-service.labels" . }}
app.kubernetes.io/component: database
app.kubernetes.io/part-of: keycloak
{{- end -}}

{{- define "registration-service.keycloak.postgresql.host" -}}
{{- if .Values.postgresOperator.enabled -}}
jdbc:postgresql://acid-registrationservice:5432/keycloak
{{- else if .Values.cloudnativePGkeycloak.enabled -}}
{{- printf "%s%s-%s" "jdbc:postgresql://" (include "registration-service.keycloak.postgresql.fullname" .) "rw:5432/keycloak" -}}
{{- else -}}
{{- fail "You need to enable .Values.cloudnativePGkeycloak.enabled or .Values.postgresOperator.enabled" -}}
{{- end -}}
{{- end -}}

{{/*
Set postgres secret
*/}}
{{- define "registration-service.keycloak.postgresql.secret" -}}
{{- if .Values.postgresOperator.enabled -}}
keycloak.acid-registrationservice.credentials.postgresql.acid.zalan.do
{{- else if .Values.cloudnativePGkeycloak.enabled -}}
{{- printf "%s-%s" (include "registration-service.keycloak.postgresql.fullname" .) "app" | trimSuffix "-" -}}
{{- else -}}
{{- fail "You need to enable .Values.cloudnativePGkeycloak.enabled or .Values.postgresOperator.enabled" -}}
{{- end -}}
{{- end -}}