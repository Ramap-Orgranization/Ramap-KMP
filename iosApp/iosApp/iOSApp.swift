import SwiftUI
import Shared
import NMapsMap
import KakaoSDKAuth
import KakaoSDKCommon
import KakaoSDKUser
import UserNotifications

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let deepLink = response.notification.request.content.userInfo["deep_link"] as? String
        NotificationLaunchDispatcher.shared.dispatch(deepLink: deepLink)
        completionHandler()
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        UnhandledExceptionLoggerKt.installUnhandledExceptionLogger()
        NMFAuthManager.shared().ncpKeyId = RamapSecrets.shared.naverMapNcpKeyId
        KakaoSDK.initSDK(appKey: RamapSecrets.shared.kakaoNativeAppKey)
        KoinInitializerKt.doInitKoin(appDeclaration: { _ in })
        NotificationCenter.default.addObserver(
            forName: Notification.Name("NotificationPermissionGranted"),
            object: nil,
            queue: .main
        ) { _ in
            UIApplication.shared.registerForRemoteNotifications()
        }
        NotificationCenter.default.addObserver(
            forName: Notification.Name("KakaoLoginRequest"),
            object: nil,
            queue: .main
        ) { _ in
            let completion: (OAuthToken?, Error?) -> Void = { token, error in
                IosKakaoLoginBridge.shared.complete(
                    idToken: token?.idToken,
                    accessToken: token?.accessToken,
                    errorMessage: error?.localizedDescription
                )
            }

            if UserApi.isKakaoTalkLoginAvailable() {
                UserApi.shared.loginWithKakaoTalk(completion: completion)
            } else {
                UserApi.shared.loginWithKakaoAccount(completion: completion)
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    if AuthApi.isKakaoTalkLoginUrl(url) {
                        _ = AuthController.handleOpenUrl(url: url)
                        return
                    }
                    AuthDeepLinkHandlerKt.handleAuthDeepLink(url: url)
                }
        }
    }
}
