{{/*
Creates a configmap with an input file for the oxprops utilty that, upon being run on container start, modifies the .properties files in /opt/open-xchange/etc. 
Properties configured here will overwrite those properties from values.yaml keys 'properties' and 'propertiesFiles'.
*/}}

{{- define "core-mw.typeSpecific.properties-overwrite-configmap.options" -}}
usedKeys:
  - properties
  - propertiesFiles
{{- end -}}

{{- define "core-mw.typeSpecific.properties-overwrite-configmap.template" -}}
{{- $properties := dict }}
{{- if ((.Values).update).enabled }}
  {{- $_ := set $properties "com.openexchange.preupgrade.enabled" .Values.update.enabled }}
{{- if ((.Values).update).schemata }}
  {{- $_ := set $properties "com.openexchange.preupgrade.database.schemata" .Values.update.schemata }}
{{- end }}
  {{- $_ := set $properties "com.openexchange.database.cleanup.enabled" "false" }}
{{- end -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .ResourceName }}
data:
  999_properties_overwrite.yaml: | 
    {{- if $properties }}
    anywhere:
      {{- range $key, $value := $properties -}}
        {{- printf "%s: %s" $key ($value | quote) | nindent 6  }}
      {{- end }}
    {{- else }}
    anywhere: {}
    {{- end }}
{{- end -}}
