# [Image Sort](https://github.com/Racopokemon/ImageSort)

Simple javafx app that lets you cycle through the .jpg files in a folder, and organize / index / sort them: 
* click and hold inside a photo to zoom (zoom adjustable)
* delete or backspace instantly move the shown image to a '/deleted' folder
* up and down change the category of the shown image, it may either be kept in the directory or moved to one of the subfolders 1, 2 or 3
* the moving to the 1, 2 and 3 folders is done when the application is closed

# Installation
* Download [Java 8](https://www.java.com/en/) (I have been informed that newer versions don't work anymore, I'm on it!)
* Download the newest `.jar` file from the release section to the right.
* Simply double click the file to start the application. It will ask you to select a folder of images to work on. 

# Supported formats / extensions
* .jpg/.jpeg
* .png

# Known issues
* See the `issues` category in the github page

# Furter feature ideas
* ~~RAW duality in Launcher file operations~~
  * ~~If there several files with the same name, but different extension, and only one is one of the supported formats, move / copy / delete all. Silently.~~
  * ~~Okay, maybe add a silent +.raw extension or whatever extensions it actually is~~
  * **Write a (+1) or +.RAW or whatever on the percentage bar to indicate this copy along (if it is activated)**
* Right click context menu
  * ~~Show in explorer~~
  * First entry just states the filename
  * Open with default app
  * Move to trash
  * (later rotate)
  * ~~Context menu key also opens context menu (did always work actually)~~
* Enter key opens with default app
* **In the info dialog before moving, indicate, how many images are moved where!**
* ~~F5 Key silently rescans the folder~~
  * ~~That is basically 2 lines~~
* Safety
  * What happens, if an image is deleted or renamed externally, and ...
    * the folder is rescanned (needs to be deleted out of all categories?)
    * the window is closed (and we probably start moving these files)
  * More output and communication when closing the window
    * n images were moved. m images were copied. 
    * for x images there was errors. \[also: show error messages every time, with option to show no more and go on]
* ~~Mouse autohide~~
  * ~~In which cases exactly?~~
    * ~~Keystroke or scroll on images?~~
    * ~~Instantly show again on movement~~
  * ~~Hide mouse after 1 sec in general?! I am starting to really need this rn!~~
* ~~Undo delete~~
  * ~~Ctrl+Z?~~
  * ~~I need it more and more often..~~
  * Advanced undo? 
    * Keep a history not only of deletion steps, but also for every image changing its category! (Also store, which filter was applied?)
    * Also allow redo steps
* Moving the mouse to the top right corner in fullscreen exits fullscreen, until the mouse is moved back again
* THE PROGRAM does not run anymore on java17, because javafx is not anymore part of the jdk. FIX THIS / port to java17! 
  * (Probably not even that easy to implement)
* Simple window with slideshow (inside the gallery which is also a window with a slideshow USE "Pagination" its already implemented!) that shows the usage, 3 slides with the hints or so
* Better scrolling on touchpads! 
* Ctrl+left/right to first and last picture? And with shift skipping 10 ones?
* ~~Youtube-like quick-jump with the number keys 0-9 (0 for the first, 5 kinda center, ...)~~
* Ctrl delete for 'instant delete' which instantly moves it to system trash?
  * **I needed it again, doit!!**
  * This requires the java Desktop api, available from Java 9. 
  * Also, use the open javafx as single library so that modern java 17 people can still run this app
  * ALSO use the desktop API to reveal in folder! 
  * On javafx 17, also find out if you get better filtering in the image view. 
* About scrolling: Maybe try quickly flashing the l/r button when you scroll forth / back?
* Add a shortcut to quit *and move to the next folder*?! I need that right
* ~~When relaunching: Select the last folder where you were in~~
* ~~Alt+Enter / F11 toggle fullscreen~~
* UI
  * ~~Small images are stretched out to fit the window. _Master, stop this!_~~
  * ~~Add small arrows \< and > to the navigation buttons at the side~~
  * Show how many images are there in each category
  * ~~Show the recording date (and time) at some place** (I really need this) (Also the weekday, please!)~~
    * ~~Maybe also when hovering the percentage center top~~
    * ~~Show even more info, like ISO or focal length (Im interested in these values now)~~
    * ~~A camera name would also be very nice!~~
  * ~~Show "filtered" when filtering in the progress bar~~
  * ~~We need an app icon!~~
  * ~~Fainly show a big arrow when reaching the first image again? On top of it all, fades quickly?~~
* Filter by category
  * ~~Copy the behavior of the bottom right text (scrolling, mouse highlight, etc)~~
  * ~~no filter, keep only, 1-3 only~~
  * change the accessibility (right now its Q and E keys, I dont want this)
  * ~~handle the case when there is no images~~
  * **indicate that a modified image is moved once the currently previewed image changes**
  * ~~before next or previous picture is called, store the last image when changing filter (to chose a close start image inside the filtered set)~~
  * Delete during filter only changes it back to keep (?)
  * ~~The buffer also needs to buffer the images in the category only~~
  * Improvements
    * Constantly find the first images of the other categories and buffer them, so that cycling through the categories is a bit better than *this* right now
      * Only the adjacent categories?
      * Disable once there are memory errors (test using the command line)
    * On the long run: Improve the priorities when loading images..
* Origin of the micro lags
  * It is directly connected to the image resolution, screenshots (relatively small) work a charm
  * I think its somewhere in the hardware acceleration, that old images sometimes hang for a second
  * Therefore it should also appear when all images are constantly preloaded, with < 7 images in a folder
  * GENERALLY for good style, RotatedImage should also read its metadata in a separate thread and then maybe update its data
* I had the impression that the ImageView loads one image after another, so internally its working with a queue, and probably this queue depends on the creation time of each image. 
  * --> Use this to our advantage to load the upcoming images (depending on the scrolling direction) first
* Rotate
  * by writing the EXIF data only (keep the existing data!)
  * Context menu and Q and E buttons
* More formats! 
  * Video support
    * Hopefully not too hard since javafx should be a media library
    * probably no exif rotation?!
  * HEIC file support
    * Never heard of this before, container format that iPhones produce. 
    * Either support them (this would finally require me to rewrite the ImageView) or make a functionality to autoextract the .jpg image before launching. 
  * RAW support
* ==Copy option==
  * every image can also be copied to the folders 1, 2, 3 (the numbering of the categories is then turned into a, b, c)
  * clickable, or with the keys 1, 2, 3
  * filter available also for these folders
* Progress bar
  * ~~127/238 images and a bar (also for the filtered view)~~
  * ~~Scroll also works on it~~
  * ~~Filename below the progress bar?~~
  * Mouse drag allows searching
* Menu bar
  * only visible in window mode
  * can rename folders (1, 2, 3, a, b, c) and change quantity of copy and move options
  * also rename button there
* Settings window
  * Accessible from the menu bar (see above)
  * ==can decide if the files should be moved to a relative folder OR to a global folder (which is also stored between the sessions)==
  * \[tick box] also show videos (if we finally have video support)
* Launcher
  * ~~A simple folder view where single folders can be double-clicked / selected and then this app launches with this folder already preselected.~~
  * ~~Became a bit bigger, this is basically our settings-window now.~~
  * ~~Mature the launcher:~~
    * ~~Clicking a folder means that the user wants to launch into this specific folder!~~
      * ~~Observe this for the launch button etc.~~
        * ~~Make a function to determine the current file (if selected, the selection, if not, what is written in the textFieldBrowser)~~
      * ~~Make a separate method for scanning for files in a separate directory, the launch button needs this then!~~
    * ~~Find out how these factories work in listBoxes, and create different custom-coded entries for folders and files:~~
      * ~~They can finally listen for themselves for double clicks etc.~~
      * ~~Clicking the outside of the listBox then also finally unselects the current folder!~~
    * ~~F5 key in browser window refreshes~~
  * also show .raw formats in the launcher
  * context menu has options: Move to /raw, move 1 folder up, and move to system trash!
  * maybe rename folders? Or is this too much? 
  * I discovered that with openfx or how its called you can also access the forward and back mouse button! Use the back one on the list view in the browser! 
  * **Make an option [x] silently move the delete folder to system trash on exit.** 
  * **If the current dir does not exist on loading, _please_ don't use the default folder but move dirs up until you find a valid dir**
    * ~~When browsing 1 dir up, select the folder you originated from!~~
    * I thought about this, if it makes sense to ask every time, since now its unavoidable etc., but usually you want to silently delete it and otherwise its only the system trash. 
* Session storage
  * Simply stores the maps to a file and continues from it if it lies in the folder (or so)
  * Also save the last position of the cursor
* Let the users decide how many categories they need (and let them rename later?) instead of the static 3
  * The 3 is not even a constant, its just hardcoded. Change this
