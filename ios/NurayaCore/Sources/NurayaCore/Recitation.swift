import Foundation
public enum WordState: String, Codable { case matched, different, notHeard, pending }
public struct ComparedWord: Identifiable { public let id: Int, text: String, state: WordState }
public struct Comparison { public let words: [ComparedWord], extras: [String]; public let final: Bool
    public var matched: Int { words.filter { $0.state == .matched }.count }
}
public enum RecitationMatcher {
    public static func normalize(_ s: String) -> String {
        var result=""
        for u in s.precomposedStringWithCompatibilityMapping.unicodeScalars {
            switch u.value {
            case 0x0623,0x0625,0x0622,0x0671:result += "ا"
            case 0x0649,0x0626:result += "ي"
            case 0x0624:result += "و"
            case 0x0640:break
            case 0x0621...0x063a,0x0641...0x064a:result.unicodeScalars.append(u)
            default:break
            }
        };return result
    }
    public static func spellings(_ s: String) -> Set<String> {
        var result: Set<String>=[""]
        for u in s.precomposedStringWithCompatibilityMapping.unicodeScalars {
            if u.value == 0x0670 { result=Set(result.union(result.map { $0+"ا" }).sorted().prefix(32)) }
            else { let n=normalize(String(u)); if !n.isEmpty { result=Set(result.map { $0+n }) } }
        }
        let plain=normalize(s)
        if s.unicodeScalars.contains(where: { $0.value == 0x0670 }) {
            for (a,b) in ["صلوة":"صلاة","زكوة":"زكاة","حيوة":"حياة"] {
                for p in ["","ال","و","وال","ب","بال","ل","لل","ف","فال"] where plain == p+a { result.insert(p+b) }
            }
        };return result
    }
    public static func compare(expected: String, recognized: String, final: Bool) -> Comparison {
        let target=Array(expected.split(whereSeparator: \.isWhitespace).map(String.init).filter { !normalize($0).isEmpty }.prefix(300))
        let heard=Array(recognized.prefix(10000).split(whereSeparator: \.isWhitespace).map(String.init).filter { !normalize($0).isEmpty }.prefix(350))
        let accept=target.map(spellings), normalized=heard.map(normalize), n=target.count, m=heard.count
        var cost=Array(repeating:Array(repeating:0,count:m+1),count:n+1)
        for i in 0...n { cost[i][0]=i }; for j in 0...m {cost[0][j]=j}
        if n>0 && m>0 { for i in 1...n { for j in 1...m {
            cost[i][j]=min(cost[i-1][j-1]+(accept[i-1].contains(normalized[j-1]) ? 0:1),min(cost[i-1][j]+1,cost[i][j-1]+1))
        } } }
        var states=Array(repeating:final ? WordState.notHeard : .pending,count:n),extras=[String]()
        var i=final ? n : (0...n).min { cost[$0][m] < cost[$1][m] }!, j=m
        while i>0 || j>0 {
            let equal=i>0 && j>0 && accept[i-1].contains(normalized[j-1])
            if i>0 && j>0 && cost[i][j] == cost[i-1][j-1]+(equal ? 0:1) { states[i-1]=equal ? .matched : (final ? .different:.pending);i-=1;j-=1 }
            else if i>0 && cost[i][j] == cost[i-1][j]+1 {i-=1}
            else {if final {extras.append(heard[j-1])};j-=1}
        }
        return Comparison(words:target.indices.map { ComparedWord(id:$0,text:target[$0],state:states[$0]) },extras:extras.reversed(),final:final)
    }
}
