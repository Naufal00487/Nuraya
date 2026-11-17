import XCTest
@testable import NurayaCore
final class ParityTests: XCTestCase {
    var assets: URL { URL(fileURLWithPath:ProcessInfo.processInfo.environment["NURAYA_CONTENT"] ?? "../Nuraya/Resources/Content",isDirectory:true) }
    func corpus() throws -> QuranCorpus { try QuranCorpus.load(Data(contentsOf:assets.appendingPathComponent("quran.json"))) }
    func testCanonicalReferencesAndOpeningPolicyForEverySurah() throws {
        let c=try corpus();var separated=0
        for s in c.surahs {
            let v=try XCTUnwrap(c.verse(Reference(s.id,1)))
            if s.id==1 || s.id==9 {XCTAssertNil(v.opening)}
            else {XCTAssertNotNil(v.opening); XCTAssertEqual(v.opening!+" "+v.text,v.arabic);separated+=1}
            XCTAssertNotNil(c.verse(Reference(s.id,s.ayahCount)))
            XCTAssertNil(c.verse(Reference(s.id,s.ayahCount+1)))
        }
        XCTAssertEqual(separated,112)
        let naml=try XCTUnwrap(c.verse(Reference(27,30)));XCTAssertEqual(naml.text,naml.arabic)
    }
    func testAll6236ReferencesInBothPageIndexes() throws {
        let c=try corpus()
        for name in ["indonesia","madinah"] {
            let index=try JSONDecoder().decode(PageIndex.self,from:Data(contentsOf:assets.appendingPathComponent("printed_mushaf/\(name)-index.json")))
            try index.validate(c)
            for s in c.surahs {let first=try index.page(for:Reference(s.id,1));XCTAssertTrue((1...604).contains(first));if s.id>2 {XCTAssertGreaterThan(first,3)}}
            XCTAssertThrowsError(try index.page(for:Reference(115,1)))
        }
    }
    func testRepeatedQueueCrossesSurahWithoutAddingBasmalah() throws {
        let c=try corpus();let a=Reference(112,4),b=Reference(113,2)
        let queue=try c.range(a,b,each:2,passes:2)
        XCTAssertEqual(queue.map { $0.reference.id },["112:4","112:4","113:1","113:1","113:2","113:2","112:4","112:4","113:1","113:1","113:2","113:2"])
        XCTAssertThrowsError(try c.range(b,a,each:1,passes:1))
        XCTAssertThrowsError(try c.range(Reference(1,1),Reference(114,6),each:20,passes:1))
    }
    func testRecognitionPartialTailIsNotMarkedMissing() {
        let c=RecitationMatcher.compare(expected:"بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",recognized:"بسم الله",final:false)
        XCTAssertEqual(c.words.map(\.state),[.matched,.matched,.pending,.pending])
        let f=RecitationMatcher.compare(expected:"بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",recognized:"بسم الله",final:true)
        XCTAssertEqual(f.words.map(\.state),[.matched,.matched,.notHeard,.notHeard])
        XCTAssertTrue(RecitationMatcher.spellings("ٱلصَّلَوٰةَ").contains("الصلاة"))
        XCTAssertFalse(RecitationMatcher.spellings("وَٰحِد").contains("احد"))
    }
    func testSpokenIndonesianNumbers() {
        XCTAssertEqual(Normalization.spoken("cari surat Al Baqarah ayat dua ratus lima puluh lima"),"cari surat al baqarah ayat 255")
        XCTAssertEqual(Normalization.spoken("surat sebelas ayat tiga"),"surat 11 ayat 3")
    }
    func testCatalogHasSame28ExplicitPairs() throws {
        let rows=try JSONDecoder().decode([ReciterSelection].self,from:Data(contentsOf:assets.appendingPathComponent("curated_reciters.json")))
        XCTAssertEqual(rows.count,28);XCTAssertEqual(Set(rows.map(\.id)).count,28)
        XCTAssertEqual(rows.first { $0.chapterId == "ar.abdulbasitmurattal" }?.verseId,"ar.abdulsamad")
        XCTAssertFalse(rows.contains { $0.name.contains("Matrood") || $0.name.contains("Jaber") })
    }
    func testTypedAndIndonesianSpokenSearchUseSameReferences() throws {
        let c=try corpus()
        let search=try QuranSearch(corpus:c,lexical:Data(contentsOf:assets.appendingPathComponent("learning_index.bin")),semantic:Data(contentsOf:assets.appendingPathComponent("semantic_index.bin")),metadata:Data(contentsOf:assets.appendingPathComponent("semantic_index_sources.json")),names:Data(contentsOf:assets.appendingPathComponent("surah_aliases.json")))
        for s in c.surahs {
            let result=search.find("surat \(s.name) ayat 1")
            XCTAssertTrue(result.verses.contains {$0.reference==Reference(s.id,1)},s.name)
        }
        XCTAssertEqual(search.find("cari surat Al Baqarah ayat dua ratus lima puluh lima").verses.first?.reference,Reference(2,255))
        XCTAssertEqual(search.find("2:255").verses.first?.reference,Reference(2,255))
        XCTAssertTrue(search.find("114:7").invalid)
        XCTAssertTrue(search.find("surat 2 ayat -1").invalid)
        XCTAssertEqual(search.find("cari ayat kursi").verses.first?.reference,Reference(2,255))
        XCTAssertEqual(search.find("cari QS 2:255").verses.first?.reference,Reference(2,255))
        XCTAssertFalse(search.find("sabar menghadapi ujian").verses.isEmpty)
        XCTAssertFalse(search.find("tolong cari surat yasinn").surahs.isEmpty)
    }
    func testLessonsResumeWithoutChangingAnswersOrAwardingDoubleXP() throws {
        let c=try corpus(),course=try JSONDecoder().decode(LearningCourse.self,from:Data(contentsOf:assets.appendingPathComponent("learning_course.json"))).lessons
        var kinds=Set<ExerciseKind>()
        for lesson in course {
            var session=LessonSession(lesson:lesson,course:course,corpus:c)
            XCTAssertFalse(session.exercises.isEmpty)
            for ex in session.exercises {
                kinds.insert(ex.kind)
                XCTAssertEqual(Set(ex.options.map(\.id)).count,ex.options.count)
                XCTAssertEqual(Set(ex.options.map(\.text)).count,ex.options.count)
                if ex.kind == .order {XCTAssertEqual(ex.expected.compactMap(Int.init),ex.expected.compactMap(Int.init).sorted())}
            }
            var guardSteps=0
            while let ex=session.current {
                session.submit(ex.kind == .recall ? ["fluent"]:ex.expected)
                // A checked answer must survive an interrupted/closed application.
                session=try JSONDecoder().decode(LessonSession.self,from:JSONEncoder().encode(session))
                XCTAssertTrue(session.checked);session.next();guardSteps+=1
                XCTAssertLessThan(guardSteps,100)
                if guardSteps>=100 {break}
            }
            var progress=LearningProgress();progress.finish(session,date:"2026-11-17")
            XCTAssertEqual(progress.xp,20);progress.finish(session,date:"2026-11-17")
            XCTAssertEqual(progress.xp,20);XCTAssertEqual(progress.daily["2026-11-17"],1)
        }
        XCTAssertEqual(kinds,[.choice,.match,.order,.recall])
    }
    func testOnlyElevenMatchingRecordingsEnableWordTiming() throws {
        struct Catalog:Decodable {let reciters:[VerseReciter]}
        let catalog=try JSONDecoder().decode(Catalog.self,from:Data(contentsOf:assets.appendingPathComponent("verse_reciters.json")))
        let curated=try JSONDecoder().decode([ReciterSelection].self,from:Data(contentsOf:assets.appendingPathComponent("curated_reciters.json")))
        let timed=catalog.reciters.filter {q in q.supportsWordSync==true && curated.contains(where:{$0.verseId==q.id})}
        XCTAssertEqual(timed.count,11)
        for qari in timed {
            let folder=try XCTUnwrap(qari.timingFolder)
            for s in 1...114 {
                let chapter=try JSONDecoder().decode(TimedChapter.self,from:Data(contentsOf:assets.appendingPathComponent("audio_timing/\(folder)/\(s).json")))
                XCTAssertTrue(chapter.matches(qari));XCTAssertEqual(chapter.surah,s)
                XCTAssertTrue(chapter.audioUrl.hasPrefix("https://"))
                for v in chapter.verses {XCTAssertGreaterThan(v.end,v.start)}
            }
        }
    }
    func testLearningDatesAndSelfAssessmentStaySeparateFromQuizScores() throws {
        var progress=LearningProgress()
        progress.daily=["2026-11-14":1,"2026-11-15":2,"2026-11-16":1]
        XCTAssertEqual(progress.streak(today:"2026-11-17"),3)
        XCTAssertEqual(progress.streak(today:"2026-11-18"),0)
        progress.review(Reference(112,1),rating:"help",date:"2026-11-17")
        XCTAssertEqual(progress.reviews?["112:1"]?.nextDate,"2026-11-18")
        progress.review(Reference(112,1),rating:"fluent",date:"2026-11-18")
        XCTAssertEqual(progress.reviews?["112:1"]?.nextDate,"2026-11-21")
        progress.review(Reference(112,1),rating:"fluent",date:"2026-11-21")
        XCTAssertEqual(progress.reviews?["112:1"]?.nextDate,"2026-11-27")
        XCTAssertEqual(progress.xp,0)
        XCTAssertTrue(progress.records.isEmpty)
        let restored=try JSONDecoder().decode(LearningProgress.self,from:JSONEncoder().encode(progress))
        XCTAssertEqual(restored.reviews?["112:1"]?.attempts,3)
    }
}
