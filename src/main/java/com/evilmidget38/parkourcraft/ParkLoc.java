package com.evilmidget38.parkourcraft;

public class ParkLoc {
  int x;
  int y;
  int z;
  String w;

  public ParkLoc(String xx, String yy, String zz, String ww) {
    this.x = Integer.parseInt(xx.substring(xx.indexOf("X") + 1));
    this.y = Integer.parseInt(yy.substring(yy.indexOf("Y") + 1));
    this.z = Integer.parseInt(zz.substring(zz.indexOf("Z") + 1));
    this.w = ww.substring(ww.indexOf("W") + 1);
  }
  public String toString() {
    return "X: " + this.x + " Y: " + this.y + " Z: " + this.z + " world: " + this.w;
  }
  public int getX() {
    return this.x;
  }
  public void setX(int x) {
    this.x = x;
  }
  public int getY() {
    return this.y;
  }
  public void setY(int y) {
    this.y = y;
  }
  public int getZ() {
    return this.z;
  }
  public void setZ(int z) {
    this.z = z;
  }
  public String getWorld() {
    return this.w;
  }
  public void setWorld(String w) {
    this.w = w;
  }
}