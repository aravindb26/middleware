{{/*
Creates a secret for adding text files containing sensitive information to /opt/open-xchange/etc. You can specify the file content verbatim. All entries below secretETCFiles are consolidated 
into one secret. The files are mounted at /injections/etc/secretEtc in the container.
and copied to /opt/open-xchange/etc/ on container start. See the values.yaml for commented out examples below the secretETCFiles key for how to specific files
*/}}

{{- define "core-mw.typeSpecific.etc-secrets-secret.options" -}}
usedKeys:
  - secretETCFiles
{{- end -}}

{{- define "core-mw.typeSpecific.etc-secrets-secret.template" -}}
apiVersion: v1
kind: Secret
metadata:
  name: {{ .ResourceName }}
data: {{- range $filename, $content := .Values.secretETCFiles }}
  {{ $filename  }}: {{ $content | b64enc }} {{printf "\n  "}}
{{- end }}  
{{- end -}}
