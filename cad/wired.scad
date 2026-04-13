////use $fn=1000 for manufacturing. 
// rendering with 1K facet number is taking 8 minutes
$fn=12; 

ld=20;  // base of main housing: 15.5 mm radius => 31mm length end to end
sd=ld/2;  // top of main housing:.7.5mm radius => 1 mm length end to end
h0=15;        // hight of main housing
ht=12;        // hight of tip from top of main housing
r1t=2.7;      // bottom radius of the tip
r2t=2;      // top radious of the tip
rTipHole=r2t*0.7; // radious of tip hole
speakerSpaceDividerWidth= 2 * rTipHole*1.1;
yScale=0.8;

    // speaker housing
// make sure this fuses with tip of speaker without sharp edges.
speakerR=(13)/2; // 15mm speaker + space
speakerFillerH=h0*0.4956; // connection point of speaker to tip's hollow channels
speakerCenterX=8;
speakerCenterY=5;
micCenterX=0;
micCenterY=-6;

//draw2();
//translate([0,-38,0]) backPlate();
//drawWithCut(0,-1);
//drawInner3();
micHousing();
//translate([0,-28,8]) PCB();

module torus(r1=10.2,r2=2.2)
{
  rotate_extrude()
    translate([(r1-r2)/2, 0, 0])
        circle(r = r2/2);
}

module drawInner3()
{
    z1=40;//4.35;
    scale([1,yScale,1]) 
      cylinder(h=h0/z1, r1=ld, r2=sd-(1-1/z1)*(sd-ld)); 
    drawSpeakersAndMic();
    //tipHole();
    tipInner();
    tipWall(0);
    tipWall(1);
    tipWall(2);
    snipPins();
    jack();
}

module drawSpeakersAndMic()
{
    drawRoundSpeaker(1);
    drawRoundSpeaker(-1);
    micHousing();
    snipPins();
    jack();
}

module drawRoundSpeaker(n=1)
{
    translate([n*speakerCenterX,speakerCenterY,0])
    {
        cylinder(h=7.5,r1=speakerR,r2=speakerR);

        rotate([0,0,n*105])
        translate([n*(speakerR-1),0,0])
        rotate([0,n*90,0])
        cylinder(h=6,r1=6/2,r2=3.5/2);
    }
    
    // Speaker to TipChannel
    union()
    {
        translate([8*n, 4, speakerFillerH-2])
        rotate([0, -n*31.8, 27*n])
          scale([1,1.35,1])
            cylinder(15, r1 = 4, r2 = 0.45);
    }    
}

module drawWithCut(x=0,y=0)
{
    difference()
    {
        draw2();
        translate([-50+x, -50+y,0])
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
    cylinder(h=h0, r1=ld+10, r2=sd); 

    translate([0,0,h0])
    cylinder(h=ht/4, r1=sd, r2=r2t); 
    
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
    w=7.5;
    dw=1.2/2;
    l=23.8;
    dl=1.8/2;
    h=2.6;
    translate([micCenterX,micCenterY,0])
    translate([l/2,-w/2,0])
    rotate([0,-90,0])
    difference()
    {
    linear_extrude(l)
    polygon(
        [
        [0,0],
        [0,w],
        [h,w-dw],
        [h,dw]
        ]);
    // Trim the 'l' dimension
    translate([0,l/2,0])
    rotate([90,0,0])
    linear_extrude(l)
    polygon(
        [
        [0,0],
        [h,dl],
        [h,0]
        ]);
    translate([0,l/2,l])
    rotate([90,0,0])
    linear_extrude(l)
    polygon(
        [
        [0,0],
        [h,-dl],
        [h,0]
        ]);

    }
    
    // Connect speaker space to above
    /*
    translate([0,-6,2.3])
      rotate([-0,0,0])
        scale([1,0.35,1])
        cylinder(4,9,3);
    
    // 3rd cylinder to connect to the below gap.
    
    translate([0,-6,4])
      rotate([-35,0,0])
        scale([1,0.6,1])
        cylinder(5,3,1.5);
    */
    // this is building  120 degree arc with 3 points. we can add more points.
    translate([0,0,13]) p6(7,rTipHole);
    //translate([0,0,10]) p6(6,rTipHole*2);
    //translate([0,0,7]) p6(6,rTipHole*3);

    //tipInner();
    
 };

module p6(h=5,r=1)
 {
   difference()
     {
   linear_extrude(h)
   polygon(
    [[0,0],
    [1*r*sin(120),1*r*cos(120)],
    [1*r*sin(150),1*r*cos(150)],
    [1*r*sin(180),1*r*cos(180)],
    [1*r*sin(210),1*r*cos(210)],
    [1*r*sin(240),1*r*cos(240)]]);
   translate([5,0,h])
     rotate([-50,0,180])
     cube(10);
     }
}
 
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
    
    sx=13;
    sy=-2;

    snipPin(sx+1,sy-1);
    snipPin(-sx-1,sy-1);
}

module jack()
{
    // wire hole for 2 microphone wires
    translate([0,10.5,2.5])
    cube([2.5,20,5],center=true);
}

module snipPin(x=0,y=0)
{
    // M3 screw, 5mm
    translate([x,y,-2])
    cylinder(h=20,r1=1.1,r2=1.1);
    
    // one edge is 4mm.
    translate([x,y,9])
    linear_extrude(20)
    circle(2.31,$fn=6);
    
    // head space at the bottom
    translate([x,y,-2])
    cylinder(h=3,r1=1.5,r2=1.5);

    // draw spaces for nut and screw head
    // see: https://www.westfieldfasteners.co.uk/Standards/Nut_Hex_M.pdf
    //https://www.engineersedge.com/hardware/_metric_socket_head_cap_screws_14054.htm
}

module backPlate()
{
    zh=5; // outer wall hight
    z1=2; // base thickness
    sd2=ld-zh/h0*(ld-sd);
    difference()
    {
    scale([1,yScale,1])
    difference()
    {
      union()
      {
        cylinder(h=zh, r1=ld+1, r2=ld); 
        translate([0,0,zh])
          cylinder(h=zh, r1=ld, r2=ld-2); 
      }
      translate([0,0,z1])
        cylinder(h=zh+10, r1=ld-2+0.05, r2=ld-2+0.05); 
    }
    snipPins();
    translate([0,16,z1+3+6])
      cube([6.1,12,15],center=true);
    
    // speaker o-ring
    translate([speakerCenterX,speakerCenterY,z1])
    torus(10.2,2.2);
    translate([-speakerCenterX,speakerCenterY,z1])
    torus(10.2,2.2);
    }

    // mic is 2mm elevated
    difference()
    {
        translate([micCenterX+4.5,micCenterY,0])
        cylinder(h=z1+2,r1=5,r2=5);
        // mic oring
        translate([micCenterX+4.5,micCenterY,z1+2])
        torus(5,2);
    }
    showInners=0;
    
    if(showInners==1)
    {
    //small bumps for speakers. 1mm portruding.
    translate([speakerCenterX,speakerCenterY,z1])
    union()
    {
      circularWallWithGap(height=zh-z1+1,innerRadius=speakerR,width=1,gapAngle=90);
    }
    
    translate([-speakerCenterX,speakerCenterY,z1])
    union()
    {
      circularWallWithGap(height=zh-z1+1,innerRadius=speakerR,width=1,gapAngle=90);
    }
    
    // very small bump for mic:
    translate([micCenterX,micCenterY,z1])
    union()
    {
      circularWallWithGap(height=zh-z1,innerRadius=3.1,width=1,gapAngle=90);
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
