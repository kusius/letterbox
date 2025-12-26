import Foundation
import UIKit
import GoogleSignIn
import ComposeApp

// Notification names used by the KMM side
private let KMMGoogleSignInStart = Notification.Name("KMMGoogleSignInStart")
private let KMMGoogleSignInRevoke = Notification.Name("KMMGoogleSignInRevoke")
private let KMMGoogleSignInCompleted = Notification.Name("KMMGoogleSignInCompleted")

// Ensure this handler is retained for the app lifetime. For SwiftUI apps, create this in the App struct and keep it
// as a @StateObject or global variable. For UIKit, instantiate this in AppDelegate.
class GoogleSignInBridge {
    
    // Keep a shared singleton to ensure observers are retained for app lifetime
    static let shared = GoogleSignInBridge()
    
    var previousSignIn: GIDGoogleUser? = nil

    private init() {
        print("GoogleSignInBridge: init - registering observers")
        NotificationCenter.default.addObserver(self, selector: #selector(handleSignInNotification(_:)), name: KMMGoogleSignInStart, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(handleSignOutNotification(_:)), name: KMMGoogleSignInRevoke, object: nil)
    }

    @objc private func handleSignInNotification(_ note: Notification) {
        print("GoogleSignInBridge: handleSignInNotification called")
        guard let clientID = Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String else {
            postCompletion(token: nil, refreshToken: nil, serverAuthCode: nil, error: "Missing CLIENT_ID in Info.plist")
            return
        }

        let configuration = GIDConfiguration(clientID: clientID)

        // Find a presenting view controller
        guard let rootVC = UIApplication.shared.connectedScenes
                .flatMap({ ($0 as? UIWindowScene)?.windows ?? [] })
                .first(where: { $0.isKeyWindow })?.rootViewController else {
            postCompletion(token: nil, refreshToken: nil, serverAuthCode: nil, error: "No root view controller to present sign-in")
            return
        }
        
        if(previousSignIn != nil) {
            self.postCompletion(
                token: previousSignIn?.accessToken.tokenString,
                refreshToken: previousSignIn?.refreshToken.tokenString,
                serverAuthCode: nil,
                error: nil
            )
            return
        }
        
        GIDSignIn.sharedInstance.signIn(withPresenting: rootVC) { user, error in
            if let error = error {
                self.postCompletion(token: nil, refreshToken: nil, serverAuthCode: nil, error: error.localizedDescription)
                return
            }

            guard let user = user else {
                self.postCompletion(token: nil,  refreshToken: nil, serverAuthCode: nil, error: "No user object returned")
                return
            }

            // Access token or ID token depending on what your backend needs
            // let idToken = user.user.idToken?.tokenString
            let accessToken = user.user.accessToken.tokenString
            let refreshToken = user.user.refreshToken.tokenString
            let serverAuthCode = user.serverAuthCode

            // Prefer ID token for authentication to backend (OIDC), or accessToken for Google APIs

            self.postCompletion(token: accessToken, refreshToken: refreshToken, serverAuthCode: serverAuthCode, error: nil)
        }
    }

    @objc private func handleSignOutNotification(_ note: Notification) {
        GIDSignIn.sharedInstance.signOut()
    }

    private func postCompletion(token: String?, refreshToken: String?,  serverAuthCode: String?, error: String?) {
        // Post completion notification; Kotlin's observer expects the notification.object to be the token string.
        // If you need to pass an error message, put it into userInfo, e.g., ["error": error]
        var userInfo: [String: Any] = [:]
        if let token = token { userInfo["token"] = token }
        if let refresh = refreshToken { userInfo["refreshToken"] = refresh }
        if let server = serverAuthCode { userInfo["serverAuthCode"] = server }
        if let err = error { userInfo["error"] = err }

        NotificationCenter.default.post(name: KMMGoogleSignInCompleted, object: nil, userInfo: userInfo.isEmpty ? nil : userInfo)
    }
}

// Usage:
// - Add GoogleSignInBridge.shared initialization in your App lifecycle so observers are registered early.
//   For SwiftUI: call `_ = GoogleSignInBridge.shared` inside your App struct's init().
//   For UIKit/AppDelegate: call `GoogleSignInBridge.shared` in application(_:didFinishLaunchingWithOptions:).
// - Add the GoogleSignIn Swift package via Xcode File → Add Packages and add the 'GoogleSignIn' package.
// - Add CLIENT_ID (your reversed client ID or web client id depending on setup) into Info.plist with key "CLIENT_ID".
// - Add URL types / reversed client id entry in Info.plist as described by GoogleSignIn docs.
