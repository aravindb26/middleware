{{/*
Creates a configmap containing shell scripts that are run before the oxprops tool applies configuration overwrites. See hooks in values.yaml
*/}}
{{- define "core-mw.typeSpecific.hook-before-apply-configmap.options" -}}
getValuesTemplate: "core-mw.typeSpecific.hook-before-apply-configmap.values"
{{- end -}}

{{- define "core-mw.typeSpecific.hook-before-apply-configmap.values" -}}
{{- $hooks := dict -}}
{{- if .Values.hooks -}}
{{-   if .Values.hooks.beforeApply -}}
{{-      $hooks = .Values.hooks.beforeApply -}}
{{-   end -}}
{{- end -}}
{{ $hooks | toYaml }}
{{- end -}}

{{- define "core-mw.typeSpecific.hook-before-apply-configmap.template" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .ResourceName }}
data: {{- range $filename, $content := .Values.hooks.beforeApply }}
  {{ $filename  }}: | {{ $content | nindent 4 }} {{printf "\n  "}}
  {{- end }}  
{{- end -}}
