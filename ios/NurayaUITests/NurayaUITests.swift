import XCTest
final class NurayaUITests:XCTestCase {
    override func setUpWithError() throws {continueAfterFailure=false}
    private func home(arguments:[String]=[])->XCUIApplication {
        let app=XCUIApplication();app.launchArguments=["--ui-testing"]+arguments;app.launch()
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
        XCTAssertTrue(app.buttons["home_settings"].waitForExistence(timeout:15))
        let link=app.buttons["home_learning"]
        // SwiftUI creates this lazy-grid row only after it approaches the viewport.
        reveal(link,in:app);XCTAssertTrue(link.isHittable);link.tap()
        XCTAssertTrue(app.buttons["learning_tab_0"].waitForExistence(timeout:5))
    }
    func testMurottalAdvancesInBackgroundAndPausedAudioStaysPaused() throws {
        let app=home(arguments:["--ui-testing-audio"])
        app.buttons["tab_2"].tap()
        let qari=app.buttons["murottal_qari_ar.alafasy"]
        reveal(qari,in:app);XCTAssertTrue(qari.isHittable);qari.tap()
        let surah=app.buttons["murottal_surah_1"]
        XCTAssertTrue(surah.waitForExistence(timeout:5));surah.tap()
        let elapsed=app.staticTexts["player_elapsed"]
        let toggle=app.buttons["player_toggle"]
        XCTAssertTrue(elapsed.waitForExistence(timeout:5))
        XCTAssertTrue(app.buttons["Tersimpan"].exists,"The actual Alafasy recording must be cached; this test must not stream from the network.")
        let moving=XCTNSPredicateExpectation(predicate:NSPredicate {_,_ in
            self.elapsedSeconds(elapsed.label)>=2 && toggle.label=="Jeda"
        },object:nil)
        XCTAssertEqual(XCTWaiter.wait(for:[moving],timeout:10),.completed)
        let before=elapsedSeconds(elapsed.label)
        capture(app,"Murottal sebelum masuk Home")
        XCUIDevice.shared.press(.home)
        XCTAssertTrue(app.wait(for:.runningBackground,timeout:5))
        // Measure actual AVPlayer time across a real background interval, rather
        // than trusting the UI's playing flag or just checking that a track exists.
        Thread.sleep(forTimeInterval:8)
        app.activate()
        XCTAssertTrue(elapsed.waitForExistence(timeout:5))
        XCTAssertEqual(toggle.label,"Jeda")
        XCTAssertGreaterThanOrEqual(elapsedSeconds(elapsed.label)-before,7)
        capture(app,"Murottal tetap berjalan setelah Home")

        toggle.tap()
        XCTAssertEqual(toggle.label,"Putar")
        let pausedAt=elapsedSeconds(elapsed.label)
        XCUIDevice.shared.press(.home)
        XCTAssertTrue(app.wait(for:.runningBackground,timeout:5))
        Thread.sleep(forTimeInterval:4)
        app.activate()
        XCTAssertTrue(elapsed.waitForExistence(timeout:5))
        XCTAssertEqual(toggle.label,"Putar")
        XCTAssertEqual(elapsedSeconds(elapsed.label),pausedAt,accuracy:1)
    }
    private func elapsedSeconds(_ text:String)->Double {
        let parts=text.split(separator:":").compactMap {Double($0)}
        return parts.count==2 ? parts[0]*60+parts[1]:0
    }
    func testSwitchingTabsDismissesTheSurahSelectionLayer() throws {
        let app=home()
        for mode in ["standard","madinah","thematic"] {
            app.buttons["tab_1"].tap()
            let choice=app.buttons["mode_"+mode]
            reveal(choice,in:app);XCTAssertTrue(choice.isHittable);choice.tap()
            if mode != "thematic" {XCTAssertTrue(app.textFields["Cari surah atau nomor"].waitForExistence(timeout:5))}
            app.buttons["tab_2"].tap()
            XCTAssertTrue(app.textFields["Cari nama qari"].waitForExistence(timeout:5))
            XCTAssertFalse(app.textFields["Cari surah atau nomor"].exists)
            XCTAssertFalse(app.staticTexts["Pilihan rujukan per tema. Buka ayat untuk membaca konteks dan tafsirnya."].exists)
            if mode=="standard" {capture(app,"Murottal setelah keluar dari pilihan surah")}
        }
        app.buttons["tab_0"].tap()
        XCTAssertTrue(app.buttons["home_settings"].waitForExistence(timeout:5))
    }
    func testSectionArtworkFillsAvailableWidth() throws {
        let app=home()
        for (tab,section) in [(1,"quran"),(2,"murottal")] {
            app.buttons["tab_\(tab)"].tap()
            let artwork=app.descendants(matching:.any)["artwork_"+section].firstMatch
            XCTAssertTrue(artwork.waitForExistence(timeout:5))
            XCTAssertGreaterThanOrEqual(artwork.frame.width,app.frame.width-44)
            XCTAssertGreaterThanOrEqual(artwork.frame.height,190)
            XCTAssertEqual(artwork.frame.minX,20,accuracy:2)
            capture(app,"Header penuh "+section)
        }
    }
    func testLaunchDisplaysBothOriginalMascots() throws {
        let app=XCUIApplication();app.launchArguments=["--ui-testing","--capture-launch"];app.launch()
        let artwork=app.images["launch_artwork"]
        XCTAssertTrue(artwork.waitForExistence(timeout:5))
        XCTAssertGreaterThan(artwork.frame.height,app.frame.height*0.8)
        capture(app,"Splash dua maskot Nuraya")
    }
    func testCollectionCategoriesAreVisibleAndExplainEmptyStates() throws {
        let app=home()
        let library=app.buttons["home_library"];reveal(library,in:app);XCTAssertTrue(library.isHittable);library.tap()
        for category in ["bookmark","favorite","playlist","download","history"] {
            let filter=app.buttons["library_filter_"+category]
            XCTAssertTrue(filter.waitForExistence(timeout:5));XCTAssertTrue(filter.isHittable)
            filter.tap()
            if ["favorite","playlist","download"].contains(category) {
                XCTAssertTrue(app.descendants(matching:.any)["library_empty_"+category].firstMatch.waitForExistence(timeout:5))
            }
            if category=="bookmark" || category=="download" {capture(app,"Koleksi "+category)}
        }
    }
    func testPerAyatNavigationBookmarkAndResumeKeepTheReadingMode() throws {
        let app=home();app.buttons["tab_1"].tap()
        let mode=app.buttons["mode_standard"];reveal(mode,in:app);mode.tap()
        let search=app.textFields["Cari surah atau nomor"]
        XCTAssertTrue(search.waitForExistence(timeout:5));search.tap();search.typeText("114")
        app.buttons["surah_114"].tap()
        let ref=app.staticTexts["reader_reference"]
        XCTAssertTrue(ref.waitForExistence(timeout:5));XCTAssertEqual(ref.label,"114:1")
        app.buttons["reader_menu"].tap()
        XCTAssertTrue(app.buttons["reader_next"].waitForExistence(timeout:5))
        XCTAssertFalse(app.buttons["reader_previous"].isEnabled)
        app.buttons["reader_next"].tap()
        let bookmark=app.buttons["reader_bookmark"]
        reveal(bookmark,in:app)
        if !bookmark.label.contains("Hapus penanda") {bookmark.tap()}
        app.buttons["reader_menu_close"].tap()
        XCTAssertEqual(ref.label,"114:2")
        capture(app,"Quran per ayat An-Nas")
        app.buttons["reader_menu"].tap();app.buttons["reader_close"].tap()
        XCTAssertTrue(search.waitForExistence(timeout:5))
        app.buttons["tab_0"].tap()
        let resume=app.buttons["home_resume"];reveal(resume,in:app);XCTAssertTrue(resume.isHittable);resume.tap()
        XCTAssertTrue(ref.waitForExistence(timeout:5));XCTAssertEqual(ref.label,"114:2")
        app.buttons["reader_menu"].tap();app.buttons["reader_close"].tap()
        app.buttons["tab_0"].tap();app.swipeDown();app.swipeDown()
        let library=app.buttons["home_library"];reveal(library,in:app);library.tap()
        let entry=app.buttons["library_entry_bookmark_114:2"]
        XCTAssertTrue(entry.waitForExistence(timeout:5));capture(app,"Koleksi penanda Quran per ayat")
        entry.tap();XCTAssertTrue(ref.waitForExistence(timeout:5));XCTAssertEqual(ref.label,"114:2")
    }
    func testVoiceSearchDoesNotReuseAnAbandonedTranscript() throws {
        let app=home()
        let search=app.buttons["Cari Ayat"]
        XCTAssertTrue(search.waitForExistence(timeout:15));search.tap()
        let voice=app.buttons["search_voice"]
        XCTAssertTrue(voice.waitForExistence(timeout:5));voice.tap()
        let transcript=app.textViews["voice_transcript"]
        XCTAssertTrue(transcript.waitForExistence(timeout:5))
        transcript.tap();transcript.typeText("cari surat yasin")
        // TextEditor can draw before its accessibility value reflects the edit.
        // Wait for the typed value instead of reading a stale AX snapshot once.
        let typed=XCTNSPredicateExpectation(predicate:NSPredicate(format:"value CONTAINS[c] %@","yasin"),object:transcript)
        XCTAssertEqual(XCTWaiter.wait(for:[typed],timeout:5),.completed,"Typed transcript: \(String(describing:transcript.value))")
        app.buttons["voice_close"].tap()
        XCTAssertTrue(voice.waitForExistence(timeout:5));voice.tap()
        XCTAssertTrue(transcript.waitForExistence(timeout:5))
        XCTAssertEqual(transcript.value as? String ?? "","")
        capture(app,"Pencarian suara Indonesia iOS")
        app.buttons["voice_close"].tap()
        XCTAssertFalse((app.textFields["Cari surah, ayat, atau makna"].value as? String ?? "").contains("yasin"))
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
