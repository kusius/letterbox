//
//  JsonLottieView.swift
//  iosApp
//
//  Created by George K on 2/1/26.
//
import UIKit
import Lottie
import ComposeApp

class LottieIOSUiKitViewProvider : LottieUiKitViewProvider {
    func jsonLottieView(json: String) -> UIView {
        return JsonLottieView(json: json)
    }
}

public class JsonLottieView: UIView {

    private let animationView = LottieAnimationView()

    init(json: String) {
        super.init(frame: .zero)

        backgroundColor = .clear
        isOpaque = false

        animationView.backgroundColor = .clear
        animationView.isOpaque = false
        animationView.loopMode = .loop
        animationView.contentMode = .scaleAspectFit

        if let data = json.data(using: .utf8),
           let animation = try? LottieAnimation.from(data: data) {
            animationView.animation = animation
            animationView.play()
        } else {
            assertionFailure("Failed to load Lottie animation from JSON")
        }

        animationView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(animationView)

        NSLayoutConstraint.activate([
            animationView.leadingAnchor.constraint(equalTo: leadingAnchor),
            animationView.trailingAnchor.constraint(equalTo: trailingAnchor),
            animationView.topAnchor.constraint(equalTo: topAnchor),
            animationView.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

