package com.rareventure.gps2.reviewer.map;
import android.content.Context;
import com.mapzen.tangram.LngLat;

public class MapDistanceScaleRegression {
 private static int failures;
 private static void near(double actual,double expected,String message) {
  if(!Double.isFinite(actual) || Math.abs(actual-expected)>0.0001) {
   failures++; System.err.println("FAIL: "+message+" expected="+expected+" actual="+actual);
  }
 }
 public static void main(String[] args) throws Exception {
  OsmMapView map=new OsmMapView(new Context(),null); map.windowWidth=1000;
  java.lang.reflect.Field sizeField=OsmMapView.class.getDeclaredField("screenSize"); sizeField.setAccessible(true);
  LngLat size=(LngLat)sizeField.get(map); size.longitude=1; size.latitude=20;
  MapScaleWidget widget=new MapScaleWidget(); map.setScaleWidget(widget);
  // Independent ground-distance fixtures: 1 degree = 111131.75m at equator;
  // east-west arc at 60 degrees is half as long, at 45 it is sqrt(1/2).
  double[][] fixtures={{0,111.13175},{45,78.582014},{60,55.565875},{-60,55.565875}};
  for(double[] fixture:fixtures) {
   double lat=fixture[0],metersPerPixel=fixture[1];
   map.getScreenTopLeft().latitude=lat+10;
   near(map.metersToPixels(),metersPerPixel,"meters per pixel at center latitude "+lat);
   map.onLayout(true,0,0,1000,1000);
   near(widget.pixelsPerMeter,1/metersPerPixel,"scale widget receives pixels per meter at "+lat);
  }
  size.longitude=2; map.windowWidth=2000;
  near(map.metersToPixels(),55.565875,"longitude span and viewport width cancel proportionally");
  if(failures>0) throw new AssertionError(failures+" scale regression assertions failed");
  System.out.println("PASS: actual OsmMapView distance and scale-widget conversion at 0, 45, +/-60 degrees");
 }
}
