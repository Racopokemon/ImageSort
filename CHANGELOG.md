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
