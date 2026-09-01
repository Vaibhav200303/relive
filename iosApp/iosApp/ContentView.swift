import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        let revenueCatApiKey = Bundle.main.object(forInfoDictionaryKey: "ReliveRevenueCatPublicApiKey") as? String ?? ""
        let termsOfServiceUrl = Bundle.main.object(forInfoDictionaryKey: "ReliveTermsOfServiceUrl") as? String ?? ""
        let privacyPolicyUrl = Bundle.main.object(forInfoDictionaryKey: "RelivePrivacyPolicyUrl") as? String ?? ""
        MainViewControllerKt.MainViewController(revenueCatApiKey: revenueCatApiKey, termsOfServiceUrl: termsOfServiceUrl, privacyPolicyUrl: privacyPolicyUrl)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
