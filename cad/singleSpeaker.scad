////use $fn=1000 for manufacturing. 
// rendering with 1K facet number is taking 8 minutes
$fn=20; 

ld=20;  // base of main housing: 15.5 mm radius => 31mm length end to end
sd=ld/2;  // top of main housing:.7.5mm radius => 1 mm length end to end
h0=15+5;        // hight of main housing
ht=12+1;        // hight of tip from top of main housing
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
micCenterX=-6;
micCenterY=-8;

draw2();
translate([0,-40,0]) backPlate();
//translate([0,40,0]) tipBlocker();
//translate([0,40,0]) testChamber();
//drawWithCut(0,51);
//drawInner3();
//micHousing();
//translate([0,-28,8]) PCB();
//mainWall();

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
    tipHole();
    tipWall();
    mainWall();
    jack();
}

module drawSpeakersAndMic()
{
    jack();
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
  union()
  {
  difference() 
  {
    drawMain_Tubes_MicTubeAllTheWay_SpeakerTubeHalfWay();
    drawSpeakersAndMic();
  }
  mainWall();
  }
  snipPins(lowerHead=0);
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
tipWall();
}
}

module mainInner()
{
  dw=3.6;
  cylinder(h=h0-6,r1=ld-1-dw,r2=ld-1-dw);
  translate([0,0,h0-6])
  cylinder(h=5,r1=ld-1-dw,r2=sd-dw);
}

module mainWall()
{
    ww=1.2;
    wBigWall=3;
    
    intersection()
    {
      translate([-50,-ww/2,0])
      cube([100,ww,h0+ht+0.4]);
        tipHole();
    }
    
    intersection()
    {
        
    mainInner();
        
    // lower wall is 0.6mm offcenter to give more space to the speaker
    translate([0,(wBigWall-ww)/2,0])
    union()
    {
      cube([100,wBigWall,100],center=true);
      // ticker walls on ends for screws
      translate([10,-6,0])
      cube([6,6,100]);
      translate([-10-6,-100,0])
      cube([6,100,100]);

      translate([-10,8.5,0])
      cube([10*2,2.8,100]);
    }
    }
}

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
  translate([0,0,speakerFillerH+8])
  cylinder(h=h0+ht+1.5,r1=rTipHole,r2=rTipHole);
  translate([0,0,speakerFillerH+5])
  scale([1,yScale,1])
  cylinder(h=6,r1=rTipHole+8.2,r2=rTipHole);
};


// tip wall width is 0.6 mm. should be ok.
// tip wall length is 1.5*.9=1.35 mm.
module tipWall()
{
}

module snipPins(lowerHead=1)
{
    sc=1.2;
    
    sx=11.5;
    sy=4;

    snipPin(sx+1,-sy+1, lowerHead);
    snipPin(-sx-1,-sy+1, lowerHead);
    snipPin(0,12, lowerHead);
}

module jack()
{
    // wire hole
    translate([11,6,0])
    rotate([0,90,0])
    scale([0.6,1,1])
    cylinder(h=9,r=1.7);

    translate([11,-7,0])
    rotate([0,90,0])
    cylinder(h=9,r=0.8);
}

module snipPin(x=0,y=0,lowerHead=1)
{
    // M3 screw, 5mm
    translate([x,y,-2])
    cylinder(h=20,r1=1.1,r2=1.1);
    
    translate([x,y,15.5])
    cylinder(h=10,r1=2,r2=2);

    // head space at the bottom
    if(lowerHead==1)
    {
    // one edge is 4mm.
    translate([x,y,0])
    cylinder(h=1,r=2.31,$fn=6);
    //linear_extrude(20)    circle(2.31,$fn=6);
    }        
}

module backPlate()
{
    zh=1.5; // outer wall hight
    z1=3; // base thickness
    
    tolerance=0.01;
    
  difference()
  {
    scale([1,yScale,1])
    cylinder(h=zh+z1, r1=ld-1, r2=ld-1); 
    translate([0,0,z1])
    {
      scale([1+tolerance,1+tolerance,1])
         draw2();
      scale([1-tolerance,1-tolerance,1])
        draw2();
    }
    
    //make sure these numbers are in sync with jack();
    /*
    
    translate([11,6,0])
    rotate([0,90,0])
    scale([0.6,1,1])
    cylinder(h=9,r=1.7);

    translate([11,-7,0])
    rotate([0,90,0])
    cylinder(h=9,r=0.8);
    */
    translate([16,-7,z1+5-1])
    cube([20,1.8,10],center=true);
    translate([16,6,z1+5-1])
    cube([20,3.5,10],center=true);
    snipPins();
  }
}

module outerBulk()
{
  difference()
  {
    union()
    {
    cylinder(h=h0, r1=ld+20, r2=sd); 

    translate([0,0,h0])
    cylinder(h=ht/4, r1=sd, r2=r2t); 
    
    translate([0,0,h0])
      cylinder(h=ht, r1=r1t, r2=r2t); 
    
    translate([0,0,h0+ht/2])
      cylinder(h=ht/2, r1=r1t*1.1, r2=r2t);
    }
  
    circularWallWithGap(height=16,innerRadius=ld-1,width=55,gapAngle=0);
  }
}

module mainBlock()
{
    scale([1,yScale,1]) 
  difference()
    {
  outerBulk();
  mainInner();
  }
}


module testChamber()
{
    scale([1,yScale,1])
    difference()
    {
    cylinder(h=40,r=ld+3);
        
    translate([0,0,20])
      cylinder(h=40,r=sd);
    
    translate([0,0,h0+ht+21.7])
    rotate([180,0,0])
    outerBulk();
    }
}

module tipBlocker()
{
    tolerance=0.04;
    difference()
    {
    cylinder(h=5,r=4);
    translate([0,0,h0+ht+3])
    rotate([180,0,0])
    scale([1+tolerance,1+tolerance,1])
    draw2();
    translate([0,0,h0+ht+3])
    rotate([180,0,0])
    scale([1-tolerance,1-tolerance,1])
    draw2();
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
