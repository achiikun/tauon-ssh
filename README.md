# Tauon SSH Terminal/SFTP client

**Easy and fun way to work with remote servers over SSH.**

Tauon is a graphical SSH client.
It has an enhanced SFTP file browser, SSH terminal emulator, remote resource/process manager, 
server disk space analyzer, remote text editor, huge remote log viewer and lots of other helpful tools, 
which makes it easy to work with remote servers. 
Tauon provides functionality similar to web based control panels but, it works over SSH from local computer, 
hence no installation required on server. It runs on Linux and Windows.
Muon has been tested with several Linux and UNIX servers, like Ubuntu server, CentOS, RHEL, OpenSUSE, FreeBSD, OpenBSD, NetBSD and HP-UX.

### Intended audience
The application is targeted mainly towards web/backend developers who often deploy/debug 
their code on remote servers and not overly fond of complex terminal based commands. 
It could also be useful for sysadmins as well who manages lots of remote servers manually.

__* You must have Java 11 installed minimum__

### History

This project is the next iteration of another one called [Muon](https://github.com/devlinx9/muon-ssh).
Its last update was a few years ago. I've been doing lots of refactorings and cleaning code in order to fix bugs and
make the development of new features easier. The main change respect Muon is that now x11 forwarding (in linux) is supported.
Some of the description above is copy-pasted from the Devlinx9 repo ;)

I started this fork when I switched my dev operating system to linux from windows and I realized that MobaXTerm was not compatible.
After some research I discovered Snowflake and Muon, but they still have lots of unsolved bugs. I decided to stick to Muon
because I think is the only application with the power to become a competitive, good and open source SSH client for linux: 
No companies, registration forms nor paywalls, and written in a nice language (Java).

I forked Muon into a new application instead of pulling request the original 
because I knew from the beginning I'd refactor it heavily.

## Building from source:

> This is a standard maven project. If you have configured Java and Maven use: 
> <b>mvn clean install</b> to build the project.
> The jar will be created in target directory

### Install as deb (Debian)

1. Execute `mvn clean install` to create the `.deb` file.
2. Install the deb file:
```shell
sudo dpkg -i target/tauonssh_*.deb
```

### Install as flatpak (Experimental)

1. Execute `mvn clean install` to create the jar. (It will be copied automatically into flatpak's folder)
2. Build the app using flatpak:
```shell
cd flatpak
flatpak-builder --force-clean --sandbox --user --install --install-deps-from=flathub --ccache --mirror-screenshots-url=https://dl.flathub.org/media/ --repo=repo builddir org.tauon_ssh.App.yml 
```

## Features:

* Simple graphical interface for common file operations
* Built in text editor with syntax highlighting and support for sudo
* Simply view and search huge log/text files in a jiffy
* Fast powerful file and content search, powered by find command
* Built in terminal and command snippet
* X11 Forwarding
* Fully equipped task manager
* Built in graphical disk space analyzer
* Linux specific tools
* Manage SSH keys easily
* Network tools
* MultiLanguage Support

The [TODO](TODO.md) file contains what is currently developed and scheduled to develop.

## Licences

Tauon respects the licence from its previous versions: [GPLv3](/LICENSE)

Jediterm (from JetBrains) has an [LGPLv3](LICENSE-LGPLv3.txt) and a [Apache 2.0](LICENSE-APACHE-2.0.txt).
For more information visit [https://github.com/JetBrains/jediterm]()

## Documentation: (from Muon)</h2>

[https://github.com/devlinx9/muon-ssh/wiki]()