@echo off
echo we assume you have already created a runtime image as follows: in vsc, in the maven area, select plugins, javafx, jlink
echo the app jpackage, that were going to use, is part of newer jdks, make sure you have one of them installed and in path etc
echo also, you need that certain ... wix tools to make a windows installer, jpackage will prompt you a download link if it is not yet installed.
echo 
echo Creating msi: 
@echo on
jpackage --type msi -n ImageSort --runtime-image "target\imagesort" --icon Logo.ico -m imagesort/com.github.racopokemon.imagesort.Main --win-dir-chooser --win-menu --win-shortcut --win-shortcut-prompt 

@echo off

echo Creating portable folder (please delete the old one first!): 

@echo on

jpackage --type app-image -n ImageSort_Portable --runtime-image "target\imagesort" --icon Logo.ico -m imagesort/com.github.racopokemon.imagesort.Main 



pause