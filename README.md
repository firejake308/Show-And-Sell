# Show & Sell
Central High School
Keller, TX

### Overview
When we began this project, we began with the following specifications in mind:
>Create a mobile application that would allow a platform for a digital yard sale to raise funds to attend NLC. The app should allow for the donation of items, including picture, suggested price, and a rating for the condition of the item. The app should allow for interaction/comments on the items. Code should be error free.

We believe that with Show & Sell, we have gone above and beyond these guidelines. Our application strives to be the most powerful, easiest-to-use platform for hosting digital yard sales, thanks to its inclusion of the following features:

* Donation of items, including picture, suggested price, condition, and more details
* Commenting on items
* Bookmarking items for future reference
* Full support for online purchasing using the Braintree Payments API
* Creation of multiple yard sales/groups
* Management of groups
* Deleting items from the server and hiding unapproved items
* Anti-abuse protections
* Finding the groups that are closest to the user with location services
* Searching for items and groups
* Help section
* Account creation and login with email or Google OAuth
* Sharing items through Twitter
* Deep links into the app from Twitter
* Both Apple and Android versions of the app, each with a user-friendly UI that fits well with native design patterns

With Show & Sell, we empower FBLA chapters and other organizations to mobilize their members and the community to harness the power of mobile technology to raise funds from the sale of donated items.

### Instructions
To run the app, please follow the instructions listed below:
1. Open Android Studio
2. Import the project using one of the following 2 methods:
a) Open Android Studio and select the File >> Open option. Navigate to the parent directory of this file, which should be called ```Show-And-Sell```. Select that directory and press ```OK```.
b) Clone the [project repository](https://github.com/firejake308/Show-And-Sell) and import into Android Studio.
3. Build and Run the app to view the debug version. Alternatively, you can go to Build >> Generate Signed APK to test the release version.
4. The app is also available on [Google Play](https://play.google.com/store/apps/details?id=com.insertcoolnamehere.showandsell).
5. [Link to the Apple version](https://github.com/mcjcloud/Show-And-Sell)
6. [Link to the Backend repo](https://github.com/mcjcloud/ShowAndSellAPI)

### Software Used
The Android app was developed with Android Studio, with code hosted on a GitHub repository and a GearHost server for Twitter deep links. 

### Templates Used
No templates were used except for those provided within Android Studio.

### Source of Information
Show & Sell was built with the help of the official [Android Developer guides, trainings, and references](https://developer.android.com/index.html), along with debugging aid from [Stack Overflow](http://stackoverflow.com/). The developer guide for [Braintree Payments](https://developers.braintreepayments.com/guides/client-sdk/setup/android/v2) helped us in setting up online purchasing.

### Copyright Notations
+ The TutorialActivity features the [Stepstone Tech material stepper](https://github.com/stepstone-tech/android-material-stepper), which is distributed under an [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0).
+ The ItemDetailActivity features the [Twitter Kit for Android](https://github.com/twitter/twitter-kit-android), which is also licensed under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0).
+ The [Fabric.io](https://get.fabric.io/) plugin for Android Studio was also used to simplify the Twitter integration.
+ The [Braintree Payments API](https://developers.braintreepayments.com/) was used for online purchasing
