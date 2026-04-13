////use $fn=1000 for manufacturing. 
// rendering with 1K facet number is taking 8 minutes
$fn=20; 

// mic dimentions
    hHousing=2.6;
    hBottomBox=hHousing*4;
    hMicSpace=3;
    rCable=0.7;
    rBendCable=3;
    bottomL=23.9;
    bottomW=7.8;
    topL=21.8;
    topW=5;
    dw=3; // wall width at W
    dl=2; // wall width at L
    
    speakerAngle=120;

// tip dimensions
    ht=20;        // hight of tip from top of main housing
    r1t=2.7;      // bottom radius of the tip
    r2t=2;      // top radious of the tip
    rTipHole=r2t*0.7; // radious of tip hole
//tips();
//backPlate();
difference()
{
union()
{
drawWithCut(30, 30, 30, 0, cube=false);
//drawWithCut(-20,50.25,0,0);

//drawAll();
//scale([0.97,0.93,1])bottom();
}
translate([0,0,-246])
cube(500,center=true);
}
//tips();
//mic();
//micHousing();
//micOuter();
//trim2(0,5,6,10);
 module bottom()
 {
     dy=9;
     dx=31;
     bh=6.4;
     translate([dx+5,0,0])
   difference()
   {
     union()
     {
     translate([-dx/2,-dy/2,0])
     cube([dx,dy,bh]);
     translate([9.5,rCable,bh])
     cableBend();
     }
     
    translate([(topL/2-8),0,0])
     drawAll();

     //translate([-8-2,-dy/2-1,5])
       //cube([11,dy+2,3]);
   }
         
 }

module torus(r1=10.2,r2=2.2)
{
  rotate_extrude()
    translate([(r1-r2)/2, 0, 0])
        circle(r = r2/2);
}

module drawAll()
{
    difference()
    {
    union()
    {
      mic();
      translate([0,0,hHousing+hBottomBox+hMicSpace])
      tips();
    }
    translate([0,0,hHousing+hBottomBox+hMicSpace])
    tipHoles();
    }
    
    tipWall();
}

module tips()
{
  tip();
}

module tipHoles()
{
    dh1=4;
    st0=0.6;

    h0=ht+(dh1)*sin(speakerAngle);
      // re-drill both holes
      translate([0,0,-0.1])
      cylinder(h=ht+2,r=rTipHole);

      translate([0,0,r2t*sin(speakerAngle)])
      //rotate([speakerAngle,0,0])
      //cylinder(h=h0,r=rTipHole+0.2);      
    translate([0,-rTipHole-0.8,-rTipHole])
    rotate([0,90,0])
    cylinder(h=h0,r=1.4);
    translate([0,-2.1,0])
    cube([1.5,2.1,2.2]);   
}

module tipWall()
{
    translate([0,0,ht/2+hHousing+hBottomBox+hMicSpace])
    cube([(r2t+rTipHole),0.5,ht+0.6],center=true);
}

module mic()
{
    translate([-(topL/2-8),0,0])
    difference()
    {
        micOuter();
        micHousing();
      snipPins();
    }
}

module micOuter()
{
    rTrim=6;
    l=bottomL+2*rBendCable+dl*2; // 1.5mm wall width from cable bend
    w=bottomW+dw*2; // 3mm wall width
    translate([-l/2,-w/2,0])
    difference()
    {
        union()
        {
        cube([l,w,hHousing+hBottomBox+hMicSpace+2*r2t]);
        
        // upper cube to connect tips
    translate([(topL/2-8)+topL/2+2*r2t+r2t,w/2,hHousing+hBottomBox+hMicSpace])
    {
          difference()
          {
            translate([-r2t-1,-w/2,0])
              cube([2*r2t+2,w,2*r2t]);
            cylinder(h=2*r2t,r=r2t);          
          }
      }  
  }      
      trim2(0,rTrim,l,w,hHousing+hBottomBox+hMicSpace);
        trim2(-90,rTrim,l,2*rTrim,hHousing+hBottomBox+hMicSpace);
        trim2(90,rTrim,2*rTrim,w,hHousing+hBottomBox+hMicSpace);
        trim2(180,rTrim,2*rTrim,2*rTrim,hHousing+hBottomBox+hMicSpace);
  
    translate([l/2+2,w/2,4])
    scale([1,0.5,1])
        difference()
        {
            cylinder(
                h=6*hHousing+hBottomBox+hMicSpace,
                r1=1.2*bottomL,
                r2=bottomL*22);
            cylinder(
                h=3*hHousing+hBottomBox+hMicSpace,
                r1=1.2*bottomL/1.1,
                r2=5);
        }
    }
}

module trim2(teta,r,l,w,h)
{
    translate([l-r,w-r,0])
    rotate([0,0,teta])
    {
    difference()
    {
     cube([r+0.01,r+0.01,h]);
     cylinder(h=h,r=r);
    }
    }
}

module micHousing()
{
    // bottom large box
    translate([-bottomL/2,-bottomW/2,0])
    cube([bottomL,bottomW,hBottomBox]);

    // top part for the mic
    translate([0,0,hBottomBox])
    cube2(hHousing,bottomL,bottomW,topL,topW);
    
    // space
    translate([topL/2-8,0,hBottomBox+hHousing-0.01])
    difference()
  {
    scale([1,0.9,1])
    cylinder(h=hMicSpace,r1=2.5,r2=1.45);
        
    translate([-10,-0.7,0])
    union()
    {
    translate([0,-20,0])
    cube(20);
    difference()
    {
    cube(20);
    rotate([-20,0,0])
    cube(30);
    }
    }
  }
    
    // right cable bend
    translate([-bottomL/2,rCable,hBottomBox+rCable])
    cableBend();

    // left cable bend
    translate([bottomL/2,rCable,hBottomBox+rCable])
    cableBend();
}

module cableBend()
{
    r0=rCable*2+0.2;
    h0=(hBottomBox+4*rCable)+0.1;
    translate([-rBendCable,-r0,-h0])
    cube([2*rBendCable+4,r0,h0]);
}

module cube2(h,bottomL,bottomW,topL,topW)
{
    w=bottomW;
    dw=(bottomW-topW)/2;
    l=bottomL;
    dl=(bottomL-topL)/2;
    translate([-l/2,-w/2,0])
    polyhedron(
       points= [
        [0,0,0],
        [0,w,0],
        [l,w,0],
        [l,0,0],
        [dl,dw,h],
        [dl,w-dw,h],
        [l-dl,w-dw,h],
        [l-dl,dw,h]],
      faces=[
          [0,1,2,3],  // bottom
          [4,5,1,0],  // front
          [7,6,5,4],  // top
          [5,6,2,1],  // right
          [6,7,3,2],  // back
          [7,4,0,3]] // left
        );
}

module tip()
{
  cylinder(h=ht,r1=r2t*1.4,r2=r2t);
  translate([0,0,ht-7])
    cylinder(h=7,r1=r1t,r2=r2t);
}

module drawWithCut(x=0,y=0,z=0,t=0,cube=false)
{
    difference()
    {
        drawAll();
        drawCubeToSubstract(x,y,z);
   }
   if (cube)
   {    
       drawCubeToSubstract(x,y,z);
   }
}

module drawCubeToSubstract(x=0,y=0,z=0)
{
    translate([x, y,z])
    translate([-50,-50,0])
    translate([0, 0,0])
    cube([100, 50, 50]);
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

module snipPins(lowerHead=1)
{
  /*  
    sx=0;
    sy=0;

    snipPin(sx,sy, lowerHead);
    */
}


module snipPin(x=0,y=0,lowerHead=1)
{
    y0=-bottomW/2-dw+1;
    // M3 screw, 5mm
    translate([x,y0,3])
    rotate([-90,0,0])
    cylinder(h=20,r1=1.1,r2=1.1);
    
    translate([x,y0-2,3])
    rotate([-90,0,0])
    cylinder(h=2,r1=2,r2=2);

    // head space at the bottom
    if(lowerHead==1)
    {
    // one edge is 4mm.
    translate([x,-y0,3])
    rotate([-90,0,0])
    cylinder(h=1,r=2.31,$fn=6);
    //linear_extrude(20)    circle(2.31,$fn=6);
    }        
}

module backPlate()
{
    t0=3.5*hHousing;
    t1=1.5;
    h0=3;
    
    difference()
    {
      micOuter();
      translate([0,0,100+t0])
      cube([200,200,200],center=true);
      translate([(topL/2-8),0,t1])
        drawAll();
        
    translate([0,0,t1])
        snipPins();
      
    // top part for the mic
    translate([0,0,t0+0.01])
    rotate([180,0,0])
    cube2(hHousing,bottomL,bottomW,topL,topW);

      //remove space for cable
      translate([rCable-bottomL/2-rBendCable,-rCable,t1])
      cube([2*rCable,2*rCable,6]);
    }
}

