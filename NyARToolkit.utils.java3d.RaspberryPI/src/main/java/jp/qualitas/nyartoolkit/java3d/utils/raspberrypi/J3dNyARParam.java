package jp.qualitas.nyartoolkit.java3d.utils.raspberrypi;

import java.io.InputStream;

import jp.nyatla.nyartoolkit.core.NyARException;
import jp.nyatla.nyartoolkit.core.param.INyARCameraDistortionFactor;
import jp.nyatla.nyartoolkit.core.param.NyARParam;
import jp.nyatla.nyartoolkit.core.param.NyARPerspectiveProjectionMatrix;
import jp.nyatla.nyartoolkit.core.types.NyARIntSize;
import jp.nyatla.nyartoolkit.core.types.matrix.NyARDoubleMatrix44;

import javax.media.j3d.Transform3D;
/**
 * NyARParamにJava3D向け関数を追加したもの
 */
public class J3dNyARParam extends NyARParam
{
	private double view_distance_min = 0.01;//1cm～10.0m
	private double view_distance_max = 10.0;
	private Transform3D m_projection = null;
	/**
	 * テストパラメータを格納したインスタンスを生成する。
	 * @return
	 * @throws NyARException 
	 */
	public static J3dNyARParam loadDefaultParameter() throws NyARException
	{
		ParamLoader pm=new ParamLoader();
		return new J3dNyARParam(pm.size,pm.pmat,pm.dist_factor);
	}
	/**
	 * i_streamからARToolkitのカメラパラメータを読み出して、インスタンスに格納して返します。
	 * @param i_stream
	 * @return
	 * @throws NyARException
	 */
	public static J3dNyARParam loadARParamFile(InputStream i_stream) throws NyARException
	{
		ParamLoader pm=new ParamLoader(i_stream);
		return new J3dNyARParam(pm.size,pm.pmat,pm.dist_factor);
	}
	public static J3dNyARParam createFromCvCalibrateCamera2Result(int i_w,int i_h,double[] i_intrinsic_matrix,double[] i_distortion_coeffs)
	{
		ParamLoader pm=new ParamLoader(i_w,i_h,i_intrinsic_matrix,i_distortion_coeffs);
		return new J3dNyARParam(pm.size,pm.pmat,pm.dist_factor);
	}
	
	public J3dNyARParam(NyARIntSize i_screen_size,NyARPerspectiveProjectionMatrix i_projection_mat,INyARCameraDistortionFactor i_dist_factor)
	{
		super(i_screen_size,i_projection_mat,i_dist_factor);
	}	

	/**
	 * 視体積の近い方をメートルで指定
	 * @param i_new_value
	 */
	public void setViewDistanceMin(double i_new_value)
	{
		m_projection = null;//キャッシュ済変数初期化
		view_distance_min = i_new_value;
	}

	/**
	 * 視体積の遠い方をメートルで指定
	 * @param i_new_value
	 */
	public void setViewDistanceMax(double i_new_value)
	{
		m_projection = null;//キャッシュ済変数初期化
		view_distance_max = i_new_value;
	}

	/**
	 * void arglCameraFrustumRH(const ARParam *cparam, const double focalmin, const double focalmax, GLdouble m_projection[16])
	 * 関数の置き換え
	 * @param focalmin
	 * @param focalmax
	 * @return
	 */
	public Transform3D getCameraTransform()
	{
		//既に値がキャッシュされていたらそれを使う
		if (m_projection != null) {
			return m_projection;
		}		
		//無ければ計算
		NyARDoubleMatrix44 tmp=new NyARDoubleMatrix44();
		this.makeCameraFrustumRH(view_distance_min, view_distance_max,tmp);
		this.m_projection =new Transform3D(new double[]{
			tmp.m00,tmp.m01,tmp.m02,tmp.m03,
			tmp.m10,tmp.m11,tmp.m12,tmp.m13,
			-tmp.m20,-tmp.m21,-tmp.m22,-tmp.m23,
			tmp.m30,tmp.m31,tmp.m32,tmp.m33
			});

		return m_projection;
	}
}
