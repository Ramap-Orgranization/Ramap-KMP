import SwiftUI
import Shared
import NMapsMap
import KakaoSDKAuth
import KakaoSDKCommon
import KakaoSDKUser

@main
struct iOSApp: App {
    init() {
        UnhandledExceptionLoggerKt.installUnhandledExceptionLogger()
        NMFAuthManager.shared().ncpKeyId = RamapAppConfig.shared.naverMapNcpKeyId
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
                    AuthDeepLinkHandlerKt.handleAuthDeepLink(url: url)
                }
        }
    }
}
