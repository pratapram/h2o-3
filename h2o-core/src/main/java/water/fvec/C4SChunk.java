package water.fvec;

import water.*;
import water.util.UnsafeUtils;

/**
 * The scale/bias function, where data is in SIGNED bytes before scaling.
 */
public class C4SChunk extends Chunk {
  static private final long _NA = Integer.MIN_VALUE;
  static protected final int _OFF=8+8;
  private transient double _scale;
  public double scale() { return _scale; }
  private transient long _bias;
  @Override public boolean hasFloat(){ return _scale != (long)_scale; }
  C4SChunk( byte[] bs, long bias, double scale ) {
    _mem=bs;
    _bias = bias; _scale = scale;
    UnsafeUtils.set8d(_mem,0,scale);
    UnsafeUtils.set8 (_mem,8,bias );
  }
  @Override
  public final long at8_impl(int i) {
    long res = UnsafeUtils.get4(_mem,(i<<2)+_OFF);
    if( res == _NA ) throw new IllegalArgumentException("at8_abs but value is missing");
    return (long)((res + _bias)*_scale);
  }
  @Override
  public final double atd_impl(int i) {
    long res = UnsafeUtils.get4(_mem,(i<<2)+_OFF);
    return (res == _NA)?Double.NaN:(res + _bias)*_scale;
  }
  @Override
  public final boolean isNA_impl(int i) { return UnsafeUtils.get4(_mem,(i<<2)+_OFF) == _NA; }
  @Override boolean set_impl(int idx, long l) {
    long res = (long)(l/_scale)-_bias; // Compressed value
    double d = (res+_bias)*_scale;     // Reverse it
    if( (long)d != l ) return false;   // Does not reverse cleanly?
    if( !(Integer.MIN_VALUE < res && res <= Integer.MAX_VALUE) ) return false; // Out-o-range for a int array
    UnsafeUtils.set4(_mem,(idx<<2)+_OFF,(int)res);
    return true;
  }
  @Override boolean set_impl(int i, double d) { return false; }
  @Override boolean set_impl(int i, float f ) { return false; }
  @Override boolean setNA_impl(int idx) { UnsafeUtils.set4(_mem,(idx<<2)+_OFF,(int)_NA); return true; }


//  public int pformat_len0() { return pformat_len0(_scale,5); }
//  public String pformat0() { return "% 10.4e"; }
  @Override public byte precision() { return (byte)Math.max(-Math.log10(_scale),0); }
  @Override public final void initFromBytes () {
    _scale= UnsafeUtils.get8d(_mem,0);
    _bias = UnsafeUtils.get8 (_mem,8);
  }

  @Override
  public int len() { return (_mem.length - _OFF) >> 2;}
  /**
   * Dense bulk interface, fetch values from the given range
   * @param vals
   * @param from
   * @param to
   */
  @Override
  public double [] getDoubles(double [] vals, int from, int to, double NA){
    for(int i = from; i < to; ++i) {
      long res = UnsafeUtils.get4(_mem,(i<<2)+_OFF);
      vals[i-from] = res != C4Chunk._NA?(res + _bias)*_scale:NA;
    }
    return vals;
  }
  /**
   * Dense bulk interface, fetch values from the given ids
   * @param vals
   * @param ids
   */
  @Override
  public double [] getDoubles(double [] vals, int [] ids){
    int j = 0;
    for(int i:ids) {
      long res = UnsafeUtils.get4(_mem,(i<<2)+_OFF);
      vals[j++] = res != C4Chunk._NA?(res + _bias)*_scale:Double.NaN;
    }
    return vals;
  }

  @Override
  public NewChunk add2NewChunk_impl(NewChunk nc, int from, int to) {
    double dx = Math.log10(_scale);
    assert water.util.PrettyPrint.fitsIntoInt(dx);
    for( int i=from; i<to; i++ ) {
      int res = UnsafeUtils.get4(_mem,(i<<2)+_OFF);
      if( res == _NA ) nc.addNA();
      else nc.addNum(res+_bias,(int)dx);
    }
    return nc;
  }

  @Override
  public NewChunk add2NewChunk_impl(NewChunk nc, int[] lines) {
    double dx = Math.log10(_scale);
    assert water.util.PrettyPrint.fitsIntoInt(dx);
    for( int i:lines) {
      int res = UnsafeUtils.get4(_mem,(i<<2)+_OFF);
      if( res == _NA ) nc.addNA();
      else nc.addNum(res+_bias,(int)dx);
    }
    return nc;
  }
}
