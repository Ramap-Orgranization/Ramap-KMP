import SwiftUI
import Shared
import KakaoMapsSDK
import KakaoSDKAuth
import KakaoSDKCommon
import KakaoSDKUser

@main
struct iOSApp: App {
    init() {
        UnhandledExceptionLoggerKt.installUnhandledExceptionLogger()
        SDKInitializer.InitSDK(appKey: RamapAppConfig.shared.kakaoNativeAppKey)
        KakaoSDK.initSDK(appKey: RamapAppConfig.shared.kakaoNativeAppKey)
        KoinInitializerKt.doInitKoin(appDeclaration: { _ in })
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
                    }
                    AuthDeepLinkHandlerKt.handleAuthDeepLink(url: url)
                }
        }
    }
}
