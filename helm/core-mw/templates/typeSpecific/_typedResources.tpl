{{/* Be sure to add all resource names that should be type specific here! */}}
{{- define "core-mw.typedResources.resources" -}}
resources:
    - as-config-configmap
    - contextsets-configmap
    - contextsets-secret
    - secret-envvars
    - etc-binaries-configmap
    - etc-binaries-secret
    - etc-files-configmap
    - etc-secrets-secret
    - hook-before-apply-configmap
    - hook-before-appsuite-start-configmap
    - hook-start-configmap
    - meta-configmap
    - mysql-secret
    - properties-lean-configmap
    - properties-configmap
    - properties-overwrite-configmap
    - properties-secret
    - ui-settings-configmap
    - ui-settings-secret
    - yaml-files-configmap
    - yaml-secrets-secret
{{- end -}}
