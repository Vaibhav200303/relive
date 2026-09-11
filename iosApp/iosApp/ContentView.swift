import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        let revenueCatApiKey = Bundle.main.object(forInfoDictionaryKey: "ReliveRevenueCatPublicApiKey") as? String ?? ""
        let termsOfServiceUrl = Bundle.main.object(forInfoDictionaryKey: "ReliveTermsOfServiceUrl") as? String ?? ""
        let privacyPolicyUrl = Bundle.main.object(forInfoDictionaryKey: "RelivePrivacyPolicyUrl") as? String ?? ""
        let supportEmail = Bundle.main.object(forInfoDictionaryKey: "ReliveSupportEmail") as? String ?? ""
        let launcherIconUpdate: (String?) -> Void = { iconName in
            let application = UIApplication.shared
            guard application.supportsAlternateIcons,
                  application.alternateIconName != iconName else {
                return
            }
            application.setAlternateIconName(iconName) { _ in }
        }
        return MainViewControllerKt.MainViewController(
            revenueCatApiKey: revenueCatApiKey,
            termsOfServiceUrl: termsOfServiceUrl,
            privacyPolicyUrl: privacyPolicyUrl,
            supportEmail: supportEmail,
            launcherIconUpdate: launcherIconUpdate
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
