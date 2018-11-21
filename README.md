# ChromedriverUpdater
App for update chromedriver file to latest version.

Tested on Mac OS and Windows 10.

# How it works
The app uses directory "drivers" (in root folder of project) and file "chromedriverCurrentVersion.txt" which contains the current version (like "2.41") of chromedriver file which you use in your Selenium tests.
If the directory or txt file are not exist it will be created and the app will download the latest version of chromedriver file.

- So, the app downloads html page with chromedriver versions and gets the latest version.
- Then the app will check current verion in txt file.
- If latest version > current version then the app generate a link to archive which contains the latest version of driver.
- The app download the archive, unzip it, make file executable and change version in txt file.
