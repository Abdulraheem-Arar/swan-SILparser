// swift-tools-version:5.1

import PackageDescription

let package = Package(
    name: "test",
    products: [
        .executable(name: "test", targets: ["test"])
    ],
    dependencies: [
        .package(url: "https://github.com/mtynior/ColorizeSwift.git", from: "1.5.0")
    ],
    targets: [
        .target(
            name: "test",
              dependencies: ["ColorizeSwift"],
            path:"Sources/test",
            sources: ["main.swift"],
            swiftSettings: [
              .unsafeFlags([
                "-Xfrontend",
                "-gsil",
                "-Xllvm",
                "-sil-print-debuginfo",
                "-Xllvm",
                "-sil-print-before=SerializeSILPass",
                ])
            ])
    ]
)
