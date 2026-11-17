import SwiftUI
import UIKit
import NurayaCore
struct MushafCanvas:UIViewRepresentable {
    let image:UIImage,regions:[MushafRegion],selected:Reference?,zoom:Double
    let tap:()->Void,doubleTap:()->Void,select:(Reference)->Void,turn:(Int)->Void
    func makeUIView(context:Context)->PageCanvas {PageCanvas()}
    func updateUIView(_ view:PageCanvas,context:Context){view.tapAction=tap;view.doubleAction=doubleTap;view.selectAction=select;view.turnAction=turn;view.regions=regions;view.selected=selected;view.set(image:image,zoom:zoom)}
}
final class PageCanvas:UIView,UIScrollViewDelegate,UIGestureRecognizerDelegate {
    let scroll=UIScrollView(),picture=UIImageView(),highlight=CAShapeLayer()
    var regions=[MushafRegion](),selected:Reference?,tapAction:(()->Void)?,doubleAction:(()->Void)?,selectAction:((Reference)->Void)?,turnAction:((Int)->Void)?
    private var lastZoom=1.0,lastBounds=CGRect.zero
    override init(frame:CGRect){super.init(frame:frame);backgroundColor=UIColor(Color.mushafPaper);scroll.delegate=self;scroll.maximumZoomScale=3;scroll.minimumZoomScale=1;scroll.showsHorizontalScrollIndicator=false;scroll.showsVerticalScrollIndicator=false;addSubview(scroll);scroll.addSubview(picture);picture.isUserInteractionEnabled=true;picture.contentMode = .scaleAspectFit;picture.layer.addSublayer(highlight);highlight.fillColor=UIColor.systemYellow.withAlphaComponent(0.25).cgColor
        let single=UITapGestureRecognizer(target:self,action:#selector(tap)),double=UITapGestureRecognizer(target:self,action:#selector(doubleTap));double.numberOfTapsRequired=2;single.require(toFail:double);addGestureRecognizer(single);addGestureRecognizer(double)
        let long=UILongPressGestureRecognizer(target:self,action:#selector(longPress(_:)));long.minimumPressDuration=0.45;picture.addGestureRecognizer(long)
        for direction in [UISwipeGestureRecognizer.Direction.left,.right] {let swipe=UISwipeGestureRecognizer(target:self,action:#selector(swipe(_:)));swipe.direction=direction;swipe.delegate=self;addGestureRecognizer(swipe);scroll.panGestureRecognizer.require(toFail:swipe)}
    }
    required init?(coder:NSCoder){fatalError("init(coder:) is not used")}
    func set(image:UIImage,zoom:Double){if picture.image !== image {scroll.setZoomScale(1,animated:false);picture.image=image;lastBounds = .zero;setNeedsLayout()};if zoom != lastZoom {lastZoom=zoom;scroll.setZoomScale(CGFloat(zoom),animated:true)};paintHighlight()}
    override func layoutSubviews(){super.layoutSubviews();scroll.frame=bounds;if lastBounds != bounds {lastBounds=bounds;scroll.setZoomScale(1,animated:false);guard let image=picture.image else {return};let scale=min(bounds.width/image.size.width,bounds.height/image.size.height);let size=CGSize(width:image.size.width*scale,height:image.size.height*scale);picture.frame=CGRect(x:(bounds.width-size.width)/2,y:(bounds.height-size.height)/2,width:size.width,height:size.height);scroll.contentSize=bounds.size;paintHighlight()}}
    func viewForZooming(in scrollView:UIScrollView)->UIView?{picture}
    func scrollViewDidZoom(_ scrollView:UIScrollView){let x=max(0,(scroll.bounds.width-picture.frame.width)/2),y=max(0,(scroll.bounds.height-picture.frame.height)/2);picture.center=CGPoint(x:scroll.contentSize.width/2+x,y:scroll.contentSize.height/2+y)}
    private func paintHighlight(){guard let image=picture.image else {return};let p=UIBezierPath();if let selected=selected,let region=regions.first(where:{$0.reference==selected}) {let sx=picture.bounds.width/image.size.width,sy=picture.bounds.height/image.size.height;for box in region.rects where box.count==4 {p.append(UIBezierPath(rect:CGRect(x:box[0]*sx,y:box[1]*sy,width:(box[2]-box[0])*sx,height:(box[3]-box[1])*sy)))}};highlight.path=p.cgPath}
    override func gestureRecognizerShouldBegin(_ gestureRecognizer:UIGestureRecognizer)->Bool {!(gestureRecognizer is UISwipeGestureRecognizer) || scroll.zoomScale<=1.01}
    @objc func tap(){tapAction?()}
    @objc func doubleTap(){doubleAction?()}
    @objc func swipe(_ gesture:UISwipeGestureRecognizer){if scroll.zoomScale<=1.01 {turnAction?(gesture.direction == .right ? 1:-1)}}
    @objc func longPress(_ gesture:UILongPressGestureRecognizer){guard gesture.state == .began,let image=picture.image else {return};let p=gesture.location(in:picture);let x=p.x*image.size.width/picture.bounds.width,y=p.y*image.size.height/picture.bounds.height;if let r=regions.first(where:{$0.contains(x:x,y:y)}) {UIImpactFeedbackGenerator(style:.light).impactOccurred();selectAction?(r.reference)}}
}
