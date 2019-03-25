# Application:
- simple task manager;
- allows to take photo of text document and recognize its contents;
- integrated with Google Drive.

# Setup
Download latest Android Studio version and open the project. Launch the project on real Android device connected via USB (won't normally work with camera on any Android Studio / Genymotion emulator).

# Application Flow
Create new task, during creation you may add photo / recognize text document via OpenCV + Tesseract combination using icon buttons. After the task is created you may still use these icon buttons for the same actions. Landing app screen provides ability to change task's status as well as detailed screen of each task.

# Text Recognition
1. OpenCV used for image processing, extracts regions of the image and passes them to Tesseract
2. Tesseract used for text recognition

# Text Recognition Process
1. Take a photo using configured in-app camera
2. Receive monochrome version of the image
3. Modify the image using simple UI for rotating / cropping / some image processing stuff
4. OpenCV looks for different regions with text on the image
5. Receive debug images on Google Drive for different recognition phases and images for each region
6. Tesseract pool recognizes each region and gives the whole text as result separated by lines (1 line = 1 region)

# Automated Testing Flow 
Espresso test which presses special button and starts test recognition of pre-loaded image.
Could be ran from OpenCVOCRTest class.
1) Pre-configured image with text for recognition
2) Digital text from this image
3) Semantic analysis (similarity) between produced result & original digital text
https://www.paralleldots.com/semantic-analysis
