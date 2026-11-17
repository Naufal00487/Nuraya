import XCTest
final class NurayaUITests:XCTestCase {
    override func setUpWithError() throws {continueAfterFailure=false}
    private func home()->XCUIApplication {
        let app=XCUIApplication();app.launchArguments=["--ui-testing"];app.launch()
        let name=app.textFields["Nama panggilan"]
        if name.waitForExistence(timeout:8){name.tap();name.typeText("Penguji Nuraya");app.buttons["Mulai menggunakan Nuraya"].tap()}
        return app
    }
    private func reveal(_ element:XCUIElement,in app:XCUIApplication) {
        for _ in 0..<7 {if element.isHittable{return};app.swipeUp()}
    }
    func testFirstRunProfileAndNavigation() throws {
        let app=home()
        XCTAssertTrue(app.buttons["Baca Al-Qur’an"].waitForExistence(timeout:15))
        let screenshot=XCTAttachment(screenshot:app.screenshot());screenshot.name="Beranda iOS";screenshot.lifetime = .keepAlways;add(screenshot)
        reveal(app.buttons["Baca Al-Qur’an"],in:app);app.buttons["Baca Al-Qur’an"].tap()
        XCTAssertTrue(app.staticTexts["Pilih cara membaca"].waitForExistence(timeout:10))
        let modes=XCTAttachment(screenshot:app.screenshot());modes.name="Pilihan Quran iOS";modes.lifetime = .keepAlways;add(modes)
    }
    func testLateSurahOpensItsActualMadinahPage() throws {
        let app=home()
        reveal(app.buttons["Baca Al-Qur’an"],in:app);app.buttons["Baca Al-Qur’an"].tap()
        let mode=app.buttons["mode_madinah"]
        reveal(mode,in:app);XCTAssertTrue(mode.exists);mode.tap()
        let search=app.textFields["Cari surah atau nomor"]
        XCTAssertTrue(search.waitForExistence(timeout:5));search.tap();search.typeText("114")
        let surah=app.buttons["surah_114"];XCTAssertTrue(surah.waitForExistence(timeout:5));surah.tap()
        // Page 604 is bundled, so this regression test does not depend on networking.
        let surface=app.otherElements["mushaf_surface"]
        XCTAssertTrue(surface.waitForExistence(timeout:10))
        surface.coordinate(withNormalizedOffset:CGVector(dx:0.5,dy:0.5)).tap()
        XCTAssertTrue(app.staticTexts["Halaman 604 / 604"].waitForExistence(timeout:5))
        let page=XCTAttachment(screenshot:app.screenshot());page.name="An-Nas halaman 604 iOS";page.lifetime = .keepAlways;add(page)
        app.buttons["Tutup Quran"].tap()
        XCTAssertTrue(search.waitForExistence(timeout:5))
    }
}
