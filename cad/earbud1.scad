//use $fn=1000 for manufacturing. 
// rendering with 1K facet number is taking 8 minutes
$fn=20; 

sd=13/2 + 1;  // top of main housing:.7.5mm radius => 1 mm length end to end
ld=29/2 + 1;  // base of main housing: 15.5 mm radius => 31mm length end to end
h0=17;        // hight of main housing
ht=12;        // hight of tip from top of main housing
r1t=2.7;      // bottom radius of the tip
r2t=2;      // top radious of the tip

// make sure this fuses with tip of speaker without sharp edges.
speakerFillerH=h0*0.4956; // connection point of speaker to tip's hollow channels

//speaker(1);
//speaker(-1);
//speaker2();
draw2();
//drawWithCut();
//drawInner();
// Change translate params to see inner parts.
module drawWithCut()
{
    difference()
    {
        draw2();
        translate([-50, 0, 0])
            //translate([8,0,0])
            cube([100, 50, 50]);
    }
}

module speaker1(n=1)
{
    // Speaker ref: https://www.knowles.com/docs/default-source/model-downloads/tec-30033-000.pdf
    // Dimensions: main: 7.87x4.09x2.79 box. with a cylinder tip of:1.60 hight and radius slightly less than 1.14. Top cylinder is on the edge, 
    //     we will model as:
    //     lower body for main box: main +0.3mm
    //     upper body for tip:      h:2mm x and y are -0.3mm
    mic1Tolerance = 0.6;
    mic1H = 7.87;
    mic1L = 4.09 + mic1Tolerance;
    mic1W = 2.79 + mic1Tolerance;

    //translate([-mic1L/2,-mic1W/2+0.5,0])
    union()
    {
      translate([-0.9+1.9*(n-1)/2,-2.7-3.6*(n-1)/2,0])
        rotate([0,0,45*n])
        cube([mic1L, mic1W, mic1H + 1]);
      translate([0,0,mic1H])
         cylinder(h = 2.5, r1 = 1.14, r2 = 1.14);

        // Add cable routing. distance between cables is 1.94. add 2.5 mm routing space
    translate([-4*n+0.2*(1-n)/2, 2+0.1*(1-n), 0])
    rotate([0,0,45])
      cube([4-(n+1)*0.8, 2.4+(n+1)*0.8, 1.2]);
}
}

module speaker2()
{      
 // add 10mm diameter housing for round speakers. so that this housing can hold both round and rectangular type speakers
  cylinder(h = 2.7, r1 = 5.1, r2 = 5.1);
}

module speaker(n= 1)
{
    displacement = 8 * n;
    //intersection()
    union()
  {
    translate([8*n, 0, 0])
      speaker1(n);
    
    translate([displacement, 0, speakerFillerH + 0.8])
    rotate([0, -90 *n+55*n, -2*n])
      cylinder(12, r1 = 1.6, r2 = 0.7);
    
      translate([8*n,1,0])  
    speaker2();
    }
}
;

// Draws inner parts of the assembly. Some 'removed' parts are drawn solid to demonstrate how it works.
module drawInner()
{
    difference(){
    tipHole();
    tipInner();
    }
//tipWall(0);
tipWall(1);
tipWall(2);
speakerFiller(0);
speakerFiller(1);
micHousing();
    speaker(-1);
    speaker(1);
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
speakerFiller(0);
speakerFiller(1);
}
micHousing();
    speaker(-1);
    speaker(1);
}
}

module draw2()
{
difference() {
    drawMain_Tubes_MicTubeAllTheWay_SpeakerTubeHalfWay();
    micHousing();
    speaker(-1);
    speaker(1);
    cableSpace();
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
speakerFiller(0);
speakerFiller(1);
}
}

module cableSpace()
{
    translate([0,6,0])
    rotate([8,0,0])
    scale([1,0.5,2])
       sphere(r=4.6);
}
  
module mainBlock()
{
 union()
 {
    scale([1,0.6,1]) 
    cylinder(h=h0, r1=ld, r2=sd); 
    
    translate([0,0,h0])
    scale([1,0.6,1]) 
    cylinder(h=ht/5, r1=sd, r2=r2t); 

    translate([0,0,h0])
        cylinder(h=ht, r1=r1t, r2=r2t); 
    translate([0,0,h0+ht/2])
        cylinder(h=ht/2, r1=r1t*1.1, r2=r2t);
}
};

module micHousing()
{
  translate([0,-3,0])
    {
  // 2.7mm height, 6mm diameter    
  cylinder(2.7,3,3);

  // chamber with slightly more hight
  cylinder(3.7,2.5,2.5);
    
  // cable route
  translate([-1.2,0,0])
  cube([2.4,8,1.2]);
    }
};

// Blocks to close 2 speaker's hollow inner channel connection to microphone housing.
module speakerFiller(n=0)
{
    hf=10;
union()
    {
    rotate(n*120)
    cube([r2t,r2t,speakerFillerH+hf]);
    rotate(n*120-30)
    cube([r2t,r2t,speakerFillerH+hf]);
    }
};


// the wall width at the top of the tip is 0.3*r2t=0.6 mm. is this too small? 
module tipHole()
{
  cylinder(h=h0+ht+1.5,r1=r2t*0.7,r2=r2t*0.7);
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
