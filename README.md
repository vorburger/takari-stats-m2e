# Information we collect

* Java version and vendor
* Operating system name, architecture and version
* Eclipse product id and build version
* Number of Maven projects in the workspace
* Bundle symbolic name and version for some active bundles (see below)
* GroupId, artifactId, version and m2e lifecycle mapping action for
  some Maven plugins used by workspace projects 

# What OSGi bundles are included in the report

* org.eclipse.osgi
* org.eclipse.m2e.core
* Bundles that have '/.takaristats' marker entry

# What Maven pligins are included in the report

Maven plugins that have the following groupId prefixes are included in the 
report

* org.apache*
* org.codehaus*
* io.takari*

# How do we upload usage statistics

Each active Eclipse workspace will upload statistics to Takari.io server one
time every 7 days. This is done with a single HTTP PUT request, where the body
is JSON encoded usage statistics data.

Usage report is not uploaded if there are no m2e projects in the workspace.
