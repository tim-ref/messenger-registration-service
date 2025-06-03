# Changelog

All notable changes to this project will be documented in this file.

**Related change logs**:

- [messenger-client](https://github.com/tim-ref/messenger-client/blob/main/CHANGELOG.md)
- [messenger-org-admin](https://github.com/tim-ref/messenger-org-admin/blob/main/CHANGELOG.md)
- [messenger-proxy](https://github.com/tim-ref/messenger-proxy/blob/main/CHANGELOG.md)
- [messenger-push](https://github.com/tim-ref/messenger-push/blob/main/CHANGELOG.md)
- [messenger-rawdata-master](https://github.com/tim-ref/messenger-rawdata-master/blob/main/CHANGELOG.md)
- [messenger-registration-service](https://github.com/tim-ref/messenger-registration-service/blob/main/CHANGELOG.md)

<!--
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
-->

## [0.6.2] - 2025-06-02

### Changed

- Adapted Keykloak message to contain automatically deployed testdriver URLs

### Fixed

- Sync date of order from Keycloak in prolongation service to fix runtime calculation


## [0.6.1] - 2025-04-09

### Added

- This change log.

### Changed

- Reduced number of and improved performance of database queries regarding federation lists.

### Fixed

- Requests for the current federation list would never return status code Not Modified.

## [0.6.0] - 2025-03-11

### Added

- Validate certificates of list of federated servers using OCSP (A_25632, A_25635).
- Configuration of contact information returned TI-Messengers (`/.well-known/matrix/support`) (A_26265).

### Changed

- Upgrade to Java 21.

## [0.5.0] - 2025-02-11

### Added

- Support for different TI-Messenger variants.
