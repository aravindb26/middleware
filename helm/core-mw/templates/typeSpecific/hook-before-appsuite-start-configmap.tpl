
{{/*
Creates a configmap containing shell scripts that are run immediately before the middleware is started, after the configfiles have been
adjusted by oxprops. See the hooks: section in values.yaml
*/}}
{{- define "core-mw.typeSpecific.hook-before-appsuite-start-configmap.options" -}}
getValuesTemplate: "core-mw.typeSpecific.hook-before-appsuite-start-configmap.values"
{{- end -}}

{{- define "core-mw.typeSpecific.hook-before-appsuite-start-configmap.values" -}}
{{- $hooks := dict -}}
{{- if .Values.hooks -}}
{{-   if .Values.hooks.beforeAppsuiteStart -}}
{{-      $hooks = .Values.hooks.beforeAppsuiteStart -}}
{{-   end -}}
{{- end -}}
{{ $hooks | toYaml }}
{{- end -}}

{{- define "core-mw.typeSpecific.hook-before-appsuite-start-configmap.template" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .ResourceName }}
data: {{- range $filename, $content := .Values.hooks.beforeAppsuiteStart }}
  {{ $filename  }}: | {{ $content | nindent 4 }} {{printf "\n  "}}
  {{- end }}  
{{- end -}}
