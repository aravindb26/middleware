{{/*
Creates a configmap for adding text files to /opt/open-xchange/etc. You can specify the file content verbatim. All entries below etcFiles are consolidated 
into one configmap. The files are mounted at /injections/etc/etc in the container.
and copied to /opt/open-xchange/etc/ on container start. See the values.yaml for commented out examples below the etcFiles key for how to specific files
*/}}

{{- define "core-mw.typeSpecific.etc-files-configmap.options" -}}
usedKeys:
  - etcFiles
{{- end -}}

{{- define "core-mw.typeSpecific.etc-files-configmap.template" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .ResourceName }}
data: {{- range $filename, $content := .Values.etcFiles }}
  {{ $filename  }}: | {{ $content | nindent 4 }} {{printf "\n  "}}
  {{- end }}  
{{- end -}}
