{{/*
Creates a configmap for adding binary files to /opt/open-xchange/etc. All files listed under etcBinaries result in an individual config map that is mounted at /injections/etc/etc 
and copied to /opt/open-xchange/etc/ on container start. See the values.yaml for commented out examples below the etcBinaries key for how to specific files
*/}}

{{- define "core-mw.typeSpecific.etc-binaries-configmap.options" -}}
usedKeys:
  - etcBinaries
{{- end -}}

{{- define "core-mw.typeSpecific.etc-binaries-configmap.template" -}}
{{ $context := . }}
{{ range .Values.etcBinaries }}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ $context.ResourceName }}-{{ .name }}
binaryData: 
  {{ .filename  }}: {{ .b64Content }}
---
{{ end }}
{{- end -}}
