{{/*
Creates a configmap with an input file for the oxprops utilty that, upon being run on container start, modifies the .properties files in /opt/open-xchange/etc.  
See values.yaml keys properties and propertiesFiles for example configuration
*/}}

{{- define "core-mw.typeSpecific.properties-configmap.options" -}}
usedKeys:
  - properties
  - propertiesFiles
{{- end -}}

{{- define "core-mw.typeSpecific.properties-configmap.template" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .ResourceName }}
data:
  998_properties.yaml: | 
    anywhere: {{ toYaml .Values.properties | nindent 6}} {{printf "\n    "}}
{{- range $filename, $contentMap := .Values.propertiesFiles -}}
    {{ $filename }}: {{ toYaml $contentMap | nindent 6 }} {{printf "\n    "}}
{{- end }}  
{{- end -}}
