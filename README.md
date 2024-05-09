# Muon SSH Terminal/SFTP client ( Formerly Snowflake )

Easy and fun way to work with remote servers over SSH.

This project is being renamed as previous name "Snowflake" is confusing since there is already a popular product with the same name.
Personally I don't like "Snowflake" nor "Muon". Maybe one day I'll change its name (to "Tauon").

Muon is a graphical SSH client. 
It has an enhanced SFTP file browser, SSH terminal emulator, remote resource/process manager, 
server disk space analyzer, remote text editor, huge remote log viewer and lots of other helpful tools, 
which makes it easy to work with remote servers. 
Muon provides functionality similar to web based control panels but, 
it works over SSH from local computer, hence no installation required on server. 
It runs on Linux and Windows.
Muon has been tested with several Linux and UNIX servers, 
like Ubuntu server, CentOS, RHEL, OpenSUSE, FreeBSD, OpenBSD, NetBSD and HP-UX.

<h3>Intended audience</h3>
<p>The application is targeted mainly towards web/backend developers who often deploy/debug 
their code on remote servers and not overly fond of complex terminal based commands. 
It could also be useful for sysadmins as well who manages lots of remote servers manually.
</p>

<p>
<b>* You must have Java 11 installed minimum</b>
</p>

### Disclaimer

This fork is intended for developing features I find interesting and/or I need for my own.
On Windows I used MobaXTerm but I'm now on Linux and Muon is the only application
with the power to become a competitive good open source SSH client for linux in this case. 
I will be developing in this repository without guarantee
and always for my own interest, I don't even try to make pull requests to the upstream repo,
although I recognize the great job done in that repo.

<h2>Building from source:</h2>
<pre> This is a standard maven project. If you have configured Java and Maven use: 
 <b>mvn clean install</b> to build the project.
 The jar will be created in target directory
 </pre>

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

### TODO 

* [x] Mouse scroll too slow
* [x] Add a cancel button to the dialog
* [x] Combination of keys to avoid inserting sudo password every time
* [x] X11 Forwarding
* [X] Bug: Session manager showed in primary screen instead of main window screen
* [X] Dates in ISO format
* [X] Don't blink the screen every time a folder is updated. Plus made more UX friendly.
* [X] Native Windows File Picker, AWT for Linux, Swing for Mac :D
* [X] Bug: SymLinks not showed in Local File Explorer
* [X] Bug: Open in explorer not supported on linux and mac. (Added opening files in explorer)
* [X] Bug: Sometimes when shell is present, no cursor is blinking (After reconnecting, cursor disappears)
* [ ] Bug: Unicode characters in terminal (tree is not displaying properly)
* [ ] Bug: Forwarded ports remain active after closing session
* [ ] When hit a CTRL+C, flush console buffer to receive the prompt ASAP
* [ ] Open the program in the last location, or in the active screen if multiple
* [ ] Copy PID from processes
* [ ] When a port forwarding fails, notify to the user
* [ ] File Browser Issues
  * [ ] Unify behavior for all file tasks (ask for sudo, ask for reconnect)
  * [ ] File browser arrows (history) don't work
  * [ ] Download is not implemented
  * [ ] Refresh window after copying files
  * [ ] Add a box to query what happens when copying a file that exists
  * [ ] Move an item to a folder in the same window


**Here goes new features**

<h2>Documentation: (from the upstream repo)</h2>

<p>
  <a href="https://github.com/devlinx9/muon-ssh/wiki">
    https://github.com/devlinx9/muon-ssh/wiki
  </a>
</p>
