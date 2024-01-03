{{/*
Creates a configmap containing a file that winds up at /opt/open-xchange/etc/meta/meta-overrides.yaml. Just specify the contents of that file directly under the "meta" key: 

meta:
  io.ox/core//design:
    protected: true
  io.ox/core//someProp:
    protected: false
  io.ox/core//apps/quickLaunchCount:
    protected: false
*/}}

{{- define "core-mw.typeSpecific.meta-configmap.options" -}}
usedKeys:
  - meta
{{- end -}}

{{- define "core-mw.typeSpecific.meta-configmap.template" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .ResourceName }}
data:
  meta-overrides.yaml: | {{ toYaml .Values.meta | nindent 4}}
{{- end -}}
