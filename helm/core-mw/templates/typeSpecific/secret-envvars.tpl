{{/*
Creates a configmap containing environment variables for the middleware pods. See the code itself for a list of env variables and values.
This is used in as an envFrom source by the middleware pods.
*/}}

{{- define "core-mw.typeSpecific.secret-envvars.options" -}}
usedKeys:
  - masterAdmin
  - masterPassword
  - hzGroupName
  - hzGroupPassword
  - basicAuthLogin
  - jolokiaLogin
  - jolokiaPassword
{{- end -}}

{{- define "core-mw.typeSpecific.secret-envvars.template" -}}
apiVersion: v1
kind: Secret
metadata:
  name: {{ .ResourceName }}
  namespace: {{ .Context.Release.Namespace | quote }}
  labels:
    {{- include "ox-common.labels.standard" .Context | nindent 4 }}
type: Opaque
data:
  MASTER_ADMIN_USER: {{ ternary (randAlphaNum 8) .Values.masterAdmin (empty .Values.masterAdmin) | b64enc | quote }}
  MASTER_ADMIN_PW: {{ ternary (randAlphaNum 32) .Values.masterPassword (empty .Values.masterPassword) | b64enc | quote }}
  HZ_GROUP_NAME: {{ ternary (randAlphaNum 12) .Values.hzGroupName (empty .Values.hzGroupName) | b64enc | quote }}
  HZ_GROUP_PASSWORD: {{ ternary (randAlphaNum 32) .Values.hzGroupPassword (empty .Values.hzGroupPassword) | b64enc | quote }}
  OX_BASIC_AUTH_LOGIN: {{ ternary (randAlphaNum 8) .Values.basicAuthLogin (empty .Values.basicAuthLogin) | b64enc | quote }}
  OX_BASIC_AUTH_PASSWORD: {{ ternary (randAlphaNum 32) .Values.basicAuthPassword (empty .Values.basicAuthPassword) | b64enc | quote }}
  JOLOKIA_LOGIN: {{ ternary (randAlphaNum 8) .Values.jolokiaLogin (empty .Values.jolokiaLogin) | b64enc | quote }}
  JOLOKIA_PASSWORD: {{ ternary (randAlphaNum 8) .Values.jolokiaPassword (empty .Values.jolokiaPassword) | b64enc | quote }}
  {{- if and (eq (include "ox-common.dcs.ssl.enabled" .Context) "true") }}
    {{- if eq (include "ox-common.dcs.ssl.useInternalCerts" .Context) "true" }}
  DCS_USEINTERNALCERTS: {{ include "ox-common.dcs.ssl.useInternalCerts" .Context | toString | b64enc }}
    {{- else }}
  DCS_SSL_CLIENT_KEYSTORE_PASSWORD: {{ include "ox-common.java.ssl.keyStorePassword" .Context | b64enc }}
  DCS_SSL_CLIENT_KEYSTORE_PATH: {{ "/opt/open-xchange/etc/security/java-ssl.ks" | b64enc }}
      {{- if eq (include "ox-common.java.ssl.useTrustStore" .Context) "true" }}
  DCS_SSL_CLIENT_TRUSTSTORE_PASSWORD: {{ include "ox-common.java.ssl.trustStorePassword" .Context | b64enc }}
  DCS_SSL_CLIENT_TRUSTSTORE_PATH: {{ "/opt/open-xchange/etc/security/java-ssl.ts" | b64enc }}
      {{- end }}
    {{- end }}
  {{- end }}
{{- end -}}
