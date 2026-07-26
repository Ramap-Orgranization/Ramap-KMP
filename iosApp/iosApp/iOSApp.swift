import SwiftUI
import Shared
import NMapsMap
import KakaoSDKAuth
import KakaoSDKCommon
import KakaoSDKUser
import UserNotifications
import FirebaseCore

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
        NotificationLaunchBridgeKt.dispatchNotificationDeepLink(deepLink: deepLink)
        completionHandler()
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        if let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") {
            FirebaseApp.configure()
        } else {
            print("GoogleService-Info.plist not found. Firebase is not configured.")
        }
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
        NotificationCenter.default.addObserver(
            forName: Notification.Name("ShopShareRequest"),
            object: nil,
            queue: .main
        ) { notification in
            guard let text = notification.object as? String, !text.isEmpty else {
                return
            }
            guard let presenter = UIApplication.shared.topViewController else {
                return
            }
            let controller = UIActivityViewController(
                activityItems: [text],
                applicationActivities: nil
            )
            if let popover = controller.popoverPresentationController {
                popover.sourceView = presenter.view
                popover.sourceRect = CGRect(
                    x: presenter.view.bounds.midX,
                    y: presenter.view.bounds.midY,
                    width: 1,
                    height: 1
                )
            }
            presenter.present(controller, animated: true)
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
                    if ShopDeepLinkBridgeKt.dispatchShopDeepLink(rawUrl: url.absoluteString) {
                        return
                    }
                    AuthDeepLinkHandlerKt.handleAuthDeepLink(url: url)
                }
                .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
                    guard let url = activity.webpageURL else {
                        return
                    }
                    _ = ShopDeepLinkBridgeKt.dispatchShopDeepLink(rawUrl: url.absoluteString)
                }
        }
    }
}

private extension UIApplication {
    var topViewController: UIViewController? {
        let activeScene = connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        var controller = activeScene?.windows.first { $0.isKeyWindow }?.rootViewController
        while let presented = controller?.presentedViewController {
            controller = presented
        }
        return controller
    }
}
