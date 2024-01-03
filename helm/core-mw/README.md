# core-mw

![Version: 4.8.1](https://img.shields.io/badge/Version-4.8.1-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 8.19.0](https://img.shields.io/badge/AppVersion-8.19.0-informational?style=flat-square)

App Suite Middleware Core Helm Chart

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| OX Software GmbH | <info@open-xchange.com> |  |

## Source Code

* <https://github.com/open-xchange/appsuite-middleware>

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| oci://registry.open-xchange.com/appsuite-core-internal/charts/3rdparty | collabora-online | 1.0.1 |
| oci://registry.open-xchange.com/appsuite-core-internal/charts/3rdparty | gotenberg | 0.4.2 |
| oci://registry.open-xchange.com/appsuite-core-internal/charts | ox-common | 1.0.33 |

## Additional informations

### 4.0.0

- This version introduces new `configuration.redis` and `configuration.sessiond` section which **adds support for** Redis. Please refer to the documentation in `values.yaml`.
- Changed the default service type from `NodePort` to `ClusterIP` for `http-api`, `sync` and `admin` service.
- Removed *all* ingress configuration settings.
- Removed `services.documentconverterHost`, `services.imageconverterHost` and `services.spellcheckHost`.
Not necessary anymore but it's possible to override them e.g. via `.Values.global.dc.serviceName`
- Renamed environment variables
  - `OX_IMAGECONVERTER_URL` &rarr; `IC_SERVER_URL`
  - `OX_SPELLCHECK_URL` &rarr; `SPELLCHECK_SERVER_URL`
  - `OX_DOCUMENTCONVERTER_URL` &rarr; `DC_SERVER_URL`
- Partially or fully override `ox-common.names.fullname` via `nameOverride` or `fullnameOverride`.

## Configuration

The following table lists the configurable parameters of the `App Suite Middleware Core` chart and their default values.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| affinity | object | `{}` | Affinity for pod assignment |
| asConfig.default.host | string | `"all"` |  |
| basicAuthLogin | string | `""` | The user name used for HTTP basic auth. |
| basicAuthPassword | string | `""` | The password used for HTTP basic auth. |
| collabora-online.enabled | bool | `false` | Whether `Collabora` should be enabled or not. |
| collabora-online.image.repository | string | `"registry.open-xchange.com/appsuite-core-internal/3rdparty/collabora-online"` |  |
| configuration | object | `{"businessmobility":{"logging":{"debug":{"enabled":false,"logPath":""}}},"languages":[],"logging":{"debug":true,"json":{"prettyPrint":false},"logger":[{"level":"WARN","name":"org.apache.cxf"},{"level":"WARN","name":"com.openexchange.soap.cxf.logger"}],"logstash":{"host":"localhost","port":31337},"queueSize":2048,"root":{"json":true,"level":"INFO","logstash":false},"syslog":{"facility":"USER","host":"localhost","port":514}},"sessiond":{"redis":{"enabled":false}}}` | Configuration |
| configuration.businessmobility.logging.debug.enabled | bool | `false` | Whether debug log is enabled or not |
| configuration.businessmobility.logging.debug.logPath | string | `""` | The path of the log file @default /var/log/open-xchange |
| configuration.languages | list | `[]` | List of languages which should be enabled. The default set of languages is `de_DE`, `en_US`, `es_ES`, `fr_FR` and `it_IT`.<br/> Example for enabling a couple of languages: `[ nl_NL, fi_FI, pl_PL ]` or for all available languages `[ all ]` |
| configuration.logging.debug | bool | `true` | Enables logback's debug mode |
| configuration.logging.json.prettyPrint | bool | `false` | Whether `PrettyPrint` is enabled |
| configuration.logging.logger | list | `[{"level":"WARN","name":"org.apache.cxf"},{"level":"WARN","name":"com.openexchange.soap.cxf.logger"}]` | List of named logger |
| configuration.logging.logstash | object | `{"host":"localhost","port":31337}` | Logstash configuration |
| configuration.logging.queueSize | int | `2048` | The number of logging events to retain for delivery |
| configuration.logging.root.json | bool | `true` | Whether `JSON` logging is enabled |
| configuration.logging.root.level | string | `"INFO"` | Sets the log level of the root logger |
| configuration.logging.root.logstash | bool | `false` | Whether logging to `logstash` is enabled |
| configuration.logging.syslog | object | `{"facility":"USER","host":"localhost","port":514}` | Syslog configuration |
| contextSets | object | `{}` | Context sets |
| createCommonEnv | bool | `true` | Whether to create a secret containing common properties as environment variables (e.g. SESSIOND_ENCRYPTION_KEY) |
| defaultRegistry | string | `"registry.open-xchange.com"` | The default registry |
| defaultScaling.nodes.default.roles[0] | string | `"hazelcast-data-holding"` |  |
| defaultScaling.nodes.default.roles[1] | string | `"http-api"` |  |
| defaultScaling.nodes.default.roles[2] | string | `"sync"` |  |
| defaultScaling.nodes.default.roles[3] | string | `"admin"` |  |
| defaultScaling.nodes.default.roles[4] | string | `"businessmobility"` |  |
| enableDBConnectionCheck | bool | `true` | Whether to wait for configdb. |
| enableInitialization | bool | `false` | Whether initial bootstraping is enabled or not. |
| etcBinaries | list | `[]` | etc files |
| etcFiles | object | `{}` | etc files |
| extraContainers | list | `[]` | List of extra sidecar containers |
| extraEnv | list | `[]` | List of extra environment variables |
| extraMounts | list | `[]` | List of extra mounts |
| extraStatefulSetProperties | object | `{}` | List of extra StatefulSet properties |
| extraVolumes | list | `[]` | List of extra volumes |
| extras.monitoring.enabled | bool | `false` | Whether monitoring resources should be created, e.g. a `ConfigMap` containing the Grafana dashboards. |
| features.definitions.admin | list | see `values.yaml` | Admin definitions |
| features.definitions.documents | list | see `values.yaml` | Documents definitions |
| features.definitions.guard | list | see `values.yaml` | Guard definitions |
| features.definitions.omf-source | list | see `values.yaml` | OX2OX Migration Framework Source definitions |
| features.definitions.plugins | list | see `values.yaml` | Plugins definitions |
| features.definitions.reseller | list | see `values.yaml` | Reseller definitions |
| features.definitions.usm-eas | list | see `values.yaml` | USM EAS sync definitions |
| features.status | object | see `values.yaml` | Choose whether to enable or disable features. |
| fullnameOverride | string | `""` | Fully override of the `ox-common.names.fullname` template |
| global.extras.monitoring.enabled | bool | `false` |  |
| global.imageRegistry | string | `""` | Sets the image registry globally |
| global.mysql.existingSecret | string | `""` |  |
| gotenberg.chromium.disableJavaScript | bool | `true` |  |
| gotenberg.enabled | bool | `false` | Whether `Gotenberg` should be enabled or not. |
| gotenberg.image.repository | string | `"registry.open-xchange.com/appsuite-core-internal/3rdparty/gotenberg"` |  |
| hooks.beforeApply | object | `{}` |  |
| hooks.beforeAppsuiteStart | object | `{}` |  |
| hooks.start | object | `{}` |  |
| hzGroupName | string | `""` | The [Hazelcast](https://hazelcast.com/) group name. |
| hzGroupPassword | string | `""` | The [Hazelcast](https://hazelcast.com/) group password. |
| image.pullPolicy | string | `"IfNotPresent"` | Image pull policy |
| image.repository | string | `"appsuite-core/middleware"` | Image repository |
| image.tag | string | `""` | Image tag |
| imagePullSecrets | list | `[]` | Reference to one or more secrets to be used when pulling images |
| istio.compression.enabled | bool | `false` | Whether to enable HTTP compression (gzip, deflate, etc.). |
| istio.injection.enabled | bool | `false` | Whether to enable sidecar injection or not. |
| javaOpts.debug.gcLogs.enabled | bool | `false` | Enables Java Garbage Collector logging |
| javaOpts.debug.heapdump.custom | object | `{}` | The definition of a custom volume excluding its name which shall be used instead of a hostpath volume. |
| javaOpts.debug.heapdump.enabled | bool | `false` | Enables Java Heap Dump creation in OOM situations |
| javaOpts.debug.heapdump.hostPath.dir | string | `"/mnt/appsuite-heap-dumps"` | hostPath directory on the k8s worker nodes, which needs to be created manually by the k8s admin. The directory will be mounted inside the core-mw container as '/heapdump'. |
| javaOpts.memory.maxHeapSize | string | `"8192M"` |  |
| javaOpts.other | string | `""` |  |
| javaOpts.server | string | `""` |  |
| jolokiaLogin | string | `""` | User used for authentication with HTTP Basic Authentication. |
| jolokiaPassword | string | `""` | Password used for authentification with HTTP Basic Authentication. |
| masterAdmin | string | `""` | The name of the master admin. |
| masterPassword | string | `""` | The password of the master admin. |
| meta | object | `{}` | Meta |
| mysql.auth.password | string | `""` | The database password. *`(read/write connection)`* |
| mysql.auth.readPassword | string | `""` | The database password. *`(read connection)`* |
| mysql.auth.readUser | string | `""` | The database user name. *`(read connection)`* |
| mysql.auth.rootPassword | string | `""` | The MySQL `root` password. |
| mysql.auth.user | string | `""` | The database user name. *`(read/write connection)`* |
| mysql.auth.writePassword | string | `""` | The database password. *`(write connection)`* |
| mysql.auth.writeUser | string | `""` | The database user name. *`(write connection)`* |
| mysql.database | string | `"configdb"` | The database/schema name. *`(read/write connection)`* |
| mysql.existingSecret | string | `""` | Name of an existing secret. |
| mysql.host | string | `""` | The database host. *`(read/write connection)`* |
| mysql.port | string | `"3306"` | The database port. *`(read/write connection)`* |
| mysql.readDatabase | string | `"configdb"` | The database/schema name. *`(read connection)`* |
| mysql.readHost | string | `""` | The database host. *`(read connection)`* |
| mysql.readPort | string | `"3306"` | The database port. *`(read connection)`* |
| mysql.writeDatabase | string | `"configdb"` | The database/schema name. *`(write connection)`* |
| mysql.writeHost | string | `""` | The database host. *`(write connection)`* |
| mysql.writePort | string | `"3306"` | The database port. *`(write connection)`* |
| nameOverride | string | `""` | Partially override of the `ox-common.names.fullname` template<br/> *NOTE: Preserves the release name.* |
| nodeSelector | object | `{}` | Tolerations for pod assignment |
| packages.minimalWhitelist | list | see `values.yaml` | Minimal package whitelist |
| packages.status | object | see `values.yaml` | Choose whether to enable or disable packages. |
| podAnnotations | object | `{"logging.open-xchange.com/format":"appsuite-json"}` | Annotations to add to the pod |
| podSecurityContext | object | `{}` | The pod security context |
| probe.liveness.enabled | bool | `true` | Enable the liveness probe |
| probe.liveness.failureThreshold | int | `15` | The liveness probe failure threshold |
| probe.liveness.periodSeconds | int | `10` | The liveness probe period (in seconds) |
| probe.readiness.enabled | bool | `true` | Enable the readiness probe |
| probe.readiness.failureThreshold | int | `2` | The readiness probe failure threshold |
| probe.readiness.initialDelaySeconds | int | `30` | The readiness probe initial delay (in seconds) |
| probe.readiness.periodSeconds | int | `5` | The readiness probe period (in seconds) |
| probe.readiness.timeoutSeconds | int | `5` | The readiness probe timeout (in seconds) |
| probe.startup.enabled | bool | `true` | Enable the startup probe |
| probe.startup.failureThreshold | int | `30` | The startup probe failure threshold |
| probe.startup.initialDelaySeconds | int | `30` | The startup probe initial delay (in seconds) |
| probe.startup.periodSeconds | int | `10` | The startup probe period (in seconds) |
| properties | object | see `values.yaml` | Properties |
| propertiesFiles | object | `{}` | Properties files |
| rbac.create | bool | `true` | Whether Role-Based Access Control (RBAC) resources should be created |
| rbac.rules | list | `[]` | Custom RBAC rules |
| redis.auth.password | string | `""` | The `Redis` password. |
| redis.auth.username | string | `""` | The `Redis` username. |
| redis.enabled | bool | `false` | Whether `Redis` should be enabled or not |
| redis.hosts | list | `[]` | List of `Redis` hosts: <br/> Example for `redis`: [ <redis_host>:<redis_port> ] <br/> Example for `redis+sentinel`: [ <sentinel1_host>:<sentinel1_port>,<sentinel2_host>:<sentinel2_port>,<sentinel3_host>:<sentinel3_port> ] <br/> > Note: If `hosts` is empty or null, then an internal `redis`-standalone instance will be deployed. |
| redis.mode | string | `"standalone"` | Redis operation mode (`standalone`, `cluster`, `sentinel`) |
| redis.sentinelMasterId | string | `"mymaster"` | Name of the `sentinel` masterSet, if operation mode is set to `sentinel`. |
| remoteDebug.enabled | bool | `false` | Whether Java Remote Debugging is enabled. |
| remoteDebug.nodePort | string | `nil` | The node port (default range: 30000-32767) |
| remoteDebug.port | int | `8102` | The Java Remote Debug port. |
| replicas | int | `1` | Number of nodes |
| resources | object | `{"limits":{"cpu":"2000m","memory":"8192Mi"},"requests":{"cpu":"1000m","memory":"1024Mi"}}` | CPU/Memory resource requests/limits |
| restricted.drive.enabled | bool | `true` | If enabled tries to mount drive restricted configuration |
| restricted.mobileApiFacade.enabled | bool | `true` | If enabled tries to mount mobile api facade configuration |
| roles.admin.services[0].ports[0].name | string | `"http"` |  |
| roles.admin.services[0].ports[0].port | int | `80` |  |
| roles.admin.services[0].ports[0].protocol | string | `"TCP"` |  |
| roles.admin.services[0].ports[0].targetPort | string | `"http"` |  |
| roles.admin.services[0].type | string | `"ClusterIP"` |  |
| roles.businessmobility.services[0].ports[0].name | string | `"http"` |  |
| roles.businessmobility.services[0].ports[0].port | int | `80` |  |
| roles.businessmobility.services[0].ports[0].protocol | string | `"TCP"` |  |
| roles.businessmobility.services[0].ports[0].targetPort | string | `"http"` |  |
| roles.businessmobility.services[0].type | string | `"ClusterIP"` |  |
| roles.businessmobility.values.features.status.usm-eas | string | `"enabled"` |  |
| roles.businessmobility.values.properties."com.openexchange.usm.ox.url" | string | `"http://localhost:8009/appsuite/api/"` |  |
| roles.hazelcast-data-holding.controller | string | `"StatefulSet"` |  |
| roles.hazelcast-data-holding.services[0].headless | bool | `true` |  |
| roles.hazelcast-data-holding.services[0].name | string | `"hazelcast-headless"` |  |
| roles.hazelcast-data-holding.services[0].ports[0].name | string | `"tcp-hazelcast"` |  |
| roles.hazelcast-data-holding.services[0].ports[0].port | int | `5701` |  |
| roles.hazelcast-data-holding.statefulSetServiceName | string | `"hazelcast-headless"` |  |
| roles.hazelcast-lite-member | object | `{}` |  |
| roles.http-api.services[0].ports[0].name | string | `"http"` |  |
| roles.http-api.services[0].ports[0].port | int | `80` |  |
| roles.http-api.services[0].ports[0].protocol | string | `"TCP"` |  |
| roles.http-api.services[0].ports[0].targetPort | string | `"http"` |  |
| roles.http-api.services[0].type | string | `"ClusterIP"` |  |
| roles.sync.services[0].ports[0].name | string | `"http"` |  |
| roles.sync.services[0].ports[0].port | int | `80` |  |
| roles.sync.services[0].ports[0].protocol | string | `"TCP"` |  |
| roles.sync.services[0].ports[0].targetPort | string | `"http"` |  |
| roles.sync.services[0].type | string | `"ClusterIP"` |  |
| secretContextSets | object | `{}` | Secret Context sets |
| secretETCBinaries | list | `[]` | Secret etc files |
| secretETCFiles | object | `{}` | Secret etc files |
| secretProperties | object | `{}` | Secret properties |
| secretPropertiesFiles | object | `{}` | Secret properties files |
| secretUISettings | object | `{}` | Secret UI settings<br/> |
| secretUISettingsFiles | object | `{}` | Secret UI settings files |
| secretYAMLFiles | object | `{}` | Secret YAML files |
| securityContext | object | `{}` | The security context |
| serviceAccount.annotations | object | `{}` | Annotations to add to the service account |
| serviceAccount.create | bool | `true` | Whether a service account should be created |
| serviceAccount.name | string | `""` | The name of the service account to use. If not set and create is true, a name is generated using the fullname template |
| terminationGracePeriodSeconds | int | `120` | Duration in seconds the pod waits to terminate gracefully. |
| tolerations | list | `[]` | Tolerations for pod assignment |
| uiSettings | object | `{}` | UI settings<br/> |
| uiSettingsFiles | object | `{}` | UI settings files |
| update.enabled | bool | `false` | Whether an update task job for the specified database schemata is created or not |
| update.job | object | `{"ttlSecondsAfterFinished":86400}` | Job object settings |
| update.job.ttlSecondsAfterFinished | int | `86400` | The number of seconds after which a job is deleted automatically |
| update.schemata | string | `""` | Database schemata to update. If empty, all schemata will be updated. |
| update.types | list | `[]` | Filter for which types the update tasks are triggered. Every type with an unique bundle set will create a container in the update job. All containers (except one) are configured as init containers to ensure they run sequentially. |
| update.values | object | `{}` | Override type sepcific update values |
| yamlFiles | object | `{}` | YAML files |