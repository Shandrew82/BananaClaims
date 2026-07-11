Banana Claims - Book GUI and Invitation Hotfix

Fixes:
- /claim book grants one physical Banana Claims written book when the player does not already have one.
- Book buttons use custom click actions instead of RUN_COMMAND, removing the unattended-command warning.
- Normal book actions keep the player on the current page.
- Text-based book actions use vanilla text-input dialogs and execute the entered value.
- Transfer, rename, description, popup text/sounds, invitations, and BlueMap values now execute instead of only suggesting chat text.
- Claim protection buttons persist their actual claim flags and provide chat confirmation.
- BlueMap fill and line hex arguments accept #RRGGBB and bare RRGGBB values.
- Invitation Accept/Deny selectors accept claim@inviter without trailing-data errors.
- Destructive book actions use vanilla confirmation dialogs and return to the same book page.

Important server-only GUI limitation:
The currently open written-book screen keeps the same page after an action, but its rendered text is a snapshot. The physical inventory copy is refreshed immediately; close and reopen the book to see updated labels and values on that page.

Install:
Extract this ZIP directly into the project root and replace existing files. Then run VERIFY_BOOK_GUI_HOTFIX.ps1 and build with .\gradlew.bat clean build.
