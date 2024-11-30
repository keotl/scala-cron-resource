## scala-cron-resource
A simple cron string parser for Concourse CI.

Supports standard cron expressions, plus the non-standard N-th
day-of-week of month. (i.e. "0 0 * * SUN#1")


### Usage
1. Declare the cron resource type.
```yaml
resource_types:
- name: cron-resource
  type: registry-image
  source:
    repository: keotl/scala-cron-resource
    tag: latest
```

2. Declare the cron schedule resource.
```yaml
resources:
- name: every-first-sunday
  type: cron-resource
  icon: clock-outline
  source:
    pattern: "0 0 * * SUN#1"
```

3. Use your resource schedule as an input trigger step in your
   pipeline.
```yaml
- name: my pipeline
  plan:
  - get: every-first-sunday
    trigger: true
  ...
```
