{{/*
Creates context set configuration for the config cascade as a secret. This results in a file at /opt/open-xchange/etc/contextSets/context-sets-secret-overrides.yaml. 
Use this for sensitive configuration values. Uses the key secretContextSets

secretContextSets:
  brand.com:
    withTags: brand.com
    com.openexchange.secretKey: abc

*/}}

{{- define "core-mw.typeSpecific.contextsets-secret.options" -}}
usedKeys:
  - secretContextSets
{{- end -}}

{{- define "core-mw.typeSpecific.contextsets-secret.template" -}}
apiVersion: v1
kind: Secret
metadata:
  name: {{ .ResourceName }}
data:
  context-sets-secret-overrides.yaml: {{ toYaml .Values.secretContextSets | b64enc }}
{{- end -}}
    