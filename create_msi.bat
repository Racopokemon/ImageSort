@echo off
echo We assume you have already created a runtime image as follows: in vsc, in the maven area, select plugins, javafx, jlink. The app jpackage, that were going to use, is part of newer jdks, make sure you have one of them installed and in path etc. Also, you need that certain ... 'wix' tools to make a windows installer, jpackage will prompt you a download link if it is not yet installed.

:: Lösche die Datei ImageSort.msi, falls sie existiert
if exist ImageSort.msi (
    del ImageSort.msi
    echo ImageSort.msi was deleted
)

:: Lösche die Datei ImageSort_Portable.zip, falls sie existiert
if exist ImageSort_Portable.zip (
    del ImageSort_Portable.zip
    echo ImageSort_Portable.zip was deleted
)

:: Lösche den Ordner ImageSort_Portable, falls er existiert
if exist ImageSort_Portable (
    rmdir /s /q ImageSort_Portable
    echo ImageSort_Portable was deleted
)

:: Create both
@echo on
jpackage --type msi -n ImageSort --runtime-image "target\imagesort" --icon Logo.ico -m imagesort/com.github.racopokemon.imagesort.Main --win-dir-chooser --win-menu --win-shortcut --win-shortcut-prompt 
jpackage --type app-image -n ImageSort_Portable --runtime-image "target\imagesort" --icon Logo.ico -m imagesort/com.github.racopokemon.imagesort.Main 
@echo off

:: Zippe den Ordner ImageSort_Portable
echo Zipping ImageSort_Portable...
powershell -command "Compress-Archive -Path .\ImageSort_Portable -DestinationPath ImageSort_Portable.zip"

:: Lösche den Ordner ImageSort_Portable
rmdir /s /q ImageSort_Portable
echo ImageSort_Portable was deleted

:: Gib eine Pause ein, damit du das Ergebnis sehen kannst
pause

::Alles übrigens wieder ChatGPT Magie