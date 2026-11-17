import Foundation
public struct LessonQuestion:Codable {public let prompt:String,options:[String],correct:Int,explanation:String}
public struct Lesson:Codable,Identifiable {public let id:String,title:String,summary:String,level:Int,kind:String,explanation:String,surah:Int,ayah:Int,sourceTitle:String,sourceUrl:String,questions:[LessonQuestion]}
public struct LearningCourse:Decodable {public let lessons:[Lesson]}
public enum ExerciseKind:String,Codable {case choice,match,order,recall}
public struct ExerciseOption:Codable,Identifiable {public let id:String,text:String}
public struct Exercise:Codable,Identifiable {
    public let id:String,kind:ExerciseKind,prompt:String,options:[ExerciseOption],expected:[String],left:[ExerciseOption],explanation:String,reference:Reference?,arabic:Bool
    public var scored:Bool {kind != .recall}
}
public struct SessionAnswer:Codable {public let response:[String],correct:Bool,scored:Bool}
public struct LessonSession:Codable,Identifiable {
    public var id=UUID().uuidString;public let lessonID:String,exercises:[Exercise];public var step=0,answers=[SessionAnswer](),retries=[Int]()
    public init(lesson:Lesson,course:[Lesson],corpus:QuranCorpus) {
        lessonID=lesson.id
        if lesson.kind=="tajwid" {
            var x=lesson.questions.enumerated().map {i,q in Exercise(id:lesson.id+"_\(i)",kind:.choice,prompt:q.prompt,options:q.options.enumerated().map {ExerciseOption(id:String($0.offset),text:$0.element)}.shuffled(),expected:[String(q.correct)],left:[],explanation:q.explanation,reference:i==2 ? Reference(lesson.surah,lesson.ayah):nil,arabic:false)}
            let peers=([lesson]+course.filter {$0.kind=="tajwid" && $0.id != lesson.id}.shuffled().prefix(2)).shuffled()
            x.insert(Exercise(id:lesson.id+"_match",kind:.match,prompt:"Pasangkan nama hukum dengan cirinya.",options:peers.map {ExerciseOption(id:$0.id,text:$0.summary)}.shuffled(),expected:peers.map(\.id),left:peers.map {ExerciseOption(id:$0.id,text:$0.title)},explanation:"Kenali ciri setiap hukum, lalu perhatikan penerapannya ketika membaca.",reference:nil,arabic:false),at:min(3,x.count));exercises=x
        } else {
            let verses=corpus.chapter(lesson.surah);var x=[Exercise]()
            for i in Array(0..<max(0,verses.count-1)).shuffled().prefix(2) {
                let cue=verses[i],next=verses[i+1]
                let distractors=Dictionary(grouping:verses.filter {$0.arabic != next.arabic && $0.ayah != cue.ayah},by: \.arabic).values.compactMap(\.first).shuffled().prefix(2)
                x.append(Exercise(id:"next_\(i)",kind:.choice,prompt:"Ayat mana yang dibaca setelah ayat ini?",options:([next]+distractors).shuffled().map {ExerciseOption(id:String($0.ayah),text:$0.text)},expected:[String(next.ayah)],left:[],explanation:"Lanjutkan ayat \(cue.ayah) dengan ayat \(next.ayah).",reference:cue.reference,arabic:true))
            }
            let windows=(0..<max(0,verses.count-2)).map {Array(verses[$0..<$0+3])}.filter {Set($0.map(\.text)).count==3}
            if let window=windows.randomElement() {x.append(Exercise(id:"order",kind:.order,prompt:"Ketuk tiga ayat sesuai urutan bacaannya.",options:window.shuffled().map {ExerciseOption(id:String($0.ayah),text:$0.text)},expected:window.map {String($0.ayah)},left:[],explanation:"Urutan: "+window.map {String($0.ayah)}.joined(separator:", ")+".",reference:nil,arabic:true))}
            x += verses.map {Exercise(id:"recall_\($0.ayah)",kind:.recall,prompt:"Coba baca ayat \($0.ayah) dari ingatan.",options:[],expected:[],left:[],explanation:"Periksa dengan teks asli. Ini penilaian sendiri, bukan penilaian suara.",reference:$0.reference,arabic:true)};exercises=x
        }
    }
    public var complete:Bool {step>=exercises.count+retries.count}
    public var checked:Bool {answers.count>step}
    public var baseIndex:Int {step<exercises.count ? step : retries[step-exercises.count]}
    public var current:Exercise? {complete ? nil:exercises[baseIndex]}
    public mutating func submit(_ response:[String]) {
        guard !complete,!checked,let ex=current else {return}
        let correct=ex.kind == .recall ? ["again","help","fluent"].contains(response.first ?? "") : response==ex.expected
        answers.append(SessionAnswer(response:response,correct:correct,scored:ex.scored))
        if !correct && ex.scored && step<exercises.count {retries.append(step)}
    }
    public mutating func next() {if checked {step+=1}}
}
public struct LessonRecord:Codable {public var attempts:Int,bestCorrect:Int,total:Int,lastDate:String}
public struct MemorizationReview:Codable,Identifiable {
    public let reference:Reference,rating:String,attempts:Int,intervalDays:Int,lastDate:String,nextDate:String
    public var id:String {reference.id}
}
public struct LearningProgress:Codable {
    public var xp=0,goal=1,goalEnabled=false,daily=[String:Int](),records=[String:LessonRecord](),session:LessonSession?,completedSessions=Set<String>()
    public var reviews:[String:MemorizationReview]?
    public init() {}
    public mutating func finish(_ session:LessonSession,date:String) {
        guard session.complete,!completedSessions.contains(session.id) else {return}
        let scored=session.answers.prefix(session.exercises.count).filter(\.scored),correct=scored.filter(\.correct).count
        let old=records[session.lessonID];xp += old == nil ? 20:5;daily[date,default:0]+=1
        records[session.lessonID]=LessonRecord(attempts:(old?.attempts ?? 0)+1,bestCorrect:max(old?.bestCorrect ?? 0,correct),total:scored.count,lastDate:date)
        for (exercise,answer) in zip(session.exercises,session.answers) where exercise.kind == .recall {
            if let reference=exercise.reference,let rating=answer.response.first {review(reference,rating:rating,date:date)}
        }
        completedSessions.insert(session.id);self.session=nil
    }
    public mutating func review(_ reference:Reference,rating:String,date:String) {
        guard ["again","help","fluent"].contains(rating),let day=Self.dateFormatter.date(from:date) else{return}
        let old=reviews?[reference.id]
        let interval=rating=="again" ? 0:rating=="help" ? 1:old?.rating=="fluent" ? min(60,max(3,(old?.intervalDays ?? 0)*2)):3
        let next=Self.dateFormatter.string(from:day.addingTimeInterval(Double(interval)*86400))
        if reviews==nil {reviews=[:]}
        reviews?[reference.id]=MemorizationReview(reference:reference,rating:rating,attempts:(old?.attempts ?? 0)+1,intervalDays:interval,lastDate:date,nextDate:next)
    }
    private static var dateFormatter:DateFormatter {let f=DateFormatter();f.calendar=Calendar(identifier:.gregorian);f.locale=Locale(identifier:"en_US_POSIX");f.timeZone=TimeZone(secondsFromGMT:0);f.dateFormat="yyyy-MM-dd";return f}
    public func streak(today:String)->Int {
        guard var date=Self.dateFormatter.date(from:today) else{return 0}
        if daily[today]==nil {date=date.addingTimeInterval(-86400)}
        var count=0
        while daily[Self.dateFormatter.string(from:date)] != nil {count+=1;date=date.addingTimeInterval(-86400)}
        return count
    }
    public var badges:[String] {
        var b=[String]();if !records.isEmpty {b.append("Langkah pertama")};if records.keys.filter({$0.hasPrefix("tajwid_")}).count>=3 {b.append("Tiga tajwid")};if records.keys.filter({$0.hasPrefix("hafalan_")}).count>=3 {b.append("Tiga surah")};if daily.count>=3 {b.append("Tiga hari belajar")};if xp>=100 {b.append("100 XP")};if records.values.contains(where:{$0.attempts>=3}) {b.append("Rajin mengulang")};return b
    }
}
