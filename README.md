# [Image Sort](https://github.com/Racopokemon/ImageSort)

Simple javafx app that lets you cycle through the .jpg files in a folder, and organize / index / sort them: 
* click and hold inside a photo to zoom (zoom adjustable)
* delete or backspace instantly move the shown image to a '/deleted' folder
* up and down change the category of the shown image, it may either be kept in the directory or moved to one of the subfolders 1, 2 or 3
* keys a, b, c, d to have a copy of the images into the corresponding folders
* the copying to the folders a, b, c, d and the moving to the 1, 2 and 3 folders is done when the application is closed (with user confirmation)

# Installation
* Requires windows, use the `.msi` installer or the portable `.zip`, available at the *releases* section to the right! 
  * If you don't have windows ... contact me, porting the app to linux or mac should not be a big thing. 

# Supported formats / extensions
* .jpg/.jpeg
* .png

# Known issues
* See the `issues` category in the github page

# Furter feature ideas

## Launcher
* Clipboard addon: Maybe check if there is image files inside before navigating there?
  * ALSO clean the clipboard afterwards. Please. 
* **Clipboard: If the folder was read from an image file or path and NOT changed, start the gallery from this file!** 
  * (Comes in handy for a folder of ~10000 images)
* ALSO: Ctrl V reads the clipboard again! 
* **Change the number of ticks and categories here!**
  * store the numbers in the preferences!
* also show .raw formats in the launcher
* maybe also show other images (?) here
* context menu (please!)
  * **Open in system explorer!**
  * **move 1 folder up**
  * (Move to /raw)
  * and move to system trash!
* Enter key should also enter the selected directory
* **Move the destination folder option to the confirmation dialog**
  * Remove the option where to place the delete-folder, I used it 0 times
  * Add the option to replace all move operations with copy operations (context sensitive, make a BUTTON for it)
* Typing keys will select the first element starting with this letter in the list!
  
## Gallery
* General
  * **indicate that a modified image is moved once the currently previewed image changes**
  * **Ctrl delete for 'instant delete' which instantly moves it to system trash?**
  * About scrolling: Maybe try quickly flashing the l/r button when you scroll forth / back?
  * Better scrolling on touchpads! 
  * GENERALLY for good style, RotatedImage should also read its metadata in a separate thread and then maybe update its data
    * This might already fix some micro-lags! 
  * I had the impression that the ImageView loads one image after another, so internally its working with a queue, and probably this queue depends on the creation time of each image. 
    * --> Use this to our advantage to load the upcoming images (depending on the scrolling direction) first
* Action indicator. An icon flashing in the middle of the screen or the whole screen shortly being lighter or so. Always consider, that both black and white images must work.
  * It flashes on slow windows commands, like open in explorer. Also on shortcuts, that don't show another feedback, maybe F5, maybe Ctrl+C, if I ever implement it. 
  * Also flash it on ctrl+Z, I just encountered that it takes a sec too much (but the reason for this is likely on the same thread, so that might not work as intended)
* Usage window
  * Simple window with slideshow (inside the gallery which is also a window with a slideshow USE "Pagination" its already implemented!) that shows the usage, 3 slides with the hints or so
* Percentage bar
  * *Write a (+1) or +.RAW or whatever on the percentage bar to indicate this copy along (if it is activated)*
  * Mouse drag allows searching
* Context menu
  * Move to trash
  * Copy image to clipboard
    * Also: Ctrl+C
  * Copy path to clipboard
    * Also: Ctrl+Shift+C
  * (later rotate)
* Advanced undo? 
    * Keep a history not only of deletion steps, but also for every image changing its category! (Also store, which filter was applied?)
    * Also allow redo steps
* Filter label
  * **Show how many images are there in each category**
  * Show here a big exit button on hover
* New file formats?
  * Video support
    * Hopefully not too hard since javafx should be a media library
    * probably no exif rotation?!
  * HEIC file support
    * Never heard of this before, container format that iPhones produce. 
    * Either support them (this would finally require me to rewrite the ImageView) or make a functionality to autoextract the .jpg image before launching. 
  * RAW support (kindof walked around this one, but still there would be options!)
* Show somewhere, how many files (general) are NOT shown in this app (bc the extension does not fit)

## File operations window
  * **Offer a button to take out the trash (move deleted folder \[4 files\]) to recycle bin, once copying has finished, IF there exists a delete folder!** 

## Other
* Progress Window
  * **once finished, add a 'put out the trash' button to move the deleted folder to system trash!**
* Session storage
  * Simply stores the maps to a file and continues from it if it lies in the folder (or so)
  * Also save the last position of the cursor
* Add some nice images or even a video to this readme! 

## Legacy / low prio ideas
* Add a shortcut to quit *and move to the next folder*?! I need that right now
* Improvements
  * Constantly find the first images of the other categories and buffer them, so that cycling through the categories is a bit better than *this* right now
    * Only the adjacent categories?
    * Disable once there are memory errors (test using the command line)
  * On the long run: Improve the priorities when loading images..
* Launcher: maybe rename folders? Or is this too much? 

