
## Launcher
* ~~On start, the launcher has the first folder selected instead of the images (if some are there), and pressing enter opens the folder instead~~
  * Fix this only for the startup, afterwards this focus is ok. Either request the focus to the start button or truly unselect everything in the listview
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
  * Typing keys will select the first element starting with this letter in the list!
* Quick n dirty 'history' that can be acessed with the mouse key and alt down. For every folder jumped in, overwrite. For every error (where does it even occur) update the history. 
* ***Ctrl+V does not work if the text field is focused in the launcher***
  * check if clipboard is non-text. then consume, otherwise let it happen
* ***Move the destination folder option to the confirmation dialog***
  * Remove the option where to place the delete-folder, I used it 0 times
* ***Bug right now: When the browser window automatically appears, the selected folder is never inserted into the text field or the prefs - what is going wrong here?***
* ***Bug right now: Type D: into the search bar in the Launcher - youll end inside our current working directory, what is going on here??***
* ***There are micro-lags when just -clicking- a folder with a lot of tiles. What is this supposed to be?***
* ***The one dir up button does not work if it starts in E:/, but in the meantime E is ejected***
* ~~Bug right now: on my desktop there is a folder where a set of broken images at some point disable the arrow key input etc~~ (didnt null check exception message, fixed now)
* Option to also cycle all subfolders!
* Implement the option for "open with" in windows. If this is chosen, don't show the launcher (?) and dont show it once you finish? 
  
## Gallery
* (check this) it kinda feels like the scroll is inverted on simply jumping between images o.0 is this true?
* ~~Make F5 also reload all images! Otherwise there is no chance to update images themselves, if they have been updated~~
  * Maybe even register FS listeners to reload an image once it changed on disk
    * Register a listener on the dir. If files change, add these changes to a datastructure that is threadsafe. Poll it in the main jfx thread / platform.runLater or so. If the file name is contained in the imageBuffer, add it again. If it is the current image, ... well, load it again I'd say. 
* General
  * **indicate that a modified image is moved once the currently previewed image changes**
  * ~~Ctrl delete for 'instant delete' which instantly moves it to system trash?~~
  * make a very short timeout for the next delete operation to be triggered - esp with del and backspace on top of another it quickly happens that 2 images in a row are deleted!
  * About scrolling: Maybe try quickly flashing the l/r button when you scroll forth / back?
  * ~~Better scrolling on touchpads!~~
  * GENERALLY for good style, RotatedImage should also read its metadata in a separate thread and then maybe update its data
    * This might already fix some micro-lags! 
  * I had the impression that the ImageView loads one image after another, so internally its working with a queue, and probably this queue depends on the creation time of each image. 
    * --> Use this to our advantage to load the upcoming images (depending on the scrolling direction) first
* ~~Action indicator. An icon flashing in the middle of the screen or the whole screen shortly being lighter or so. Always consider, that both black and white images must work.~~
  * ~~It flashes on slow windows commands, like open in explorer. Also on shortcuts, that don't show another feedback, maybe F5, maybe Ctrl+C, if I ever implement it.~~
  * ~~Also flash it on ctrl+Z, I just encountered that it takes a sec too much (but the reason for this is likely on the same thread, so that might not work as intended)~~
* ~~However you will implement this in detail, add the zoom percentage indicator ALSO when only hovering the percentage bar!~~
* ~~In the percentage bar in main view, show in brackets how many images were deleted (-6)~~
* To complete keyboard navigation, make that keeping the space bar pressed does the same zoom in as clicking the screen center
  * In the same manner, make I, P and M do the same as hovering the percentage bar
* Usage window
  * Simple window with slideshow (inside the gallery which is also a window with a slideshow USE "Pagination" its already implemented!) that shows the usage, 3 slides with the hints or so
* ~~Feature request: Show the current folder name in the window title (with a - ImageSort as suffix)~~
* Percentage bar
  * ~~Write a (+1) or +.RAW or whatever on the percentage bar to indicate this copy along (if it is activated)~~
  * ~~also show +0.3EV or what this is called, if it was applied only~~
  * **Mouse drag allows searching**
    * snap at the current image & mark it in a big search bar that appears for this purpose
    * also show image number and filename
    * on the long run, also show the recording date of an image - however this is realized, also with updates etc. the simplest solution is just to cache this for every image that is being loaded, so the info is shown once the image was already seen
      * With this feature ahead, mark date changes (or better: 5am=new day) in the search bar as well, only discrete however and if it is not too much, as this might not always how the image dates are (maximum of 60 markers and also at least 1.2 pictures per day or so)
      * Knowing the record dates, we can even go further and mark the time passed between two consecutive images, maybe less than 10 sec, less than 1 min, less than 10 minutes, less than 1h
      * also, we could indicate when the lense (and cameras) changed
    * Once this is finished, the seek keys 0-9 are prob. not needed anymore - therefore we can use them as shortcuts for copies (swapping letters & numbers) and reimplement wasd etc. 
  * Bookmarking: Pressing B or M places markers in the percentage bar (that are also visible when seeking, maybe even snapping)
* ~~Bug: The context menu does not disappear when arrow keys are pressed and another image is selected...~~
* ~~Rotate images with R button or context menu.~~
  * ~~My metadata lib supports such a wide range of formats, however its only read access. JPEG should suffice for rotations right now.~~
  * Internal cleanness: Maybe entirely switch to this library? 
* ~~Context menu~~
  * ~~Move to trash~~
  * ~~(later rotate)~~
* ~~Bug right now: For 2 images, the percentage bar is filled on both images. For 1 image, it is not even rendered lol~~
* **Filter label menu**
  * **Show how many images are there in each category**
  * Show here a big exit button on hover
  * Two buttons: +1 category, +1 copies. You cant reduce them again, but who cares even? Nobody wants to set this inside the prep window! 
  * Show shortcuts at the right to all menu entries
* ~~Show somewhere, how many files (general) are NOT shown in this app (bc the extension does not fit)~~
  * ~~Might be covered enough already by the Launcher "other files" now.~~
Further workflow change ideas:
- Always show again
- On top show in green "successful +// take out trash"
- usage hint tick is gone. automatically if 2 months not used. Option to show again. In menu, show usage hints
- ~~Show more exif info, if its relevant~~
  - ~~If the software is one of the big 5 or so (is there a list online, also for names? contains 'adobe' etc), write this in a new line.~~
  - ~~Also, mark the aperture / exposure with a * if they are set in the exposure program. Also check for other possible values, there is a few, also weird creative modes and normal modes etc.~~


New file formats?
* Video support
  * Hopefully not too hard since javafx should be a media library
  * probably no exif rotation?!
* HEIC file support
  * Never heard of this before, container format that iPhones produce. 
  * Either support them (this would finally require me to rewrite the ImageView) or make a functionality to autoextract the .jpg image before launching. 
* RAW support (kindof walked around this one, but still there would be options!)

## File operations window
  * Add the option to replace all move operations with copy operations (context sensitive, make a BUTTON for it)
  * **Offer a button to take out the trash (move deleted folder \[4 files\]) to recycle bin, once copying has finished, IF there exists a delete folder!** 
  * **Option to automatically rename images that have the same name. Why was this never a feature request before?**
  * Show summary of what was done? Same as what was shown before? ("moved 5 files" kind?)

## Other
* Progress Window
  * **once finished, add a 'put out the trash' button to move the deleted folder to system trash!**
  * **also id really like a button with 'go to created folder abc / show in folder abc' if exactly one folder was created**
* Session storage
  * Simply stores the maps to a file and continues from it if it lies in the folder (or so)
  * Also save the last position of the cursor
* Add some nice images or even a video to this readme! 

## Legacy / low prio ideas
* Advanced undo in the gallery? 
    * Keep a history not only of deletion steps, but also for every image changing its category! (Also store, which filter was applied?)
    * Also allow redo steps
    * low prio
* Add a shortcut to quit *and move to the next folder*?! I need that right now
* Improvements
  * Constantly find the first images of the other categories and buffer them, so that cycling through the categories is a bit better than *this* right now
    * Only the adjacent categories?
    * Disable once there are memory errors (test using the command line)
  * On the long run: Improve the priorities when loading images..
* Launcher: maybe rename folders? Or is this too much? 

