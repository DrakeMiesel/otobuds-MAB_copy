// Similar to earbud 2 but with 10mm speakers.

//use $fn=1000 for manufacturing. 
// rendering with 1K facet number is taking 8 minutes
$fn=30; 

ld=17;  // base of main housing: 15.5 mm radius => 31mm length end to end
sd=ld/2;  // top of main housing:.7.5mm radius => 1 mm length end to end
h0=15;        // hight of main housing
ht=12;        // hight of tip from top of main housing
r1t=2.7;      // bottom radius of the tip
r2t=2;      // top radious of the tip
rTipHole=r2t*0.7; // radious of tip hole
speakerSpaceDividerWidth= 2 * rTipHole*1.1;
yScale=0.7;

    // speaker housing
// make sure this fuses with tip of speaker without sharp edges.
speakerR=(10+0.6)/2; // 15mm speaker + space
speakerFillerH=h0*0.4956; // connection point of speaker to tip's hollow channels
speakerCenterX=8;
speakerCenterY=1;
micCenterX=0;
micCenterY=-6;

draw2();
//drawWithCut();
//drawInner3();

translate([0,-38,0]) backPlate();

//translate([0,-28,8]) PCB();

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
    translate([n*speakerCenterX,speakerCenterY,0])
    cylinder(h=4,r1=speakerR,r2=speakerR);

    // Speaker to TipChannel
    displacement = 3.8*n;
    union()
    {
        translate([displacement, 0, speakerFillerH + 0.8+6])
        rotate([0, n*(-90 + 50), -14*n])
          scale([1,1.35,1])
            cylinder(5, r1 = 1.2, r2 = 0.35);
        
        translate([n*9,-2,5])
        rotate([0, n*(-30), -20*n])
            cylinder(h=11,r1=7,r2=1);
    }    
}

module drawWithCut()
{
    difference()
    {
        draw2();
        translate([-50, -51,0])
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
    scale([1,yScale,1]) 
  difference()
    {
  union()
  {
    union()
    {
      cylinder(h=h0, r1=ld+10, r2=sd); 
    
      translate([0,0,h0])
      cylinder(h=ht/5, r1=sd, r2=r2t); 
    }
    
    translate([0,0,h0])
      cylinder(h=ht, r1=r1t, r2=r2t); 
    
    translate([0,0,h0+ht/2])
      cylinder(h=ht/2, r1=r1t*1.1, r2=r2t);
  }
  circularWallWithGap(height=16,innerRadius=ld-2,width=15,gapAngle=0);
}
};

module micHousing()
{
    sr=3.2;
    difference()
    {
    union()
    {
    translate([0,micCenterY,0])
    union()
    {
    //cylinder(h=4,r1=4,r2=4);
    cylinder(h=2.4,r1=sr,r2=sr);

    // conic chamber on top of previous.
    translate([0,0,1])
      rotate([-40,0,0])
        cylinder(16.5,2.2,0.8);
    }
    
    // 3rd cylinder to connect to the below gap.
    translate([0,-1.2,13])
        cylinder(2,1,0.3);
    
    // this is building  120 degree arc with 3 points. we can add more points.
    translate([0,0,13])
    linear_extrude(5)
      polygon(
        [[0,0],
        [1*rTipHole*sin(120),1*rTipHole*cos(120)],
        [1*rTipHole*sin(150),1*rTipHole*cos(150)],
        [1*rTipHole*sin(180),1*rTipHole*cos(180)],
        [1*rTipHole*sin(210),1*rTipHole*cos(210)],
        [1*rTipHole*sin(240),1*rTipHole*cos(240)]]);
    }
    tipInner();
    }
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
    
    sx=6;
    sy=12;

    snipPin(sx+1,sy-1,0,sc);
    snipPin(-sx-1,sy-1,0,sc);
    snipPin(sx,-sy-0.5,0,sc);
    snipPin(-sx,-sy-0.5,0,sc);

    //pcb jack
    translate([0,6,2.5])
    union()
    {
      cube([6,12.1,5],center=true);
      translate([0,(12.1+2)/2,0])
      rotate([90,0,0])
      cylinder(h=2,r1=2,r2=2,center=true);
    }
}

module snipPin(x=0,y=0,z1=0,sc=1)
{
    // M2 screw, 5mm
    translate([x,y,-2])
    cylinder(h=20,r1=1.5,r2=1.5);
    
    // draw spaces for nut and screw head
    // see: https://www.westfieldfasteners.co.uk/Standards/Nut_Hex_M.pdf
    //https://www.engineersedge.com/hardware/_metric_socket_head_cap_screws_14054.htm
}

module backPlate()
{
    zh=7; // outer wall hight
    z1=5;// base thickness
    sc=1; // pin top hole ratio. 1=>cylinder
    sd2=ld-zh/h0*(ld-sd);
    difference()
    {
    scale([1,yScale,1]) 
    difference()
    {
      // The +0.1 is to remove circle's imperfections
      cylinder(h=zh, r1=ld+1, r2=ld); 
      translate([0,0,z1])
        cylinder(h=zh+2, r1=ld-2, r2=ld-2); 
    }
      snipPins();
    }
   
    showInners=1;
    
    if(showInners==1)
    {
    //small bumps for speakers. 1mm portruding.
    // z: 5mm for the jack, which is below the pcb
    //    1mm for pcb ticknes
    translate([0,0,5+1])
    union()
    {
    translate([speakerCenterX,speakerCenterY,0])
      circularWallWithGap(height=2,innerRadius=speakerR,width=1,gapAngle=90);
    
    translate([-speakerCenterX,speakerCenterY,0])
      circularWallWithGap(height=2,innerRadius=speakerR,width=1,gapAngle=90);
    
    // very small bump for mic:
    translate([micCenterX,micCenterY,0])
      circularWallWithGap(height=2,innerRadius=3.1,width=1,gapAngle=90);
    }
    }
}

module circularWallWithGap(height=2,innerRadius=11,width=1,
gapAngle=90)
{
    difference()
    {
        cylinder(h=height,r1=innerRadius+width,r2=innerRadius+width,center=false);
        cylinder(h=height,r1=innerRadius,r2=innerRadius,center=false);
        //translate([20,-innerRadius*1.5,0])
        //cube([3*innerRadius,3*innerRadius,height]);
    }
}

module PCB()
{
    // PCB outer dimensions.
    // This should fit in the base
    cube([22,18,1],center=true);
}
