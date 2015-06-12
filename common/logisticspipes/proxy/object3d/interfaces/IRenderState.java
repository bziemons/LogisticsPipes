package logisticspipes.proxy.object3d.interfaces;

public interface IRenderState {

	void reset();

	void setUseNormals(boolean b);

	void setAlphaOverride(int i);

	void draw();

	void setBrightness(int brightness);

	void startDrawing();

}
