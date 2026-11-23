import SwiftUI
import NurayaCore
extension Color {
    init(hex:UInt32){self.init(.sRGB,red:Double((hex>>16)&255)/255,green:Double((hex>>8)&255)/255,blue:Double(hex&255)/255,opacity:1)}
    static let nuraya=Color(hex:0x2C4066)
    static let mushafPaper=Color(hex:0xFFFDF4)
}
struct Palette {
    let dark:Bool
    var background:Color {Color(hex:dark ? 0x111C30:0xF6F8FC)}
    var card:Color {Color(hex:dark ? 0x17253B:0xEEF3FB)}
    var raised:Color {Color(hex:dark ? 0x293A55:0xE4EBF6)}
    var primary:Color {Color(hex:dark ? 0xBACDF4:0x2C4066)}
    var text:Color {Color(hex:dark ? 0xE5ECF8:0x213352)}
    var secondary:Color {Color(hex:dark ? 0xB9C5DC:0x52617A)}
}
struct VectorRecord:Decodable {let id:String,width:Double,height:Double,path:String}
struct AppVector:Shape {
    let name:String
    static let records:[String:VectorRecord] = {
        guard let base=Bundle.main.resourceURL,let data=try? Data(contentsOf:base.appendingPathComponent("Content/ui_vectors.json")),let values=try? JSONDecoder().decode([VectorRecord].self,from:data) else {return [:]}
        return Dictionary(values.map {($0.id,$0)},uniquingKeysWith:{first,_ in first})
    }()
    func path(in rect:CGRect)->Path {
        guard let r=Self.records[name] else {return Path()}
        let regex=try! NSRegularExpression(pattern:"[A-Za-z]|[-+]?(?:[0-9]*\\.)?[0-9]+")
        let ns=r.path as NSString;let tokens=regex.matches(in:r.path,range:NSRange(location:0,length:ns.length)).map {ns.substring(with:$0.range)}
        var p=Path(),i=0,command="",point=CGPoint.zero
        func number()->CGFloat {defer{i+=1};return CGFloat(Double(tokens[i]) ?? 0)}
        while i<tokens.count {
            if tokens[i].first?.isLetter==true {command=tokens[i];i+=1;if command=="Z" || command=="z" {p.closeSubpath();continue}}
            let needed=["M":2,"L":2,"H":1,"V":1,"Q":4,"C":6][command] ?? 0
            if needed==0 || i+needed>tokens.count {break}
            switch command {
            case "M":point=CGPoint(x:number(),y:number());p.move(to:point);command="L"
            case "L":point=CGPoint(x:number(),y:number());p.addLine(to:point)
            case "H":point.x=number();p.addLine(to:point)
            case "V":point.y=number();p.addLine(to:point)
            case "Q":let c=CGPoint(x:number(),y:number());point=CGPoint(x:number(),y:number());p.addQuadCurve(to:point,control:c)
            case "C":let a=CGPoint(x:number(),y:number()),b=CGPoint(x:number(),y:number());point=CGPoint(x:number(),y:number());p.addCurve(to:point,control1:a,control2:b)
            default:break
            }
        }
        return p.applying(CGAffineTransform(scaleX:rect.width/r.width,y:rect.height/r.height).concatenating(CGAffineTransform(translationX:rect.minX,y:rect.minY)))
    }
}
struct AppIcon:View {let name:String;var size:CGFloat=24
    var body:some View {AppVector(name:name).fill(.foreground).frame(width:size,height:size).accessibilityHidden(true)}
}
struct AssetImage:View {
    let content:ContentStore,path:String;var fit=false
    var body:some View {if let ui=content.image(path){Image(uiImage:ui).resizable().aspectRatio(contentMode:fit ? .fit:.fill)}else {Color.clear}}
}
struct QariPhoto:View {
    let content:ContentStore,qari:ReciterSelection;var size:CGFloat=80;var rounded:CGFloat=40
    var body:some View {Group {if let ui=content.portrait(qari.id){Image(uiImage:ui).resizable().scaledToFill()}else {Text(qari.name.split(separator:" ").prefix(2).compactMap(\.first).map(String.init).joined()).font(.title)}}.frame(width:size,height:size).background(Color.nuraya.opacity(0.1)).clipShape(RoundedRectangle(cornerRadius:rounded)).accessibilityLabel("Foto "+qari.name)}
}
/// Uses the same artwork, full-width crop and blue caption as Android's SectionArtwork.
struct SectionArtwork:View {
    let content:ContentStore,section:String,title:String,subtitle:String
    var dark=false
    var body:some View {
        VStack(alignment:.leading,spacing:8){
            Spacer(minLength:80)
            Text(title).font(.system(size:28,weight:.bold))
            Text(subtitle).font(.system(size:16)).fixedSize(horizontal:false,vertical:true)
        }.padding(22).frame(maxWidth:.infinity,minHeight:190,alignment:.bottomLeading)
            .foregroundStyle(.white)
            .background {
                GeometryReader {geometry in
                    AssetImage(content:content,path:"brand/\(section)_\(dark ? "dark":"light").jpg")
                        .frame(width:geometry.size.width,height:geometry.size.height).clipped()
                        .overlay(LinearGradient(colors:[Color(hex:0x152339).opacity(0.13),Color(hex:0x223556).opacity(0.88)],startPoint:.top,endPoint:.bottom))
                }
            }
            .background(Color.nuraya)
            .clipShape(RoundedRectangle(cornerRadius:24))
            .accessibilityElement(children:.ignore)
            .accessibilityLabel(title+". "+subtitle)
            .accessibilityIdentifier("artwork_"+section)
    }
}
struct PrimaryButton:View {
    let title:String;var icon:String?=nil;var action:()->Void
    var body:some View {Button(action:action){HStack(spacing:12){if let icon=icon{AppIcon(name:icon)};Text(title).font(.system(size:18,weight:.semibold))}.frame(maxWidth:.infinity,minHeight:56).padding(.horizontal,14)}.buttonStyle(.plain).foregroundStyle(.white).background(Color.nuraya,in:RoundedRectangle(cornerRadius:18))}
}
struct MenuRow:View {
    let title:String;var subtitle:String?=nil;var icon:String="angle-right";var dark=false
    var body:some View {let p=Palette(dark:dark);HStack(spacing:16){AppIcon(name:icon).frame(width:48,height:48).background(p.raised,in:RoundedRectangle(cornerRadius:14));VStack(alignment:.leading,spacing:6){Text(title).font(.system(size:20,weight:.semibold));if let s=subtitle {Text(s).font(.system(size:16)).foregroundStyle(p.secondary).fixedSize(horizontal:false,vertical:true)}};Spacer(minLength:3);AppIcon(name:"angle-right",size:20)}.foregroundStyle(p.text).padding(18).frame(maxWidth:.infinity,minHeight:80,alignment:.leading).background(p.card,in:RoundedRectangle(cornerRadius:22))}
}
struct PageContainer<Content:View>:View {
    let dark:Bool;@ViewBuilder var content:()->Content
    var body:some View {ScrollView{VStack(alignment:.leading,spacing:20,content:content).padding(20).frame(maxWidth:740)}.frame(maxWidth:.infinity).background(Palette(dark:dark).background).foregroundStyle(Palette(dark:dark).text)}
}
struct ArabicText:View {
    let text:String;var size:Double=30;var study:StudyVerse?=nil;var color=true;var highlight:Int?=nil
    func attributed()->AttributedString {
        let source=study?.arabic ?? text;let ns=NSMutableAttributedString(string:source)
        if color,let study=study {for span in study.tajwidSpans {
            let r=NSRange(location:span.start,length:span.end-span.start);guard r.location>=0,NSMaxRange(r)<=ns.length else {continue}
            let rule=span.rule;let hex:UInt32=rule.contains("ghunnah") || rule.contains("ikhfa") ? 0x21864C : rule.contains("idgham") || rule.contains("shams") ? 0xB96822 : rule.contains("iqlab") ? 0x9054BD : rule.contains("qalaq") || rule.contains("qalq") ? 0x267BCC : rule.contains("madd") ? 0xC5344A:0x899196
            ns.addAttribute(.foregroundColor,value:UIColor(Color(hex:hex)),range:r)
        }}
        if let position=highlight,let w=study?.words.first(where:{$0.position==position}),let a=w.start,let b=w.end,a>=0,b>a,b<=ns.length {ns.addAttribute(.backgroundColor,value:UIColor.systemYellow.withAlphaComponent(0.35),range:NSRange(location:a,length:b-a))}
        return (try? AttributedString(ns,including:\.uiKit)) ?? AttributedString(source)
    }
    var body:some View {Text(attributed()).font(.custom("AmiriQuran-Regular",size:size)).lineSpacing(12).multilineTextAlignment(.leading).frame(maxWidth:.infinity,alignment:.leading).environment(\.layoutDirection,.rightToLeft).textSelection(.enabled)}
}
func timeText(_ seconds:Double)->String {guard seconds.isFinite else {return "0:00"};let n=max(0,Int(seconds));return String(format:"%d:%02d",n/60,n%60)}
