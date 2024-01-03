{{/*
Creates a secret for adding yaml files with sensitive content to /opt/open-xchange/etc. You can specify the file content as a yaml subtree. All entries below secretYAMLFiles are consolidated 
into one secret. The files are mounted at /injections/etc/secretYaml in the container.
and copied to /opt/open-xchange/etc/ on container start. See the values.yaml for commented out examples below the secretYAMLFiles key for how to specific files
*/}}

{{- define "core-mw.typeSpecific.yaml-secrets-secret.options" -}}
usedKeys:
  - secretYAMLFiles
{{- end -}}

{{- define "core-mw.typeSpecific.yaml-secrets-secret.template" -}}
apiVersion: v1
kind: Secret
metadata:
  name: {{ .ResourceName }}
data: {{- range $relativePath, $content := .Values.secretYAMLFiles }}
  {{ splitList "/" $relativePath | last }}: {{ toYaml $content | b64enc }} {{printf "\n  "}}
{{- end }}
{{- end -}}
