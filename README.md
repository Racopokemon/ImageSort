# Image Sort

[github.com/Racopokemon/ImageSort](https://github.com/Racopokemon/ImageSort)

Simple javafx app that lets you cycle through the .jpg files in a folder, and organise / index / sort them: 
* click and hold inside a photo to zoom (zoom adjustable)
* delete or backspace instantly move the shown image to a '/deleted' folder
* up and down change the category of the shown image, it may either be kept in the directory or moved to one of the subfolders 1, 2 or 3
* the moving to the 1, 2 and 3 folders is done when the application is closed

Yeah, thats it already. But it really comes in handy when you got a bunch of vacation pics and quickly want to select the best ones to share with your friends. 

## Furter feature ideas
* ~~Right click context menu~~
  * ~~Show in explorer~~
  * Open with default app
  * (later rotate)
  * Context key also opens it
  * Enter key opens with default app
* UI
  * Add small arrows \< and > to the navigation buttons at the side
* Filter by category
  * Copy the behavior of the bottom right text (scrolling, mouse highlight, etc)
  * no filter, keep only, 1-3 only
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
  * every image can also be copied to the folders a, b, c
  * clickable, or with the keys
  * filter also for these folders
* Progress bar
  * 127/238 images and a bar (also for the filtered view)
* Menu bar
  * only visible in window mode
  * can rename folders (1, 2, 3, a, b, c) and change quantity of copy and move options
  * also rename button there
* Session storage
  * Simply stores the maps to a file and continues from it if it lies in the folder (or so)
