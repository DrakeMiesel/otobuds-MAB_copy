////use $fn=1000 for manufacturing. 
// rendering with 1K facet number is taking 8 minutes
$fn=20; 

sd=13/2 + 1;  // top of main housing:.7.5mm radius => 1 mm length end to end
ld=29/2 + 1;  // base of main housing: 15.5 mm radius => 31mm length end to end
h0=15;        // hight of main housing
ht=12;        // hight of tip from top of main housing
r1t=2.7;      // bottom radius of the tip
r2t=2;      // top radious of the tip
rTipHole=r2t*0.7; // radious of tip hole
speakerSpaceDividerWidth= 2 * rTipHole*1.1;
yScale=0.8;

// make sure this fuses with tip of speaker without sharp edges.
speakerFillerH=h0*0.4956; // connection point of speaker to tip's hollow channels
speakerCenterX=11.5/2+2;
speakerCenterY=0;
micCenterX=0;
micCenterY=-7;

draw2();
//drawWithCut();
//drawInner3();

translate([0,-28,0]) backPlate();

module drawInner3()
{
    z1=40;//4.35;
    scale([1,yScale,1]) 
      cylinder(h=h0/z1, r1=ld, r2=sd-(1-1/z1)*(sd-ld)); 
    drawSpeakersAndMic();
    tipHole();
    tipInner();
    tipWall(0);
    tipWall(1);
    tipWall(2);
    snipPins();
}

module drawSpeakersAndMic()
{
    drawRoundSpeaker(1);
    drawRoundSpeaker(-1);
    micHousing();
    snipPins();
}

module drawRoundSpeaker(n=1)
{
    // speaker housing
    translate([n*speakerCenterX,speakerCenterY,0])
    cylinder(h=3,r1=11/2,r2=11/2);

    // Speaker to TipChannel
    displacement = 3.8*n;
    union()
    {
        translate([displacement, 0, speakerFillerH + 0.8+6])
        rotate([0, n*(-90 + 50), -14*n])
          scale([1,1.35,1])
            cylinder(5, r1 = 1.2, r2 = 0.35);
        
        translate([n*(11.5/2+2),0.,0])
        rotate([0, n*(-16), 0])
            cylinder(h=15.5,r1=4,r2=1);
    }    
}

module drawWithCut()
{
    difference()
    {
        draw2();
        translate([0, -50, 0])
        //translate([0,0.75,0])
          cube([100, 50, 50]);
    }
}

module draw2()
{
  difference() 
  {
    drawMain_Tubes_MicTubeAllTheWay_SpeakerTubeHalfWay();
    drawSpeakersAndMic();
  }
}


module drawMain_Tubes_MicTubeAllTheWay_SpeakerTubeHalfWay()
{
union()
{
difference()
    {
  mainBlock();
  tipHole();
    }
tipInner();
tipWall(0);
tipWall(1);
tipWall(2);
}
}


module mainBlock()
{
  union()
  {
    scale([1,yScale,1]) 
    union()
    {
      cylinder(h=h0, r1=ld, r2=sd); 
    
      translate([0,0,h0])
      cylinder(h=ht/5, r1=sd, r2=r2t); 
    }
    
    translate([0,0,h0])
      cylinder(h=ht, r1=r1t, r2=r2t); 
    
    translate([0,0,h0+ht/2])
      cylinder(h=ht/2, r1=r1t*1.1, r2=r2t);
}
};

module micHousing()
{
    sr=3.1;
    translate([0,micCenterY,0])
    union()
    {
    //cylinder(h=4,r1=4,r2=4);
    cylinder(h=3,r1=sr,r2=sr);

    // conic chamber on top of previous.
    translate([0,0,1])
      rotate([-26,0,0])
        cylinder(14,2.2,0.8);
    }
    
    // 3rd cylinder to connect to the below gap.
    translate([0,-1.2,13])
        cylinder(2,1,0.3);
    
    // this is building  120 degree arc with 3 points. we can add more points.
    translate([0,0,13])
    linear_extrude(8)
      polygon(
        [[0,0],
        [1*rTipHole*sin(120),1*rTipHole*cos(120)],
        [1*rTipHole*sin(150),1*rTipHole*cos(150)],
        [1*rTipHole*sin(180),1*rTipHole*cos(180)],
        [1*rTipHole*sin(210),1*rTipHole*cos(210)],
        [1*rTipHole*sin(240),1*rTipHole*cos(240)]]);
 };

// the wall width at the top of the tip is 0.3*r2t=0.6 mm. is this too small? 
module tipHole()
{
  translate([0,0,speakerFillerH+9.8])
  cylinder(h=h0+ht+1.5,r1=rTipHole,r2=rTipHole);
};

// inner tip. smallest dimension. r=0.8 mm. 1.6mm radius.
module tipInner()
{
  cylinder(h=h0+ht+1.5,r1=r2t*0.4,r2=r2t*0.4);
};

// tip wall width is 0.6 mm. should be ok.
// tip wall length is 1.5*.9=1.35 mm.
module tipWall(n=0)
{
    ww=0.6;
    rotate(n*120)
    translate([-ww/2,0,0])
    cube([ww,r2t*0.9,h0+ht+1]);
}

module snipPins()
{
    sc=1.2;
    
    sx=5.5;
    sy=7;

    snipPin(sx+1,sy+1,0,sc); // off center
    snipPin(-sx,sy,0,sc);
    snipPin(sx,-sy,0,sc);
    snipPin(-sx,-sy,0,sc);
    
    //Cable hole
    translate([0,14,0.5])
    rotate([90,0,0])
    cylinder(h=12,r=2.4);

    z1=2;//4.35;
    sd2=ld-z1/h0*(ld-sd);
    scale([1,yScale,1]) 
    difference()
    {
      // The +0.1 is to remove circle's imperfections
      cylinder(h=z1, r1=ld+0.1, r2=sd2); 
      cylinder(h=z1, r1=sd2, r2=sd2); 
    }
}

module snipPin(x=0,y=0,z1=0,sc=1)
{
    translate([x,y,z1])
    cylinder(h=3,r1=1*sc,r2=0.6*sc);
    
    if(z1!=0)
    {
      translate([x,y,0])
      cylinder(h=z1,r1=1.5,r2=1.5);
    }
}

module backPlate()
{
    difference()
    {
        backPlateWithoutCableHole();

        //Cable hole. We are using slightly bigger radious then upper hole
        translate([0,14,6.5])
        rotate([90,0,0])
        cylinder(h=12,r=2.6);
    }
}

module backPlateWithoutCableHole()
{
    sx=5.5;
    sy=7;
    zh=5;
    z1=1.5;
    sc=1;
    snipPin(sx+1,sy+1,zh,sc);
    snipPin(-sx,sy,zh,sc);
    snipPin(sx,-sy,zh,sc);
    snipPin(-sx,-sy,zh,sc);

    sd2=ld-2/h0*(ld-sd);
    scale([1,yScale,1]) 
    difference()
    {
      // The +0.1 is to remove circle's imperfections
      cylinder(h=zh+2, r1=ld+1, r2=sd2); 
      translate([0,0,z1])
        cylinder(h=zh+2, r1=sd2+0.1, r2=sd2+0.1); 
    }
    
    //small bumps for speakers. 1mm portruding.
    translate([speakerCenterX,speakerCenterY,z1])
    union()
    {
      cylinder(h=2, r1=3, r2=3); 
      rotate([0,0,180])
      circularWallWithGap(height=zh-z1,innerRadius=11.5/2,width=1,gapAngle=90);
    }
    
    translate([-speakerCenterX,speakerCenterY,z1])
    union()
    {
      cylinder(h=2, r1=3, r2=3); 
      circularWallWithGap(height=zh-z1,innerRadius=11.5/2,width=1,gapAngle=90);
    }
    
    // very small bump for mic:
    translate([micCenterX,micCenterY,z1])
    union()
    {
      cylinder(h=2, r1=1, r2=1); 
      rotate([0,0,90])
      circularWallWithGap(height=zh-z1,innerRadius=3.1,width=1,gapAngle=90);
    }
}

module circularWallWithGap(height=2,innerRadius=11,width=1,
gapAngle=90)
{
    difference()
    {
        cylinder(h=height,r1=innerRadius+width,r2=innerRadius+width,center=false);
        cylinder(h=height,r1=innerRadius,r2=innerRadius,center=false);
        translate([2,-innerRadius*1.5,0])
        cube([3*innerRadius,3*innerRadius,height]);
    }
}
