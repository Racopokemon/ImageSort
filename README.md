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

# Known issues
* See the `issues` category in the github page

# Furter feature ideas

## Launcher
* **Change the number of ticks and categories here!**
  * store the numbers in the preferences!
* better show other (not supported) file formats: 
  * heic, tga, webp are candidates (not shown to be pictures yet)
  * mov, (avi)
  * The only thing happening right now are .mp4 files as movies
  * the rest is shown simply as extension. 
* context menu (please!)
  * **Open in system explorer!**
  * **move 1 folder up**
  * (Move to /raw)
  * and move to system trash!
* **Its annoying, but please make something like typing inside the listView already sorting / filtering the folders!**
* Quick n dirty 'history' that can be acessed with the mouse key and alt down. For every folder jumped in, overwrite. For every error (where does it even occur) update the history. 
* ***Ctrl+V does not work if the text field is focused in the gallery***
  * check if clipboard is non-text. then consume, otherwise let it happen
* **Move the destination folder option to the confirmation dialog**
  * Remove the option where to place the delete-folder, I used it 0 times
  * Add the option to replace all move operations with copy operations (context sensitive, make a BUTTON for it)
* Typing keys will select the first element starting with this letter in the list!
* ***Bug right now: When the browser window automatically appears, the selected folder is never inserted into the text field or the prefs - what is going wrong here?***
* ***Bug right now: Type D: into the search bar in the Launcher - youll end inside our current working directory, what is going on here??***
* ***There are micro-lags when just -clicking- a folder with a lot of tiles. What is this supposed to be?***
* ***The one dir up button does not work if it starts in E:/, but in the meantime E is ejected***
* ~~Bug right now: on my desktop there is a folder where a set of broken images at some point disable the arrow key input etc~~ (didnt null check exception message, fixed now)
* Option to also cycle all subfolders! 
  
## Gallery
* ~~Make F5 also reload all images! Otherwise there is no chance to update images themselves, if they have been updated~~
  * Maybe even register FS listeners to reload an image once it changed on disk
    * Register a listener on the dir. If files change, add these changes to a datastructure that is threadsafe. Poll it in the main jfx thread / platform.runLater or so. If the file name is contained in the imageBuffer, add it again. If it is the current image, ... well, load it again I'd say. 
* General
  * **indicate that a modified image is moved once the currently previewed image changes**
  * **Ctrl delete for 'instant delete' which instantly moves it to system trash?**
  * About scrolling: Maybe try quickly flashing the l/r button when you scroll forth / back?
  * Better scrolling on touchpads! 
  * GENERALLY for good style, RotatedImage should also read its metadata in a separate thread and then maybe update its data
    * This might already fix some micro-lags! 
  * I had the impression that the ImageView loads one image after another, so internally its working with a queue, and probably this queue depends on the creation time of each image. 
    * --> Use this to our advantage to load the upcoming images (depending on the scrolling direction) first
* ~~Action indicator. An icon flashing in the middle of the screen or the whole screen shortly being lighter or so. Always consider, that both black and white images must work.~~
  * ~~It flashes on slow windows commands, like open in explorer. Also on shortcuts, that don't show another feedback, maybe F5, maybe Ctrl+C, if I ever implement it.~~
  * ~~Also flash it on ctrl+Z, I just encountered that it takes a sec too much (but the reason for this is likely on the same thread, so that might not work as intended)~~
* ~~However you will implement this in detail, add the zoom percentage indicator ALSO when only hovering the percentage bar!~~
* ~~In the percentage bar in main view, show in brackets how many images were deleted (-6)~~
* Usage window
  * Simple window with slideshow (inside the gallery which is also a window with a slideshow USE "Pagination" its already implemented!) that shows the usage, 3 slides with the hints or so
* Feature request: Show the current folder name in the window title (with a - ImageSort as suffix)
* Percentage bar
  * *Write a (+1) or +.RAW or whatever on the percentage bar to indicate this copy along (if it is activated)*
  * Mouse drag allows searching
* Context menu
  * Move to trash
  * (later rotate)
* Advanced undo? 
    * Keep a history not only of deletion steps, but also for every image changing its category! (Also store, which filter was applied?)
    * Also allow redo steps
    * low prio
* Filter label menu
  * **Show how many images are there in each category**
  * Show here a big exit button on hover
  * Two buttons +1 category, +1 copies
* ~~Show somewhere, how many files (general) are NOT shown in this app (bc the extension does not fit)~~
  * ~~Might be covered enough already by the Launcher "other files" now.~~

New file formats?
* Video support
  * Hopefully not too hard since javafx should be a media library
  * probably no exif rotation?!
* HEIC file support
  * Never heard of this before, container format that iPhones produce. 
  * Either support them (this would finally require me to rewrite the ImageView) or make a functionality to autoextract the .jpg image before launching. 
* RAW support (kindof walked around this one, but still there would be options!)

## File operations window
  * **Offer a button to take out the trash (move deleted folder \[4 files\]) to recycle bin, once copying has finished, IF there exists a delete folder!** 
  * **Option to automatically rename images that have the same name. Why was this never a feature request before?**
  * Show summary of what was done? Same as what was shown before?

## Other
* Progress Window
  * **once finished, add a 'put out the trash' button to move the deleted folder to system trash!**
  * **also id really like a button with 'go to created folder abc / show in folder abc' if exactly one folder was created**
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

