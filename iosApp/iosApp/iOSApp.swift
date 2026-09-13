import SwiftUI
import UIKit
import UserNotifications

private final class ReliveNotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        if notification.request.identifier.hasPrefix("relive.export.") {
            completionHandler([.banner, .list])
        } else {
            completionHandler([])
        }
    }
}

private final class ReliveAppDelegate: NSObject, UIApplicationDelegate {
    private let notificationDelegate = ReliveNotificationDelegate()

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = notificationDelegate
        return true
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(ReliveAppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    let scoped = url.startAccessingSecurityScopedResource()
                    defer { if scoped { url.stopAccessingSecurityScopedResource() } }
                    IosPortableArchiveIngress.shared.openPath(path: url.path)
                }
        }
    }
}
