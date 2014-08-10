package jp.qualitas.nyartoolkit.webcam.utils;

import jp.nyatla.nyartoolkit.core.NyARException;
import jp.nyatla.nyartoolkit.core.types.NyARBufferType;
import jp.nyatla.nyartoolkit.utils.j2se.NyARBufferedImageRaster;

public class WebCamNyARRGBRaster extends NyARBufferedImageRaster {

	public WebCamNyARRGBRaster(int i_width,int i_height) throws NyARException {
		super(i_width, i_height, NyARBufferType.BYTE1D_R8G8B8_24, false);
	}

}
