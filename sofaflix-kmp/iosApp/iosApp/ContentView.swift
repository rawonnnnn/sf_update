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
        contentViewController.didMove(toParent: this)
    }
    
    override var prefersHomeIndicatorAutoHidden: Bool {
        return isLandscape
    }
}

struct ComposeView: UIViewControllerRepresentable {
    let isLandscape: Bool
    
    func makeUIViewController(context: Context) -> FullscreenViewController {
        let mainVC = MainKt.MainViewController()
        return FullscreenViewController(contentViewController: mainVC)
    }

    func updateUIViewController(_ uiViewController: FullscreenViewController, context: Context) {
        uiViewController.isLandscape = isLandscape
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
