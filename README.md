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

> __You must have at least version 11 of Java (JRE11) installed__
>
> If not, you can find it here: https://adoptium.net/temurin/releases/

### History

This project is the next iteration of another one called [Muon](https://github.com/devlinx9/muon-ssh).
I've been doing lots of refactorings and cleaning code in order to fix bugs and make the development of new features easier.
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

The [TODO](TODO.md) file contains what is currently developed and scheduled to develop.

The [CHANGES](CHANGES.md) file contains the changes in each version and kind of roadmap for the next one.

### Not features

* Dropped support for multilanguage as it messes with the length of buttons and also thinking as English as the only universal programming language in this beautiful world.

## Licences

Tauon respects the licence from its previous versions (Muon & Snowflake): [GPLv3](/LICENSE)

Jediterm (from JetBrains) has double license: [LGPLv3](https://github.com/achiikun/jediterm/blob/master/LICENSE-LGPLv3.txt) and [Apache 2.0](https://github.com/achiikun/jediterm/blob/master/LICENSE-APACHE-2.0.txt).
For more information visit [https://github.com/JetBrains/jediterm](https://github.com/JetBrains/jediterm).

### Fonts

- The font **JetbrainsMono** uses a [OFL](https://github.com/JetBrains/JetBrainsMono/blob/master/OFL.txt) license.
- The font **Hack** uses a [MIT](https://github.com/source-foundry/Hack/blob/master/LICENSE.md) licence.

## Documentation: (from Muon)</h2>

[https://github.com/devlinx9/muon-ssh/wiki](https://github.com/devlinx9/muon-ssh/wiki)
