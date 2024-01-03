local grafana = import 'grafonnet/grafana.libsonnet';
local dashboard = grafana.dashboard;
local template = grafana.template;
local annotation = grafana.annotation;

{
  default::
    {
      datasource: 'Prometheus',
    },
  newDashboard(title, metric, tags=[]):: dashboard.new(
    title=title,
    tags=tags,
    schemaVersion=26,
    refresh='1m',
    editable=true,
    graphTooltip='shared_crosshair',
  ).addTemplates(
    [
      template.interval(
        name='interval',
        label='Interval',
        query='auto,1m,5m,1h,6h,1d',
        auto_count=200,
        auto_min='1s',
        current='5m',
      ),
      template.new(
        name='namespace',
        label='Namespace',
        datasource=self.default.datasource,
        query='label_values(kube_pod_info, namespace)',
        refresh=2,
        sort=1
      ),
      template.new(
        name='app',
        label='App',
        datasource=self.default.datasource,
        query='label_values(up{namespace="$namespace"}, app)',
        refresh=2,
        sort=1
      ),
      template.new(
        name='pod',
        label='Pod',
        datasource=self.default.datasource,
        query='label_values(kube_pod_info{namespace="$namespace", pod=~".*$app.*"}, pod)',
        refresh=2,
        sort=1
      ),
    ]
  ),
}
