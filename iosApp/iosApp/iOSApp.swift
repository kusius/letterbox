import SwiftUI
import GoogleSignIn
import ComposeApp

@main
struct iOSApp: App {
    
    
    init() {
        let platformKt = Platform_iosKt.getPlatform()
        platformKt.debugBuild()
        _ = GoogleSignInBridge.shared
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    let handled = GIDSignIn.sharedInstance.handle(url)
            }
            
            .onAppear {
                GIDSignIn.sharedInstance.restorePreviousSignIn { user, error in
                  if error != nil || user == nil {
                      // We must sign in again ...
                  } else {
                      // TODO: Store the user's access token inside the bridge for Kotlin code to get it.
                      //   or callback to kotlin code ...
                      GoogleSignInBridge.shared.previousSignIn = user
                  }
                }
            }
            
        }
    }
}
