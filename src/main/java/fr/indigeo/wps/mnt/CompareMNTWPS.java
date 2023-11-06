package fr.indigeo.wps.mnt;

import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.image.io.ImageIOExt;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.factory.StaticMethodsProcessFactory;
import org.geotools.process.vector.VectorToRasterProcess;
import org.geotools.text.Text;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.core.io.ClassPathResource;

import com.thoughtworks.xstream.core.util.Base64Encoder;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.nio.file.Files;

/**
 * @author JDev https://jdev.fr
 *
 */
@DescribeProcess(title = "Compare eleveation beetween two MNTS", description = "WPS tracking MNT differences")
public class CompareMNTWPS extends StaticMethodsProcessFactory<CompareMNTWPS> implements GeoServerProcess {

	private static final Logger LOGGER = Logger.getLogger(CompareMNTWPS.class);

	private static final String INPUT_TIFF_PATH = "/data/MADDOG/imagemosaic/mnt/";
	private static final String OUTPUT_COMPARE_PATH = "/data/MADDOG/compare/mnt/";

	public CompareMNTWPS() {
		super(Text.text("WPS to compare MNT"), "mnt", CompareMNTWPS.class);
	}

	/**
	 * @param mnt1 reference
	 * @param mnt2 to compare
	 * @return
	 */
	@DescribeProcess(title = "Compare MNT cloud point", description = "give Mnt point")
	@DescribeResult(name = "resultFeatureCollection", description = "A mnt cloud point diff")
	public static FeatureCollection<SimpleFeatureType, SimpleFeature> compareMNT(
			@DescribeParameter(name = "mnt1", description = "first mnt to compare") final FeatureCollection<SimpleFeatureType, SimpleFeature> mnt1,
			@DescribeParameter(name = "mnt2", description = "second mnt to compare") final FeatureCollection<SimpleFeatureType, SimpleFeature> mnt2) {

		DefaultFeatureCollection resultFeatureCollection = null;

		LOGGER.info("Compare mnt");

		// Init simple feature to add in collection
		SimpleFeatureTypeBuilder simpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder();
		simpleFeatureTypeBuilder.setName("featureType");
		simpleFeatureTypeBuilder.add("geometry", Point.class);
		simpleFeatureTypeBuilder.add("elevationDiff", Double.class);

		// init DefaultFeatureCollection
		SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(
				simpleFeatureTypeBuilder.buildFeatureType());
		resultFeatureCollection = new DefaultFeatureCollection(null, simpleFeatureBuilder.getFeatureType());

		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 2154);

		// For each point of MNT1 check nearest point on mnt2 and get elevation diff
		FeatureIterator<SimpleFeature> iteratorMNT1 = mnt1.features();

		// check if the FeatureCollection contains features
		int id = 0;
		while (iteratorMNT1.hasNext()) {

			SimpleFeature featureMNT1 = iteratorMNT1.next();
			Geometry PointMNT1 = (Geometry) featureMNT1.getDefaultGeometry();

			LOGGER.debug("Geometry type : " + PointMNT1.getClass());

			if (PointMNT1 instanceof Point) {
				final Point ptMNT1 = (Point) PointMNT1;
				final double pt1X = ptMNT1.getCoordinate().getX();
				final double pt1Y = ptMNT1.getCoordinate().getY();
				final double pt1Z = ptMNT1.getCoordinate().getZ();
				LOGGER.debug("Point référence X : " + ptMNT1.getCoordinate().getX() + " Y : "
						+ ptMNT1.getCoordinate().getY() + " Z : " + ptMNT1.getCoordinate().getZ());

				// For each point of MNT1 check nearest point on mnt2 and get elevation diff
				FeatureIterator<SimpleFeature> iteratorMNT2 = mnt2.features();
				Boolean pointFound = false;

				double referenceX = -1;
				double referenceY = -1;
				double elevationDiff = 0;

				while (iteratorMNT2.hasNext() && !pointFound) {
					SimpleFeature featureMNT2 = iteratorMNT2.next();
					Geometry PointMNT2 = (Geometry) featureMNT2.getDefaultGeometry();

					if (PointMNT1 instanceof Point) {
						final Point ptMNT2 = (Point) PointMNT2;
						final double pt2X = ptMNT2.getCoordinate().getX();
						final double pt2Y = ptMNT2.getCoordinate().getY();

						final double xDiff = Math.abs(pt1X - pt2X);
						final double yDiff = Math.abs(pt1Y - pt2Y);
						// If Nearest X and Nearest Y store Z
						if (referenceX == -1 || (xDiff <= referenceX && yDiff <= referenceY)) {
							LOGGER.debug("Nearest point for the moment X,Y : " + pt2X + "," + pt2Y);
							referenceX = Math.abs(xDiff);
							referenceY = Math.abs(yDiff);
							final double pt2Z = ptMNT2.getCoordinate().getZ();
							elevationDiff = pt1Z - pt2Z;
						}
						;
					}
				}
				// add id
				id = id++;
				iteratorMNT2.close();

				LOGGER.debug("Ajout point à la liste ");
				Coordinate coordinate = new Coordinate(pt1X, pt1Y, elevationDiff);
				Point point = geometryFactory.createPoint(coordinate);
				simpleFeatureBuilder.add(point);
				simpleFeatureBuilder.add(elevationDiff);
				resultFeatureCollection.add(simpleFeatureBuilder.buildFeature(Integer.toString(id)));
			}
		}
		iteratorMNT1.close();
		return resultFeatureCollection;
	}

	/**
	 * @param codeSite      reference
	 * @param initDate
	 * @param dateToCompare
	 * @return FeatureCollection
	 * @throws IOException
	 */
	@DescribeProcess(title = "Compare Raster mnt and get TIFF", description = "give diff raster mnt as TIFF")
	@DescribeResult(name = "rasterResultAsTIFF", description = "Raster with comparaison band")
	public static GridCoverage2D compareMNToTiff(
			@DescribeParameter(name = "codeSite", description = "id site on 6 char") final String codeSite,
			@DescribeParameter(name = "initDate", description = "first date") final String initDate,
			@DescribeParameter(name = "dateToCompare", description = "second date to compare") final String dateToCompare,
			@DescribeParameter(name = "evaluationInterval", description = "in meter beetween to point") Double interval)
			throws IOException {

		LOGGER.info("Compare Raster mnt");

		String mnt1Path = getTiffPath(codeSite, initDate);
		String mnt2Path = getTiffPath(codeSite, dateToCompare);

		File file1 = new File(mnt1Path);
		File file2 = new File(mnt2Path);
		LOGGER.debug("File 1 : " + mnt1Path);
		LOGGER.debug("File 2 : " + mnt2Path);

		AbstractGridFormat format1 = GridFormatFinder.findFormat(file1);
		AbstractGridFormat format2 = GridFormatFinder.findFormat(file2);

		GridCoverage2DReader reader1 = format1.getReader(file1);
		GridCoverage2DReader reader2 = format2.getReader(file2);

		GridCoverage2D coverage1 = reader1.read(null);
		GridCoverage2D coverage2 = reader2.read(null);

		double[] resolutionLevel1 = reader1.getResolutionLevels()[0];

		// create intersect beetween enveloppe to keep comparable coordinate
		GridCoverage2D cropCover = cropCovers(coverage1, coverage2);
		if (interval == null || interval <= 0) {
			interval = resolutionLevel1[0];
		}
		DefaultFeatureCollection gridToPoints = diffByPixel(coverage1, coverage2, cropCover, interval);

		// vectorize
		RenderedImage image = coverage2.getRenderedImage();
		Dimension dimension = new Dimension(image.getWidth(), image.getHeight());
		GridCoverage2D vectorized = VectorToRasterProcess.process(gridToPoints, "elevationDiff", dimension,
				coverage2.getEnvelope(),
				"vectorToRaster", null);

		GridCoverageFactory gcf = new GridCoverageFactory();
		GridCoverage2D gc = gcf.create("name", vectorized.getRenderedImage(), vectorized.getEnvelope2D());
		return gc;
	}

	/**
	 * @param codeSite      reference
	 * @param initDate
	 * @param dateToCompare
	 * @return FeatureCollection
	 * @throws IOException
	 */
	@DescribeProcess(title = "Compare Raster mnt and get JSON", description = "give diff raster mnt as JSON")
	@DescribeResult(name = "rasterResultAsJSON", description = "Raster with comparaison band")
	public static FeatureCollection<SimpleFeatureType, SimpleFeature> compareRasterMNT(
			@DescribeParameter(name = "codeSite", description = "id site on 6 char") final String codeSite,
			@DescribeParameter(name = "initDate", description = "first date") final String initDate,
			@DescribeParameter(name = "dateToCompare", description = "second date to compare") final String dateToCompare,
			@DescribeParameter(name = "evaluationInterval", description = "in meter beetween to point") Double interval)
			throws IOException {

		LOGGER.info("Compare Raster mnt");

		String mnt1Path = getTiffPath(codeSite, initDate);
		String mnt2Path = getTiffPath(codeSite, dateToCompare);

		File file1 = new File(mnt1Path);
		File file2 = new File(mnt2Path);
		LOGGER.debug("File 1 : " + mnt1Path);
		LOGGER.debug("File 2 : " + mnt2Path);

		AbstractGridFormat format1 = GridFormatFinder.findFormat(file1);
		AbstractGridFormat format2 = GridFormatFinder.findFormat(file2);

		GridCoverage2DReader reader1 = format1.getReader(file1);
		GridCoverage2DReader reader2 = format2.getReader(file2);

		GridCoverage2D coverage1 = reader1.read(null);
		GridCoverage2D coverage2 = reader2.read(null);

		// create intersect beetween enveloppe to keep comparable coordinate
		GridCoverage2D cropCover = cropCovers(coverage1, coverage2);
		double[] resolutionLevel1 = reader1.getResolutionLevels()[0];
		if (interval == null || interval <= 0) {
			interval = resolutionLevel1[0];
		}
		DefaultFeatureCollection gridToPoints = diffByPixel(coverage1, coverage2, cropCover, interval);
		return gridToPoints;
	}

	/**
	 * Get Path file name using parameter
	 * 
	 * @param codeSite
	 * @param date
	 * @return String PATH/codeSite_date.tiff
	 */
	public static String getTiffPath(String codeSite, String date) {

		StringBuffer sbFileName = new StringBuffer(INPUT_TIFF_PATH);
		sbFileName.append(codeSite);
		sbFileName.append("_");
		sbFileName.append(date);
		sbFileName.append(".tiff");

		return sbFileName.toString();
	}

	public static String getTiffOutputPath(String codeSite, String dateStart, String dateEnd) {

		StringBuffer sbFileName = new StringBuffer(OUTPUT_COMPARE_PATH);
		sbFileName.append(codeSite);
		sbFileName.append("_");
		sbFileName.append("compare");
		sbFileName.append("_");
		sbFileName.append(dateStart);
		sbFileName.append("_");
		sbFileName.append(dateEnd);
		sbFileName.append(".tiff");

		return sbFileName.toString();
	}

	public static GridCoverage2D cropCovers(GridCoverage2D cov1, GridCoverage2D cov2) {
		CoverageProcessor processor = CoverageProcessor.getInstance();

		// An example of manually creating the operation and parameters we want
		final ParameterValueGroup param = processor.getOperation("CoverageCrop").getParameters();
		param.parameter("Source").setValue(cov1);
		param.parameter("Envelope").setValue(cov2.getEnvelope());

		return (GridCoverage2D) processor.doOperation(param);
	}

	/**
	 * 
	 * @param coverage1
	 * @param coverage2
	 * @param rectangle
	 * @param interval  space between two points in meter
	 * @return
	 */
	private static DefaultFeatureCollection diffByPixel(GridCoverage2D coverage1, GridCoverage2D coverage2,
			GridCoverage2D rectangle, double interval) {

		DefaultFeatureCollection resultFeatureCollection = null;

		LOGGER.info("Compare mnt");

		// Init simple feature to add in collection
		SimpleFeatureTypeBuilder simpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder();
		simpleFeatureTypeBuilder.setName("featureType");
		simpleFeatureTypeBuilder.add("geometry", Point.class);
		simpleFeatureTypeBuilder.add("elevationDiff", Float.class);

		// init DefaultFeatureCollection
		SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(
				simpleFeatureTypeBuilder.buildFeatureType());
		resultFeatureCollection = new DefaultFeatureCollection(null, simpleFeatureBuilder.getFeatureType());

		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 2154);

		int nbPointResult = 0;
		int nbPointRead = 0;

		Envelope2D covTarget = rectangle.getEnvelope2D();

		// from Xmin to Xmax
		for (double x = covTarget.getMinX(); x < (covTarget.getMaxX() - interval); x = x + interval) {

			// from Ymin to Ymax
			for (double y = covTarget.getMinY(); y < (covTarget.getMaxY() - interval); y = y + interval) {
				// compare elevation on both image
				try {
					DirectPosition position = new DirectPosition2D(rectangle.getCoordinateReferenceSystem2D(), x, y);

					double[] elevation1 = (double[]) coverage1.evaluate(position);
					double[] elevation2 = (double[]) coverage2.evaluate(position);

					// getElevation value on Band 0 and remove NoData values
					float elevationDiff = (float) elevation1[0] - (float) elevation2[0];
					Coordinate coordinate = new Coordinate(x, y, elevationDiff);
					if (elevation1[0] != -100 && elevation2[0] != -100) {
						// double elevationDiff=elevation1[0]-elevation2[0];
						// Coordinate coordinate = new Coordinate(x, y, elevationDiff);
						Point point = geometryFactory.createPoint(coordinate);
						simpleFeatureBuilder.add(point);
						simpleFeatureBuilder.add(elevationDiff);
						SimpleFeature feature = simpleFeatureBuilder.buildFeature(Integer.toString(nbPointResult));
						resultFeatureCollection.add(feature);
						// hm.put(x +","+ y, )
						nbPointResult++;
					}
				} catch (PointOutsideCoverageException e) {
					// Normal case in just on limit rectangle
				}
				nbPointRead++;
			}
		}
		LOGGER.info("Nb Points with diff : " + nbPointResult + " on " + nbPointRead + " read");

		return resultFeatureCollection;
	}

}
