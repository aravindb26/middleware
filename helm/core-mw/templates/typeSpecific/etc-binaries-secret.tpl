{{/*
Creates secrets for adding binary files to /opt/open-xchange/etc. All files listed under secretETCBinaries result in an individual secret that is mounted at /injections/etc/secretEtc
and copied to /opt/open-xchange/etc/ on container start. See the values.yaml for commented out examples below the secretETCBinaries key for how to specific files
*/}}

{{- define "core-mw.typeSpecific.etc-binaries-secret.options" -}}
usedKeys:
  - secretETCBinaries
{{- end -}}

{{- define "core-mw.typeSpecific.etc-binaries-secret.template" -}}
{{ $context := . }}
{{ range .Values.secretETCBinaries }}
apiVersion: v1
kind: Secret
metadata:
  name: {{ $context.ResourceName }}-{{ .name }}
data: 
  {{ .filename  }}: {{ .b64Content }}
---
{{ end }}
{{- end -}}
