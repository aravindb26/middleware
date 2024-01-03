{{/*
Creates a configmap for adding yaml files to /opt/open-xchange/etc. You can specify the file content as a yaml subtree. All entries below yamlFiles are consolidated 
into one configmap. The files are mounted at /injections/etc/yaml in the container.
and copied to /opt/open-xchange/etc/ on container start. See the values.yaml for commented out examples below the yamlFiles key for how to specific files
*/}}

{{- define "core-mw.typeSpecific.yaml-files-configmap.options" -}}
usedKeys:
  - yamlFiles
{{- end -}}

{{- define "core-mw.typeSpecific.yaml-files-configmap.template" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .ResourceName }}
data: {{- range $relativePath, $content := .Values.yamlFiles }}
  {{ splitList "/" $relativePath | last }}: |
    {{ toYaml $content | nindent 4 }} {{printf "\n  "}}
  {{- end }}
{{- end -}}
