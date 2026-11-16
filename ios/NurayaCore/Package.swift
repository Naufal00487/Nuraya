// swift-tools-version: 5.9
import PackageDescription
let package = Package(name: "NurayaCore", platforms: [.iOS(.v16), .macOS(.v13)], products: [.library(name: "NurayaCore", targets: ["NurayaCore"])], targets: [.target(name: "NurayaCore"), .testTarget(name: "NurayaCoreTests", dependencies: ["NurayaCore"])])
