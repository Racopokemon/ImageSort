# [Image Sort](https://github.com/Racopokemon/ImageSort)

Simple javafx app that lets you cycle through the .jpg files in a folder, and organize / index / sort them: 
* click and hold inside a photo to zoom (zoom adjustable)
* delete or backspace instantly move the shown image to a '/deleted' folder
* up and down change the category of the shown image, it may either be kept in the directory or moved to one of the subfolders 1, 2 or 3
* keys a, b, c, d to have a copy of the images into the corresponding folders
* the copying to the folders a, b, c, d and the moving to the 1, 2 and 3 folders is done when the application is closed (with user confirmation)

# Installation
* Requires windows, use the `.msi` installer or the portable `.zip`, available at the *releases* section to the right! 
  * If you don't have Windows ... contact me, porting the app to linux or mac should not be a big thing. 

# Supported formats / extensions
* .jpg/.jpeg
* .png
* .gif
* .bmp

# Known issues
* See the `issues` category in the github page

# Future features
* A big list of further feature ideas and requests can be found [here](futureFeatures.md).

# The dev stuff: Building, running, etc.
## Toolchain
- Use openjdk 17
- Use and install maven (I know..)
- In your IDE, `imagesort->Plugins->javafx->run` should do the trick
## Compiling for your OS
- With maven, do `imagesort->Plugins->javafx->jlink`
- This crashes the first time, because there are compatibility issues with a non-modular package we want to use anyways
  - Ugly workaround: The promted `.jars` to be non-modular are rewritten (moditect-plugin in the pom) to be modular. Copy both from `target/modules` to the promted path and replace
- Hopefully, jlink works now. 
- jlink writes to `target/imagesort`, but this bunch of files is not bundled yet into an executable
- Run `create_msi.bat` (for Windows, the steps inside the `.bat` can be easily modified to other OS), which calls `jpackage` (included in newer jdks and creates packages for the OS you are currently on)
  - On Windows, you need some ominos 'wix'-tools to generate a `.msi` file, but the `.bat` will tell you more when you run it