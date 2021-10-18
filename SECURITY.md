# Security Policy

JShell deals with user code execution and is thus not spared of security vulnerabilities.
They will most likely not affect the browser/client but the server side. 

It is recommended not to run any component that evaluates code in an environment without
isolation/security. Jsheets provides mechanisms to control/configure what code can do,
io operations are disabled by default. Be **careful** when editing security configs.

## Supported Versions

Ensure that your deployment of JSheets uses a supported version. We do not provide security 
updates for old versions. It is always important to keep up to date and, if possible, use the
latest version.

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |

## Discovering a Vulnerability

Please contact the administrator of the Jsheets instance and inform him/her about the vulnerability
(or at least that one exists). It is in their hands to decide wether to temporarily disable execution
of code until the vulnerability is fixed. Please **report** all vulnerabilities you find to protect
both administrators and users.

## Reporting a Vulnerability

Prefer to report "severe" vulnerabilities to [merlinosayimwen@gmail.com](mailto://merlinosayimwen@gmail.com) to
ensure that as view people can abuse them until they are fixed. If you do not get a response within a few days, you
are free to contact other maintainers directly and, if no one responds, create an issue.
