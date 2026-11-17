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
    private func capture(_ app:XCUIApplication,_ name:String) {
        let attachment=XCTAttachment(screenshot:app.screenshot())
        attachment.name=name;attachment.lifetime = .keepAlways;add(attachment)
    }
    private func openLearning(_ app:XCUIApplication) {
        let link=app.buttons["home_learning"]
        XCTAssertTrue(link.waitForExistence(timeout:15));reveal(link,in:app);link.tap()
        XCTAssertTrue(app.buttons["learning_tab_0"].waitForExistence(timeout:5))
    }
    func testSettingsKeepsNavigationAndChangesAppearance() throws {
        let app=home()
        let settings=app.buttons["home_settings"]
        XCTAssertTrue(settings.waitForExistence(timeout:15));settings.tap()
        XCTAssertTrue(app.staticTexts["Qari pilihan"].waitForExistence(timeout:5))
        XCTAssertTrue(app.buttons["tab_0"].isHittable)
        capture(app,"Pengaturan iOS")
        let dark=app.switches["settings_dark"]
        reveal(dark,in:app);XCTAssertTrue(dark.isHittable)
        if dark.value as? String != "1" {dark.tap()}
        XCTAssertEqual(dark.value as? String,"1")
        capture(app,"Pengaturan biru gelap iOS")
        app.buttons["tab_0"].tap()
        XCTAssertTrue(app.buttons["home_settings"].waitForExistence(timeout:5))
        capture(app,"Beranda biru gelap iOS")
        app.buttons["home_settings"].tap();reveal(dark,in:app);dark.tap()
        XCTAssertEqual(dark.value as? String,"0")
    }
    func testFreeMemorizationProgressSurvivesRelaunch() throws {
        let app=home();openLearning(app)
        capture(app,"Jalur belajar iOS")
        app.buttons["learning_tab_2"].tap()
        app.buttons["learning_free_practice"].tap()
        let revealVerse=app.buttons["Tampilkan ayat"]
        XCTAssertTrue(revealVerse.waitForExistence(timeout:5));reveal(revealVerse,in:app);revealVerse.tap()
        let rating=app.buttons["Lancar menurut saya"]
        reveal(rating,in:app);XCTAssertTrue(rating.isHittable);rating.tap()
        XCTAssertTrue(app.staticTexts["Catatan tersimpan."].waitForExistence(timeout:5))
        capture(app,"Hafalan mandiri iOS")
        app.buttons["Selesai"].tap()
        app.buttons["learning_tab_3"].tap()
        let count=app.staticTexts["learning_review_count"]
        XCTAssertTrue(count.waitForExistence(timeout:5))
        let savedCount=count.label;XCTAssertNotEqual(savedCount,"0 ayat pernah dinilai mandiri")
        app.terminate();app.launch();openLearning(app)
        app.buttons["learning_tab_3"].tap()
        XCTAssertTrue(count.waitForExistence(timeout:5));XCTAssertEqual(count.label,savedCount)
        capture(app,"Progres tersimpan iOS")
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
