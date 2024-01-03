{{/*
Creates context set configuration for the config cascade. This results in a file at /opt/open-xchange/etc/contextSets/context-sets-overrides.yaml. Specify context set 
configuration in the key "contextSets"

contextSets:
  brand.com:
    withTags: brand.com
    com.openexchange.sessiond.maxSession: 10

*/}}

{{- define "core-mw.typeSpecific.contextsets-configmap.options" -}}
usedKeys:
  - contextSets
{{- end -}}

{{- define "core-mw.typeSpecific.contextsets-configmap.template" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .ResourceName }}
data:
  context-sets-overrides.yaml: | {{ toYaml .Values.contextSets | nindent 4}}
{{- end -}}
