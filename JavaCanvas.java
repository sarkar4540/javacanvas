/*

		JavaCanvas by Jumis, Inc

	Unless otherwise noted:
	All source code is hereby released into public domain and the CC0 license.
	http://creativecommons.org/publicdomain/zero/1.0/
	http://creativecommons.org/licenses/publicdomain/

	Based on Rhino Canvas by Stefan Haustein
	Lead development by Alex Padalka and Charles Pritchard
	with code review and support from Paul Wheaton


*/
package com.w3canvas.javacanvas.rt;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;

import javax.swing.JFrame;

import com.w3canvas.javacanvas.js.impl.event.JSMouseEvent;
import com.w3canvas.javacanvas.js.impl.gradient.LinearCanvasGradient;
import com.w3canvas.javacanvas.js.impl.gradient.RadialCanvasGradient;
import com.w3canvas.javacanvas.js.impl.node.CanvasPattern;
import com.w3canvas.javacanvas.js.impl.node.CanvasPixelArray;
import com.w3canvas.javacanvas.js.impl.node.CanvasRenderingContext2D;
import com.w3canvas.javacanvas.js.impl.node.Document;
import com.w3canvas.javacanvas.js.impl.node.HTMLCanvasElement;
import com.w3canvas.javacanvas.js.impl.node.Image;
import com.w3canvas.javacanvas.js.impl.node.ImageData;
import com.w3canvas.javacanvas.js.impl.node.Node;
import com.w3canvas.javacanvas.js.impl.node.StyleHolder;
import com.w3canvas.javacanvas.js.impl.node.TextMetrics;
import com.w3canvas.javacanvas.js.impl.node.Window;
import com.w3canvas.javacanvas.utils.PropertiesHolder;
import com.w3canvas.javacanvas.utils.RhinoCanvasUtils;
import com.w3canvas.javacanvas.utils.ScriptLogger;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

@SuppressWarnings("serial")
public class JavaCanvas extends JFrame
{

    private RhinoRuntime runtime;
    private String basePath;
//    private boolean isInitialized = false;

    private JavaCanvas(String title, String resourcePath)
    {
        super(title);
        basePath = resourcePath;

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(screenSize.width, screenSize.height - 100);
    }

    private static class DemoWindowAdapter extends WindowAdapter
    {
        private final JavaCanvas canvas;

        private DemoWindowAdapter(JavaCanvas canvas)
        {
            this.canvas = canvas;
        }

        public void windowClosing(WindowEvent e)
        {
            Window.getInstance().callCloseFunction();
            Context context = Context.getCurrentContext();
            if (context != null)
            {
                Context.exit();
            }
            System.exit(0);
        }

        public void windowOpened(WindowEvent e)
        {
            canvas.init();
            Window.getInstance().callLoadFunction();
        }
    }


    private void init()
    {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(screenSize.width - 200, screenSize.height - 100);
        setLocation(200,0);

        runtime = new RhinoRuntime();

        try
        {
            ScriptableObject.defineClass(runtime.getScope(), Document.class, false, true);
            Document document = RhinoCanvasUtils.getScriptableInstance(Document.class, null);
            document.initInstance(this);
            runtime.defineProperty("document", document);

            ScriptableObject.defineClass(runtime.getScope(), Window.class, false, true);
            Window window = RhinoCanvasUtils.getScriptableInstance(Window.class, null);
            window.initInstance(getWidth(), getHeight());
            window.setDocument(document);
            runtime.defineProperty("window", window);

            ScriptableObject.defineClass(runtime.getScope(), Image.class, false, true);
            ScriptableObject.defineClass(runtime.getScope(), HTMLCanvasElement.class, false, true);

            ScriptableObject.defineClass(runtime.getScope(), CanvasRenderingContext2D.class, false, true);
            ScriptableObject.defineClass(runtime.getScope(), LinearCanvasGradient.class, false, true);
            ScriptableObject.defineClass(runtime.getScope(), RadialCanvasGradient.class, false, true);
            ScriptableObject.defineClass(runtime.getScope(), CanvasPattern.class, false, true);
            ScriptableObject.defineClass(runtime.getScope(), TextMetrics.class, false, true);
            ScriptableObject.defineClass(runtime.getScope(), CanvasPixelArray.class, false, true);
            ScriptableObject.defineClass(runtime.getScope(), ImageData.class, false, true);
            ScriptableObject.defineClass(runtime.getScope(), StyleHolder.class, false, true);
            ScriptableObject.defineClass(runtime.getScope(), JSMouseEvent.class, false, true);

            runtime.defineProperty("log", new ScriptLogger());

            runtime.setSource(basePath);
            executeJSCode();

//			StringBuffer sb = new StringBuffer();
//			readContent(sb);
//			runtime.exec(sb.toString());
//            isInitialized = true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void executeJSCode() throws InstantiationException, IllegalAccessException, ClassNotFoundException
    {
        Properties properties = PropertiesHolder.getInstance().getProperties();
        List<String> jsClasses = PropertiesHolder.getJSClasses(properties);

        for (String className : jsClasses)
        {
            Object instance = Class.forName(className).newInstance();
            if (instance instanceof Script)
            {
                ((Script) instance).exec(Context.getCurrentContext(), runtime.getScope());
            }
        }
    }

    private static class CanvasMouseListener extends MouseAdapter
    {
    	protected Node getDestinationNode(JSMouseEvent mouseEvent) {
        	return Document.getInstance().getEventDestination(new Point(mouseEvent.jsGet_clientX(), mouseEvent.jsGet_clientY()));
    	}

        public void mouseClicked(MouseEvent e)
        {
        	JSMouseEvent mouseEvent = JSMouseEvent.convert(e);
        	Node dstNode = getDestinationNode(mouseEvent);
            if (e.getClickCount() == 2) {
            	dstNode.callDoubleclickFunction(mouseEvent);
            } else if (e.getClickCount() == 1) {
            	dstNode.callClickFunction(mouseEvent);
            }
        }

        public void mousePressed(MouseEvent e)
        {
        	JSMouseEvent mouseEvent = JSMouseEvent.convert(e);
        	Node dstNode = getDestinationNode(mouseEvent);
        	dstNode.callMousedownFunction(mouseEvent);
        }

        public void mouseReleased(MouseEvent e)
        {
        	JSMouseEvent mouseEvent = JSMouseEvent.convert(e);
        	Node dstNode = getDestinationNode(mouseEvent);
        	dstNode.callMouseupFunction(JSMouseEvent.convert(e));
        }

    }

    private static class CanvasMouseMotionListener extends MouseAdapter
    {
        public void mouseMoved(MouseEvent e)
        {
                Document.getInstance().callMousemoveFunction(JSMouseEvent.convert(e));
        }
    }

    private static class CanvasComponentListener extends ComponentAdapter
    {
        private Container contentPane;

        private CanvasComponentListener(Container contentPane)
        {
            this.contentPane = contentPane;
        }

        public void componentResized(ComponentEvent e)
        {
            super.componentResized(e);
            Window w = Window.getInstance();
            if (w != null) {
                w.setSize(contentPane.getWidth(), contentPane.getHeight());
                w.callResizeFunction();
            }
        }

    }

    public static void main(String[] args)
    {
        PropertiesHolder pHolder = PropertiesHolder.getInstance();
        pHolder.processCommandLineParams(args);
        JavaCanvas canvas = new JavaCanvas(PropertiesHolder.getInstance().getAppTitle(), pHolder.getBaseDir());
        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvas.addWindowListener(new DemoWindowAdapter(canvas));
        Container contentPane = canvas.getContentPane();
        contentPane.addMouseListener(new CanvasMouseListener());
        contentPane.addMouseMotionListener(new CanvasMouseMotionListener());
        contentPane.addComponentListener(new CanvasComponentListener(contentPane));
        canvas.setVisible(true);
    }

	protected void readContent(StringBuffer sb) throws FileNotFoundException, IOException {
		String[] sources = new String[] { "Global.js", "Canvas.js", "Event.js", "Math.js", "Project.js",
				"Project.X_Slide.js", "Raster2D.js", "Raster2D.CLUT.js", "Raster2D.ColorMatrix.js", "Raster2D.Crop.js",
				"Raster2D.GeoMatrix.js", "Raster2D.Histogram.js", "Raster2D.Levels.js", "Vector2D.Font.js",
				"Vector2D.Font.Arial.js", "Vector2D.SVG.js", "Scriptograph.js" };

		int i = 1;
//		for (String itemSource : sources) {
			// if (i > 1)
			// return;
			BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(
					"E:/dev/prj/JavaCanvas/trunk/src/js/development/test.js")));
//			String scriptLocation = "E:/dev/prj/JavaCanvas/trunk/src/js/";//development\";
//			BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(scriptLocation + itemSource)));

			while (true) {
				String line = r.readLine();

				// if (i == 3783) {
				// System.out.println(line);
				// }

				if (line == null) {
					break;
				}

				sb.append(line).append('\n');
				i++;
			}
			r.close();
//		}
	}

}
