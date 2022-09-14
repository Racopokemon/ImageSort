# Image Sort

Simple javafx app that lets you cycle through the .jpg files in a folder, and organise / index / sort them: 
* click and hold inside a photo to zoom (zoom adjustable)
* delete or backspace instantly move the shown image to a '/deleted' folder
* up and down change the category of the shown image, it may either be kept in the directory or moved to one of the subfolders 1, 2 or 3
* the moving to the 1, 2 and 3 folders is done when the application is closed

Yeah, thats it already. But it really comes in handy when you got a bunch of vacation pics and quickly want to select the best ones to share with your friends. 

## Furter feature ideas
* Right click context menu  
  * Show in explorer
  * Open with default app
  * (later rotate)
  * Context key also opens it
  * Enter key opens with default app
* Filter by category
  * Copy the behavior of the bottom right text (scrolling, mouse highlight, etc)
  * no filter, keep only, 1-3 only
  * handle the case when there is no images
  * before next or previous picture is called, store the last image when changing filter (to chose a close start image inside the filtered set)
  * Delete during filter only changes it back to keep (?)
  * The buffer also needs to buffer the images in the category only
* Origin of the micro lags
  * I think its somewhere in the hardware acceleration, that old images sometimes hang for a second
  * Therefore it should also appear when all images are constantly preloaded, with < 7 images in a folder
  * Test this
* Rotate
  * by writing the EXIF data only (keep the existing data!)
  * Context menu and Q and E buttons
* Video support
  * Hopefully not too hard since javafx should be a media library
  * probably no exif rotation?!

## Getting Started

Welcome to the VS Code Java world. Here is a guideline to help you get started to write Java code in Visual Studio Code.

## Folder Structure

The workspace contains two folders by default, where:

- `src`: the folder to maintain sources
- `lib`: the folder to maintain dependencies

Meanwhile, the compiled output files will be generated in the `bin` folder by default.

> If you want to customize the folder structure, open `.vscode/settings.json` and update the related settings there.

## Dependency Management

The `JAVA PROJECTS` view allows you to manage your dependencies. More details can be found [here](https://github.com/microsoft/vscode-java-dependency#manage-dependencies).
