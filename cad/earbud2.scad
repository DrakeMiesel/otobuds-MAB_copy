////use $fn=1000 for manufacturing. 
// rendering with 1K facet number is taking 8 minutes
$fn=40; 

sd=13/2 + 1;  // top of main housing:.7.5mm radius => 1 mm length end to end
ld=29/2 + 1;  // base of main housing: 15.5 mm radius => 31mm length end to end
h0=17;        // hight of main housing
ht=12;        // hight of tip from top of main housing
r1t=2.7;      // bottom radius of the tip
r2t=2;      // top radious of the tip
rTipHole=r2t*0.7; // radious of tip hole
speakerSpaceDividerWidth= 2 * rTipHole*1.1;

// make sure this fuses with tip of speaker without sharp edges.
speakerFillerH=h0*0.4956; // connection point of speaker to tip's hollow channels

//draw2();
drawWithCut();
//drawInner2();

module drawWithCut()
{
    difference()
    {
        draw2();
        translate([-50, 0, 0])
        //translate([0,0.75,0])
          cube([100, 50, 50]);
    }
}

// Draws inner parts of the assembly. Some 'removed' parts are drawn solid to demonstrate how it works.
module drawInner()
{
  union()
  {
    difference(){
      tipHole();
      tipInner();
    }
    
    tipWall(1);
    tipWall(2);
    speakerFiller();
    micHousing();
    speakerToTipChannel(1);
    speakerToTipChannel(-1);
  }
}

module drawInner2()
{
    difference()
    {
    union()
    {
    difference()
    {
    tipHole();
    tipInner();
    }
tipWall(0);
tipWall(1);
tipWall(2);
speakerFiller();
}
micHousing();
    speakerToTipChannel(1);
    speakerToTipChannel(-1);
}
}

module draw2()
{
  difference() 
  {
    drawMain_Tubes_MicTubeAllTheWay_SpeakerTubeHalfWay();
    micHousing();
    speakerToTipChannel(1);
    speakerToTipChannel(-1);
  }
  
  micWall();
}

module drawMain_Tubes_MicTubeAllTheWay_SpeakerTubeHalfWay()
{
union()
{
difference()
    {
  mainBlock();
  tipHole();
  speakerSpace();
}
tipInner();
tipWall(0);
tipWall(1);
tipWall(2);
speakerFiller();
}
}


module mainBlock()
{
  union()
  {
    scale([1,0.6,1]) 
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

module speakerSpace()
{
  wallWidth=2;
  union()
  {
    difference()
    {
    scale([1,0.6,1]) 
      cylinder(
        h=h0-0.5, 
        r1=ld-wallWidth, 
        r2=sd-wallWidth); 

    translate([0,0,h0/2])
    //scale([0.2,0.6,1]) 
      cube([speakerSpaceDividerWidth,ld*2,h0], center=true);
    }
  }
}

module speakerToTipChannel(n= 1)
{
    displacement = 4.5*n;
    
    translate([displacement, 0, speakerFillerH + 0.8+6])
//    rotate([0, n*(-90 + 55), -16*n])
//      scale([3,1.6,1])
//        cylinder(4.5, r1 = 1.5, r2 = 0.6);
    rotate([0, n*(-90 + 45), -4*n])
      scale([1,1.35,1])
        cylinder(4.88, r1 = 1.5, r2 = 0.42);
};


module micHousing()
{
  // 2.7mm height, 6mm diameter    
  cylinder(2.7,3,3);

  // conic chamber on top of previous.
  translate([0,0,2.7])
    cylinder(2,2.7,speakerWallDividerWidth/2);
  t0=2.7+2;
 
  // Larger hole up
  t1=speakerFillerH+10-2-t0;
  translate([0,0,t0])
    cylinder(t1,r2t*0.4,speakerWallDividerWidth/2);

  // conic chamber at the top of hole 
  translate([0,0,t0+t1])
    cylinder(1,speakerWallDividerWidth/2,0.1);
 };

module micWall()
{
  difference()
  {
      union()
      {
    translate([0,0,0])
      cylinder(3.2,4,4);
    translate([0,0,3.2])
      cylinder(6.4,4,rTipHole);
    }
    micHousing();
    tipHole();
  }        
};

// Blocks to close 2 speaker's hollow inner channel connection to microphone housing.
module speakerFiller()
{
    difference()
    {
    linear_extrude(speakerFillerH+10)
      circle(r=r2t);
        
    linear_extrude(speakerFillerH+10)
      polygon(
        [[0,0],
        [2*r2t*sin(120),2*r2t*cos(120)],
        [2*r2t*sin(-120),2*r2t*cos(-120)]]);
    }
}

// the wall width at the top of the tip is 0.3*r2t=0.6 mm. is this too small? 
module tipHole()
{
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
