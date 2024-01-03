{{/*
Using the ox-commons.mysql conventions (see there in _mysql.tpl) generates a secret with mysql connection data for the configdb. This winds up as environment variables
in both the init container and the core-mw container of the middleware pods. 
*/}}
{{- define "core-mw.typeSpecific.mysql-secret.options" -}}
usedKeys:
  - mysql
nameTemplate: "core-mw.typeSpecific.mysql-secret.name"
{{- end -}}

{{- define "core-mw.typeSpecific.mysql-secret.name" -}}
{{- $mysqlDict := (dict "mysql" .Values.mysql "context" .Context "global" .Context) -}}
{{- if .TypeName -}}
{{ include "ox-common.mysql.secretName" $mysqlDict }}-{{ .TypeName }}
{{- else -}}
{{ include "ox-common.mysql.secretName" $mysqlDict }}
{{- end -}}
{{- end -}}

{{- define "core-mw.typeSpecific.mysql-secret.template" -}}
{{- $mysqlDict := (dict "mysql" .Values.mysql "context" .Context "global" .Context) -}}
{{- if eq (include "ox-common.mysql.createSecret" $mysqlDict) "true" }}
apiVersion: v1
kind: Secret
metadata:
  name: {{ .ResourceName }}
  labels:
    {{- include "ox-common.labels.standard" .Context | nindent 4 }}
type: Opaque
stringData:
  MYSQL_WRITE_HOST: {{ include "ox-common.mysql.writeHost" $mysqlDict }}
  MYSQL_WRITE_PORT: {{ include "ox-common.mysql.writePort" $mysqlDict | quote }}
  MYSQL_WRITE_DATABASE: {{ include "ox-common.mysql.writeDatabase" $mysqlDict }}
  MYSQL_WRITE_USER: {{ include "ox-common.mysql.writeUser" $mysqlDict }}
  MYSQL_WRITE_PASSWORD: {{ include "ox-common.mysql.writePassword" $mysqlDict }}
  MYSQL_READ_HOST: {{ include "ox-common.mysql.readHost" $mysqlDict }}
  MYSQL_READ_PORT: {{ include "ox-common.mysql.readPort" $mysqlDict | quote }}
  MYSQL_READ_DATABASE: {{ include "ox-common.mysql.readDatabase" $mysqlDict }}
  MYSQL_READ_USER: {{ include "ox-common.mysql.readUser" $mysqlDict }}
  MYSQL_READ_PASSWORD: {{ include "ox-common.mysql.readPassword" $mysqlDict }}
  MYSQL_HOST: {{ required "MySQL host is required!" (include "ox-common.mysql.host" $mysqlDict) }}
  MYSQL_PORT: {{ include "ox-common.mysql.port" $mysqlDict | quote }}
  MYSQL_DATABASE: {{ required "MySQL database is required!" (include "ox-common.mysql.database" $mysqlDict) }}
  MYSQL_USER: {{ required "MySQL user is required!" (include "ox-common.mysql.user" $mysqlDict) }}
  MYSQL_PASSWORD: {{ required "MySQL password is required!" (include "ox-common.mysql.password" $mysqlDict) }}
{{- end }}
{{- end -}}
