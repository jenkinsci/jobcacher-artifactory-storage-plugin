# Jobcacher Artifactory storage Extension plugin

> [!NOTE]
> This plugin is maintained by the Jenkins Community and not by JFrog.

<p align="center">
  <img src="docs/artifactory_logo.png">
</p>

## Introduction

## Getting started

See [jobcacher-plugin](https://plugins.jenkins.io/jobcacher/) for usage.

You only need to configure the extension to use Artifactory under System Configuration.

## Configuration as Code

```yaml
unclassified:
  globalItemStorage:
    storage:
      artifactory:
        prefix: "jenkins/"
        repository: "my-generic-repo"
        serverUrl: "http://localhost:7000"
        storageCredentialId: "the-credentials-id"
```

## CONTRIBUTING

See [CONTRIBUTING](CONTRIBUTING.md)

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)

