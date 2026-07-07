import UIKit
import SwiftUI
import ComposeApp

class FullscreenViewController: UIViewController {
    private let contentViewController: UIViewController
    var isLandscape: Bool = false {
        didSet {
            setNeedsUpdateOfHomeIndicatorAutoHidden()
        }
    }
    
    init(contentViewController: UIViewController) {
        self.contentViewController = contentViewController
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        addChild(contentViewController)
        view.addSubview(contentViewController.view)
        contentViewController.view.frame = view.bounds
        contentViewController.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        contentViewController.didMove(toParent: self)
    }
    
    override var prefersHomeIndicatorAutoHidden: Bool {
        return isLandscape
    }
}

struct ComposeView: UIViewControllerRepresentable {
    let isLandscape: Bool
    
    func makeUIViewController(context: Context) -> FullscreenViewController {
        let mainVC = MainKt.MainViewController()
        let controller = FullscreenViewController(contentViewController: mainVC)
        
        // Listen for orientation change requests from Kotlin via NSNotificationCenter
        NotificationCenter.default.addObserver(
            forName: NSNotification.Name("SofaFlixRequestLandscape"),
            object: nil,
            queue: .main
        ) { _ in
            requestOrientation(landscape: true)
        }
        
        NotificationCenter.default.addObserver(
            forName: NSNotification.Name("SofaFlixRequestPortrait"),
            object: nil,
            queue: .main
        ) { _ in
            requestOrientation(landscape: false)
        }
        
        return controller
    }

    func updateUIViewController(_ uiViewController: FullscreenViewController, context: Context) {
        uiViewController.isLandscape = isLandscape
    }
}

private func requestOrientation(landscape: Bool) {
    if #available(iOS 16.0, *) {
        if let windowScene = UIApplication.shared.connectedScenes.first(where: { $0 is UIWindowScene }) as? UIWindowScene {
            let mask: UIInterfaceOrientationMask = landscape ? .landscapeRight : .portrait
            let geometryPreferences = UIWindowScene.GeometryPreferences.iOS(interfaceOrientations: mask)
            windowScene.requestGeometryUpdate(geometryPreferences) { error in
                print("Orientation update error: \(error)")
            }
            windowScene.keyWindow?.rootViewController?.setNeedsUpdateOfSupportedInterfaceOrientations()
        }
    } else {
        let value = landscape ? UIInterfaceOrientation.landscapeRight.rawValue : UIInterfaceOrientation.portrait.rawValue
        UIDevice.current.setValue(value, forKey: "orientation")
    }
}

struct ContentView: View {
    var body: some View {
        GeometryReader { geometry in
            let isLandscape = geometry.size.width > geometry.size.height
            ComposeView(isLandscape: isLandscape)
                .ignoresSafeArea(.all)
                .statusBarHidden(isLandscape)
        }
    }
}
