# Play Google Glass Launcher

## Overview

This project provides a simple demonstration of how to implement the Google Glass [Mirror API](https://developers.google.com/glass/develop/mirror/index) using Typesafe's [Play Framework](https://www.playframework.com). The web application allows an authenticated user (paired with a Google Glass) to deliver financial data such as stock charts and dividends data to a Google Glass timeline as static cards. It also allows a user to view Google Glass timeline cards that have been created on the device as well as remove them from the timeline. 

This application is loosely based on the Quick Start Java example provided in the Mirror API Guides located at [https://developers.google.com/glass/develop/mirror/quickstart/java](https://developers.google.com/glass/develop/mirror/quickstart/java). Instead of going the Java Servlet approach, this application uses Play to drive all HTTP requests into the application, routing the requests through OAuth2 authentication services and dedicate Controllers to the required endpoints


In addition to relying on the Play Framework, the application uses Google's OAuth2 authentication mechanism coupled with the Google Client API. The Quick Start example has been adapted to use the Play build and artificat management system (as opposed to Maven).


## Technologies Used

The following are the key technologies used:

- Java 8
- Play Framework (2.3.4)
- Activator (1.2.10)
- Bootstrap (3.1.1)
- Google Client (1.18.0-rc and all it's dependent libraries)
- Google Service API (v2-rev70-1.18.0-rc and all it's dependent libraries)


## Installation and Configuration

To setup the project, follow these steps:

1. If not already done, install the latest Typesafe Activator. The application is compatible with Play 2.3.4, so Activator 1.2.10 should work well. The package can be downloaded from [https://www.playframework.com/download](https://www.playframework.com/download). 

2. Create your own Google APIs Console project. The steps to perform this task are outlined at [https://developers.google.com/glass/develop/mirror/quickstart/java#creating_a_google_apis_console_project](https://developers.google.com/glass/develop/mirror/quickstart/java#creating_a_google_apis_console_project). A few items of note:

	- The screenshots in the quick start guide are dated. But the general steps are the same: you need to create the project, specify the API services that need to be available to your project, define your credentials (OAuth >> Web Application) and define the Conscent screen.

	- The Redirect  URI for the Client ID should be `http://localhost:9000/oauth2callback`. If you are running this application outside of a local environment and/or have a different port number for the Play application, please adjust the URL as needed.

	- Once you're done defining the project, make note of the resulting Client ID and Client Secret; you will need these values for your Play configuration file.

3. Clone the project locally from GitHub. 

4. Update the Google OAuth2 properties in [googleglass.conf](conf/googleglass.conf) to use the defined Client ID and Client Secret. See the `clientId` and `clientSecret` properties in the file.


## Running the application

Once the application has been configured, you can run it using the following commands from the root directory of the application:
```
$ activator
```

and after the Play console is started, then ...

```
[play-glass-launcher] $ run
```

To view the application go to [http://localhost:9000](http://localhost:9000). This should display the OAuth authorization screen from Google.  You will need a Google Account to be able to authorize the application to have access to your Google Glass Timeline and a some basic account information. The conscent page should state which data elements you're exposing via Google's OAuth2 flow. Once the authorization is granted to the application, it should display the landing page with the actions that can be performed.

Given that the OAuth2 access token is not persisted, should the server be restarted, you will likely need to reauthorize the application to access your account. To do this simply click on the [Re-Login](http://localhost:9000/relogin) link on the right side of the header.


## OAuth2 Flow

- Google's OAuth2 architecture is used to handle authentication for the application
- The singular OAuth endpoint in the application is at the route `/oauth2callback` which is handled by the controller [GoogleClientOAuthController.java](app/controllers/GoogleClientOAuthController.java). This class will orchestrate the OAuth dance.
- Controller endpoints that interact with the Glass Timeline and hence need to be secured are annotated with 
```
@GlassSecurity.GlassAuthenticated
```
This will ensure that the contained `GlassAuthenticatedAction` action class in the [GlassSecurity.java](app/controllers/GlassSecurity.java) will intercept the request and determine if the current user has gone through the OAuth flow or not.
