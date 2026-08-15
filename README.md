Overview:  
"Cherry Browser" (test.cherrybrowser) is a starting Android browser designed to reduce repetitive tasks whether when building a custom browser app or a new webview app, it provides a basic browser app that behaves normally like a normal Android Chromium browser out of the box. Build your browser apk and prototye fast without pulling hundreds of Megabytes of dependencies.

Constraints:
- Using Android system webview.
- Minimum SDK 26
- Target SDK 33
- SDK build tools 33.0.3
- Java ≤ 17
- Gradle 7.6.6
- Kotlin 1.7.10 (Preferred language)
- Only use built-in classes. No external dependencies unless it's a dead end.
- External dependencies that pulls hundreds of Megabytes of dependency tree is a big no.

Features:
- Solving common issues on a fresh bare Android webview app such as:
  - Loading common non standard hosts such as localhost, IP address, host with ports, etc.
  - Input upload
  - Multi window and pop ups
  - Intent links
  - Default webview user agent being blocked on sites like Google search, etc.
  - Web APIs
  - Javascript Console
  - etc.
- When we run `gradle assembleDebug` on the fresh project, expect a normal browser apk that behaves like a standard Android Chromium browser.
- Multitab browsing capabilities with background tab preserved in memory.
- Custom download engine that avoiding system's download manager to solve inconsistencies with system's download manager across OEMs.
- Intercept download on blobs using Javascript techniques that solve "Refused to connect because it violates the document's Content Security Policy..."
- Anti-wipe web storages: localStorage and IndexedDB are treated like a precious app's data and prevented from being wiped on low storage condition.
- Good caching mechanisms for fast page load and reduced data usage.

UI:
- Clean, minimalist UI out of the box: Bottom browser bar combining address bar, navigation buttons, and menu button that will open a small menu window.
- Native homepage.
- Minimalist tab list view.
- Settings page:
  - Preferred search engine.
  - Custom homepage url.
  - Single tab mode: Only foreground tab kept-alive, background tabs will be saved in a snapshot when switching tab and reloaded on demand.
  - User agent selection/custom.
  - Manage webview caches, cookies, and web storages.
