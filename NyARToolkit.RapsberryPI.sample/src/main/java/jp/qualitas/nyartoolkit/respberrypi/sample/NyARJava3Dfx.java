package jp.qualitas.nyartoolkit.respberrypi.sample;

import java.io.IOException;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Locale;
import javax.media.j3d.Node;
import javax.media.j3d.PhysicalBody;
import javax.media.j3d.PhysicalEnvironment;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.media.j3d.ViewPlatform;
import javax.media.j3d.VirtualUniverse;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import jp.nyatla.nyartoolkit.core.NyARCode;
import jp.nyatla.nyartoolkit.core.NyARException;
import jp.qualitas.nyartoolkit.java3d.utils.raspberrypi.J3dNyARParam;
import jp.qualitas.nyartoolkit.java3d.utils.raspberrypi.NyARSingleMarkerBehaviorHolder;
import jp.qualitas.nyartoolkit.java3d.utils.raspberrypi.NyARSingleMarkerBehaviorListener;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.ds.fswebcam.FsWebcamDriver;
import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.universe.SimpleUniverse;

public class NyARJava3Dfx extends Application implements
		NyARSingleMarkerBehaviorListener {
	// set capture driver for fswebcam tool
	static {
		String osName = System.getProperty("os.name");
		String arch = System.getProperty("os.arch");
		if (osName.equals("Linux") & arch.equals("arm")) {
			Webcam.setDriver(new FsWebcamDriver());
		}
	}
	private final String CARCODE_FILE = "/data/patt.hiro";

	private final String PARAM_FILE = "/data/camera_para4.dat";

	// NyARToolkit関係
	private NyARSingleMarkerBehaviorHolder nya_behavior;

	private J3dNyARParam ar_param;

	// universe関係
	private Canvas3D canvas;

	private Locale locale;

	private VirtualUniverse universe;

	private Stage stage;

	private Group root;

	public void onUpdate(boolean i_is_marker_exist, Transform3D i_transform3d) {
		/*
		 * TODO:Please write your behavior operation code here.
		 * マーカーの姿勢を元に他の３Dオブジェクトを操作するときは、ここに処理を書きます。
		 */
		if (i_transform3d != null) {
			System.out.println("getScale()=" + i_transform3d.getScale());
	        
			Vector3d vector = new Vector3d();
			i_transform3d.get(vector);
			System.out.println("vector.length()=" + vector.length());
			System.out.println("vector.lengthSquared()=" + vector.lengthSquared());
			if (vector.lengthSquared() > 0.15) {
				System.out.println("Hit!!!");
			}
			System.out.println("vector.getX()=" + vector.getX());
			System.out.println("vector.getY()=" + vector.getY());
			System.out.println("vector.getZ()=" + vector.getZ());
			
			Matrix3d matrix = new Matrix3d();
			i_transform3d.get(matrix);
			System.out.println("M00" + matrix.getM00());			
		}

	}

	public void startCapture() throws Exception {
		// キャプチャ開始
		nya_behavior.start();

		// localeの作成とlocateとviewの設定
		universe = new VirtualUniverse();
		locale = new Locale(universe);
		canvas = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
		View view = new View();
		ViewPlatform viewPlatform = new ViewPlatform();
		view.attachViewPlatform(viewPlatform);
		view.addCanvas3D(canvas);
		view.setPhysicalBody(new PhysicalBody());
		view.setPhysicalEnvironment(new PhysicalEnvironment());

		// 視界の設定(カメラ設定から取得)
		Transform3D camera_3d = ar_param.getCameraTransform();
		view.setCompatibilityModeEnable(true);
		view.setProjectionPolicy(View.PERSPECTIVE_PROJECTION);
		view.setLeftProjection(camera_3d);

		// 視点設定(0,0,0から、Y軸を180度回転してZ+方向を向くようにする。)
		TransformGroup viewGroup = new TransformGroup();
		Transform3D viewTransform = new Transform3D();
		viewTransform.rotY(Math.PI);
		viewTransform.setTranslation(new Vector3d(0.0, 0.0, 0.0));
		viewGroup.setTransform(viewTransform);
		viewGroup.addChild(viewPlatform);
		BranchGroup viewRoot = new BranchGroup();
		viewRoot.addChild(viewGroup);
		locale.addBranchGraph(viewRoot);

		// バックグラウンドの作成
		Background background = new Background();
		BoundingSphere bounds = new BoundingSphere();
		bounds.setRadius(10.0);
		background.setApplicationBounds(bounds);
		background.setImageScaleMode(Background.SCALE_FIT_ALL);
		background.setCapability(Background.ALLOW_IMAGE_WRITE);
		BranchGroup root = new BranchGroup();
		root.addChild(background);

		// TransformGroupで囲ったシーングラフの作成
		TransformGroup transform = new TransformGroup();
		transform.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		transform.addChild(createSceneGraph());
		root.addChild(transform);

		// Behaviorに連動するグループをセット
		nya_behavior.setTransformGroup(transform);
		nya_behavior.setBackGround(background);

		// 出来たbehaviorをセット
		root.addChild(nya_behavior.getBehavior());
		nya_behavior.setUpdateListener(this);

		// 表示ブランチをLocateにセット
		locale.addBranchGraph(root);

		
		// ウインドウの設定
		//setLayout(new BorderLayout());
		//add(canvas, BorderLayout.CENTER);
	}
	@Override
	public void start(Stage stage) throws NyARException, IOException {
		this.stage = stage;
		this.showWindow();
		
		// //NyARToolkitの準備
		NyARCode ar_code = NyARCode.createFromARPattFile(this.getClass()
				.getResourceAsStream(CARCODE_FILE), 16, 16);
		ar_param = J3dNyARParam.loadARParamFile(this.getClass()
				.getResourceAsStream(PARAM_FILE));
		ar_param.changeScreenSize(320, 240);

		// NyARToolkitのBehaviorを作る。(マーカーサイズはメートルで指定すること)
		nya_behavior = new NyARSingleMarkerBehaviorHolder(ar_param, 30f,
				ar_code, 0.08);
		nya_behavior.setWebcapOpenListener(this);
		nya_behavior.open();
	}
	private void showWindow() throws IOException {
		Group root = new Group();

		Scene scene = new Scene(root, 640, 480);

		stage.setTitle("Translate Transition Demo");
		stage.setScene(scene);
		
		stage.show();
		//stage.setFullScreen(true);

		this.appendAnimation(root, "hiragana_01_a.png");
	}
	private void appendAnimation(final Group root, final String hiraganaImage) {
		final ImageView hiragana = new ImageView(new Image(hiraganaImage));
		hiragana.setScaleX(0.1);
		hiragana.setScaleY(0.1);

		Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

		System.out.println("primaryScreenBounds.getWidth():"
				+ primaryScreenBounds.getWidth());
		hiragana.setLayoutX(primaryScreenBounds.getWidth() / 2
				- hiragana.getLayoutBounds().getWidth() / 2);
		hiragana.setLayoutY(primaryScreenBounds.getHeight() / 2
				- hiragana.getLayoutBounds().getHeight() / 2);

		root.getChildren().add(hiragana);

		// 移動を行なうアニメーション
		// TranslateTransition transition = new TranslateTransition();
		// // アニメーション対象の設定
		// transition.setNode(hiragana);
		// // アニメーションの時間は4000ミリ秒
		// transition.setDuration(Duration.millis(4_000L + y));
		// // 開始位置の設定
		// transition.setFromX(0.0);
		// // 終了位置の設定
		// transition.setToX(300.0);
		// // 繰り返し回数の設定
		// transition.setCycleCount(2);
		// // アニメーションを反転させる
		// transition.setAutoReverse(true);

		// System.out.println(transition.getInterpolator());

		ScaleTransition scale = new ScaleTransition(Duration.millis(4_000),
				hiragana);
		scale.setFromX(0.1);
		scale.setFromY(0.1);
		scale.setToX(2.0);
		scale.setToY(2.0);

		FadeTransition fade = new FadeTransition(Duration.millis(3000),
				hiragana);
		fade.setFromValue(0.1);
		fade.setToValue(1.0);

		RotateTransition rotate = new RotateTransition(Duration.millis(4_000),
				hiragana);
		rotate.setFromAngle(0.0);
		rotate.setToAngle(1440.0);

		ParallelTransition transition = new ParallelTransition(scale, fade);
		transition.setCycleCount(4);
		transition.setAutoReverse(false);

		transition.setOnFinished(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				System.out.println("on Animation Finished");
				root.getChildren().remove(hiragana);
			}
		});

		transition.play();
	}
	/**
	 * シーングラフを作って、そのノードを返す。 このノードは40mmの色つき立方体を表示するシーン。ｚ軸を基準に20mm上に浮かせてる。
	 * 
	 * @return
	 */
	private Node createSceneGraph() {
		TransformGroup tg = new TransformGroup();
		Transform3D mt = new Transform3D();
		mt.setTranslation(new Vector3d(0.00, 0.0, 20 * 0.001));
		// 大きさ 40mmの色付き立方体を、Z軸上で20mm動かして配置）
		tg.setTransform(mt);
		tg.addChild(new ColorCube(20 * 0.001));
		return tg;
	}

	public void onWebcamOpen() {
		try {
			this.startCapture();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
	public static void main(String[] args) {
		launch(args);
	}
}
