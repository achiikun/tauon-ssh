# TODO

List of scheduled features and bugs to be solved:

## Features

* [x] Mouse scroll too slow
* [x] Add a cancel button to the dialog
* [x] Combination of keys to avoid inserting sudo password every time
* [x] X11 Forwarding
* [X] Bug: Session manager showed in primary screen instead of main window screen
* [X] Dates in ISO format
* [X] Don't blink the screen every time a folder is updated. Plus made more UX friendly.
* [X] Native Windows File Picker, AWT for Linux, Swing for Mac :D
* [X] Support X11 on Mac
* [ ] Add a connection status page (list of active forwarding ports)
* [ ] Add a name and an enable check to each port forwarding
* [ ] Run ports with sudo by default
* [ ] Open the program in the last location, or in the active screen if multiple
* [ ] Copy PID from processes
* [ ] When a port forwarding fails, notify to the user
* [ ] Notification tray
* [ ] Store securely proxy password
* [ ] Let user upload private key files and store them securely
* [ ] UI: Add countdown in the connecting dialog
* [ ] Add VNC viewer (from tightvnc-java)
* [ ] Windows with cygwin (https://x.cygwin.com/docs/ug/cygwin-x-ug.pdf)
* [ ] Recover text editor from old snowflake
* [ ] Toolbox
  * [ ] Show executed commands
* [ ] Diskspace
  * [ ] Show files in analyzed volumes
  * [ ] Copy paths
* [ ] File Browser:
  * [X] Download is not implemented
  * [X] Unify behavior for all file tasks (ask for sudo, ask for reconnect)
  * [ ] Refresh window after copying files
  * [ ] Add a box to query what happens when copying a file that exists
  * [ ] Move an item to a folder in the same window
  * [ ] Create a remote folder picker
  * [ ] Cancel file transfers (current button is useless)
* [ ] SSH Keys
  * [ ] Show server fingerprints
  

## Improvements

* [ ] Get rid of jsch
* [ ] When hit a CTRL+C, flush console buffer to receive the prompt ASAP

## Bugs

* [X] Bug: SymLinks not showed in Local File Explorer
* [X] Bug: Open in explorer not supported on linux and mac. (Added opening files in explorer)
* [X] Bug: Sometimes when shell is present, no cursor is blinking (After reconnecting, cursor disappears)
* [X] Bug: Unicode characters in terminal (tree is not displaying properly)
* [X] Bug: Forwarded ports remain active after closing session
* [X] Terminal
  * [X] Snippets aren't saved
* [ ] File Browser:
    * [ ] File browser arrows (history) don't work

