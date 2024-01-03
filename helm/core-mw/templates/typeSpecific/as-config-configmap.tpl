{{/*
Creates a configmap containing the as-config.yml. Specify the yaml directly under the asConfig key

asConfig:
  myhost:
    host: myexchange.myhost.mytld
    someConfig: some overriding value

*/}}

{{- define "core-mw.typeSpecific.as-config-configmap.options" -}}
usedKeys:
  - asConfig
{{- end -}}

{{- define "core-mw.typeSpecific.as-config-configmap.template" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .ResourceName }}
data:
  as-config.yml: | {{ toYaml .Values.asConfig | nindent 4}}
{{- end -}}
