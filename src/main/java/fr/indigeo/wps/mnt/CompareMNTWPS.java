package fr.indigeo.wps.mnt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.factory.StaticMethodsProcessFactory;
import org.geotools.text.Text;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * @author Pierre Jego https://jdev.fr
 *
 */
@DescribeProcess(title = "Compare eleveation beetween two MNTS", description = "WPS tracking MNT differences")
public class CompareMNTWPS extends StaticMethodsProcessFactory<CompareMNTWPS> implements GeoServerProcess {

	private static final Logger LOGGER = Logger.getLogger(CompareMNTWPS.class);

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
		SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(simpleFeatureTypeBuilder.buildFeatureType());
		resultFeatureCollection = new DefaultFeatureCollection(null, simpleFeatureBuilder.getFeatureType());
			
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 2154);

		//For each point of MNT1 check nearest point on mnt2 and get elevation diff	
		FeatureIterator<SimpleFeature> iteratorMNT1 = mnt1.features();
			
		//check if the FeatureCollection contains features
		int id = 0;
		while(iteratorMNT1.hasNext()){

			SimpleFeature featureMNT1 = iteratorMNT1.next();
			Geometry PointMNT1 = (Geometry) featureMNT1.getDefaultGeometry();
				
			LOGGER.debug("Geometry type : "+ PointMNT1.getClass());

			if (PointMNT1 instanceof Point){
				final Point ptMNT1 = (Point) PointMNT1;
				final double pt1X = ptMNT1.getCoordinate().getX();
				final double pt1Y = ptMNT1.getCoordinate().getY();
				final double pt1Z = ptMNT1.getCoordinate().getZ();
				LOGGER.debug("Point référence X : "+ ptMNT1.getCoordinate().getX() + " Y : " + ptMNT1.getCoordinate().getY() + " Z : " +ptMNT1.getCoordinate().getZ());
			
				//For each point of MNT1 check nearest point on mnt2 and get elevation diff	
				FeatureIterator<SimpleFeature> iteratorMNT2 = mnt2.features();
				Boolean pointFound = false;

				double referenceX = -1;
				double referenceY = -1;
				double elevationDiff = 0;
				
				while(iteratorMNT2.hasNext() && !pointFound){
					SimpleFeature featureMNT2 = iteratorMNT2.next();
					Geometry PointMNT2 = (Geometry) featureMNT2.getDefaultGeometry();

					if (PointMNT1 instanceof Point){
						final Point ptMNT2 = (Point) PointMNT2;
						final double pt2X = ptMNT2.getCoordinate().getX();
						final double pt2Y = ptMNT2.getCoordinate().getY();
												
						final double xDiff = Math.abs(pt1X-pt2X);
						final double yDiff = Math.abs(pt1Y-pt2Y);
						// If Nearest X and Nearest Y store Z
						if(referenceX == -1 || (xDiff<= referenceX && yDiff <= referenceY)){
							LOGGER.debug("Nearest point for the moment X,Y : " + pt2X + "," + pt2Y);
							referenceX = Math.abs(xDiff);
							referenceY = Math.abs(yDiff);
							final double pt2Z = ptMNT2.getCoordinate().getZ();
							elevationDiff = pt1Z - pt2Z;
						};
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
	 * @param codeSite reference 
	 * @param initDate
	 * @param dateToCompare
	 * @return Tiff
	 * @throws IOException
	 */
	@DescribeProcess(title = "Compare Raster mnt", description = "give diff raster mnt")
	@DescribeResult(name = "rasterResult", description = "Raster with comparaison band")
	public static FeatureCollection<SimpleFeatureType, SimpleFeature> compareRasterMNT(
			@DescribeParameter(name = "codeSite", description = "id site on 6 char") final String codeSite,
			@DescribeParameter(name = "initDate", description = "first date") final String initDate,
			@DescribeParameter(name = "dateToCompare", description = "second date to compare") final String dateToCompare) throws IOException {
		
		DefaultFeatureCollection resultFeatureCollection = null;

		LOGGER.info("Compare Raster mnt");

		// Init simple feature to add in collection
		SimpleFeatureTypeBuilder simpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder();
		simpleFeatureTypeBuilder.setName("featureType");
		simpleFeatureTypeBuilder.add("geometry", Point.class);
		simpleFeatureTypeBuilder.add("elevationDiff", Double.class);

		// init DefaultFeatureCollection
		SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(simpleFeatureTypeBuilder.buildFeatureType());
		resultFeatureCollection = new DefaultFeatureCollection(null, simpleFeatureBuilder.getFeatureType());
			
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 2154);
		//final String LAYERNAME = "mnt";
		
		// get value from geoserver workspace
		//GeoServer gs = GeoServerExtensions.bean(GeoServerImpl.class);
		
		// How to get specific image in imagemosaic layer how to set CQL filter on Location and date
		// via coverage normally but doesnot work
		
		//LayerInfo info = gs.getCatalog().getLayerByName(LAYERNAME);
		//if( info == null ) {
		//	throw new WPSException("Layer not found in catalog : "+LAYERNAME);
		//}
		//if( !info.getType().equals(PublishedType.RASTER) ) {
		//	throw new WPSException("Layer found in catalog but not a raster one : "+LAYERNAME);
		//}
		//	MapLayerInfo mapInfo = new MapLayerInfo(info);
		
		// get tiff from datadir

		String mnt1Path = getTiffPath(codeSite, initDate);
		String mnt2Path = getTiffPath(codeSite, dateToCompare);
		
		// get band from raster 1
		Dataset dataset1 = gdal.Open(mnt1Path, gdalconst.GA_ReadOnly);
		Band band1 = dataset1.GetRasterBand(1);

		// get band from raster 2
		Dataset dataset2 = gdal.Open(mnt1Path, gdalconst.GA_ReadOnly);
		Band band2 = dataset2.GetRasterBand(1);
		
		LOGGER.info(dataset1.getRasterXSize());
		LOGGER.info(dataset1.getRasterYSize());
		
		// get maxExtend
	
		//band1.ReadRaster(x, y, 1, 1, result);

		return resultFeatureCollection;
	}

	private static String getTiffPath(String codeSite, String date) throws IOException {
        
		InputStream input = CompareMNTWPS.class.getClassLoader().getResourceAsStream("config.properties");
        Properties prop = new Properties();      
       	if (input == null) {
            LOGGER.error("Unable to find config.properties");
        }
        //load a properties file from class path, inside static method
        prop.load(input);
        final String TIFF_FOLDER = prop.getProperty("tiff.folder");
        if (TIFF_FOLDER == null) {
            LOGGER.error("Unable to find folder in configuration");
        }
		
        StringBuffer sbFileName = new StringBuffer(TIFF_FOLDER);
        sbFileName.append(codeSite);
        sbFileName.append("_");
        sbFileName.append(date);
        sbFileName.append(".tiff");

        return sbFileName.toString();
    }
}
