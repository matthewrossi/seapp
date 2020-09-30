# SEApp policy modules

To support the use of ad-hoc Mandatory Access Control policies, we want the
developer to have control of the security context of subjects (i.e., processes)
and objects related to her app.

## Policy module structure

The developer is able to operate at two different levels:

 - the actual definition of the app policy logic using the policy language described in [Policy module syntax](#Policy-module-syntax) (written to the `sepolicy.cil` file)

 - the configuration of the security context described in [Policy configration](#Policy-configuration) for each process (in the `seapp_contexts` and `mac_permissions.xml`) and for each file directory (in the file `file_contexts`).

## Policy module syntax

SELinux policies come to Android devices written in the SELinux Common
Intermediate Language (CIL).
Therefore, to avoid any additional translation step on the device, it
represents most fitting language to define SEApp policy modules.

CIL offers a multitude of commands to define a policy, but only a subset has
been selected for the representation of an app policy module.

The syntax is shortly shown in the following picture.

<p align="center">
    <img src="https://user-images.githubusercontent.com/15113769/94329703-a75ccc80-ffbd-11ea-9d89-2412d3ddf149.png"
        alt="Application policy module CIL syntax">
</p>

For a more detailed explanation we suggest to look at the [CIL documentations](https://android.googlesource.com/platform/external/selinux/+/master/secilc/docs/README.md)

## Policy constraints

The introduction of dedicated modules for apps raises the need to carefully
consider the integration of apps and system policies.

To ensure that policy modules do not interfere with the system policy and among
each other, a first necessity is that policy modules are wrapped in a unique
namespace.

This is done through the definition of a CIL block using the package name,
which prevents the definition of the same SELinux type twice, as the resulting
global identifier is formed by the concatenation of the namespace and the local
type identifier.

The use of a namespace specific for the policy module permits to discriminate
between local types _T<sub>A</sub>_ (namespace equal to the current app package
name), types of other modules _T<sub>A'≠A</sub>_ (namespace equal to some other
app package name), and global system types _T<sub>S</sub>_ (system namespace).

Each Access Vector rule, declared by an `allow` statement, is evaluated looking
at the source and target type identifiers.

Here are the emerging cases and how they are constrained:

<p align="center">
    <img src="https://user-images.githubusercontent.com/15113769/94330301-a333ad80-ffc3-11ea-8133-fc023bfe5275.png"
        alt="Varieties of allow statements" width="70%">
</p>

- _AllowSS_ is prohibited, , as it represents a direct platform policy
modification

- _AllowSA_ is prohibited, as it might change system services security
assumptions, however indirect assignment through the use of `typeattributeset`
is available to permit system services necessary to the functioning of apps to
work as intended by the system policy

- _AllowAS_ cannot be enstablished as it is, therefore it is delegated to the
SELinux decision engine during policy enforcement.
This crucial postponed restriction depends on the constraint that all app types
have to appear in a `typebounds` statement, which limits the bounded type to
have at most the access privileges of the bounding type. As Android 10 assigns
to generic third-party apps the `untrusted_app` domain, this is the candidate we
use to bound the app types

- _AllowAA_ is always permitted, as it only defines access privileges internal
to the policy module

## Policy configuration

### Processes

SEApp permits to assign a SELinux domain to each process of the security
enhanced app. To do this, the developer lists in the local seapp_contexts a set
of entries that determine the security context to use for its processes.

For each entry, we restrict the list of valid input selectors to `user`, 
`seinfo` and `name`.

- `user` is a selector based upon the type of UID
- `seinfo` matches the app seinfo tag contained in the `mac_permissions.xml`
configuration file
- `name` matches either a prefix or the whole process name

The conjunction of these selectors determines a class of processes, to which the
context specified by domain is assigned. To avoid privilege escalation, the
only permitted domains are the ones the app defines within its policy module
and `untrusted_app`. As a process may fall into multiple classes, the most
selective one, with respect to the input selector, is chosen.

Here is an example of valid `seapp_contexts` entries:

```
user=_app seinfo=cert_id domain=package_name.unclassified
name=package.name:unclassified

user=_app seinfo=cert_id domain=package_name.secret
name=package.name:secret
```

In this example, the two domains unclassified and secret are assigned to the
packageName `:unclassified` and `:secret` processes, respectively.
In Android developers have to focus on components rather than processes.
Normally, all components of an application run in a single process.
However, it is possible to change this default behavior setting the
`android:process` attribute of the respective component inside the
AndroidManifest.xml.
With the specification of an `android:process` consistent with the
`seapp_contexts` configuration, we support the assignment of distinct
domains to app components.

### Files

Developers state the SELinux security contexts of internal files in the
`file_contexts`.
Each of its entries presents three syntactic elements: `pathname_regexp`,
`file_type` and `security_context`.

- `pathname_regexp` defines the directory the entry is referred to (it can be a
specific path or a regular expression)
- `file_type` describes the class of filesystem resource (i.e., directory,
file, etc)
- `security_context` is the security context used to label the resource.

The admissible entries are those confined to the app dedicated directory
and using types defined by the app policy module, with the exception of
`app_data_file` (useful to enable inter-app file sharing).
Due to the regexp support, a path may suit more entries, in which case the most
specific one is used.

Here is an example of valid `file_contexts` entries:

```
.*                      u:object_r:app_data_file:s0
dir/unclassified        u:object_r:package_name.unclassified_file:s0
dir/secret              u:object_r:package_name.secret_file:s0
```

where the first line describes the default label for app files, the other two
lines specify a respective label for files in directories dir/unclassified and
dir/secret.

### System services

In order to support any third party app, `untrusted_app` domain grants processes
permissions to access all system services an app could require in the
`AndroidManifest.xml`.
To prevent certain components of the app from holding the privilege to bind to
unnecessary system services, the developer defines a domain with a subset of
the `untrusted_app` access privileges (in the `sepolicy.cil` file), and then
she ensures the components are executed in the process labeled with it.

Here is an example in which the cameraserver service is made accessible
to the secret process:

```
(block package_name
    (type secret)
    (typeattributeset domain (secret))
    (allow secret cameraserver_service (service_manager (find)))
...)
```

## Example

You can find a simple security-enhanced application [here](app/SEPolicyTestApp) and its policy module [here](app/SEPolicyTestApp/policy).
