{{/*
Creates a configmap containing shell scripts that are run immediately on container start before anything else. See the hooks: section in values.yaml
*/}}

{{- define "core-mw.typeSpecific.hook-start-configmap.options" -}}
getValuesTemplate: "core-mw.typeSpecific.hook-start-configmap.values"
{{- end -}}

{{- define "core-mw.typeSpecific.hook-start-configmap.values" -}}
{{- $hooks := dict -}}
{{- if .Values.hooks -}}
{{-   if .Values.hooks.start -}}
{{-      $hooks = .Values.hooks.start -}}
{{-   end -}}
{{- end -}}
{{ $hooks | toYaml }}
{{- end -}}

{{- define "core-mw.typeSpecific.hook-start-configmap.template" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .ResourceName }}
data: {{- range $filename, $content := .Values.hooks.start }}
  {{ $filename  }}: | {{ $content | nindent 4 }} {{printf "\n  "}}
  {{- end }}  
{{- end -}}
