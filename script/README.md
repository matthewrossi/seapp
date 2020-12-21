# Scripts

## [backup.sh](backup.sh)

Automates the creation of a backup by making a copy of all those files with
committed changes among two code states (i.e., commits, branches, tags) over
multiple git repositories.

[backup.sh](backup.sh) is the script we use to extract our changes from the
root of the AOSP source tree. You can find these set of "patches" under the
[platform](../platform) directory.

### Prerequisites

- [Install Git](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)
- [Install the Repo Launcher](https://source.android.com/setup/develop#installing-repo)
- [Download the Android source tree](https://source.android.com/setup/build/downloading)

### Usage

```
Usage:
  backup.sh [-h] [-s source] [-b backup] from to

  From the root of the AOSP source tree, backup files with committed changes
  among 'from' and 'to' commits.

  Options:
    -b  backup directory (defaults to 'backup' directory within source directory)
    -h  display help
    -s  source tree directory (defaults to current directory)
```
