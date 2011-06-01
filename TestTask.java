package app.task;

import app.Err;
import app.Opts;
import java.util.logging.Level;
import java.util.logging.Logger;

import lib.tilers.RectangularTiler;
import lib.tilers.Tiler;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import lib.Comparison;
import lib.tilers.AdaptiveRectangularTiler;

import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.ImageCommand;
import org.im4java.process.ArrayListOutputConsumer;

/**
 * Test cases
 */
public class TestTask extends Task {

	private BufferedImage image;
	private File input, output;

	public TestTask(Properties properties) {
		super(properties);
		input = new File(properties.getProperty(Opts.INPUT.toString()));
		output = new File(properties.getProperty(Opts.OUTPUT.toString()));
		try {
			image = ImageIO.read(input);
		} catch (IOException ex) {
			System.err.println(Err.IMAGE_NOT_FOUND.description());
			System.exit(Err.IMAGE_NOT_FOUND.errcode());
		}
	}

	@Override
	public void run() {
		//testSplitImage();
		//testIm4javaResize();
		//testIm4javaConvertDiff();
		//testIm4javaCompare();
		//testRectTiler();
		//testAdaptRectTiler();
                testComparison();
	}

	/**
	 * Split an image to blocks
	 */
	public void testSplitImage() {
		int rows = 4;
		int cols = 4;
		int blockwidth = image.getWidth() / cols;
		int blockheight = image.getHeight() / rows;
		BufferedImage[][] blocks = new BufferedImage[rows][cols];
		for (int x = 0; x < rows; x++) {
			for (int y = 0; y < cols; y++) {
				blocks[x][y] = image.getSubimage(x * blockheight, y * blockwidth, blockwidth, blockheight);
				try {
					ImageIO.write(blocks[x][y], "PNG",
								  new File(String.format("%s/block_%d%d_%d%d.png",
														 output.getParent(),
														 rows, cols, x, y)));
				} catch (IOException ex) {
					System.err.printf("Couldn't write image: %d%d\n", x, y);
				}
			}
		}
	}

	/**
	 * Test the rectangular tiler.
	 * Write the tiled blocks.
	 */
	private void testRectTiler() {
		Tiler tiler = new RectangularTiler(5, 5);
		BufferedImage[] blocks = tiler.tile(image);
		System.out.println(blocks.length);
		for (int i = 0; i < blocks.length; i++) {
			try {
				ImageIO.write(blocks[i], "PNG", new File(String.format("%s/tile_block_%d.png", output.getParent(), i)));
			} catch (IOException ex) {
				System.err.printf("Couldn't write image: %d\n", i);
			}
		}
	}

	/**
	 * Test the adaptive rectangular tiler.
	 * Write the tiled blocks.
	 */
	private void testAdaptRectTiler() {
		Tiler tiler = new AdaptiveRectangularTiler(5, 6);
		BufferedImage[] blocks = tiler.tile(image);
		System.out.println(blocks.length);
		for (int i = 0; i < blocks.length; i++) {
			try {
				ImageIO.write(blocks[i], "PNG", new File(String.format("%s/tile_block_%d.png", output.getParent(), i)));
			} catch (IOException ex) {
				System.err.printf("Couldn't write image: %d\n", i);
			}
		}
	}
        
        /**
         * Test the Comparison of images.
         */
        private void testComparison() {
            Comparison comparison = new Comparison(Comparison.Metric.AE);
            comparison.setVerboseOn();
            comparison.compare(image, image);
        }

	/** *********************
	 * ImageMagick Bindings *
	 *                      *
	 * evaluate im4java lib *
	 * TODO: Pipe + BuffImg *
	 * **********************/
	/**
	 * Resize to half input image and store to output
	 */
	private void testIm4javaResize() {
		ImageCommand cmd = new ConvertCmd();
		IMOperation op = new IMOperation();
		op.resize(image.getHeight() / 2, image.getWidth() / 2);
		op.addImage(input.getPath());
		op.addImage(output.getParent() + "/half" + "_small.jpg");
		try {
			cmd.run(op);
		} catch (IOException ex) {
			System.err.printf("Couldn't run op: resize ioe\n");
		} catch (InterruptedException ex) {
			System.err.printf("Couldn't run op: resize ie\n");
		} catch (IM4JavaException ex) {
			System.err.printf("Couldn't run op: resize im4jve\n");
		}
	}

	/**
	 * search for similarity -- one of billions of ways.
	 *
	 * cli equivalent:
	 * $ convert img1.png img2.png -compose difference \
	 *	-composite -separate -background black -compose plus \
	 *	-flatten diff.png
	 *
	 */
	private void testIm4javaConvertDiff() {
		ImageCommand cmd = new ConvertCmd();
		IMOperation op = new IMOperation();
		op.addImage(output.getParent() + "/block_44_11.png");
		op.addImage(output.getParent() + "/output.png");
		op.compose("difference");
		op.composite();
		op.separate();
		op.background("black");
		op.compose("plus");
		op.flatten();
		op.addImage(output.getParent() + "/diff_00_01.png");
		try {
			ArrayListOutputConsumer oc = new ArrayListOutputConsumer();
			cmd.setOutputConsumer(oc);
			cmd.run(op);
			System.out.println("OC :: " + oc.getOutput());
			System.out.println("EC :: " + cmd.getErrorText().toString());
		} catch (IOException ex) {
			System.err.printf("Couldn't run op: convert ioe\n");
		} catch (InterruptedException ex) {
			System.err.printf("Couldn't run op: convert ie\n");
		} catch (IM4JavaException ex) {
			System.err.printf("Couldn't run op: convert im4jve\n");
		}
	}

	/**
	 * TODO: im4java compare + NCC metric + streams
	 * 
	 * cli equivalent:
	 * $ compare -metric NCC -fuzz 5% in1.jpg in2.jpg diff.jpg
	 */
	private void testIm4javaCompare() {
		ImageCommand cmd = new ConvertCmd();
		IMOperation op = new IMOperation();
		op.metric("NCC");
		try {
			cmd.run(op);
		} catch (IOException ex) {
			System.err.printf("Couldn't run op: compare ioe\n");
		} catch (InterruptedException ex) {
			System.err.printf("Couldn't run op: compare ie\n");
		} catch (IM4JavaException ex) {
			System.err.printf("Couldn't run op: compare im4jve\n");
		}
	}
}
