# ImageSort 1.6
_December 17th, 2023_
*The pre-christmas release*

This update features various quality of life improvements that improve the usability at least for my use cases. 

Launcher
- Clipboard-Integration
  - Reads your clipboard on startup and enters this directory
  - You can always paste paths / images / folders to navigate to with Ctrl+V
  - When pasting an image, the gallery will also start with this image
- More detailled view of the folder contents, also showing RAWs, videos and 'other' files
- Better browser navigation using the keyboard
  - Enter, arrow keys, backspace etc work for in-folder navigation
  - Shift+Enter / Shift+Double click opens the system explorer instead

Gallery
- Copy image and image path in the context menu or with shortcuts
- Indicating image resolution while zooming or hovering percentage bar
  - Percentage bar indicating image size, as well as a categorization into the well-known youtube resolutions (480p, 4k, etc)
- Slighly prettier percentage bar

File IO
- Internally rewrote the entire IO code. This will come in handy in later updates, allowing for bulk file operations also from the launcher (delete / move one dir up / ...)

# ImageSort 1.5

_September 02, 2023_

That was more complicated than I thought. And looking back, there were quite some features that I added! But what else should I do during the last free weeks before my job starts? 

Organization
- **We now don't require an installation of java anymore!** 
  - Instead, we require *windows*. 
    - *Yes, I know. The app can however easily be ported to all major OS. Contact me!*
  - We provide an installer file (`.msi`) and a portable `.zip` folder. 
  - Internally, we have switched to java 11 (or so, now its just one config file) to use newer features of openjfx and the java desktop api etc. 
    - Internally, this also included the integration to maven and a very hacky approach to get the dependencies modular - please don't ask. 

Features
- You can now **copy AND move** files from the same UI. Bottom left is the well-known move option. We added ticks at the right side, to first copy the selected images to folders with the names `a` to `d`.
  - UI is fully adapted to this.
  - The keys `a` to `d` also toggle the copy option.
  - Filters also support the new copy options.
  - Mid-click on any move- or copy-label toggles the filter to exactly preview these images.
- `.raw` duality: For any image, if there exist other, non-image files with the *same file name* (except for the file extension which will differ), the app will silently delete, move and copy these files along. This is especially interesting for cameras saving both the `.jpg` and `.raw` file: While the app cannot yet *show* the raw-version, it still can give you the `.jpg` preview and just move the connected `.raw`-file along to whatever place you desire! 
- In gallery, enter key opens the 'open with' dialog for the given file. Ctrl+Enter shows the file in the system explorer. 
- We also show the lens name and manufacturer in the percentage bar. Works very well esp. with google pixel photos ('google pixel front camera' ...)
- Zoom indicator while zooming.
  - Mid-clicking or pressing spacebar while zooming sets the zoom to 100%. 
- Top-Right hotcorner: 'click to exit fullscreen'.
- Top-Left hotcorner: Hides all UI as long as the mouse stays there. Good for very dark images. 
- Home, End, Ctrl-left, Ctrl-right, Shift-left and Shift-right can be used to navigate the images.
- Ctrl+up and Ctrl+down to walk through the filters (instead of q and e).

More stuff for convenience
- We now show, how many files are to be moved & copied when asking for user confirmation.
- Way better progress indication and error communication when applying the file operations, with its own little window. 
- When the launcher opens, it checks the user clipboard for files, folders or strings containing a path. If one of it exists, the navigation starts from this point. Great for saving time. 
- When the launcher opens and the last known folder does not exist anymore, we select the first parent folder of this old path that still exists, instead of switching to a default path.
- LR-Buttons and the hotcorners now can be scrolled like the rest of the image. 
- First entry of the image context menu just shows the filename. 
- Back mouse key now works in the launcher browser window.
- (Cleaned up the readme feature ideas)

Removed features
- The previous handling of copy / move in the launcher.
- The keys `wasd` navigating were removed to make space for setting the copy option `abcd`. 

# ImageSort 1.4

_March 29, 2023_

(Actually, I should be doing my Master's thesis right now)

We now have a **Launcher window**! 
_It functions a bit like a big settings window_
- Select your images folder, using the native system dialog or the launcher itself
- Decide if you want to *move* or *copy* your images
- Decide your target directory (relative to your current folder, or an external location)
- Decide if you want the launcher to reopen
- Decide if you want to be shown the usage hints
- All settings made are stored between sessions

Quality of life improvements
- The mouse generally hides after 1 second
- We now show more image information: Camera name, exposure and aperture
- Indication when the search wraps reaching the first (or last) image again

... and more
- We have an app icon!

# ImageSort 1.3
_Feb 1, 2023_

Small quality of life improvements
- Images smaller than the viewport are not anymore stretched to fit the screen
- When hovering the percentage bar, you also see the image a recording date and focal length + ISO, if available
- Ctrl+Z to undo deletions
- Keys 0 ... 9 to skim through the images (as the youtube-player does it)

# ImageSort 1.2
_Jan 7, 2023_

Various bigger and smaller quality of life improvements
- Filtering! Filter for all images, the ones in only one category, or the ones to keep.
- Progress bar on top (42/136 images), shows filename on hover
- Mouse auto-(un)hide when scrolling through images
- Right-click context menu
- F5 key silently rescans folder
- F11 / Alt+Enter toggle fullscreen

# ImageSort 1.1
_Sep 19, 2022_

Kind of the 'what happens if things go wrong' update
- Indicators for image loading progress
- Error descriptions for not loading images
- Improved recovery from cycling fast through images
- Adaptive image buffer size now enables using the app also in JVMs with lower memory, also with higher resolution images

# ImageSort 1.0
_Sep 18, 2022_

Initial version. 
- Starts by asking for a folder to load
- Scroll through the images in the folder, or click the interface
- Hold the mouse button pressed to zoom
- Image buffering
- Exif-tag reading to obtain actual image orientation
- Select categories: Either keep the image in its original folder, or move it to one of the folders 1-3
- Instant delete with del or backspace
- Fullscreen and window mode
- When pressing the `X` button, you are asked if the marked images should now be moved to the specified folders
