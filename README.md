# [Image Sort](https://github.com/Racopokemon/ImageSort)

Simple javafx app that lets you cycle through the .jpg files in a folder, and organise / index / sort them: 
* click and hold inside a photo to zoom (zoom adjustable)
* delete or backspace instantly move the shown image to a '/deleted' folder
* up and down change the category of the shown image, it may either be kept in the directory or moved to one of the subfolders 1, 2 or 3
* the moving to the 1, 2 and 3 folders is done when the application is closed

# Installation
* Download [Java](https://www.java.com/en/) (at least version 8)
* Download the newest `.jar` file from the release section to the right.
* Simply double click the file to start the application. It will ask you to select a folder of images to work on. 

## Furter feature ideas
* ~~Right click context menu~~
  * ~~Show in explorer~~
  * First entry just states the filename
  * Open with default app
  * (later rotate)
  * Context key also opens it
  * Enter key opens with default app
* ~~F5 Key silently rescans the folder~~
  * ~~That is basically 2 lines~~
* Safety
  * What happens, if an image is deleted or renamed externally, and ...
    * the folder is rescanned (needs to be deleted out of all categories?)
    * the window is closed
  * More output and communication when closing the window
    * n images were moved. m images were copied. 
    * for x images there was errors. \[also: show error messages every time, with option to show no more and go on]
* UI
  * Add small arrows \< and > to the navigation buttons at the side
  * Show how many images are in each category
  * Show "filtered" when filtering in the progress bar
* Filter by category
  * Copy the behavior of the bottom right text (scrolling, mouse highlight, etc)
  * no filter, keep only, 1-3 only
  * change the accessibility (right now its Q and E keys, I dont want this)
  * handle the case when there is no images
  * before next or previous picture is called, store the last image when changing filter (to chose a close start image inside the filtered set)
  * Delete during filter only changes it back to keep (?)
  * The buffer also needs to buffer the images in the category only
  * Improvements
    * Constantly find the first images of the other categories and buffer them, so that cycling through the categories is a bit better than *this* right now
      * Only the adjacent categories?
      * Disable once there are memory errors (test using the command line)
    * On the long run: Improve the priorities when loading images..
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
* Copy option
  * every image can also be copied to the folders 1, 2, 3 (the numbering of the categories is turned into a, b, c)
  * clickable, or with the keys 1, 2, 3
  * filter also for these folders
* Progress bar
  * ~~127/238 images and a bar (also for the filtered view)~~
  * ~~Scroll also works on it~~
  * Mouse drag allows searching
* Menu bar
  * only visible in window mode
  * can rename folders (1, 2, 3, a, b, c) and change quantity of copy and move options
  * also rename button there
* Session storage
  * Simply stores the maps to a file and continues from it if it lies in the folder (or so)
