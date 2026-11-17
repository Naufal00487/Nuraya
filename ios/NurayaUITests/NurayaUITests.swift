import XCTest
final class NurayaUITests:XCTestCase {
    func testFirstRunProfileAndNavigation() throws {
        let app=XCUIApplication();app.launch()
        let name=app.textFields["Nama panggilan"]
        if name.waitForExistence(timeout:35){name.tap();name.typeText("Penguji Nuraya");app.buttons["Mulai menggunakan Nuraya"].tap()}
        XCTAssertTrue(app.buttons["Baca Al-Qur’an"].waitForExistence(timeout:15))
        let screenshot=XCTAttachment(screenshot:app.screenshot());screenshot.name="Beranda iOS";screenshot.lifetime = .keepAlways;add(screenshot)
        app.buttons["Baca Al-Qur’an"].tap()
        XCTAssertTrue(app.staticTexts["Pilih cara membaca"].waitForExistence(timeout:10))
        let modes=XCTAttachment(screenshot:app.screenshot());modes.name="Pilihan Quran iOS";modes.lifetime = .keepAlways;add(modes)
    }
}
